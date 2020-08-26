package com.google.ads.mediation.nend;

import android.os.Bundle;

/**
 * The {@link NendExtrasBundleBuilder} class builds a {@link Bundle} containing mediation extras for
 * the nend ad network. The bundles built by this class should be used with {@link
 * com.google.android.gms.ads.AdRequest.Builder#addNetworkExtrasBundle(Class, Bundle)}.
 */
public class NendExtrasBundleBuilder {

  /*
   * The nend User ID.
   */
  private String userId;

  /*
   * Type of interstitial ad to be loaded.
   */
  private NendAdapter.InterstitialType interstitialType;

  /*
   * Type of native ad to be loaded.
   */
  private NendMediationAdapter.FormatType nativeAdsType;

  public NendExtrasBundleBuilder setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public NendExtrasBundleBuilder setInterstitialType(
      NendAdapter.InterstitialType interstitialType) {
    this.interstitialType = interstitialType;
    return this;
  }

  public NendExtrasBundleBuilder setNativeAdsType(NendMediationAdapter.FormatType nativeAdsType) {
    this.nativeAdsType = nativeAdsType;
    return this;
  }

  public Bundle build() {
    Bundle bundle = new Bundle();
    bundle.putString(NendAdapter.KEY_USER_ID, userId);
    bundle.putSerializable(NendAdapter.KEY_INTERSTITIAL_TYPE, interstitialType);
    bundle.putSerializable(NendNativeAdForwarder.KEY_NATIVE_ADS_FORMAT_TYPE, nativeAdsType);
    return bundle;
  }
}
