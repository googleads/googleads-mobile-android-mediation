package com.google.android.gms.ads.mediation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.ads.mediation.zucks.ZucksMediationAdapter;

import com.google.ads.mediation.zucks.AdMobUtil;
import com.google.ads.mediation.zucks.UniversalInterstitialListener;
import com.google.ads.mediation.zucks.ErrorMapper;
import net.zucks.view.AdFullscreenInterstitial;
import net.zucks.view.AdInterstitial;
import net.zucks.view.IZucksInterstitial;

import java.util.Locale;

class ZucksInterstitialAdapter implements MediationInterstitialAd {

  /** {@link Log} is not acceptable >23 length string as tag. */
  private static final String TAG = "ZucksISAdapter";

  @NonNull private final MediationInterstitialAdConfiguration adConfiguration;

  @NonNull
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;

  @Nullable private MediationInterstitialAdCallback interstitialAdCallback = null;

  @NonNull
  private final UniversalInterstitialListener.Callback callback =
      new UniversalInterstitialListener.Callback() {

        @Override
        public void onReceiveAd() {
          interstitialAdCallback = adLoadCallback.onSuccess(ZucksInterstitialAdapter.this);
        }

        @Override
        public void onShowAd() {
          interstitialAdCallback.onAdOpened();
          interstitialAdCallback.reportAdImpression();
        }

        @Override
        public void onCancelDisplayRate() {
          // no-op
        }

        @Override
        public void onTapAd() {
          interstitialAdCallback.reportAdClicked();
          interstitialAdCallback.onAdLeftApplication();
        }

        @Override
        public void onCloseAd() {
          interstitialAdCallback.onAdClosed();
        }

        @Override
        public void onLoadFailure(Exception exception) {
          notifySdkLoadFailure(exception);
        }

        @Override
        public void onShowFailure(Exception exception) {
          notifySdkFailedToShow(exception);
        }
      };

  /** Interstitial instance of Zucks Ad Network SDK. */
  private IZucksInterstitial zucksInterstitial = null;

  public ZucksInterstitialAdapter(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback) {
    this.adConfiguration = mediationInterstitialAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
  }

  void loadInterstitialAd() {
    Context context = adConfiguration.getContext();
    String adFrameId;

    // Check a supported context.
    if (!(context instanceof Activity)) {
      notifyAdapterLoadFailure(
          ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST, "Context not an Activity.");
      return;
    }

    if ((adFrameId = AdMobUtil.getFrameId(adConfiguration.getServerParameters())) == null) {
      notifyAdapterLoadFailure(
          ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST, "FrameID not contained in serverParameters.");
      return;
    }

    if (isFullscreenInterstitial(adConfiguration.getMediationExtras())) {
      zucksInterstitial =
          new AdFullscreenInterstitial(
              context,
              adFrameId,
              new UniversalInterstitialListener.FullscreenInterstitial(callback).use());
    } else {
      zucksInterstitial =
          new AdInterstitial(
              context, adFrameId, new UniversalInterstitialListener.Interstitial(callback).use());
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

  // region Notify and logging errors
  // @see <a
  // href="https://github.com/googleads/googleads-mobile-android-mediation/pull/337#discussion_r764662057">GitHub review</a>
  private void notifyAdapterLoadFailure(@ErrorMapper.AdapterError int code, @NonNull String msg) {
    Log.w(TAG, String.format(Locale.ROOT, "%d: %s", code, msg));
    adLoadCallback.onFailure(ErrorMapper.createAdapterError(code, msg));
  }

  private void notifySdkLoadFailure(@NonNull Exception exception) {
    Log.w(TAG, exception);
    adLoadCallback.onFailure(ErrorMapper.convertSdkError(exception));
  }

  private void notifySdkFailedToShow(@NonNull Exception exception) {
    Log.w(TAG, exception);
    interstitialAdCallback.onAdFailedToShow(ErrorMapper.convertSdkError(exception));
  }
  // endregion

}
