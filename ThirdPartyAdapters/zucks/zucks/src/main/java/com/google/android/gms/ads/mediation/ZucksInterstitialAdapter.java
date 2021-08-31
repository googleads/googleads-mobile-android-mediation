package com.google.android.gms.ads.mediation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.ads.mediation.zucks.ZucksMediationAdapter;

import com.google.ads.mediation.zucks.AdMobUtil;
import com.google.ads.mediation.zucks.UniversalInterstitialListener;
import com.google.ads.mediation.zucks.ErrorMapper;
import net.zucks.view.AdFullscreenInterstitial;
import net.zucks.view.AdInterstitial;
import net.zucks.view.IZucksInterstitial;

/**
 * Interstitial Ad Adapter implementation for calls from ZucksAdapter. Can **NOT** use this as a
 * standalone adapter implementation.
 *
 * @see com.google.android.gms.ads.mediation.ZucksAdapter ZucksAdapter
 */
class ZucksInterstitialAdapter extends ZucksMediationAdapter
    implements MediationInterstitialAdapter, MediationInterstitialAd {

  /** New adapter implementation */
  @VisibleForTesting
  static class ZucksMediationInterstitialAd implements MediationInterstitialAd {

    @NonNull private final ZucksInterstitialAdapter self;

    @Nullable
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
        loadCallback;

    @Nullable private MediationInterstitialAdCallback adCallback;

    @NonNull
    private final UniversalInterstitialListener.Callback callback =
        new UniversalInterstitialListener.Callback() {

          @Override
          public void onReceiveAd() {
            adCallback = loadCallback.onSuccess(self.root);
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

    @VisibleForTesting
    ZucksMediationInterstitialAd(@NonNull ZucksInterstitialAdapter self) {
      this.self = self;
    }

    @VisibleForTesting
    void loadInterstitialAd(
        MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
            mediationAdLoadCallback) {
      loadCallback = mediationAdLoadCallback;
      AdError error = self.configureInterstitialAd(mediationInterstitialAdConfiguration, callback);
      if (error != null) {
        loadCallback.onFailure(error);
      }
    }

    @Override
    public void showAd(Context context) {
      self.mAdInterstitial.show();
    }
  }

  /** Legacy adapter implementation This class will be removed. */
  @Deprecated
  private static class ZucksMediationInterstitialAdapter implements MediationInterstitialAdapter {

    @NonNull private final ZucksInterstitialAdapter self;

    @Nullable private MediationInterstitialListener oldCallback = null;

    @NonNull
    private final UniversalInterstitialListener.Callback callback =
        new UniversalInterstitialListener.Callback() {

          @Override
          public void onReceiveAd() {
            oldCallback.onAdLoaded(self.root);
          }

          @Override
          public void onShowAd() {
            oldCallback.onAdOpened(self.root);
          }

          @Override
          public void onCancelDisplayRate() {
            // no-op
          }

          @Override
          public void onTapAd() {
            oldCallback.onAdClicked(self.root);
            oldCallback.onAdLeftApplication(self.root);
          }

          @Override
          public void onCloseAd() {
            oldCallback.onAdClosed(self.root);
          }

          @Override
          public void onLoadFailure(Exception exception) {
            AdError error = ErrorMapper.convertSdkError(exception);
            oldCallback.onAdFailedToLoad(self.root, error);
          }

          @Override
          public void onShowFailure(Exception exception) {
            AdMobUtil.ZUCKS_LOG.d(
                "Call #onShowFailure(Exception exception) in AdMob adapter.", exception);
            oldCallback.onAdOpened(self.root);
            oldCallback.onAdClosed(self.root);
          }
        };

    private ZucksMediationInterstitialAdapter(@NonNull ZucksInterstitialAdapter self) {
      this.self = self;
    }

    @Override
    public void requestInterstitialAd(
        Context context,
        MediationInterstitialListener mediationInterstitialListener,
        Bundle serverParameters,
        MediationAdRequest mediationAdRequest,
        Bundle mediationExtras) {
      oldCallback = mediationInterstitialListener;
      AdError error =
          self.configureInterstitialAd(context, serverParameters, mediationExtras, callback);
      if (error != null) {
        oldCallback.onAdFailedToLoad(self.root, error);
      }
    }

    @Override
    public void showInterstitial() {
      self.mAdInterstitial.show();
    }

    @Override
    public void onDestroy() {
      // no-op
    }

    @Override
    public void onPause() {
      // no-op
    }

    @Override
    public void onResume() {
      // no-op
    }
  }

  @NonNull private final ZucksAdapter root;

  /** Interstitial instance of Zucks Ad Network SDK. */
  private IZucksInterstitial mAdInterstitial = null;

  // region New adapter
  @Nullable private ZucksMediationInterstitialAd mediationInterstitialAd = null;

  @NonNull
  private ZucksMediationInterstitialAd useInterstitialAd() {
    if (mediationInterstitialAd == null) {
      mediationInterstitialAd = new ZucksMediationInterstitialAd(this);
    }
    return mediationInterstitialAd;
  }
  // endregion

  // region Legacy adapter
  @Deprecated @Nullable
  private ZucksMediationInterstitialAdapter mediationInterstitialAdapter = null;

  @Deprecated
  @NonNull
  private ZucksMediationInterstitialAdapter useInterstitialAdapter() {
    if (mediationInterstitialAdapter == null) {
      mediationInterstitialAdapter = new ZucksMediationInterstitialAdapter(this);
    }
    return mediationInterstitialAdapter;
  }
  // endregion

  public ZucksInterstitialAdapter(@NonNull ZucksAdapter root) {
    this.root = root;
  }

  @VisibleForTesting
  @Nullable
  AdError configureInterstitialAd(
      MediationInterstitialAdConfiguration configuration,
      UniversalInterstitialListener.Callback callback) {
    return configureInterstitialAd(
        configuration.getContext(),
        configuration.getServerParameters(),
        configuration.getMediationExtras(),
        callback);
  }

  @VisibleForTesting
  @Nullable
  AdError configureInterstitialAd(
      Context context,
      Bundle serverParameters,
      Bundle mediationExtras,
      UniversalInterstitialListener.Callback callback) {
    // Check a supported context.
    if (!(context instanceof Activity)) {
      return ErrorMapper.createAdapterError(
          ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST, "Context not an Activity.");
    }

    String adFrameId = AdMobUtil.getFrameId(serverParameters);

    if (adFrameId == null) {
      return ErrorMapper.createAdapterError(
          ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST, "FrameID not contained in serverParameters.");
    } else {
      if (isFullscreenInterstitial(mediationExtras)) {
        mAdInterstitial =
            new AdFullscreenInterstitial(
                context,
                adFrameId,
                new UniversalInterstitialListener.FullscreenInterstitial(callback).use());
      } else {
        mAdInterstitial =
            new AdInterstitial(
                context, adFrameId, new UniversalInterstitialListener.Interstitial(callback).use());
      }

      AdMobUtil.configurePlatform(mAdInterstitial);

      mAdInterstitial.load();
      return null;
    }
  }

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
    useInterstitialAd()
        .loadInterstitialAd(mediationInterstitialAdConfiguration, mediationAdLoadCallback);
  }

  @Deprecated
  @Override
  public void showInterstitial() {
    useInterstitialAdapter().showInterstitial();
  }

  @Deprecated
  @Override
  public void onDestroy() {
    useInterstitialAdapter().onDestroy();
  }

  @Deprecated
  @Override
  public void onPause() {
    useInterstitialAdapter().onPause();
  }

  @Deprecated
  @Override
  public void onResume() {
    useInterstitialAdapter().onResume();
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
  public void showAd(Context context) {
    useInterstitialAd().showAd(context);
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
