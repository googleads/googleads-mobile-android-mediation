package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.vungle.mediation.AdapterParametersParser;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

public class VungleRtbInterstitialAd implements MediationInterstitialAd {

  private static final String TAG = VungleRtbInterstitialAd.class.getSimpleName();

  @NonNull
  private final MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration;
  @NonNull
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback;
  @Nullable
  private MediationInterstitialAdCallback mediationInterstitialAdCallback;

  private AdConfig mAdConfig;
  private String mPlacementForPlay;
  private String mAdMarkup;

  public VungleRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    this.mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
    this.mMediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    Bundle mediationExtras = mediationInterstitialAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mPlacementForPlay = VungleManager.getInstance()
        .findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = mediationInterstitialAdConfiguration.getBidResponse();
    if (TextUtils.isEmpty(mAdMarkup)) {
      mAdMarkup = null;
    }
    Log.d(TAG, "Render interstitial mAdMarkup=" + mAdMarkup);

    AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);
    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
    VungleInitializer.getInstance()
        .initialize(
            config.getAppId(),
            mediationInterstitialAdConfiguration.getContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                loadAd();
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.getMessage());
                mMediationAdLoadCallback.onFailure(error);
              }
            });
  }

  private void loadAd() {
    if (Vungle.canPlayAd(mPlacementForPlay, mAdMarkup)) {
      mediationInterstitialAdCallback = mMediationAdLoadCallback
          .onSuccess(VungleRtbInterstitialAd.this);
      return;
    }

    // Placement ID is not what Vungle's SDK gets back after init/config.
    if (!VungleManager.getInstance().isValidPlacement(mPlacementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    Vungle.loadAd(mPlacementForPlay, mAdMarkup, mAdConfig, new LoadAdCallback() {
      @Override
      public void onAdLoad(String placementID) {
        mediationInterstitialAdCallback = mMediationAdLoadCallback
            .onSuccess(VungleRtbInterstitialAd.this);
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w(TAG, error.getMessage());
        mMediationAdLoadCallback.onFailure(error);
      }
    });
  }

  @Override
  public void showAd(@NonNull Context context) {
    Vungle.playAd(mPlacementForPlay, mAdMarkup, mAdConfig, new PlayAdCallback() {

      @Override
      public void creativeId(String creativeId) {
        // no-op
      }

      @Override
      public void onAdStart(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdOpened();
        }
      }

      @Override
      public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
        // Deprecated, no-op.
      }

      @Override
      public void onAdEnd(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdClosed();
        }
      }

      @Override
      public void onAdClick(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.reportAdClicked();
        }
      }

      @Override
      public void onAdRewarded(String placementID) {
        // No-op for interstitial ads.
      }

      @Override
      public void onAdLeftApplication(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdLeftApplication();
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w(TAG, error.getMessage());
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdClosed();
        }
      }

      @Override
      public void onAdViewed(String id) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.reportAdImpression();
        }
      }
    });
  }

}
