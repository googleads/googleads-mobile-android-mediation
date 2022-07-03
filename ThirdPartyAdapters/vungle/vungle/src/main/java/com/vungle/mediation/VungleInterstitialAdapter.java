package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using Google
 * Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter
    implements MediationInterstitialAdapter, MediationBannerAdapter {

  private static final String TAG = VungleInterstitialAdapter.class.getSimpleName();
  private MediationInterstitialListener mMediationInterstitialListener;
  private VungleManager mVungleManager;
  private AdConfig mAdConfig;
  private String mPlacementForPlay;

  // banner/MREC
  private MediationBannerListener mMediationBannerListener;
  private VungleBannerAdapter vungleBannerAdapter;

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener mediationInterstitialListener,
      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      if (mediationInterstitialListener != null) {
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
            "Missing or invalid App ID.", ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
      return;
    }

    mMediationInterstitialListener = mediationInterstitialListener;
    mVungleManager = VungleManager.getInstance();
    mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    VungleInitializer.getInstance()
            .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);
    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
    VungleInitializer.getInstance()
        .initialize(
            config.getAppId(),
            context.getApplicationContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                loadAd();
              }

              @Override
              public void onInitializeError(AdError error) {
                if (mMediationInterstitialListener != null) {
                  mMediationInterstitialListener
                      .onAdFailedToLoad(VungleInterstitialAdapter.this, error);
                  Log.w(TAG, error.getMessage());
                }
              }
            });
  }

  private void loadAd() {
    if (Vungle.canPlayAd(mPlacementForPlay)) {
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
      }
      return;
    }

    // Placement ID is not what Vungle's SDK gets back after init/config.
    if (!mVungleManager.isValidPlacement(mPlacementForPlay)) {
      if (mMediationInterstitialListener != null) {
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
            "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
      return;
    }

    Vungle.loadAd(mPlacementForPlay, new LoadAdCallback() {
      @Override
      public void onAdLoad(String placementID) {
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w(TAG, error.getMessage());
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
        }
      }
    });
  }

  @Override
  public void showInterstitial() {
    Vungle.playAd(mPlacementForPlay, mAdConfig, new PlayAdCallback() {

      @Override
      public void creativeId(String creativeId) {
        // no-op
      }

      @Override
      public void onAdStart(String placementID) {
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
        // Deprecated, no-op.
      }

      @Override
      public void onAdEnd(String placementID) {
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onAdClick(String placementID) {
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onAdRewarded(String placementID) {
        // No-op for interstitial ads.
      }

      @Override
      public void onAdLeftApplication(String placementID) {
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w(TAG, error.getMessage());
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onAdViewed(String id) {
        // No-op.
      }
    });
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: " + hashCode());
    if (vungleBannerAdapter != null) {
      vungleBannerAdapter.destroy();
      vungleBannerAdapter = null;
    }
  }

  // banner
  @Override
  public void onPause() {
    Log.d(TAG, "onPause");
    if (vungleBannerAdapter != null) {
      vungleBannerAdapter.updateVisibility(false);
    }
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume");
    if (vungleBannerAdapter != null) {
      vungleBannerAdapter.updateVisibility(true);
    }
  }

  @Override
  public void requestBannerAd(@NonNull Context context,
      @NonNull final MediationBannerListener mediationBannerListener,
      @NonNull Bundle serverParameters,
      @NonNull AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {
    mMediationBannerListener = mediationBannerListener;
    String appID = serverParameters.getString(KEY_APP_ID);
    AdapterParametersParser.Config config;
    config = AdapterParametersParser.parse(appID, mediationExtras);

    if (TextUtils.isEmpty(appID)) {

      if (mediationBannerListener != null) {
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
            "Failed to load ad from Vungle. Missing or invalid app ID.", ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
      return;
    }

    VungleInitializer.getInstance()
            .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    mVungleManager = VungleManager.getInstance();

    String placementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    Log.d(TAG,
        "requestBannerAd for Placement: " + placementForPlay + " ### Adapter instance: " + this
            .hashCode());

    if (TextUtils.isEmpty(placementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!VungleManager.getInstance().hasBannerSizeAd(context, adSize, adConfig)) {

      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to load ad from Vungle. Invalid banner size.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    // Adapter does not support multiple Banner instances playing for same placement except for
    // refresh.
    String uniqueRequestId = config.getRequestUniqueId();
    if (!mVungleManager.canRequestBannerAd(placementForPlay, uniqueRequestId)) {
      AdError error = new AdError(ERROR_AD_ALREADY_LOADED,
          "Vungle adapter does not support multiple banner instances for same placement.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    vungleBannerAdapter = new VungleBannerAdapter(placementForPlay, uniqueRequestId, adConfig,
        VungleInterstitialAdapter.this);
    Log.d(TAG, "New banner adapter: " + vungleBannerAdapter + "; size: " + adConfig.getAdSize());

    VungleBannerAd vungleBanner = new VungleBannerAd(placementForPlay, vungleBannerAdapter);
    mVungleManager.registerBannerAd(placementForPlay, vungleBanner);

    Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
    vungleBannerAdapter
        .requestBannerAd(context, config.getAppId(), adSize, mMediationBannerListener);
  }

  @NonNull
  @Override
  public View getBannerView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return vungleBannerAdapter.getAdLayout();
  }

}
