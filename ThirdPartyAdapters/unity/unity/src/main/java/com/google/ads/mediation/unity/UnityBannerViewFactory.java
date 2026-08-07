package com.google.ads.mediation.unity;

import android.content.Context;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

/** A factory to create UnityAds {@link BannerView} for Banner Ads */
class UnityBannerViewFactory {
  UnityBannerViewWrapper createBannerView(
      Context context, String placementId, UnityBannerSize bannerSize) {
    BannerView bannerView = new BannerView(context, placementId, bannerSize);
    return new UnityBannerViewWrapper(bannerView);
  }
}
