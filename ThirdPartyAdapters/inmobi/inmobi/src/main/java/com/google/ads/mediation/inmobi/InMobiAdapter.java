package com.google.ads.mediation.inmobi;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

/**
 * InMobi Adapter for AdMob Mediation used to load and show banner, interstitial and native ads.
 * This class should not be used directly by publishers.
 */
@Keep
public final class InMobiAdapter extends InMobiMediationAdapter {

  private InMobiBannerAd inMobiBanner;
  private InMobiInterstitialAd inMobiInterstitial;
  private InMobiNativeAd inMobiNativeAd;

  @Override
  public void loadBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    inMobiBanner = new InMobiBannerAd(mediationBannerAdConfiguration, callback);
    inMobiBanner.loadAd();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    inMobiInterstitial = new InMobiInterstitialAd(mediationInterstitialAdConfiguration, callback);
    inMobiInterstitial.loadAd();
  }

  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    inMobiNativeAd = new InMobiNativeAd(mediationNativeAdConfiguration, callback);
    inMobiNativeAd.loadAd();
  }
}
