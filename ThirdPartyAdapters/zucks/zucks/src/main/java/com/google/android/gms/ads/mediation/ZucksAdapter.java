package com.google.android.gms.ads.mediation;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.ads.mediation.zucks.ZucksMediationAdapter;

/**
 * The {@link ZucksAdapter} class is used to load Zucks banner and interstitial ads using Google Mobile Ads SDK mediation.
 */
public class ZucksAdapter extends ZucksMediationAdapter
    implements MediationBannerAd,
        MediationInterstitialAd {

  @Nullable private ZucksBannerAdapter bannerAdapter = null;
  @Nullable private ZucksInterstitialAdapter interstitialAdapter = null;

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback
  ) {
    bannerAdapter = new ZucksBannerAdapter(mediationBannerAdConfiguration, mediationAdLoadCallback);
    bannerAdapter.loadBannerAd();
  }

  @NonNull
  @Override
  public View getView() {
    return bannerAdapter.getView();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback
  ) {
    interstitialAdapter = new ZucksInterstitialAdapter(mediationInterstitialAdConfiguration, mediationAdLoadCallback);
    interstitialAdapter.loadInterstitialAd();
  }

  @Override
  public void showAd(@NonNull Context context) {
    interstitialAdapter.showAd(context);
  }

  /**
   * Deprecated. Please use MediationExtrasBundleBuilder.
   *
   * @see ZucksMediationAdapter.MediationExtrasBundleBuilder MediationExtrasBundleBuilder
   */
  @Deprecated
  public static AdRequest.Builder addFullscreenInterstitialAdRequest(AdRequest.Builder builder) {
    return ZucksInterstitialAdapter.addFullscreenInterstitialAdRequest(builder);
  }

  /**
   * Alias of MediationExtrasBundleBuilder.
   *
   * @see ZucksMediationAdapter.MediationExtrasBundleBuilder MediationExtrasBundleBuilder
   */
  @Deprecated
  public static class MediationExtrasBundleBuilder
      extends ZucksMediationAdapter.MediationExtrasBundleBuilder {}
}
