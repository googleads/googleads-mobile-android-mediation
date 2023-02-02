package com.google.ads.mediation.inmobi;

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
public final class InMobiAdapter extends InMobiMediationAdapter {

  static final String TAG = InMobiAdapter.class.getSimpleName();
  private InMobiBannerAd mInMobiBanner;
  private InMobiInterstitialAd mInMobiInterstitial;
  private InMobiNativeAd mInMobiNativeAd;

  @Override
  public void loadBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
                           @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    mInMobiBanner = new InMobiBannerAd(mediationBannerAdConfiguration, callback);
    mInMobiBanner.load();
  }

  @Override
  public void loadInterstitialAd(@NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
                                 @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    mInMobiInterstitial = new InMobiInterstitialAd(mediationInterstitialAdConfiguration, callback);
    mInMobiInterstitial.load();
  }

  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
                           @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    mInMobiNativeAd = new InMobiNativeAd(mediationNativeAdConfiguration, callback);
    mInMobiNativeAd.load();
  }
}
