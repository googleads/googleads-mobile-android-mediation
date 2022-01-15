// Copyright 2016 Google Inc.
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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Banner.BannerSize;
import com.chartboost.sdk.Chartboost.CBFramework;
import com.chartboost.sdk.ChartboostBanner;
import com.chartboost.sdk.ChartboostBannerListener;
import com.chartboost.sdk.Events.ChartboostCacheError;
import com.chartboost.sdk.Events.ChartboostCacheEvent;
import com.chartboost.sdk.Events.ChartboostClickError;
import com.chartboost.sdk.Events.ChartboostClickEvent;
import com.chartboost.sdk.Events.ChartboostShowError;
import com.chartboost.sdk.Events.ChartboostShowEvent;
import com.chartboost.sdk.Model.CBError;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@link ChartboostAdapter} class is used to load Chartboost rewarded-based video &
 * interstitial ads and mediate the callbacks between Chartboost SDK and Google Mobile Ads SDK.
 */
@Keep
public class ChartboostAdapter extends ChartboostMediationAdapter
    implements MediationInterstitialAdapter, MediationBannerAdapter {

  static final String TAG = ChartboostAdapter.class.getSimpleName();

  /**
   * Flag to keep track of whether or not this {@link ChartboostAdapter} is loading ads.
   */
  private boolean mIsLoading;

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
   * Banner object {@link ChartboostBanner}
   */
  private ChartboostBanner mChartboostBanner;

  /**
   * FrameLayout use as a {@link ChartboostBanner} container.
   */
  private FrameLayout mBannerContainer;

  /**
   * Context reference {@link Context}
   */
  private Context mContext;

  /**
   * Boolean which keeps track if onAdCached() was called to avoid forwarding load callbacks more
   * than once.
   */
  private final AtomicBoolean onAdCachedCalled = new AtomicBoolean(false);

  /**
   * The Abstract Chartboost adapter delegate used to forward events received from {@link
   * ChartboostSingleton} to Google Mobile Ads SDK for interstitial ads.
   */
  private final AbstractChartboostAdapterDelegate mChartboostInterstitialDelegate =
      new AbstractChartboostAdapterDelegate() {

        @Override
        public ChartboostParams getChartboostParams() {
          return mChartboostParams;
        }

        @Override
        public void didInitialize() {
          super.didInitialize();

          // Request ChartboostSingleton to load interstitial ads once the Chartboost SDK is
          // initialized.
          mIsLoading = true;
          ChartboostSingleton.loadInterstitialAd(mChartboostInterstitialDelegate);
        }

        @Override
        public void didCacheInterstitial(String location) {
          super.didCacheInterstitial(location);

          if (mMediationInterstitialListener != null
              && mIsLoading
              && location.equals(mChartboostParams.getLocation())) {
            mIsLoading = false;
            mMediationInterstitialListener.onAdLoaded(ChartboostAdapter.this);
          }
        }

        @Override
        public void didFailToLoadInterstitial(String location, CBError.CBImpressionError error) {
          super.didFailToLoadInterstitial(location, error);
          AdError loadError = ChartboostAdapterUtils.createSDKError(error);
          Log.i(TAG, loadError.toString());

          if (mMediationInterstitialListener != null
              && location.equals(mChartboostParams.getLocation())) {
            if (mIsLoading) {
              mIsLoading = false;
              mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this, loadError);
            } else if (error == CBError.CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW) {
              // Chartboost sends the CBErrorInternetUnavailableAtShow error when
              // the Chartboost SDK fails to show an ad because no network connection
              // is available.
              mMediationInterstitialListener.onAdOpened(ChartboostAdapter.this);
              mMediationInterstitialListener.onAdClosed(ChartboostAdapter.this);
            }
          }
        }

        @Override
        public void onAdFailedToLoad(@NonNull AdError loadError) {
          Log.e(TAG, loadError.toString());
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this, loadError);
          }
        }

        @Override
        public void didDismissInterstitial(String location) {
          super.didDismissInterstitial(location);
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdClosed(ChartboostAdapter.this);
          }
        }

        @Override
        public void didClickInterstitial(String location) {
          super.didClickInterstitial(location);
          if (mMediationInterstitialListener != null) {
            // Chartboost doesn't have a delegate method for when an ad left
            // application. Assuming that when an interstitial ad is clicked and the
            // user is taken out of the application to show a web page, we forward
            // the ad left application event to Google Mobile Ads SDK.
            mMediationInterstitialListener.onAdClicked(ChartboostAdapter.this);
            mMediationInterstitialListener.onAdLeftApplication(ChartboostAdapter.this);
          }
        }

        @Override
        public void didDisplayInterstitial(String location) {
          super.didDisplayInterstitial(location);
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdOpened(ChartboostAdapter.this);
          }
        }
      };

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener mediationInterstitialListener,
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
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this, loadError);
      }
      return;
    }

    ChartboostSingleton.startChartboostInterstitial(context, mChartboostInterstitialDelegate);
  }

  @Override
  public void showInterstitial() {
    // Request ChartboostSingleton to show interstitial ads.
    ChartboostSingleton.showInterstitialAd(mChartboostInterstitialDelegate);
  }

  @Override
  public void onDestroy() {
    if (mChartboostBanner != null) {
      ChartboostSingleton.removeBannerDelegate(mChartboostBannerDelegate);
      mChartboostBanner.detachBanner();
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
  public void requestBannerAd(@NonNull Context context,
      @NonNull MediationBannerListener mediationBannerListener, @NonNull Bundle serverParameters,
      @NonNull AdSize adSize, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle networkExtras) {
    mMediationBannerListener = mediationBannerListener;
    mContext = context;

    mChartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters, networkExtras);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      AdError loadError = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Invalid server parameters.",
          ERROR_DOMAIN);
      Log.e(TAG, loadError.toString());
      if (mMediationBannerListener != null) {
        mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, loadError);
      }
      return;
    }

    BannerSize supportedAdSize = ChartboostAdapterUtils.findClosestBannerSize(context, adSize);
    if (supportedAdSize == null) {
      String errorMessage = String.format("Unsupported size: %s", adSize.toString());
      AdError sizeError = new AdError(ERROR_BANNER_SIZE_MISMATCH, errorMessage, ERROR_DOMAIN);
      Log.e(TAG, sizeError.toString());
      mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, sizeError);
      return;
    }

    mChartboostParams.setBannerSize(supportedAdSize);
    ChartboostSingleton.startChartboostBanner(context, mChartboostBannerDelegate);
  }

  @NonNull
  @Override
  public View getBannerView() {
    return mBannerContainer;
  }

  private final AbstractChartboostAdapterDelegate mChartboostBannerDelegate =
      new AbstractChartboostAdapterDelegate() {
        @Override
        public ChartboostParams getChartboostParams() {
          return mChartboostParams;
        }

        @Override
        public void onAdFailedToLoad(@NonNull AdError loadError) {
          Log.e(TAG, loadError.toString());
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, loadError);
          }
        }

        @Override
        public void didInitialize() {
          super.didInitialize();

          String location = mChartboostParams.getLocation();
          // Attach object to layout to inflate the banner.
          mBannerContainer = new FrameLayout(mContext);
          FrameLayout.LayoutParams paramsLayout =
              new FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
          paramsLayout.gravity = Gravity.CENTER_HORIZONTAL;

          mChartboostBanner =
              new ChartboostBanner(
                  mContext, location, mChartboostParams.getBannerSize(), mChartboostBannerListener);
          mChartboostBanner.setAutomaticallyRefreshesContent(false);
          mBannerContainer.addView(mChartboostBanner, paramsLayout);
          mChartboostBanner.cache();
        }
      };

  private final ChartboostBannerListener mChartboostBannerListener =
      new ChartboostBannerListener() {
        @Override
        public void onAdCached(
            ChartboostCacheEvent chartboostCacheEvent, ChartboostCacheError chartboostCacheError) {
          // onAdCached() should only forward ad load events once per banner ad request.
          if (onAdCachedCalled.getAndSet(true) || mMediationBannerListener == null) {
            return;
          }

          if (chartboostCacheError != null) {
            AdError cacheError = ChartboostAdapterUtils.createSDKError(chartboostCacheError);
            Log.i(TAG, "Failed to load banner ad: " + cacheError.toString());
            mMediationBannerListener.onAdFailedToLoad(ChartboostAdapter.this, cacheError);
            ChartboostSingleton.removeBannerDelegate(mChartboostBannerDelegate);
            return;
          }

          mMediationBannerListener.onAdLoaded(ChartboostAdapter.this);
          mChartboostBanner.show();
        }

        @Override
        public void onAdShown(ChartboostShowEvent chartboostShowEvent,
            ChartboostShowError chartboostShowError) {
          if (chartboostShowError != null) {
            AdError showError = ChartboostAdapterUtils.createSDKError(chartboostShowError);
            Log.i(TAG, "Failed to show banner ad: " + showError.toString());
          }
        }

        @Override
        public void onAdClicked(ChartboostClickEvent chartboostClickEvent,
            ChartboostClickError chartboostClickError) {
          if (mMediationBannerListener == null) {
            return;
          }

          if (chartboostClickError != null) {
            AdError clickError = ChartboostAdapterUtils.createSDKError(chartboostClickError);
            Log.i(TAG, "Chartboost click event had an error: " + clickError.toString());
            return;
          }

          mMediationBannerListener.onAdClicked(ChartboostAdapter.this);
          mMediationBannerListener.onAdOpened(ChartboostAdapter.this);
          mMediationBannerListener.onAdLeftApplication(ChartboostAdapter.this);
        }
      };

  /**
   * The {@link com.google.ads.mediation.chartboost.ChartboostAdapter
   * .ChartboostExtrasBundleBuilder} class is used to create a networkExtras bundle which can be
   * passed to the adapter to make network specific customizations.
   */
  public static final class ChartboostExtrasBundleBuilder {

    /**
     * Key to add and obtain {@link #cbFramework}.
     */
    static final String KEY_FRAMEWORK = "framework";

    /**
     * Key to add and obtain {@link #cbFrameworkVersion}.
     */
    static final String KEY_FRAMEWORK_VERSION = "framework_version";

    /**
     * Framework being used to load Charboost ads.
     */
    private CBFramework cbFramework;

    /**
     * The version name of {@link #cbFramework}.
     */
    private String cbFrameworkVersion;

    public ChartboostExtrasBundleBuilder setFramework(CBFramework framework, String version) {
      this.cbFramework = framework;
      this.cbFrameworkVersion = version;
      return this;
    }

    public Bundle build() {
      Bundle bundle = new Bundle();
      bundle.putSerializable(KEY_FRAMEWORK, cbFramework);
      bundle.putString(KEY_FRAMEWORK_VERSION, cbFrameworkVersion);
      return bundle;
    }
  }
}
