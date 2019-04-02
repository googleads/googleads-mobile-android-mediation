package com.vungle.mediation;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vungle.warren.AdConfig;
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
public class VungleManager {

    private static final String TAG = VungleManager.class.getSimpleName();
    private static final String PLAYING_PLACEMENT = "placementID";

    private static VungleManager sInstance;
    private String mCurrentPlayId = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Map<String, VungleListener> mListeners;

    public static VungleManager getInstance() {
        if (sInstance == null) {
            sInstance = new VungleManager();
        }
        return sInstance;
    }

    private VungleManager() {
        mListeners = new HashMap<>();

        VungleApiClient.addWrapperInfo(VungleApiClient.WrapperFramework.admob,
                com.vungle.warren.BuildConfig.VERSION_NAME.replace('.', '_'));
    }

    @Nullable
    public String findPlacement(Bundle networkExtras, Bundle serverParameters) {
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
        return placement;
    }

    void removeListener(String id) {
        mListeners.remove(id);
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

    boolean isValidPlacement(String placementId) {
        return Vungle.isInitialized() &&
                Vungle.getValidPlacements().contains(placementId);
    }

}
