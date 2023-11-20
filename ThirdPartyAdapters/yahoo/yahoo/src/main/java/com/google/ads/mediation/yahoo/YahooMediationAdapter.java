// Copyright 2023 Google LLC
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

package com.google.ads.mediation.yahoo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.yahoo.ads.ActivityStateManager;
import com.yahoo.ads.YASAds;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

public class YahooMediationAdapter extends Adapter implements MediationBannerAdapter,
    MediationInterstitialAdapter, MediationNativeAdapter {

  public static final String TAG = YahooMediationAdapter.class.getSimpleName();

  // region Error codes
  // Yahoo Mobile adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.yahoo";

  // Yahoo Mobile SDK error domain.
  public static final String YAHOO_MOBILE_SDK_ERROR_DOMAIN = "com.yahoo.ads";

  /**
   * Yahoo Mobile adapter errors.
   */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {
      ERROR_INVALID_SERVER_PARAMETERS,
      ERROR_REQUIRES_ACTIVITY_CONTEXT,
      ERROR_INITIALIZATION_ERROR,
      ERROR_BANNER_SIZE_MISMATCH,
      ERROR_FAILED_TO_LOAD_NATIVE_ASSETS,
      ERROR_AD_NOT_READY_TO_SHOW,
  })
  public @interface AdapterError {

  }

  /**
   * Server parameters, such as Site ID or Placement ID, are invalid.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * Yahoo Mobile SDK requires an {@link Activity} to initialize.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 102;

  /**
   * Yahoo Mobile SDK failed to initialize.
   */
  public static final int ERROR_INITIALIZATION_ERROR = 103;

  /**
   * The requested ad size does not match a Yahoo Mobile SDK supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 104;

  /**
   * Failed to load the native ad media view and/or icon assets.
   */
  public static final int ERROR_FAILED_TO_LOAD_NATIVE_ASSETS = 105;

  /**
   * There are no ads to be shown.
   */
  public static final int ERROR_AD_NOT_READY_TO_SHOW = 106;
  // endregion

  /**
   * The pixel-to-dpi scale for images downloaded Yahoo Mobile SDK.
   */
  static final double YAS_IMAGE_SCALE = 1.0;

  /**
   * Weak reference of context.
   */
  private WeakReference<Context> contextWeakRef;

  /**
   * The Yahoo interstitial ad renderer.
   */
  private YahooInterstitialRenderer yahooInterstitialRenderer;

  /**
   * The Yahoo rewarded ad renderer.
   */
  private YahooRewardedRenderer yahooRewardedRenderer;

  /**
   * The Yahoo banner ad renderer.
   */
  private YahooBannerRenderer yahooBannerRenderer;

  /**
   * The Yahoo native ad renderer.
   */
  private YahooNativeRenderer yahooNativeRenderer;

  /** The Yahoo ad object factory. */
  private final YahooFactory yahooFactory;

  public YahooMediationAdapter() {
    this.yahooFactory = new YahooFactory();
  }

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = YahooAdapterUtils.getAdapterVersion();
    String[] splits = versionString.split("\\.");
    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format(
        "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
        versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = YahooAdapterUtils.getSDKVersionInfo();
    String[] splits = versionString.split("\\.");
    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format(
        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (YASAds.isInitialized()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    HashSet<String> siteIDs = new HashSet<>();
    for (MediationConfiguration mediationConfiguration : mediationConfigurations) {
      String siteID = YahooAdapterUtils.getSiteId(mediationConfiguration.getServerParameters(),
          (Bundle) null);
      if (!TextUtils.isEmpty(siteID)) {
        siteIDs.add(siteID);
      }
    }

    int count = siteIDs.size();
    if (count <= 0) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Site ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      initializationCompleteCallback.onInitializationFailed(parameterError.getMessage());
      return;
    }

    String siteID = siteIDs.iterator().next();
    if (count > 1) {
      String message = String.format("Multiple '%s' entries found: %s. " +
              "Using '%s' to initialize the Yahoo Mobile SDK.", YahooAdapterUtils.SITE_KEY,
          siteIDs, siteID);
      Log.w(TAG, message);
    }

    AdError initializationError = initializeYahooSDK(context, siteID);
    if (initializationError == null) {
      initializationCompleteCallback.onInitializationSucceeded();
    } else {
      Log.e(TAG, initializationError.toString());
      initializationCompleteCallback.onInitializationFailed(initializationError.getMessage());
    }
  }

  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull final MediationBannerListener listener, @NonNull final Bundle serverParameters,
      @NonNull AdSize adSize, @NonNull final MediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    yahooBannerRenderer = new YahooBannerRenderer(YahooMediationAdapter.this, yahooFactory);
    yahooBannerRenderer.render(context, listener, serverParameters, adSize, mediationAdRequest,
        mediationExtras);
  }

  @NonNull
  @Override
  public View getBannerView() {
    return yahooBannerRenderer.getBannerView();
  }

  @Override
  public void requestInterstitialAd(@NonNull final Context context,
      @NonNull final MediationInterstitialListener listener, @NonNull final Bundle serverParameters,
      @NonNull final MediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    setContext(context);
    yahooInterstitialRenderer =
        new YahooInterstitialRenderer(YahooMediationAdapter.this, yahooFactory);
    yahooInterstitialRenderer.render(context, listener, mediationAdRequest, serverParameters,
        mediationExtras);
  }

  @Override
  public void showInterstitial() {
    Context context = getContext();
    if (context == null) {
      Log.w(TAG, "Failed to show: context is null");
      return;
    }
    yahooInterstitialRenderer.showInterstitial(context);
  }

  @Override
  public void loadRewardedAd(
      @NonNull final MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    yahooRewardedRenderer =
        new YahooRewardedRenderer(
            mediationRewardedAdConfiguration, mediationAdLoadCallback, yahooFactory);
    yahooRewardedRenderer.render();
  }

  @Override
  public void requestNativeAd(@NonNull final Context context,
      @NonNull final MediationNativeListener listener, @NonNull final Bundle serverParameters,
      @NonNull final NativeMediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    yahooNativeRenderer = new YahooNativeRenderer(YahooMediationAdapter.this, yahooFactory);
    yahooNativeRenderer.render(context, listener, serverParameters, mediationAdRequest,
        mediationExtras);
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "Aborting.");
    if (yahooInterstitialRenderer != null) {
      yahooInterstitialRenderer.destroy();
    }
    if (yahooBannerRenderer != null) {
      yahooBannerRenderer.destroy();
    }
    if (yahooNativeRenderer != null) {
      yahooNativeRenderer.destroy();
    }
    if (yahooRewardedRenderer != null) {
      yahooRewardedRenderer.destroy();
    }
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  /**
   * Checks whether Yahoo Mobile SDK is initialized, if not initializes Yahoo Mobile SDK.
   */
  @Nullable
  protected static AdError initializeYahooSDK(@NonNull final Context context,
      @NonNull final String siteId) {
    if (YASAds.isInitialized()) {
      return null;
    }

    if (!(context instanceof Activity)) {
      AdError contextError = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Yahoo Mobile SDK requires an Activity context to initialize.", ERROR_DOMAIN);
      return contextError;
    }
    Activity activity = (Activity) context;

    if (TextUtils.isEmpty(siteId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Site ID.", ERROR_DOMAIN);
      return parameterError;
    }

    Application application = activity.getApplication();
    Log.d(TAG, "Initializing using site ID: " + siteId);
    try {
      boolean success = YASAds.initialize(application, siteId);
      if (!success) {
        return new AdError(ERROR_INITIALIZATION_ERROR, "Yahoo Mobile SDK failed to initialize.",
            ERROR_DOMAIN);
      }
    } catch (Exception exception) {
      String errorMessage = String.format(
          "Exception thrown when initializing the Yahoo Mobile SDK: %s", exception.getMessage());
      AdError exceptionError = new AdError(ERROR_INITIALIZATION_ERROR, errorMessage, ERROR_DOMAIN);
      Log.w(TAG, "Exception thrown when initializing Yahoo Mobile SDK.", exception);
      return exceptionError;
    }

    YASAds.getActivityStateManager().setState(activity, ActivityStateManager.ActivityState.RESUMED);
    return null;
  }

  private void setContext(@NonNull Context context) {
    contextWeakRef = new WeakReference<>(context);
  }

  @Nullable
  private Context getContext() {
    if (contextWeakRef == null) {
      return null;
    }
    return contextWeakRef.get();
  }
}
