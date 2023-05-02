package com.google.ads.mediation.unity;

import android.app.Activity;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

/** A factory to create UnityAds {@link BannerView} for Banner Ads */
class UnityBannerViewFactory {
  UnityBannerViewWrapper createBannerView(
      Activity activity, String placementId, UnityBannerSize bannerSize) {
    BannerView bannerView = new BannerView(activity, placementId, bannerSize);
    return new UnityBannerViewWrapper(bannerView);
  }
}
