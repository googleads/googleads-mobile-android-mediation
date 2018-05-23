package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.vungle.warren.AdConfig;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.network.VungleApiClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to load and show Vungle ads and keep track of multiple
 * {@link VungleInterstitialAdapter} instances.
 */
class VungleManager {

    private static final String TAG = VungleManager.class.getSimpleName();
    private static final String PLAYING_PLACEMENT = "placementID";
    private static final String VERSION = "6.2.5";

    private static VungleManager sInstance;
    private String mCurrentPlayId = null;
    private boolean mIsInitialising = false;
    private String mAppId;
    private String[] mPlacements;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Map<String, VungleListener> mListeners;

    static VungleManager getInstance(String appId, String[] placements) {
        if (sInstance == null) {
            sInstance = new VungleManager(appId, placements);
        }
        return sInstance;
    }

    private VungleManager(String appId, String[] placements) {
        mListeners = new HashMap<>();

        VungleApiClient.addWrapperInfo(VungleApiClient.WrapperFramework.admob,
                VERSION.replace('.', '_'));

        this.mAppId = appId;
        this.mPlacements = placements;
    }

    boolean isInitialized() {
        return Vungle.isInitialized();
    }

    @SuppressWarnings("SameParameterValue")
    void setIncentivizedFields(String userID, String title, String body, String keepWatching,
                               String close) {
        Vungle.setIncentivizedFields(userID, title, body, keepWatching, close);
    }

    String findPlacement(Bundle networkExtras, Bundle serverParameters) {
        String placement = null;
        if (networkExtras != null
                && networkExtras.containsKey(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT)) {
            placement = networkExtras.getString(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT);
        }
        if (serverParameters != null && serverParameters.containsKey(PLAYING_PLACEMENT)) {
            if (placement != null) {
                Log.i(TAG, "'placementID' had a value in both serverParameters and networkExtras. "
                        + "Used one from serverParameters");
            }
            placement = serverParameters.getString(PLAYING_PLACEMENT);
        }
        if (placement == null) {
            placement = mPlacements[0];
            Log.i(TAG, String.format("'placementID' not specified. Used first from 'allPlacements'"
                    + ": %s", placement));
        }
        return placement;
    }

    void init(Context context) {
        if (Vungle.isInitialized()) {
            for (VungleListener cb : mListeners.values()) {
                if (cb.isWaitingInit()) {
                    cb.setWaitingInit(false);
                    cb.onInitialized(Vungle.isInitialized());
                }
            }
            return;
        }
        if (mIsInitialising) {
            return;
        }
        mIsInitialising = true;

        Vungle.init(Arrays.asList(mPlacements), mAppId, context.getApplicationContext(),
                new InitCallback() {
            @Override
            public void onSuccess() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIsInitialising = false;
                        Vungle.updateConsentStatus(VungleConsent.getCurrentVungleConsent());
                        for (VungleListener cb : mListeners.values()) {
                            if (cb.isWaitingInit()) {
                                cb.setWaitingInit(false);
                                cb.onInitialized(Vungle.isInitialized());
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIsInitialising = false;
                        for (VungleListener cb : mListeners.values()) {
                            if (cb.isWaitingInit()) {
                                cb.setWaitingInit(false);
                                cb.onInitialized(Vungle.isInitialized());
                            }
                        }
                    }
                });
            }

            @Override
            public void onAutoCacheAdAvailable(String placementId) {
                // not used
            }
        });
    }

    void removeListener(String id) {
        if (mListeners.containsKey(id)) {
            mListeners.remove(id);
        }
    }

    void addListener(String id, VungleListener listener) {
        removeListener(id);
        mListeners.put(id, listener);
    }

    void playAd(String placement, AdConfig cfg, String id) {
        if (mCurrentPlayId != null) {
            return;
        }
        mCurrentPlayId = id;
        Vungle.playAd(placement, cfg, new PlayAdCallback() {
            @Override
            public void onAdStart(final String id) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map.Entry<String, VungleListener> entry : mListeners.entrySet()) {
                            try {
                                if (mCurrentPlayId == null || mCurrentPlayId.equals(entry.getKey())) {
                                    entry.getValue().onAdStart(id);
                                }
                            } catch (Exception exception) {
                                Log.w(TAG, exception);
                            }
                        }
                    }
                });
            }

            @Override
            public void onAdEnd(final String id, final boolean completed, final boolean isCTAClicked) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map.Entry<String, VungleListener> entry : mListeners.entrySet()) {
                            try {
                                if (mCurrentPlayId == null || mCurrentPlayId.equals(entry.getKey())) {
                                    entry.getValue()
                                            .onAdEnd(id, completed, isCTAClicked);
                                }
                            } catch (Exception exception) {
                                Log.w(TAG, exception);
                            }
                        }
                        mCurrentPlayId = null;
                    }
                });
            }

            @Override
            public void onError(final String id, Throwable error) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map.Entry<String, VungleListener> entry : mListeners.entrySet()) {
                            try {
                                if (mCurrentPlayId == null || mCurrentPlayId.equals(entry.getKey())) {
                                    entry.getValue().onAdFail(id);
                                }
                            } catch (Exception exception) {
                                Log.w(TAG, exception);
                            }
                        }
                        mCurrentPlayId = null;
                    }
                });
            }
        });
    }

    boolean isAdPlayable(String placement) {
        return Vungle.canPlayAd(placement);
    }

    void loadAd(String placement) {
        if (Vungle.canPlayAd(placement)) {
            notifyAdIsReady(placement, true);
            return;
        }
        Vungle.loadAd(placement, new LoadAdCallback() {
            @Override
            public void onAdLoad(final String id) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyAdIsReady(id, true);
                    }
                });
            }

            @Override
            public void onError(final String id, Throwable cause) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyAdIsReady(id, false);
                    }
                });
            }
        });
    }

    private void notifyAdIsReady(String placement, boolean success) {
        for (VungleListener cb : mListeners.values()) {
            try {
                if (cb.getWaitingForPlacement() != null
                        && cb.getWaitingForPlacement().equals(placement)) {
                    if (success)
                        cb.onAdAvailable();
                    else
                        cb.onAdFailedToLoad();
                    cb.waitForAd(null);
                }
            } catch (Exception exception) {
                Log.w(TAG, exception);
            }
        }
    }
}
