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

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;

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

  /**
   * Ad container for Vungle's banner ad.
   */
  private volatile View adLayout;
  private VungleBannerAdapter vungleBannerAdapter;

  @Override
  public void requestInterstitialAd(Context context,
      MediationInterstitialListener mediationInterstitialListener, Bundle serverParameters,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Failed to load ad from Vungle", e);
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
      Log.w(TAG, "Failed to load ad from Vungle: Missing or Invalid Placement ID");
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
    } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
      mVungleManager.loadAd(
          mPlacementForPlay,
          new VungleListener() {
            @Override
            void onAdAvailable() {
              if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
              }
            }

            @Override
            void onAdFailedToLoad(int errorCode) {
              Log.w(TAG, "Failed to load ad from Vungle: " + errorCode);
              if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(
                    VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
              }
            }
          });
    } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdFailedToLoad(
            VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
    }
  }

  @Override
  public void showInterstitial() {
    if (mVungleManager != null) {
      mVungleManager.playAd(
          mPlacementForPlay,
          mAdConfig,
          new VungleListener() {
            @Override
            void onAdClick(String placementId) {
              if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
              }
            }

            @Override
            void onAdEnd(String placementId) {
              if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
              }
            }

            @Override
            void onAdLeftApplication(String placementId) {
              if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
              }
            }

            @Override
            void onAdStart(String placement) {
              if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
              }
            }

            @Override
            void onAdFail(String placement) {
              if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
              }
            }
          });
    }
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: " + hashCode());
    if (vungleBannerAdapter != null) {
      vungleBannerAdapter.onDestroy();
      vungleBannerAdapter = null;
      adLayout = null;
    }
  }

  @Override
  public void onPause() {
    Log.d(TAG, "onPause");
    if (vungleBannerAdapter != null) {
      vungleBannerAdapter.onPause();
    }
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume");
    if (vungleBannerAdapter != null) {
      vungleBannerAdapter.onResume();
    }
  }

  @Override
  public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener,
      Bundle serverParameters, AdSize adSize, MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    vungleBannerAdapter = new VungleBannerAdapter(context, VungleInterstitialAdapter.this,
        mediationBannerListener);
    adLayout = vungleBannerAdapter
        .requestBannerAd(adSize, mediationAdRequest, serverParameters, mediationExtras);
  }

  @Override
  public View getBannerView() {
    return adLayout;
  }

}
