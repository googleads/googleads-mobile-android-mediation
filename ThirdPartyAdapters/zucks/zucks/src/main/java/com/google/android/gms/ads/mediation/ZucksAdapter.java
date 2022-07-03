package com.google.android.gms.ads.mediation;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import com.google.android.gms.ads.AdSize;

public class ZucksAdapter implements MediationBannerAdapter, MediationInterstitialAdapter {

  // region Lifecycle methods
  // TODO: Fill with any required lifecycle events.
  @Override
  public void onPause() {

  }

  @Override
  public void onResume() {

  }

  @Override
  public void onDestroy() {

  }
  // endregion

  // region MediationBannerAdapter methods
  @Override
  public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener,
      Bundle serverParameters, AdSize adSize, MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    // TODO: Load banner ad and forward the success callback:
    mediationBannerListener.onAdLoaded(ZucksAdapter.this);
  }

  @Override
  public View getBannerView() {
    // TODO: Return the Zucks banner ad View object.
    return null;
  }
  // endregion

  // region MediationInterstitialAdapter methods
  @Override
  public void requestInterstitialAd(Context context,
      MediationInterstitialListener mediationInterstitialListener, Bundle serverParameters,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
    // TODO: Load interstitial ad and forward the success callback:
    mediationInterstitialListener.onAdLoaded(ZucksAdapter.this);
  }

  @Override
  public void showInterstitial() {
    // TODO: Show interstitial ad.
  }
  // endregion
}
