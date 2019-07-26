package com.vungle.mediation;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleNativeAd;
import com.vungle.warren.error.VungleException;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class to load and show Vungle ads and keep track of multiple
 * {@link VungleInterstitialAdapter} instances.
 */
public class VungleManager {

    private static final String TAG = VungleManager.class.getSimpleName();
    private static final String PLAYING_PLACEMENT = "placementID";

    private static VungleManager sInstance;

    private ConcurrentHashMap<String, Pair<String, VungleNativeAd>> activeBannerAds;

    public static synchronized VungleManager getInstance() {
        if (sInstance == null) {
            sInstance = new VungleManager();
        }
        return sInstance;
    }

    private VungleManager() {
        activeBannerAds = new ConcurrentHashMap<>();
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
                        + "Used one from serverParameters.");
            }
            placement = serverParameters.getString(PLAYING_PLACEMENT);
        }
        if (placement == null) {
            Log.e(TAG, "placementID not provided from serverParameters. Load and play functionality will not work.");
        }
        return placement;
    }

    void loadAd(String placement, @Nullable final VungleListener listener) {
        Vungle.loadAd(placement, new LoadAdCallback() {
            @Override
            public void onAdLoad(String placement) {
                if (listener != null) {
                    listener.onAdAvailable();
                }
            }

            @Override
            public void onError(String placement, Throwable cause) {
                if (listener != null) {
                    listener.onAdFailedToLoad();
                }
            }
        });
    }

    void playAd(String placement, AdConfig cfg, @Nullable VungleListener listener) {
        Vungle.playAd(placement, cfg, playAdCallback(listener));
    }

    private PlayAdCallback playAdCallback(@Nullable final VungleListener listener) {
        return new PlayAdCallback() {
            @Override
            public void onAdStart(String id) {
                if (listener != null) {
                    listener.onAdStart(id);
                }
            }

            @Override
            public void onAdEnd(String id, boolean completed, boolean isCTAClicked) {
                if (listener != null) {
                    listener.onAdEnd(id, completed, isCTAClicked);
                }
            }

            @Override
            public void onError(String id, Throwable error) {
                if (listener != null) {
                    listener.onAdFail(id);
                }
            }
        };
    }

    void removeListeners(String adapterId) {
    }

    boolean isAdPlayable(String placement) {
        return (placement != null && !placement.isEmpty()) &&
                Vungle.canPlayAd(placement);
    }

    /**
     * Checks and returns if the passed Placement ID is a valid placement for App ID
     *
     * @param placementId
     * @return
     */
    boolean isValidPlacement(String placementId) {
        return Vungle.isInitialized() &&
                Vungle.getValidPlacements().contains(placementId);
    }

    VungleNativeAd getVungleNativeAd(String adapterId, String placement, AdConfig adConfig, VungleListener vungleListener) {
        Log.d(TAG, "getVungleNativeAd");
        //Since we VungleInterstitialAdapter#onDestroy() does not called by AdMob SDK,
        // we have to take care of removal of listener
        cleanUpBanner(placement);
        //Fetch new ad

        VungleNativeAd bannerAd = Vungle.getNativeAd(placement, adConfig, playAdCallback(vungleListener));
        if (bannerAd != null) {
            activeBannerAds.put(placement, new Pair<>(adapterId, bannerAd));
        }

        return bannerAd;
    }

    void removeActiveBanner(String placementId, String adapterId) {
        if (placementId == null)
            return;
        Pair<String, VungleNativeAd> pair = activeBannerAds.get(placementId);
        if (pair != null && adapterId != null && adapterId.equals(pair.first)) {
            activeBannerAds.remove(placementId, pair);
        }
    }

    /**
     * called from adapters to clean, and remove
     *
     * @param placementId
     */
    void cleanUpBanner(String placementId) {
        Log.d(TAG, "cleanUpBanner");
        Pair<String, VungleNativeAd> pair = activeBannerAds.get(placementId);
        if (pair != null) {
            String adapterId = pair.first;
            removeListeners(adapterId);
            //Remove ad
            VungleNativeAd vungleNativeAd = pair.second;
            if (vungleNativeAd != null) {
                //We should do Report ad
                Log.d(TAG, "cleanUpBanner # finishDisplayingAd");
                vungleNativeAd.finishDisplayingAd();
                removeActiveBanner(placementId, adapterId);
            }
        }
    }
}
