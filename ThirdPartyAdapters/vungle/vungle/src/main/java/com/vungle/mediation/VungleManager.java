package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.vungle.publisher.AdConfig;
import com.vungle.publisher.VungleAdEventListener;
import com.vungle.publisher.VungleInitListener;
import com.vungle.publisher.VunglePub;
import com.vungle.publisher.env.WrapperFramework;
import com.vungle.publisher.inject.Injector;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to load and show Vungle ads and keep track of multiple
 * {@link VungleInterstitialAdapter} instances.
 */
class VungleManager implements VungleAdEventListener {

    private static final String TAG = VungleManager.class.getSimpleName();
    private static final String PLAYING_PLACEMENT = "placementID";
    private static final String VERSION = "5.3.2";

    private static VungleManager sInstance;
    private VunglePub mVunglePub;
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

        Injector injector = Injector.getInstance();
        injector.setWrapperFramework(WrapperFramework.admob);
        injector.setWrapperFrameworkVersion(VERSION.replace('.', '_'));

        this.mAppId = appId;
        this.mPlacements = placements;

        mVunglePub = VunglePub.getInstance();
    }

    boolean isInitialized() {
        return mVunglePub.isInitialized();
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
        if (mVunglePub.isInitialized()) {
            for (VungleListener cb : mListeners.values()) {
                if (cb.isWaitingInit()) {
                    cb.setWaitingInit(false);
                    cb.onInitialized(mVunglePub.isInitialized());
                }
            }
            return;
        }
        if (mIsInitialising) {
            return;
        }
        mIsInitialising = true;

        mVunglePub.init(context, mAppId, mPlacements, new VungleInitListener() {
            @Override
            public void onSuccess() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIsInitialising = false;
                        mVunglePub.clearAndSetEventListeners(VungleManager.this);
                        for (VungleListener cb : mListeners.values()) {
                            if (cb.isWaitingInit()) {
                                cb.setWaitingInit(false);
                                cb.onInitialized(mVunglePub.isInitialized());
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIsInitialising = false;
                        for (VungleListener cb : mListeners.values()) {
                            if (cb.isWaitingInit()) {
                                cb.setWaitingInit(false);
                                cb.onInitialized(mVunglePub.isInitialized());
                            }
                        }
                    }
                });
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
        mVunglePub.playAd(placement, cfg);
    }

    void onPause() {
        mVunglePub.onPause();
    }

    void onResume() {
        mVunglePub.onResume();
    }

    boolean isAdPlayable(String placement) {
        return mVunglePub.isAdPlayable(placement);
    }

    void loadAd(String placement) {
        if (mVunglePub.isAdPlayable(placement)) {
            notifyAdIsReady(placement);
            return;
        }
        mVunglePub.loadAd(placement);
    }

    private void notifyAdIsReady(String placement) {
        for (VungleListener cb : mListeners.values()) {
            try {
                if (cb.getWaitingForPlacement() != null
                        && cb.getWaitingForPlacement().equals(placement)) {
                    cb.onAdAvailable();
                    cb.waitForAd(null);
                }
            } catch (Exception exception) {
                Log.w(TAG, exception);
            }
        }
    }

    //region VungleAdEventListener implementation.
    @Override
    public void onAdEnd(final @NonNull String placement,
                        final boolean wasSuccessfulView,
                        final boolean wasCallToActionClicked) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, VungleListener> entry : mListeners.entrySet()) {
                    try {
                        if (mCurrentPlayId == null || mCurrentPlayId.equals(entry.getKey())) {
                            entry.getValue()
                                    .onAdEnd(placement, wasSuccessfulView, wasCallToActionClicked);
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
    public void onAdStart(final @NonNull String placement) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, VungleListener> entry : mListeners.entrySet()) {
                    try {
                        if (mCurrentPlayId == null || mCurrentPlayId.equals(entry.getKey())) {
                            entry.getValue().onAdStart(placement);
                        }
                    } catch (Exception exception) {
                        Log.w(TAG, exception);
                    }
                }
            }
        });
    }

    @Override
    public void onUnableToPlayAd(final @NonNull String placement, String reason) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, VungleListener> entry : mListeners.entrySet()) {
                    try {
                        if (mCurrentPlayId == null || mCurrentPlayId.equals(entry.getKey())) {
                            entry.getValue().onAdFail(placement);
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
    public void onAdAvailabilityUpdate(final @NonNull String placement, boolean isAdAvailable) {
        if (!isAdAvailable) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyAdIsReady(placement);
            }
        });
    }
    //endregion
}
