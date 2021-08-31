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
 * [LEGACY] Mediation Adapter for Zucks Ad Network.
 * <p>
 * Supported formats:
 *   - Banner
 *   - Interstitial
 * <p>
 * Unsupported formats:
 *   - Rewarded Ad
 * <p>
 * Currently, Rewarded Ad is not supported in this version.
 * It's will be implemented in the new mediation adapter (for the latest FQCN naming convention reason).
 *
 * @see ZucksMediationAdapter ZucksMediationAdapter
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
    bannerAdapter = new ZucksBannerAdapter(this, mediationBannerAdConfiguration, mediationAdLoadCallback);
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
    interstitialAdapter = new ZucksInterstitialAdapter(this, mediationInterstitialAdConfiguration, mediationAdLoadCallback);
    interstitialAdapter.loadInterstitialAd();
  }

  @Override
  public void showAd(Context context) {
    interstitialAdapter.showAd();
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
