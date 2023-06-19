// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

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
 * A {@link MediationInterstitialAdapter} used to load and show Liftoff Monetize interstitial ads
 * using Google Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter
    implements MediationInterstitialAdapter, MediationBannerAdapter {

  private MediationInterstitialListener mediationInterstitialListener;
  private VungleManager vungleManager;
  private AdConfig adConfig;
  private String placement;

  // banner/MREC
  private MediationBannerListener mediationBannerListener;
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
        Log.w(TAG, error.toString());
        mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
      return;
    }

    this.mediationInterstitialListener = mediationInterstitialListener;
    vungleManager = VungleManager.getInstance();
    placement = vungleManager.findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(placement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Liftoff Monetize. Missing or Invalid Placement ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      this.mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);
    // Unmute full-screen ads by default.
    adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
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
                if (VungleInterstitialAdapter.this.mediationInterstitialListener != null) {
                  VungleInterstitialAdapter.this.mediationInterstitialListener
                      .onAdFailedToLoad(VungleInterstitialAdapter.this, error);
                  Log.w(TAG, error.toString());
                }
              }
            });
  }

  private void loadAd() {
    if (Vungle.canPlayAd(placement)) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
      }
      return;
    }

    Vungle.loadAd(placement, new LoadAdCallback() {
      @Override
      public void onAdLoad(String placementID) {
        if (mediationInterstitialListener != null) {
          mediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w(TAG, error.toString());
        if (mediationInterstitialListener != null) {
          mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
        }
      }
    });
  }

  @Override
  public void showInterstitial() {
    Vungle.playAd(placement, adConfig, new PlayAdCallback() {

      @Override
      public void creativeId(String creativeId) {
        // no-op
      }

      @Override
      public void onAdStart(String placementID) {
        if (mediationInterstitialListener != null) {
          mediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
        // Deprecated, no-op.
      }

      @Override
      public void onAdEnd(String placementID) {
        if (mediationInterstitialListener != null) {
          mediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onAdClick(String placementID) {
        if (mediationInterstitialListener != null) {
          mediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onAdRewarded(String placementID) {
        // No-op for interstitial ads.
      }

      @Override
      public void onAdLeftApplication(String placementID) {
        if (mediationInterstitialListener != null) {
          mediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w(TAG, error.toString());
        if (mediationInterstitialListener != null) {
          mediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
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
      @NonNull final MediationBannerListener bannerListener,
      @NonNull Bundle serverParameters, @NonNull AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {
    mediationBannerListener = bannerListener;
    String appID = serverParameters.getString(KEY_APP_ID);
    AdapterParametersParser.Config config;
    config = AdapterParametersParser.parse(appID, mediationExtras);

    if (TextUtils.isEmpty(appID)) {

      if (mediationBannerListener != null) {
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
            "Failed to load ad from Liftoff Monetize. Missing or invalid app ID.",
            ERROR_DOMAIN);
        Log.w(TAG, error.toString());
        mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
      return;
    }

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    vungleManager = VungleManager.getInstance();

    String placement = vungleManager.findPlacement(mediationExtras, serverParameters);
    Log.d(TAG,
        "requestBannerAd for Placement: " + placement + " ### Adapter instance: " + this
            .hashCode());

    if (TextUtils.isEmpty(placement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Liftoff Monetize. Missing or Invalid placement ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!vungleManager.hasBannerSizeAd(context, adSize, adConfig)) {

      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to load ad from Liftoff Monetize. Invalid banner size.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    // Adapter does not support multiple Banner instances playing for same placement except for
    // refresh.
    String uniqueRequestId = config.getRequestUniqueId();
    if (!vungleManager.canRequestBannerAd(placement, uniqueRequestId)) {
      AdError error = new AdError(ERROR_AD_ALREADY_LOADED,
          "Liftoff Monetize adapter does not support multiple banner instances for same"
              + "placement.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    vungleBannerAdapter = new VungleBannerAdapter(placement, uniqueRequestId, adConfig,
        VungleInterstitialAdapter.this);
    Log.d(TAG, "New banner adapter: " + vungleBannerAdapter + "; size: " + adConfig.getAdSize());

    VungleBannerAd vungleBanner = new VungleBannerAd(placement, vungleBannerAdapter);
    vungleManager.registerBannerAd(placement, vungleBanner);

    Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
    vungleBannerAdapter.requestBannerAd(context, config.getAppId(), adSize,
        mediationBannerListener);
  }

  @NonNull
  @Override
  public View getBannerView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return vungleBannerAdapter.getAdLayout();
  }

}
