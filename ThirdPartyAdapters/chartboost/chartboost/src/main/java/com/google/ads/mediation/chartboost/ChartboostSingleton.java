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

import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_DOMAIN;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Libraries.CBLogging;
import com.chartboost.sdk.Model.CBError;
import com.google.android.gms.ads.AdError;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * The {@link ChartboostSingleton} class is used to load Chartboost ads and manage multiple {@link
 * ChartboostAdapter} instances.
 */
public final class ChartboostSingleton {

  /**
   * Synchronized HashMaps of {@link AbstractChartboostAdapterDelegate} weak references keyed by
   * their Chartboost location.
   */
  private static HashMap<String, WeakReference<AbstractChartboostAdapterDelegate>>
      mInterstitialDelegates = new HashMap<>();
  private static HashMap<String, WeakReference<AbstractChartboostAdapterDelegate>>
      mRewardedDelegates = new HashMap<>();
  private static HashMap<String, WeakReference<AbstractChartboostAdapterDelegate>>
      mBannerDelegates = new HashMap<>();

  /**
   * Flag to keep track of whether or not {@link Chartboost} has initialized.
   */
  private static boolean mIsChartboostInitialized;

  /**
   * Flag to keep track of whether or not {@link Chartboost} is in progress of initializing.
   */
  private static boolean mIsChartboostInitializing;

  /**
   * The only instance of {@link ChartboostSingletonDelegate}.
   */
  private static ChartboostSingletonDelegate chartboostSingletonDelegate;

  /**
   * This method will return an instance of {@link ChartboostSingletonDelegate}.
   *
   * @return the only instance of {@link ChartboostSingletonDelegate}.
   */
  private static ChartboostSingletonDelegate getInstance() {
    if (chartboostSingletonDelegate == null) {
      chartboostSingletonDelegate = new ChartboostSingletonDelegate();
    }
    return chartboostSingletonDelegate;
  }

  /**
   * Adds the given {@link AbstractChartboostAdapterDelegate} to the map of Chartboost locations.
   *
   * @param delegate the delegate that needs to be added to {@link #mInterstitialDelegates}.
   */
  private static void addInterstitialDelegate(
      String location, AbstractChartboostAdapterDelegate delegate) {
    if (!TextUtils.isEmpty(location) && delegate != null) {
      mInterstitialDelegates.put(location, new WeakReference<>(delegate));
    }
  }

  /**
   * Adds the given {@link AbstractChartboostAdapterDelegate} to the map of Chartboost locations.
   *
   * @param delegate the delegate that needs to be added to {@link #mRewardedDelegates}.
   */
  private static void addRewardedDelegate(
      String location, AbstractChartboostAdapterDelegate delegate) {
    if (!TextUtils.isEmpty(location) && delegate != null) {
      mRewardedDelegates.put(location, new WeakReference<>(delegate));
    }
  }

  /**
   * Adds the given {@link AbstractChartboostAdapterDelegate} to the map of Chartboost locations.
   *
   * @param delegate the delegate that needs to be added to {@link #mBannerDelegates}.
   */
  private static void addBannerDelegate(
      String location, AbstractChartboostAdapterDelegate delegate) {
    if (!TextUtils.isEmpty(location) && delegate != null) {
      mBannerDelegates.put(location, new WeakReference<>(delegate));
    }
  }

  /**
   * Gets the {@link AbstractChartboostAdapterDelegate} linked to a given Chartboost location.
   *
   * @param location the Chartboost location.
   * @return the interstitial delegate for the location.
   */
  @Nullable
  private static AbstractChartboostAdapterDelegate getInterstitialDelegate(
      @NonNull String location) {
    if (!TextUtils.isEmpty(location) && mInterstitialDelegates.containsKey(location)) {
      return mInterstitialDelegates.get(location).get();
    }
    return null;
  }

  /**
   * Gets the {@link AbstractChartboostAdapterDelegate} linked to a given Chartboost location.
   *
   * @param location the Chartboost location.
   * @return the rewarded delegate for the location.
   */
  @Nullable
  private static AbstractChartboostAdapterDelegate getRewardedDelegate(String location) {
    if (!TextUtils.isEmpty(location) && mRewardedDelegates.containsKey(location)) {
      return mRewardedDelegates.get(location).get();
    }
    return null;
  }

