package com.google.ads.mediation.dap;

import android.os.Bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The {@link DuAdExtrasBundleBuilder} class is used to create a network extras bundle that
 * can be passed to the adapter to make network-specific customizations.
 */
public class DuAdExtrasBundleBuilder {
    private BannerCloseStyle bannerCloseStyle;
    private BannerStyle bannerStyle;
    private InterstitialAdType interstitialAdType;

    private ArrayList<Integer> placementIds;
    private ArrayList<Integer> videoPlacementIds;

    public DuAdExtrasBundleBuilder bannerCloseStyle(BannerCloseStyle bannerCloseStyle) {
        this.bannerCloseStyle = bannerCloseStyle;
        return DuAdExtrasBundleBuilder.this;
    }

    public DuAdExtrasBundleBuilder bannerStyle(BannerStyle bannerStyle) {
        this.bannerStyle = bannerStyle;
        return DuAdExtrasBundleBuilder.this;
    }

    /**
     * Add placement IDs for native/banner/interstitial ad
     * @param ids placement IDs for native/banner/interstitial ad
     * @return
     */
    public DuAdExtrasBundleBuilder addAllPlacementId(Integer... ids) {
        if (ids == null || ids.length <= 0) {
            return DuAdExtrasBundleBuilder.this;
        }
        if (placementIds == null) {
            placementIds = new ArrayList<>();
        }
        placementIds.addAll(Arrays.asList(ids));
        return DuAdExtrasBundleBuilder.this;
    }
    /**
     * Add placement IDs for rewarded video ad
     * @param ids placement IDs for rewarded video ad
     * @return
     */
    public DuAdExtrasBundleBuilder addAllVideoPlacementId(Integer... ids) {
        if (ids == null || ids.length <= 0) {
            return DuAdExtrasBundleBuilder.this;
        }
        if (videoPlacementIds == null) {
            videoPlacementIds = new ArrayList<>();
        }
        videoPlacementIds.addAll(Arrays.asList(ids));
        return DuAdExtrasBundleBuilder.this;
    }

    public DuAdExtrasBundleBuilder interstitialAdType(InterstitialAdType interstitialAdType) {
        this.interstitialAdType = interstitialAdType;
        return DuAdExtrasBundleBuilder.this;
    }

    public Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(DuAdAdapter.KEY_BANNER_CLOSE_STYLE, bannerCloseStyle);
        bundle.putSerializable(DuAdAdapter.KEY_BANNER_STYLE, bannerStyle);
        bundle.putSerializable(DuAdAdapter.KEY_INTERSTITIAL_TYPE, interstitialAdType);

        if (placementIds != null) {
            bundle.putIntegerArrayList(DuAdMediation.KEY_ALL_PLACEMENT_ID, placementIds);
        }
        if (videoPlacementIds != null) {
            bundle.putIntegerArrayList(
                    DuAdMediation.KEY_ALL_VIDEO_PLACEMENT_ID, videoPlacementIds);
        }

        return bundle;
    }

    public enum BannerCloseStyle implements Serializable {
        STYLE_TOP,
        STYLE_BOTTOM
    }

    public enum BannerStyle implements Serializable {
        STYLE_BLUE,
        STYLE_GREEN
    }

    public enum InterstitialAdType implements Serializable {
        NORMAL,
        SCREEN
    }
}
