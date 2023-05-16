// Copyright 2019 Google LLC
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

package com.google.ads.mediation.applovin;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static com.applovin.mediation.ApplovinAdapter.log;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.mediation.BuildConfig;
import com.applovin.mediation.rtb.AppLovinRtbInterstitialRenderer;
import com.applovin.mediation.rtb.AppLovinRtbRewardedRenderer;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class AppLovinMediationAdapter extends RtbAdapter {

  /**
   * AppLovin SDK settings.
   */
  @Nullable
  public static AppLovinSdkSettings appLovinSdkSettings;

  private AppLovinBannerAd bannerAd;

  private AppLovinWaterfallInterstitialAd waterfallInterstitialAd;

  /**
   * AppLovin bidding interstitial ad renderer.
   */
  private AppLovinRtbInterstitialRenderer rtbInterstitialRenderer;

  /**
   * AppLovin bidding rewarded ad renderer.
   */
  private AppLovinRtbRewardedRenderer rtbRewardedRenderer;

  /** AppLovin waterfall rewarded ad renderer. */
  private AppLovinWaterfallRewardedRenderer rewardedRenderer;

  /** AppLovinInitializer singleton instance. */
  private final AppLovinInitializer appLovinInitializer;

  private final AppLovinAdFactory appLovinAdFactory;
  
  private final AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper;

  /**
   * Applovin adapter errors.
   */
  // AppLovin adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.applovin";

  // AppLovin SDK error domain.
  public static final String APPLOVIN_SDK_ERROR_DOMAIN = "com.applovin.sdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {
      ERROR_BANNER_SIZE_MISMATCH,
      ERROR_EMPTY_BID_TOKEN,
      ERROR_AD_ALREADY_REQUESTED,
      ERROR_PRESENTATON_AD_NOT_READY,
      ERROR_AD_FORMAT_UNSUPPORTED,
      ERROR_INVALID_SERVER_PARAMETERS}
  )

  public @interface AdapterError {

  }

  /**
   * Banner size mismatch.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 101;

  /**
   * AppLovin bid token is empty.
   */
  public static final int ERROR_EMPTY_BID_TOKEN = 104;

  /**
   * Requested multiple ads for the same zone. AppLovin can only load 1 ad at a time per zone.
   */
  public static final int ERROR_AD_ALREADY_REQUESTED = 105;

  /**
   * Ad is not ready to display.
   */
  public static final int ERROR_PRESENTATON_AD_NOT_READY = 106;

  /**
   * Adapter does not support the ad format being requested.
   */
  public static final int ERROR_AD_FORMAT_UNSUPPORTED = 108;

  /**
   * Invalid server parameters (e.g. SDK key is null).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 110;

  @VisibleForTesting static final String ERROR_MSG_MISSING_SDK = "Missing or invalid SDK Key.";

  @VisibleForTesting
  static final String ERROR_MSG_BANNER_SIZE_MISMATCH =
      "Failed to request banner with unsupported size.";

  public AppLovinMediationAdapter() {
    appLovinInitializer = AppLovinInitializer.getInstance();
    appLovinAdFactory = new AppLovinAdFactory();
    appLovinSdkUtilsWrapper = new AppLovinSdkUtilsWrapper();
  }

  @VisibleForTesting
  AppLovinMediationAdapter(AppLovinInitializer appLovinInitializer, AppLovinAdFactory appLovinAdFactory, AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper) {
    this.appLovinInitializer = appLovinInitializer;
    this.appLovinAdFactory = appLovinAdFactory;
    this.appLovinSdkUtilsWrapper = appLovinSdkUtilsWrapper;
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    final HashSet<String> sdkKeys = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      String sdkKey = configuration.getServerParameters().getString(ServerParameterKeys.SDK_KEY);
      if (!TextUtils.isEmpty(sdkKey)) {
        sdkKeys.add(sdkKey);
      }
    }

    // Include the SDK key declared in the AndroidManifest.xml file.
    String manifestSdkKey = AppLovinUtils.retrieveSdkKey(context, null);
    if (!TextUtils.isEmpty(manifestSdkKey)) {
      sdkKeys.add(manifestSdkKey);
    }

    if (sdkKeys.isEmpty()) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid SDK Key.",
          ERROR_DOMAIN);
      log(WARN, error.getMessage());
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    // Keep track of the SDK keys that were used to initialize the AppLovin SDK. Once all of them
    // have been initialized, then the completion callback is invoked.
    final HashSet<String> initializedSdkKeys = new HashSet<>();

    for (String sdkKey : sdkKeys) {
      appLovinInitializer.initialize(
          context,
          sdkKey,
          new OnInitializeSuccessListener() {
            @Override
            public void onInitializeSuccess(@NonNull String sdkKey) {
              initializedSdkKeys.add(sdkKey);
              if (initializedSdkKeys.equals(sdkKeys)) {
                initializationCompleteCallback.onInitializationSucceeded();
              }
            }
          });
    }
  }

  @Override
  @NonNull
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
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
    log(WARN, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String versionString = AppLovinSdk.VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int patch = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, patch);
    }

    String logMessage = String.format(
        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    log(WARN, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    final MediationConfiguration config = rtbSignalData.getConfiguration();

    // Check if supported ad format
    if (config.getFormat() == AdFormat.NATIVE) {
      AdError error = new AdError(ERROR_AD_FORMAT_UNSUPPORTED,
          "Requested to collect signal for unsupported native ad format. Ignoring...",
          ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      signalCallbacks.onFailure(error);
      return;
    }

    // Check if the publisher provided extra parameters
    log(INFO, "Extras for signal collection: " + rtbSignalData.getNetworkExtras());
    AppLovinSdk sdk =
        appLovinInitializer.retrieveSdk(config.getServerParameters(), rtbSignalData.getContext());
    String bidToken = sdk.getAdService().getBidToken();

    if (TextUtils.isEmpty(bidToken)) {
      AdError error = new AdError(ERROR_EMPTY_BID_TOKEN, "Failed to generate bid token.",
          ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      signalCallbacks.onFailure(error);
      return;
    }

    log(INFO, "Generated bid token: " + bidToken);
    signalCallbacks.onSuccess(bidToken);
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    bannerAd =
        AppLovinBannerAd.newInstance(
            adConfiguration, callback, appLovinInitializer, appLovinAdFactory);
    bannerAd.loadAd();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    waterfallInterstitialAd =
        new AppLovinWaterfallInterstitialAd(
            adConfiguration, callback, appLovinInitializer, appLovinAdFactory);
    waterfallInterstitialAd.loadAd();
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    rtbInterstitialRenderer =
        new AppLovinRtbInterstitialRenderer(
            adConfiguration, callback, appLovinInitializer, appLovinAdFactory);
    rtbInterstitialRenderer.loadAd();
  }

  @Override
  public void loadRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    rewardedRenderer =
        new AppLovinWaterfallRewardedRenderer(adConfiguration, callback, appLovinInitializer, appLovinAdFactory, appLovinSdkUtilsWrapper);
    rewardedRenderer.loadAd();
  }

  @Override
  public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    rtbRewardedRenderer =
        new AppLovinRtbRewardedRenderer(adConfiguration, callback, appLovinInitializer, appLovinAdFactory, appLovinSdkUtilsWrapper);
    rtbRewardedRenderer.loadAd();
  }
}