  /**
   * Gets the {@link AbstractChartboostAdapterDelegate} linked to a given Chartboost location.
   *
   * @param location location the Chartboost location.
   * @return the banner delegate for the location.
   */
  @Nullable
  private static AbstractChartboostAdapterDelegate getBannerDelegate(String location) {
    if (!TextUtils.isEmpty(location) && mBannerDelegates.containsKey(location)) {
      return mBannerDelegates.get(location).get();
    }
    return null;
  }

  /**
   * Removes the specified {@link AbstractChartboostAdapterDelegate} from the map of Chartboost
   * locations.
   *
   * @param bannerDelegate the banner delegate.
   */
  static void removeBannerDelegate(@NonNull AbstractChartboostAdapterDelegate bannerDelegate) {
    String location = bannerDelegate.getChartboostParams().getLocation();
    if (TextUtils.isEmpty(location) || !mBannerDelegates.containsKey(location)) {
      return;
    }

    if (bannerDelegate.equals(mBannerDelegates.get(location).get())) {
      mBannerDelegates.remove(location);
    }
  }

  /**
   * This method will initialize Chartboost SDK for interstitial ads.
   *
   * @param context         Required to initialize Chartboost SDK.
   * @param adapterDelegate The adapter delegate to which to forward Chartboost callbacks.
   */
  public static void startChartboostInterstitial(
      Context context, AbstractChartboostAdapterDelegate adapterDelegate) {
    String location = adapterDelegate.getChartboostParams().getLocation();

    // Checks if an ad has already been sent for caching for the requested location, and fail
    // the ad request if it is.
    AbstractChartboostAdapterDelegate delegate = getInterstitialDelegate(location);
    if (delegate != null) {
      String errorMessage = String
          .format("An ad has already been requested for the location: %s.", location);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
      adapterDelegate.onAdFailedToLoad(concurrentError);
      return;
    }

    // Add this adapter delegate to mAdapterDelegates so that the events from
    // Chartboost SDK can be forwarded.
    addInterstitialDelegate(location, adapterDelegate);
    startChartboost(context, adapterDelegate.getChartboostParams(), adapterDelegate);
  }

  /**
   * This method will initialize Chartboost SDK for rewarded video ads.
   *
   * @param context         Required to initialize Chartboost SDK.
   * @param adapterDelegate The adapter delegate to which to forward Chartboost callbacks.
   */
  public static void startChartboostRewardedVideo(
      Context context, AbstractChartboostAdapterDelegate adapterDelegate) {
    String location = adapterDelegate.getChartboostParams().getLocation();

    // Checks if an ad has already been sent for caching for the requested location, and fail
    // the ad request if it is.
    AbstractChartboostAdapterDelegate delegate = getRewardedDelegate(location);
    if (delegate != null) {
      String errorMessage = String
          .format("An ad has already been requested for the location: %s.", location);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
      adapterDelegate.onAdFailedToLoad(concurrentError);
      return;
    }

    // Add this adapter delegate to mAdapterDelegates so that the events from
    // Chartboost SDK can be forwarded.
    addRewardedDelegate(location, adapterDelegate);
    startChartboost(context, adapterDelegate.getChartboostParams(), adapterDelegate);
  }

  /**
   * This method will initialize Chartboost SDK for banner ads.
   *
   * @param context         Required to initialize Chartboost SDK.
   * @param adapterDelegate The adapter delegate to which to forward Chartboost callbacks.
   */
  public static void startChartboostBanner(
      Context context, AbstractChartboostAdapterDelegate adapterDelegate) {
    String location = adapterDelegate.getChartboostParams().getLocation();

    // Checks if an ad has already been sent for caching for the requested location, and fail
    // the ad request if it is.
    AbstractChartboostAdapterDelegate delegate = getBannerDelegate(location);
    if (delegate != null) {
      String errorMessage = String
          .format("An ad has already been requested for the location: %s.", location);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
      adapterDelegate.onAdFailedToLoad(concurrentError);
      return;
    }

    addBannerDelegate(location, adapterDelegate);
    startChartboost(context, adapterDelegate.getChartboostParams(), adapterDelegate);
  }

