package com.google.android.gms.ads.mediation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.ads.AdRequest;
import com.google.ads.mediation.zucks.ZucksMediationAdapter;

import com.google.ads.mediation.zucks.AdMobUtil;
import com.google.ads.mediation.zucks.UniversalInterstitialListener;
import com.google.ads.mediation.zucks.ErrorMapper;
import net.zucks.view.AdFullscreenInterstitial;
import net.zucks.view.AdInterstitial;
import net.zucks.view.IZucksInterstitial;

class ZucksInterstitialAdapter implements MediationInterstitialAd {

  @NonNull private final Context context;
  @NonNull private final Bundle serverParameters;
  @NonNull private final Bundle mediationExtras;
  @NonNull private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> loadCallback;

  @Nullable private MediationInterstitialAdCallback adCallback = null;

  @NonNull
  private final UniversalInterstitialListener.Callback callback =
          new UniversalInterstitialListener.Callback() {

            @Override
            public void onReceiveAd() {
              adCallback = loadCallback.onSuccess(ZucksInterstitialAdapter.this);
            }

            @Override
            public void onShowAd() {
              adCallback.onAdOpened();
              adCallback.reportAdImpression();
            }

            @Override
            public void onCancelDisplayRate() {
              // no-op
            }

            @Override
            public void onTapAd() {
              adCallback.reportAdClicked();
              adCallback.onAdLeftApplication();
            }

            @Override
            public void onCloseAd() {
              adCallback.onAdClosed();
            }

            @Override
            public void onLoadFailure(Exception exception) {
              loadCallback.onFailure(ErrorMapper.convertSdkError(exception));
            }

            @Override
            public void onShowFailure(Exception exception) {
              adCallback.onAdFailedToShow(ErrorMapper.convertSdkError(exception));
            }

          };

  /** Interstitial instance of Zucks Ad Network SDK. */
  private IZucksInterstitial zucksInterstitial = null;

  public ZucksInterstitialAdapter(
          @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
          @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback
  ) {
    this(
            mediationInterstitialAdConfiguration.getContext(),
            mediationInterstitialAdConfiguration.getServerParameters(),
            mediationInterstitialAdConfiguration.getMediationExtras(),
             mediationAdLoadCallback
    );
  }

  @VisibleForTesting
  ZucksInterstitialAdapter(
          @NonNull Context context,
          @NonNull Bundle serverParameters,
          @NonNull Bundle mediationExtras,
          @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> loadCallback
  ) {
    this.context = context;
    this.serverParameters = serverParameters;
    this.mediationExtras = mediationExtras;
    this.loadCallback = loadCallback;
  }

  void loadInterstitialAd() {
    String adFrameId;

    // Check a supported context.
    if (!(context instanceof Activity)) {
      loadCallback.onFailure(
              ErrorMapper.createAdapterError(
                      ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST, "Context not an Activity."
              )
      );
      return;
    }

    if ((adFrameId = AdMobUtil.getFrameId(serverParameters)) == null) {
      loadCallback.onFailure(
              ErrorMapper.createAdapterError(
                      ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST,
                      "FrameID not contained in serverParameters."
              )
      );
      return;
    }

    if (isFullscreenInterstitial(mediationExtras)) {
      zucksInterstitial =
              new AdFullscreenInterstitial(
                      context,
                      adFrameId,
                      new UniversalInterstitialListener.FullscreenInterstitial(callback).use()
              );
    } else {
      zucksInterstitial =
              new AdInterstitial(
                      context,
                      adFrameId,
                      new UniversalInterstitialListener.Interstitial(callback).use()
              );
    }

    AdMobUtil.configurePlatform(zucksInterstitial);

    zucksInterstitial.load();
  }

  /**
   * Returns false if defined `KEY_FULLSCREEN_FOR_INTERSTITIAL` as `false` explicitly or value not
   * defined.
   */
  private static boolean isFullscreenInterstitial(@Nullable Bundle mediationExtras) {
    return mediationExtras != null
        && mediationExtras.getBoolean(
            ZucksMediationAdapter.MediationExtrasBundleBuilder.KEY_FULLSCREEN_FOR_INTERSTITIAL);
  }

  @Override
  public void showAd(@NonNull Context context) {
    zucksInterstitial.show();
  }

  /**
   * Deprecated. Please use MediationExtrasBundleBuilder.
   *
   * @see ZucksMediationAdapter.MediationExtrasBundleBuilder MediationExtrasBundleBuilder
   */
  @Deprecated
  public static AdRequest.Builder addFullscreenInterstitialAdRequest(AdRequest.Builder builder) {
    builder.addNetworkExtrasBundle(
        ZucksAdapter.class,
        new ZucksMediationAdapter.MediationExtrasBundleBuilder()
            .setInterstitialType(ZucksMediationAdapter.InterstitialType.FULLSCREEN)
            .build());
    return builder;
  }

}
