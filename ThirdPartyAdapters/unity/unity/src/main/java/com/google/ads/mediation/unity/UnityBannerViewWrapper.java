package com.google.ads.mediation.unity;

import androidx.annotation.NonNull;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.services.banners.BannerView;

/**
 * Wrapper class for an instance {@link BannerView} created by {@link UnityBannerViewFactory}. It is
 * used as a layer between the Adapter's and the UnityAds SDK to facilitate unit testing.
 */
class UnityBannerViewWrapper {

  private final BannerView bannerView;

  UnityBannerViewWrapper(@NonNull BannerView bannerView) {
    this.bannerView = bannerView;
  }

  public void setListener(BannerView.IListener listener) {
    bannerView.setListener(listener);
  }

  public void load(UnityAdsLoadOptions loadOptions) {
    bannerView.load(loadOptions);
  }

  public BannerView getBannerView() {
    return bannerView;
  }
}
