package com.jirbo.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.util.Log;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

/**
 * The {@link AdColonyAdListener} class is used to forward Interstitial ad events from AdColony SDK
 * to Google Mobile Ads SDK.
 */
class AdColonyAdListener extends AdColonyInterstitialListener {

  private MediationInterstitialListener mediationInterstitialListener;
  private AdColonyAdapter adapter;

  AdColonyAdListener(@NonNull AdColonyAdapter adapter, @NonNull MediationInterstitialListener listener) {
    this.mediationInterstitialListener = listener;
    this.adapter = adapter;
  }

  @Override
  public void onRequestFilled(AdColonyInterstitial ad) {
    if (adapter != null && mediationInterstitialListener != null) {
      adapter.setAd(ad);
      mediationInterstitialListener.onAdLoaded(adapter);
    }
  }

  @Override
  public void onClicked(AdColonyInterstitial ad) {
    if (adapter != null && mediationInterstitialListener != null) {
      adapter.setAd(ad);
      mediationInterstitialListener.onAdClicked(adapter);
    }
  }

  @Override
  public void onClosed(AdColonyInterstitial ad) {
    if (adapter != null && mediationInterstitialListener != null) {
      adapter.setAd(ad);
      mediationInterstitialListener.onAdClosed(adapter);
    }
  }

  @Override
  public void onExpiring(AdColonyInterstitial ad) {
    if (adapter != null) {
      adapter.setAd(ad);
      AdColony.requestInterstitial(ad.getZoneID(), this);
    }
  }

  @Override
  public void onIAPEvent(AdColonyInterstitial ad, String productId, int engagementType) {
    if (adapter != null) adapter.setAd(ad);
  }

  @Override
  public void onLeftApplication(AdColonyInterstitial ad) {
    if (adapter != null && mediationInterstitialListener != null) {
      adapter.setAd(ad);
      mediationInterstitialListener.onAdLeftApplication(adapter);
    }
  }

  @Override
  public void onOpened(AdColonyInterstitial ad) {
    if (adapter != null && mediationInterstitialListener != null) {
      adapter.setAd(ad);
      mediationInterstitialListener.onAdOpened(adapter);
    }
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    if (adapter != null && mediationInterstitialListener != null) {
      adapter.setAd(null);
      AdError error = createSdkError();
      Log.w(TAG, error.getMessage());
      mediationInterstitialListener.onAdFailedToLoad(adapter, error);
    }
  }

  void destroy() {
    adapter = null;
    mediationInterstitialListener = null;
  }
}