  /**
   * This method will initialize the Chartboost SDK if it is not already initialized and set its
   * delegate.
   *
   * @param context         required to initialize {@link Chartboost}.
   * @param params          The Chartboost params containing server parameters and network extras to
   *                        be used to start {@link Chartboost}.
   * @param adapterDelegate The adapter delegate to which to forward initialization callbacks.
   */
  private static void startChartboost(
      Context context, ChartboostParams params, AbstractChartboostAdapterDelegate adapterDelegate) {
    if (params.getFramework() != null && !TextUtils.isEmpty(params.getFrameworkVersion())) {
      Chartboost.setFramework(params.getFramework(), params.getFrameworkVersion());
    }

    if (mIsChartboostInitializing) {
      return;
    }

    if (mIsChartboostInitialized) {
      adapterDelegate.didInitialize();
      return;
    }

    mIsChartboostInitializing = true;
    Chartboost.setDelegate(getInstance());
    Chartboost.startWithAppId(context, params.getAppId(), params.getAppSignature());
    Chartboost.setMediation(
        Chartboost.CBMediation.CBMediationAdMob,
        Chartboost.getSDKVersion(),
        com.google.ads.mediation.chartboost.BuildConfig.ADAPTER_VERSION);
    Chartboost.setLoggingLevel(CBLogging.Level.INTEGRATION);
    Chartboost.setAutoCacheAds(false);
  }

  /**
   * This method will check if {@link Chartboost} already has ads loaded, if so sends the necessary
   * callbacks, else requests {@link Chartboost} to cache interstitial ads.
   *
   * @param delegate Required to obtain the location for which to load the ad and to forward the
   *                 events.
   */
  protected static void loadInterstitialAd(AbstractChartboostAdapterDelegate delegate) {
    // Get the location for which the ads need to be loaded.
    String location = delegate.getChartboostParams().getLocation();
    if (Chartboost.hasInterstitial(location)) {
      // Interstitial ad already cached and is ready to be shown, send onAdLoaded event to
      // the adapter.
      delegate.didCacheInterstitial(location);
    } else {
      // Ad not cached for mLocation yet, request Chartboost to cache interstitial ads for
      // the given location.
      Chartboost.cacheInterstitial(location);
    }
  }

  /**
   * This method will show an interstitial ad.
   *
   * @param delegate that will be showing the ad, needed to set the current showing adapter's
   *                 delegate.
   */
  protected static void showInterstitialAd(AbstractChartboostAdapterDelegate delegate) {
    // Displays a cached interstitial if available, else loads from server.
    Chartboost.showInterstitial(delegate.getChartboostParams().getLocation());
  }

  /**
   * This method will check if {@link Chartboost} already has rewarded video ads loaded, if so, will
   * send the necessary callbacks, else will request {@link Chartboost} to start caching rewarded
   * video ads.
   *
   * @param delegate needed to get the location for which to load Chartboost ads and to forward
   *                 necessary events.
   */
  protected static void loadRewardedVideoAd(AbstractChartboostAdapterDelegate delegate) {
    // Get the location for which the ads need to be loaded.
    String location = delegate.getChartboostParams().getLocation();
    if (Chartboost.hasRewardedVideo(location)) {
      // Video ad already cached and ready to show, send onAdLoaded event to the adapter.
      delegate.didCacheRewardedVideo(location);
    } else {
      // Ad not cached for mLocation yet, request Chartboost to cache rewarded video for
      // the given location.
      Chartboost.cacheRewardedVideo(location);
    }
  }

  /**
   * This method will show rewarded video ad.
   *
   * @param delegate needed to set the currently showing ad and get the location for which to show
   *                 ads.
   */
  protected static void showRewardedVideoAd(AbstractChartboostAdapterDelegate delegate) {
    // Displays a cached video if available, else loads from server.
    Chartboost.showRewardedVideo(delegate.getChartboostParams().getLocation());
  }

  /**
   * The {@link ChartboostSingletonDelegate} class is used to forward events from Chartboost SDK to
   * Google Mobile Ads SDK for adapters based on which adapters are currently loading ads and which
   * adapter is currently displaying ad.
   */
  private static final class ChartboostSingletonDelegate extends ChartboostDelegate {

    @Override
    public void didInitialize() {
      // Chartboost SDK has been successfully initialized.
      // Rewarded video pre-fetching has been completed.
      super.didInitialize();
      mIsChartboostInitializing = false;
      mIsChartboostInitialized = true;

      for (WeakReference<AbstractChartboostAdapterDelegate> reference :
          mInterstitialDelegates.values()) {
        if (reference.get() != null) {
          reference.get().didInitialize();
        }
      }

      for (WeakReference<AbstractChartboostAdapterDelegate> reference :
          mRewardedDelegates.values()) {
        if (reference.get() != null) {
          reference.get().didInitialize();
        }
      }

      for (WeakReference<AbstractChartboostAdapterDelegate> reference : mBannerDelegates.values()) {
        if (reference.get() != null) {
          reference.get().didInitialize();
        }
      }
    }

