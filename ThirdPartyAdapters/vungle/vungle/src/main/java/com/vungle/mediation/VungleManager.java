package com.vungle.mediation;

import android.os.Bundle;
import android.util.Log;

import com.vungle.warren.AdConfig;
import com.vungle.warren.AdConfig.AdSize;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleNativeAd;
import com.vungle.warren.error.VungleException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A helper class to load and show Vungle ads and keep track of multiple
 * {@link VungleInterstitialAdapter} instances.
 */
public class VungleManager {

    private static final String TAG = VungleManager.class.getSimpleName();
    private static final String PLAYING_PLACEMENT = "placementID";

    private static VungleManager sInstance;

    static class BannerRequest {
        private String placementId;
        private String uniquePubRequestId;

        BannerRequest(String placementId, String uniquePubRequestId) {
            this.placementId = placementId;
            this.uniquePubRequestId = uniquePubRequestId;
        }

        String getPlacementId() {
            return placementId;
        }

        String getUniquePubRequestId() {
            return uniquePubRequestId;
        }
    }

    private ConcurrentHashMap<BannerRequest, VungleBanner> activeBannerAds;
    private ConcurrentHashMap<BannerRequest, VungleNativeAd> activeNativeAds;
    private Set<String> mProgressPlacements;

    public static synchronized VungleManager getInstance() {
        if (sInstance == null) {
            sInstance = new VungleManager();
        }
        return sInstance;
    }

    private VungleManager() {
        activeBannerAds = new ConcurrentHashMap<>();
        activeNativeAds = new ConcurrentHashMap<>();
        mProgressPlacements = new CopyOnWriteArraySet<>();
    }

    void removeInProgressPlacement(String placementId) {
        mProgressPlacements.remove(placementId);
    }

    synchronized boolean isPlacementInProgressAndSet(String placementId) {
        if (mProgressPlacements.contains(placementId)) {
            return true;
        } else {
            mProgressPlacements.add(placementId);
            return false;
        }
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
        if (placement == null) {
            Log.e(TAG, "placementID not provided from serverParameters.");
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
            public void onError(String placement, VungleException cause) {
                if (listener != null) {
                    listener.onAdFailedToLoad();
                }
            }
        });
    }

    void loadAd(@NonNull String placement, @NonNull AdConfig.AdSize adSize,
                @Nullable final VungleListener listener) {
        if (AdSize.isBannerAdSize(adSize)) {
            Banners.loadBanner(placement, adSize, new LoadAdCallback() {
                @Override
                public void onAdLoad(String placement) {
                    if (listener != null) {
                        listener.onAdAvailable();
                    }
                }

                @Override
                public void onError(String placement, VungleException exception) {
                    if (listener != null) {
                        listener.onAdFailedToLoad();
                    }
                }
            });
        } else {
            loadAd(placement, listener);
        }
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
            public void onError(String id, VungleException error) {
                if (listener != null) {
                    listener.onAdFail(id);
                }
            }
        };
    }

    boolean isAdPlayable(String placement) {
        return (placement != null && !placement.isEmpty()) &&
                Vungle.canPlayAd(placement);
    }

    boolean isAdPlayable(@NonNull String placement, @NonNull AdSize adSize) {
        if (AdSize.isBannerAdSize(adSize)) {
            return Banners.canPlayAd(placement, adSize);
        } else {
            return isAdPlayable(placement);
        }
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

    BannerRequest activeBannerRequest(String placementId) {
        for (BannerRequest bannerRequest : activeBannerAds.keySet()) {
            if(placementId.equals(bannerRequest.getPlacementId())) {
                return bannerRequest;
            }
        }
        return null;
    }

    BannerRequest activeMRECRequest(String placementId) {
        for (BannerRequest bannerRequest : activeNativeAds.keySet()) {
            if(placementId.equals(bannerRequest.getPlacementId())) {
                return bannerRequest;
            }
        }
        return null;
    }

    VungleBanner getVungleBanner(BannerRequest bannerRequest, AdConfig.AdSize adSize, VungleListener vungleListener) {
        Log.d(TAG, "getVungleBanner");
        VungleBanner bannerAd = Banners.getBanner(bannerRequest.getPlacementId(), adSize, playAdCallback(vungleListener));
        if (bannerAd != null) {
            activeBannerAds.put(bannerRequest, bannerAd);
        }
        return bannerAd;
    }

    VungleNativeAd getVungleNativeAd(BannerRequest bannerRequest, AdConfig adConfig,
                                     VungleListener vungleListener) {
        Log.d(TAG, "getVungleNativeAd");
        VungleNativeAd bannerAd = Vungle.getNativeAd(bannerRequest.getPlacementId(), adConfig,
                playAdCallback(vungleListener));
        if (bannerAd != null) {
            activeNativeAds.put(bannerRequest, bannerAd);
        }

        return bannerAd;
    }

    void cleanUpBanner(@Nullable BannerRequest bannerRequest, @Nullable VungleNativeAd bannerAd) {
        VungleNativeAd activeBannerAd = activeNativeAds.get(bannerRequest);
        if (activeBannerAd != null && activeBannerAd == bannerAd) {
            Log.d(TAG, "cleanUpBanner # finishDisplayingAd");
            activeBannerAd.finishDisplayingAd();
            activeNativeAds.remove(bannerRequest);
        }
    }

    void cleanUpBanner(@Nullable BannerRequest bannerRequest, @Nullable VungleBanner bannerAd) {
        VungleBanner activeBannerAd = activeBannerAds.get(bannerRequest);
        if (activeBannerAd != null && activeBannerAd == bannerAd) {
            Log.d(TAG, "cleanUpBanner # destroyAd");
            activeBannerAd.destroyAd();
            activeBannerAds.remove(bannerRequest);
        }
    }

    /**
     * called from adapters to clean, and remove
     *
     * @param bannerRequest
     */
    void cleanUpBanner(@NonNull BannerRequest bannerRequest) {
        Log.d(TAG, "cleanUpBanner");
        VungleBanner activeBannerAd = activeBannerAds.get(bannerRequest);
        if (activeBannerAd != null) {
            cleanUpBanner(bannerRequest, activeBannerAd);
        } else {
            VungleNativeAd vungleNativeAd = activeNativeAds.get(bannerRequest);
            if (vungleNativeAd != null) {
                cleanUpBanner(bannerRequest, vungleNativeAd);
            }
        }
    }
}
