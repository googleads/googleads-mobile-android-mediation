package com.jirbo.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.util.Log;
import androidx.annotation.NonNull;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationBannerListener;

/**
 * The {@link AdColonyBannerAdListener} class is used to forward Banner ad events from AdColony SDK
 * to Google Mobile Ads SDK.
 */
class AdColonyBannerAdListener extends AdColonyAdViewListener {

  /**
   * The MediationBannerListener used to report callbacks.
   */
  private MediationBannerListener mediationBannerListener;
  /**
   * The AdColony banner adapter.
   */
  private AdColonyAdapter adapter;

  AdColonyBannerAdListener(@NonNull AdColonyAdapter adapter,
      @NonNull MediationBannerListener listener) {
    this.mediationBannerListener = listener;
    this.adapter = adapter;
  }

  @Override
  public void onClicked(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdClicked(adapter);
    }
  }

  @Override
  public void onOpened(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdOpened(adapter);
    }
  }

  @Override
  public void onClosed(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdClosed(adapter);
    }
  }

  @Override
  public void onLeftApplication(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdLeftApplication(adapter);
    }
  }

  @Override
  public void onRequestFilled(AdColonyAdView adColonyAdView) {
    if (mediationBannerListener != null && adapter != null) {
      adapter.setAdView(adColonyAdView);
      mediationBannerListener.onAdLoaded(adapter);
    }
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    if (mediationBannerListener != null && adapter != null) {
      AdError error = createSdkError();
      Log.w(TAG, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(adapter, error);
    }
  }

  void destroy() {
    adapter = null;
    mediationBannerListener = null;
  }
}