    @Override
    public void didCacheInterstitial(String location) {
      // Interstitial ad has been loaded from the Chartboost API servers and cached locally.
      super.didCacheInterstitial(location);

      AbstractChartboostAdapterDelegate delegate = getInterstitialDelegate(location);
      if (delegate != null) {
        delegate.didCacheInterstitial(location);
      }
    }

    @Override
    public void didFailToLoadInterstitial(String location, CBError.CBImpressionError error) {
      // Interstitial ad has attempted to load from the Chartboost API servers but failed.
      super.didFailToLoadInterstitial(location, error);

      AbstractChartboostAdapterDelegate delegate = getInterstitialDelegate(location);
      if (delegate != null) {
        delegate.didFailToLoadInterstitial(location, error);
      }
      mInterstitialDelegates.remove(location);
    }

    @Override
    public void didDisplayInterstitial(String location) {
      // Interstitial ad has been displayed on the screen.
      super.didDisplayInterstitial(location);

      AbstractChartboostAdapterDelegate delegate = getInterstitialDelegate(location);
      if (delegate != null) {
        delegate.didDisplayInterstitial(location);
      }
    }

    @Override
    public void didDismissInterstitial(String location) {
      // Interstitial ad has been dismissed.
      super.didDismissInterstitial(location);

      AbstractChartboostAdapterDelegate reference = getInterstitialDelegate(location);
      if (reference != null) {
        reference.didDismissInterstitial(location);
      }
      mInterstitialDelegates.remove(location);
    }

    @Override
    public void didClickInterstitial(String location) {
      // Interstitial ad has been clicked.
      super.didClickInterstitial(location);

      AbstractChartboostAdapterDelegate reference = getInterstitialDelegate(location);
      if (reference != null) {
        reference.didClickInterstitial(location);
      }
    }

    @Override
    public void didCompleteInterstitial(String location) {
      super.didCompleteInterstitial(location);
      // No relevant callback to be forwarded to the Google Mobile Ads SDK.
    }

    @Override
    public void didCacheRewardedVideo(String location) {
      // Rewarded video has been loaded from the Chartboost API servers and cached locally.
      super.didCacheRewardedVideo(location);

      AbstractChartboostAdapterDelegate delegate = getRewardedDelegate(location);
      if (delegate != null) {
        delegate.didCacheRewardedVideo(location);
      }
    }

    @Override
    public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
      super.didFailToLoadRewardedVideo(location, error);

      AbstractChartboostAdapterDelegate delegate = getRewardedDelegate(location);
      if (delegate != null) {
        delegate.didFailToLoadRewardedVideo(location, error);
      }
      mRewardedDelegates.remove(location);
    }

    @Override
    public void didClickRewardedVideo(String location) {
      // Rewarded video has been clicked.
      super.didClickRewardedVideo(location);

      AbstractChartboostAdapterDelegate delegate = getRewardedDelegate(location);
      if (delegate != null) {
        delegate.didClickRewardedVideo(location);
      }
    }

    @Override
    public void didDisplayRewardedVideo(String location) {
      // Rewarded video has been displayed on the screen.
      super.didDisplayRewardedVideo(location);

      AbstractChartboostAdapterDelegate delegate = getRewardedDelegate(location);
      if (delegate != null) {
        delegate.didDisplayRewardedVideo(location);
      }
    }

    @Override
    public void didCompleteRewardedVideo(String location, int reward) {
      // Rewarded video has been viewed completely and user is eligible for reward.
      super.didCompleteRewardedVideo(location, reward);

      AbstractChartboostAdapterDelegate delegate = getRewardedDelegate(location);
      if (delegate != null) {
        delegate.didCompleteRewardedVideo(location, reward);
      }
    }

    @Override
    public void didDismissRewardedVideo(String location) {
      // Rewarded video has been dismissed.
      super.didDismissRewardedVideo(location);

      AbstractChartboostAdapterDelegate delegate = getRewardedDelegate(location);
      if (delegate != null) {
        delegate.didDismissRewardedVideo(location);
      }
      mRewardedDelegates.remove(location);
    }
  }
}
