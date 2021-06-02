package com.google.android.gms.ads.mediation;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.ads.mediation.zucks.ZucksMediationAdapter;

import net.zucks.admob.BaseMediationAdapter;

/**
 * [LEGACY] Mediation Adapter for Zucks Ad Network.
 *
 * <p>Supported formats: - Banner - Interstitial
 *
 * <p>Unsupported formats: - Rewarded Ad
 *
 * <p>If you want to integrate Rewarded Ad, see the new mediation adapter implementation.
 *
 * @see ZucksMediationAdapter ZucksMediationAdapter
 */
public class ZucksAdapter extends BaseMediationAdapter
    implements MediationBannerAdapter,
        MediationInterstitialAdapter,
        MediationBannerAd,
        MediationInterstitialAd {

  // region Banner
  @Nullable private ZucksBannerAdapter bannerAdapter = null;

  @NonNull
  private ZucksBannerAdapter useBannerAdapter() {
    if (bannerAdapter == null) {
      bannerAdapter = new ZucksBannerAdapter(this);
    }
    return bannerAdapter;
  }
  // endregion

  // region Interstitial
  @Nullable private ZucksInterstitialAdapter interstitialAdapter = null;

  @NonNull
  private ZucksInterstitialAdapter useInterstitialAdapter() {
    if (interstitialAdapter == null) {
      interstitialAdapter = new ZucksInterstitialAdapter(this);
    }
    return interstitialAdapter;
  }
  // endregion

  @Override
  public void onDestroy() {
    if (bannerAdapter != null) {
      bannerAdapter.onDestroy();
    }
    if (interstitialAdapter != null) {
      interstitialAdapter.onDestroy();
    }
  }

  @Override
  public void onPause() {
    if (bannerAdapter != null) {
      bannerAdapter.onPause();
    }
    if (interstitialAdapter != null) {
      interstitialAdapter.onPause();
    }
  }

  @Override
  public void onResume() {
    if (bannerAdapter != null) {
      bannerAdapter.onResume();
    }
    if (interstitialAdapter != null) {
      interstitialAdapter.onResume();
    }
  }

  @Override
  public void loadBannerAd(
      MediationBannerAdConfiguration mediationBannerAdConfiguration,
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    useBannerAdapter().loadBannerAd(mediationBannerAdConfiguration, mediationAdLoadCallback);
  }

  /** NOTE: This method will be removed. */
  @Deprecated
  @Override
  public void requestBannerAd(
      Context context,
      MediationBannerListener mediationBannerListener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    useBannerAdapter()
        .requestBannerAd(
            context,
            mediationBannerListener,
            serverParameters,
            adSize,
            mediationAdRequest,
            mediationExtras);
  }

  /** NOTE: This method will be removed. */
  @Deprecated
  @Override
  public View getBannerView() {
    return useBannerAdapter().getBannerView();
  }

  @NonNull
  @Override
  public View getView() {
    return useBannerAdapter().getView();
  }

  /** NOTE: This method will be removed. */
  @Deprecated
  @Override
  public void requestInterstitialAd(
      Context context,
      MediationInterstitialListener mediationInterstitialListener,
      Bundle serverParameters,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    useInterstitialAdapter()
        .requestInterstitialAd(
            context,
            mediationInterstitialListener,
            serverParameters,
            mediationAdRequest,
            mediationExtras);
  }

  @Override
  public void loadInterstitialAd(
      MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback) {
    useInterstitialAdapter()
        .loadInterstitialAd(mediationInterstitialAdConfiguration, mediationAdLoadCallback);
  }

  /** NOTE: This method will be removed. */
  @Deprecated
  @Override
  public void showInterstitial() {
    useInterstitialAdapter().showInterstitial();
  }

  @Override
  public void showAd(Context context) {
    useInterstitialAdapter().showAd(context);
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
