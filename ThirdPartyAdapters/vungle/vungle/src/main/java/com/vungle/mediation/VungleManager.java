package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vungle.warren.AdConfig;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.network.VungleApiClient;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to load and show Vungle ads and keep track of multiple
 * {@link VungleInterstitialAdapter} instances.
 */
class VungleManager {

    private static final String TAG = VungleManager.class.getSimpleName();
    private static final String PLAYING_PLACEMENT = "placementID";
    private static final String VERSION = "6.3.24";

    private static VungleManager sInstance;
    private String mCurrentPlayId = null;
    private boolean mIsInitialising = false;
    private String mAppId;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Map<String, VungleListener> mListeners;

    static VungleManager getInstance(String appId) {
        if (sInstance == null) {
            sInstance = new VungleManager(appId);
        }
        return sInstance;
    }

    private VungleManager(String appId) {
        mListeners = new HashMap<>();

        VungleApiClient.addWrapperInfo(VungleApiClient.WrapperFramework.admob,
                VERSION.replace('.', '_'));

        this.mAppId = appId;
    }

    boolean isInitialized() {
        return Vungle.isInitialized();
    }

    @SuppressWarnings("SameParameterValue")
    void setIncentivizedFields(String userID, String title, String body, String keepWatching,
                               String close) {
        Vungle.setIncentivizedFields(userID, title, body, keepWatching, close);
    }

    @Nullable
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
            Log.e(TAG, "placementID not provided from serverParameters. Please check your AdMob dashboard settings." +
                    "load and play functionality will not work");
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

        Vungle.init(mAppId, context.getApplicationContext(), new InitCallback() {
            @Override
            public void onSuccess() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIsInitialising = false;
                        if(VungleConsent.getCurrentVungleConsent() != null) {
                            Vungle.updateConsentStatus(VungleConsent.getCurrentVungleConsent(),
                                    VungleConsent.getCurrentVungleConsentMessageVersion());
                        }
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
        return (placement != null && !placement.isEmpty()) &&
                    Vungle.canPlayAd(placement);
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

    /**
     * Checks and returns if the passed Placement ID is a valid placement for App ID
     * @param placementId
     * @return
     */
    boolean isValidPlacement(String placementId) {
        return Vungle.isInitialized() &&
                Vungle.getValidPlacements().contains(placementId);
    }

}
