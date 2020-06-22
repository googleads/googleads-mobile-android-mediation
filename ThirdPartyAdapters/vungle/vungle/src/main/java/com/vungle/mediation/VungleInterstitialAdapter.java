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
import android.widget.RelativeLayout;

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

import java.util.ArrayList;

import androidx.annotation.Keep;

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

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
  private volatile RelativeLayout adLayout;
  private MediationBannerListener mMediationBannerListener;
  private VungleBannerAdapter mBannerRequest;

  @Override
  public void requestInterstitialAd(
      Context context,
      MediationInterstitialListener mediationInterstitialListener,
      Bundle serverParameters,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {

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

    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
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
    if (mBannerRequest != null) {
      mBannerRequest.destroy(adLayout);
      mBannerRequest = null;
    }
    adLayout = null;
  }

  // banner
  @Override
  public void onPause() {
    Log.d(TAG, "onPause");
    if (mBannerRequest != null) {
      mBannerRequest.updateVisibility(false);
    }
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume");
    if (mBannerRequest != null) {
      mBannerRequest.updateVisibility(true);
    }
  }

  @Override
  public void requestBannerAd(
      Context context,
      final MediationBannerListener mediationBannerListener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    Log.d(TAG, "requestBannerAd");
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
    Log.d(
        TAG,
        "requestBannerAd for Placement: "
            + placementForPlay
            + " ###  Adapter instance: "
            + this.hashCode());

    if (TextUtils.isEmpty(placementForPlay)) {
      String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
      Log.w(TAG, message);
      mMediationBannerListener.onAdFailedToLoad(
          VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
    if (VungleExtrasBuilder.isStartMutedNotConfigured(mediationExtras)) {
      adConfig.setMuted(true); // start muted by default
    }
    if (!hasBannerSizeAd(context, adSize, adConfig)) {
      String message = "Failed to load ad from Vungle: Invalid banner size.";
      Log.w(TAG, message);
      mMediationBannerListener.onAdFailedToLoad(
          VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    // Create the adLayout wrapper with the requested ad size, as Vungle's ad uses MATCH_PARENT for
    // its dimensions.
    adLayout = new RelativeLayout(context) {
      @Override
      protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mBannerRequest != null) {
          mBannerRequest.attach();
        }
      }

      @Override
      protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBannerRequest != null) {
          mBannerRequest.detach();
        }
      }
    };
    int adLayoutHeight = adSize.getHeightInPixels(context);
    // If the height is 0 (e.g. for inline adaptive banner requests), use the closest supported size
    // as the height of the adLayout wrapper.
    if (adLayoutHeight <= 0) {
      float density = context.getResources().getDisplayMetrics().density;
      adLayoutHeight = Math.round(adConfig.getAdSize().getHeight() * density);
    }
    RelativeLayout.LayoutParams adViewLayoutParams =
        new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context), adLayoutHeight);
    adLayout.setLayoutParams(adViewLayoutParams);

    mBannerRequest =
        mVungleManager.getBannerRequest(placementForPlay, config.getRequestUniqueId(), adConfig);
    if (mBannerRequest == null) {
      // Adapter does not support multiple Banner instances playing for same placement except for
      // Refresh
      mMediationBannerListener.onAdFailedToLoad(
          VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    mBannerRequest.setAdLayout(adLayout);
    mBannerRequest.setVungleListener(mVungleBannerListener);

    Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
    mBannerRequest.requestBannerAd(context, config.getAppId());
  }

  private VungleListener mVungleBannerListener =
      new VungleListener() {
        @Override
        void onAdClick(String placementId) {
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
            mMediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
          }
        }

        @Override
        void onAdEnd(String placementId) {
          //no op
        }

        @Override
        void onAdLeftApplication(String placementId) {
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdLeftApplication(VungleInterstitialAdapter.this);
          }
        }

        @Override
        void onAdClosed(String placementId) {
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdClosed(VungleInterstitialAdapter.this);
          }
        }

        @Override
        void onAdAvailable() {
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
          }
        }

        @Override
        void onAdStart(String placement) {
          // let's load it again to mimic auto-cache, don't care about errors
          if (mBannerRequest != null) {
            mBannerRequest.preCache();
          }
        }

        @Override
        void onAdFail(String placement) {
          Log.w(TAG, "Ad playback error Placement: " + placement + ";" + mBannerRequest);
        }

        @Override
        void onAdFailedToLoad(int errorCode) {
          Log.w(TAG, "Failed to load ad from Vungle: " + errorCode + ";" + mBannerRequest);
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, errorCode);
          }
        }
      };

  @Override
  public View getBannerView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return adLayout;
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
