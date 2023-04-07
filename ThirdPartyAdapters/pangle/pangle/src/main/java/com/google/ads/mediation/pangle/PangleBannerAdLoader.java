package com.google.ads.mediation.pangle;

import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest;

/** Loads Pangle banner ad. */
public class PangleBannerAdLoader {

  public void loadAd(
      String placementId, PAGBannerRequest request, PAGBannerAdLoadListener listener) {
    PAGBannerAd.loadAd(placementId, request, listener);
  }
}
