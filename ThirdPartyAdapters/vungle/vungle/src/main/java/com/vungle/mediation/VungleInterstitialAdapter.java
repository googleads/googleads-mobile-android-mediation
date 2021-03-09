// Copyright 2014 Google Inc.
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

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
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
import java.util.ArrayList;

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
  public void requestInterstitialAd(Context context,
      MediationInterstitialListener mediationInterstitialListener, Bundle serverParameters,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Failed to load ad from Vungle.", e);
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdFailedToLoad(
            VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    mMediationInterstitialListener = mediationInterstitialListener;
    mVungleManager = VungleManager.getInstance();

    mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacementForPlay)) {
      Log.w(TAG, "Failed to load ad from Vungle: Missing or Invalid Placement ID.");
      mMediationInterstitialListener.onAdFailedToLoad(
          VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

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
              public void onInitializeError(String errorMessage) {
                Log.w(TAG, "Failed to load ad from Vungle: " + errorMessage);
                if (mMediationInterstitialListener != null) {
                  mMediationInterstitialListener.onAdFailedToLoad(
                      VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
              }
            });
  }

  private void loadAd() {
    if (mVungleManager.isAdPlayable(mPlacementForPlay)) {
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
      }
      return;
    }

    // Placement ID is not what Vungle's SDK gets back after init/config.
    if (!mVungleManager.isValidPlacement(mPlacementForPlay)) {
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdFailedToLoad(
            VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
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
        Log.w(TAG, "Failed to load ad from Vungle: " + exception.getLocalizedMessage());
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdFailedToLoad(
              VungleInterstitialAdapter.this, exception.getExceptionCode());
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
        Log.w(TAG, "Failed to play ad from Vungle: " + exception.getLocalizedMessage());
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
  public void requestBannerAd(Context context,
      final MediationBannerListener mediationBannerListener, Bundle serverParameters, AdSize adSize,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
    mMediationBannerListener = mediationBannerListener;

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Failed to load ad from Vungle.", e);
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdFailedToLoad(
            VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    mVungleManager = VungleManager.getInstance();

    String placementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    Log.d(TAG, "requestBannerAd for Placement: " + placementForPlay
        + " ### Adapter instance: " + this.hashCode());

    if (TextUtils.isEmpty(placementForPlay)) {
      String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
      Log.w(TAG, message);
      mMediationBannerListener.onAdFailedToLoad(
          VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!hasBannerSizeAd(context, adSize, adConfig)) {
      String message = "Failed to load ad from Vungle: Invalid banner size.";
      Log.w(TAG, message);
      mMediationBannerListener.onAdFailedToLoad(
          VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    // Adapter does not support multiple Banner instances playing for same placement except for
    // refresh.
    String uniqueRequestId = config.getRequestUniqueId();
    if (!mVungleManager.canRequestBannerAd(placementForPlay, uniqueRequestId)) {
      mMediationBannerListener.onAdFailedToLoad(
          VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
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

  @Override
  public View getBannerView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return vungleBannerAdapter.getAdLayout();
  }

  private boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(BANNER_SHORT.getWidth(), BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(BANNER.getWidth(), BANNER.getHeight()));
    potentials.add(new AdSize(BANNER_LEADERBOARD.getWidth(), BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(VUNGLE_MREC.getWidth(), VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      Log.i(TAG, "Not found closest ad size: " + adSize);
      return false;
    }
    Log.i(
        TAG,
        "Found closest ad size: " + closestSize.toString() + " for requested ad size: " + adSize);

    if (closestSize.getWidth() == BANNER_SHORT.getWidth()
        && closestSize.getHeight() == BANNER_SHORT.getHeight()) {
      adConfig.setAdSize(BANNER_SHORT);
    } else if (closestSize.getWidth() == BANNER.getWidth()
        && closestSize.getHeight() == BANNER.getHeight()) {
      adConfig.setAdSize(BANNER);
    } else if (closestSize.getWidth() == BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == BANNER_LEADERBOARD.getHeight()) {
      adConfig.setAdSize(BANNER_LEADERBOARD);
    } else if (closestSize.getWidth() == VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == VUNGLE_MREC.getHeight()) {
      adConfig.setAdSize(VUNGLE_MREC);
    }

    return true;
  }
}
