// Copyright 2022 Google Inc.
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

package com.google.ads.mediation.chartboost;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.ads.Interstitial;
import com.chartboost.sdk.callbacks.BannerCallback;
import com.chartboost.sdk.callbacks.InterstitialCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

/**
 * The {@link ChartboostAdapter} class is used to load Chartboost rewarded-based video &
 * interstitial ads and mediate the callbacks between Chartboost SDK and Google Mobile Ads SDK.
 */
@Keep
public class ChartboostAdapter extends ChartboostMediationAdapter
    implements MediationInterstitialAdapter, MediationBannerAdapter {

  static final String TAG = ChartboostAdapter.class.getSimpleName();

  /**
   * Mediation interstitial listener used to forward interstitial ad events from Chartboost SDK to
   * Google Mobile Ads SDK.
   */
  private MediationInterstitialListener mMediationInterstitialListener;

  /**
   * Mediation banner listener used to forward banner ad events from Chartboost SDK to Google Mobile
   * Ads SDK.
   */
  private MediationBannerListener mMediationBannerListener;

  /**
   * A Chartboost extras object used to store optional information used when loading ads.
   */
  private ChartboostParams mChartboostParams = new ChartboostParams();

  /**
   * Interstitial object {@link Interstitial} Chartboost ad object can hold only one ad per
   * location. Once cached it will be loaded until shown or cleared.
   */
  private Interstitial mChartboostInterstitial;

  /**
   * Banner object {@link Banner}
   */
  private Banner mChartboostBanner;

  /**
   * FrameLayout use as a {@link Banner} container.
   */
  private FrameLayout mBannerContainer;

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull final MediationInterstitialListener mediationInterstitialListener,
      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle networkExtras) {
    mMediationInterstitialListener = mediationInterstitialListener;
    mChartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters, networkExtras);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      AdError loadError = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Invalid server parameters.",
          ERROR_DOMAIN);
      Log.e(TAG, loadError.toString());
      mediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this, loadError);
      return;
    }

    ChartboostInitializer.getInstance()
        .updateCoppaStatus(context, mediationAdRequest.taggedForChildDirectedTreatment());
    ChartboostInitializer.getInstance()
        .init(context, mChartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            if (mChartboostParams == null) {
              onAdFailedWithInvalidServerParameters();
              return;
            }
            createAndLoadInterstitialAd(mChartboostParams.getLocation());
          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            Log.w(TAG, error.getMessage());
            mediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this, error);
          }
        });
  }

  @Override
  public void showInterstitial() {
    if (mChartboostInterstitial == null || !mChartboostInterstitial.isCached()) {
      AdError error = new AdError(ERROR_AD_NOT_READY,
          "Chartboost Interstitial ad is not yet ready to be shown.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      return;
    }
    mChartboostInterstitial.show();
  }

  @Override
  public void onDestroy() {
    if (mChartboostBanner != null) {
      mChartboostBanner.detach();
      mChartboostBanner = null;
    }
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull MediationBannerListener mediationBannerListener, @NonNull Bundle serverParameters,
      @NonNull AdSize adSize, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle networkExtras) {
    mMediationBannerListener = mediationBannerListener;
    mChartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters, networkExtras);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      onAdFailedWithInvalidServerParameters();
      return;
    }

    Banner.BannerSize supportedAdSize = ChartboostAdapterUtils.findClosestBannerSize(context,
        adSize);
    if (supportedAdSize == null) {
      String errorMessage = String.format("Unsupported size: %s", adSize);
      AdError sizeError = new AdError(ERROR_BANNER_SIZE_MISMATCH, errorMessage, ERROR_DOMAIN);
      Log.e(TAG, sizeError.toString());
      mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, sizeError);
      return;
    }

    mChartboostParams.setBannerSize(supportedAdSize);
    ChartboostInitializer.getInstance()
        .updateCoppaStatus(context, mediationAdRequest.taggedForChildDirectedTreatment());
    ChartboostInitializer.getInstance()
        .init(context, mChartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            if (mChartboostParams == null) {
              onAdFailedWithInvalidServerParameters();
              return;
            }
            createAndLoadBannerAd(context, mChartboostParams.getLocation(),
                mChartboostParams.getBannerSize());
          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            Log.w(TAG, error.getMessage());
            if (mMediationBannerListener != null) {
              mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, error);
            }
          }
        });
  }

  @NonNull
  @Override
  public View getBannerView() {
    return mBannerContainer;
  }

  private void onAdFailedWithInvalidServerParameters() {
    AdError loadError = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Invalid server parameters.",
        ERROR_DOMAIN);
    Log.e(TAG, loadError.toString());
    if (mMediationBannerListener != null) {
      mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, loadError);
    }
  }

  private void createAndLoadInterstitialAd(@Nullable String location) {
    if (TextUtils.isEmpty(location)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid location.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this, error);
      }
      return;
    }

    mChartboostInterstitial = new Interstitial(location, new InterstitialCallback() {

      @Override
      public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
        Log.d(TAG, "Chartboost interstitial ad has been dismissed.");
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener.onAdClosed(ChartboostAdapter.this);
        }
      }

      @Override
      public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
        Log.d(TAG, "Chartboost interstitial ad impression recorded.");
      }

      @Override
      public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
        if (showError == null) {
          Log.d(TAG, "Chartboost interstitial has been shown.");
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdOpened(ChartboostAdapter.this);
          }
        } else {
          AdError error = ChartboostAdapterUtils.createSDKError(showError);
          Log.w(TAG, error.getMessage());
        }
      }

      @Override
      public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
        Log.d(TAG, "Chartboost interstitial ad will be shown.");
      }

      @Override
      public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
        if (cacheError == null) {
          Log.d(TAG, "Chartboost interstitial ad has been loaded.");
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdLoaded(ChartboostAdapter.this);
          }
        } else {
          AdError error = ChartboostAdapterUtils.createSDKError(cacheError);
          Log.w(TAG, error.getMessage());
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this, error);
          }
        }
      }

      @Override
      public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
        if (clickError == null) {
          Log.d(TAG, "Chartboost interstitial ad has been clicked.");
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdClicked(ChartboostAdapter.this);
          }
        } else {
          AdError error = ChartboostAdapterUtils.createSDKError(clickError);
          Log.w(TAG, error.getMessage());
        }
      }
    }, ChartboostAdapterUtils.getChartboostMediation());
    mChartboostInterstitial.cache();
  }

  private void createAndLoadBannerAd(@NonNull Context context,
      @Nullable String location,
      @Nullable Banner.BannerSize mediationAdSize) {
    if (TextUtils.isEmpty(location) || mediationAdSize == null) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid location.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, error);
      return;
    }

    // Attach object to layout to inflate the banner.
    mBannerContainer = new FrameLayout(context);
    FrameLayout.LayoutParams paramsLayout =
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
    paramsLayout.gravity = Gravity.CENTER_HORIZONTAL;

    mChartboostBanner =
        new Banner(context, location, mediationAdSize, new BannerCallback() {
          @Override
          public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
            if (cacheError == null) {
              Log.d(TAG, "Chartboost banner has been loaded.");
              mMediationBannerListener.onAdLoaded(ChartboostAdapter.this);
              cacheEvent.getAd().show();
            } else {
              AdError error = ChartboostAdapterUtils.createSDKError(cacheError);
              Log.w(TAG, error.getMessage());
              mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, error);
            }
          }

          @Override
          public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
            Log.d(TAG, "Chartboost banner requested to show.");
          }

          @Override
          public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
            if (showError == null) {
              Log.d(TAG, "Chartboost banner opened a full screen view.");
              mMediationBannerListener.onAdOpened(ChartboostAdapter.this);
            } else {
              AdError error = ChartboostAdapterUtils.createSDKError(showError);
              Log.w(TAG, error.getMessage());
            }
          }

          @Override
          public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
            if (clickError == null) {
              Log.d(TAG, "Chartboost banner has been clicked.");
              mMediationBannerListener.onAdClicked(ChartboostAdapter.this);
            } else {
              AdError error = ChartboostAdapterUtils.createSDKError(clickError);
              Log.w(TAG, error.getMessage());
            }
          }

          @Override
          public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
            Log.d(TAG, "Chartboost banner recorded impression.");
          }
        }, ChartboostAdapterUtils.getChartboostMediation());
    mBannerContainer.addView(mChartboostBanner, paramsLayout);
    mChartboostBanner.cache();
  }

  /**
   * The {@link com.google.ads.mediation.chartboost.ChartboostAdapter
   * .ChartboostExtrasBundleBuilder} class is used to create a networkExtras bundle which can be
   * passed to the adapter to make network specific customizations.
   */
  public static final class ChartboostExtrasBundleBuilder {

    public Bundle build() {
      return new Bundle();
    }
  }
}
