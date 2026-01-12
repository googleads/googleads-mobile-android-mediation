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

import static com.applovin.mediation.AppLovinUtils.ERROR_MSG_CHILD_USER;
import static com.applovin.mediation.AppLovinUtils.getChildUserError;
import static com.applovin.mediation.AppLovinUtils.isChildUser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
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
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;
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

  private final AppLovinSdkWrapper appLovinSdkWrapper;

  private final AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper;

  /**
   * Applovin adapter errors.
   */
  // AppLovin adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.applovin";

  // AppLovin SDK error domain.
  public static final String APPLOVIN_SDK_ERROR_DOMAIN = "com.applovin.sdk";

  private static final String TAG = AppLovinMediationAdapter.class.getSimpleName();

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
        ERROR_BANNER_SIZE_MISMATCH,
        ERROR_EMPTY_BID_TOKEN,
        ERROR_AD_ALREADY_REQUESTED,
        ERROR_PRESENTATION_AD_NOT_READY,
        ERROR_AD_FORMAT_UNSUPPORTED,
        ERROR_MISSING_SDK_KEY,
        ERROR_CHILD_USER
      })
  public @interface AdapterError {}

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

  /** Ad is not ready to display. */
  public static final int ERROR_PRESENTATION_AD_NOT_READY = 106;

  /**
   * Adapter does not support the ad format being requested.
   */
  public static final int ERROR_AD_FORMAT_UNSUPPORTED = 108;

  /** Error code for missing SDK key. */
  public static final int ERROR_MISSING_SDK_KEY = 110;

  /**
   * User is a child.
   *
   * <p>Shouldn't call AppLovin SDK if the user is a child. Adapter will respond with this error
   * code if adapter is requested to load ad or collect signals when the user is a child.
   *
   * <p>Starting with AppLovin SDK 13.0.0, AppLovin no longer supports child user flags and you may
   * not initialize or use the AppLovin SDK in connection with a "child" as defined under applicable
   * laws. For more information, see AppLovin's documentation on <a
   * href="https://developers.applovin.com/en/max/android/overview/privacy/#children">Prohibition on
   * Children's Data or Using the Services for Children or Apps Exclusively Targeted to
   * Children</a>.
   */
  public static final int ERROR_CHILD_USER = 112;

  /** Error code for missing ad unit ID. */
  public static final int ERROR_MISSING_AD_UNIT_ID = 113;

  static final String ERROR_MSG_MISSING_SDK = "Missing or invalid SDK Key.";

  @VisibleForTesting
  static final String ERROR_MSG_BANNER_SIZE_MISMATCH =
      "Failed to request banner with unsupported size.";

  public AppLovinMediationAdapter() {
    appLovinInitializer = AppLovinInitializer.getInstance();
    appLovinAdFactory = new AppLovinAdFactory();
    appLovinSdkWrapper = new AppLovinSdkWrapper();
    appLovinSdkUtilsWrapper = new AppLovinSdkUtilsWrapper();
  }

  @VisibleForTesting
  AppLovinMediationAdapter(
      AppLovinInitializer appLovinInitializer,
      AppLovinAdFactory appLovinAdFactory,
      AppLovinSdkWrapper appLovinSdkWrapper,
      AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper) {
    this.appLovinInitializer = appLovinInitializer;
    this.appLovinAdFactory = appLovinAdFactory;
    this.appLovinSdkWrapper = appLovinSdkWrapper;
    this.appLovinSdkUtilsWrapper = appLovinSdkUtilsWrapper;
  }

  /**
   * Called to initialize or get the existing instance of the {@link AppLovinSdkSettings} sent to
   * the AppLovin Sdk.
   *
   * <p>Used to configure AppLovin SDK with specific requirements.
   */
  @NonNull
  public static AppLovinSdkSettings getSdkSettings(@NonNull Context context) {
    return AppLovinSdk.getInstance(context).getSettings();
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {
    if (isChildUser()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_CHILD_USER);
      return;
    }

    final HashSet<String> sdkKeys = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      String sdkKey = configuration.getServerParameters().getString(ServerParameterKeys.SDK_KEY);
      if (!TextUtils.isEmpty(sdkKey)) {
        sdkKeys.add(sdkKey);
      }
    }

    if (sdkKeys.isEmpty()) {
      AdError error = new AdError(ERROR_MISSING_SDK_KEY, ERROR_MSG_MISSING_SDK, ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    String sdkKey = sdkKeys.iterator().next();

    if (sdkKeys.size() > 1) {
      Log.w(
          TAG,
          String.format(
              "Found more than one AppLovin SDK key. Using %s. Please update your app's ad unit"
                  + " mappings on Admob/GAM UI to use a single SDK key for ad serving to work as"
                  + " expected.",
              sdkKey));
    }

    appLovinInitializer.initialize(
        context,
        sdkKey,
        new OnInitializeSuccessListener() {
          @Override
          public void onInitializeSuccess() {
            initializationCompleteCallback.onInitializationSucceeded();
          }
        });
  }

  @Override
  @NonNull
  public VersionInfo getVersionInfo() {
    return getVersionInfo(BuildConfig.ADAPTER_VERSION);
  }

  @NonNull
  @VisibleForTesting
  VersionInfo getVersionInfo(String versionString) {
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

  @Override
  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String versionString = appLovinSdkWrapper.getSdkVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int patch = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, patch);
    }

    String logMessage = String.format(
        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    if (isChildUser()) {
      signalCallbacks.onFailure(getChildUserError());
      return;
    }

    final List<MediationConfiguration> configs = rtbSignalData.getConfigurations();

    if (!configs.isEmpty()) {
      // Check if supported ad format. Note: Even if there are multiple configs, they will all have
      // the same format. So, just checking the first one works.
      if (configs.get(0).getFormat() == AdFormat.NATIVE) {
        AdError error =
            new AdError(
                ERROR_AD_FORMAT_UNSUPPORTED,
                "Requested to collect signal for unsupported native ad format. Ignoring...",
                ERROR_DOMAIN);
        Log.e(TAG, error.getMessage());
        signalCallbacks.onFailure(error);
        return;
      }
    }

    // Check if the publisher provided extra parameters
    Log.i(TAG, "Extras for signal collection: " + rtbSignalData.getNetworkExtras());
    AppLovinSdk sdk = appLovinInitializer.retrieveSdk(rtbSignalData.getContext());
    String bidToken = sdk.getAdService().getBidToken();

    if (TextUtils.isEmpty(bidToken)) {
      AdError error = new AdError(ERROR_EMPTY_BID_TOKEN, "Failed to generate bid token.",
          ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      signalCallbacks.onFailure(error);
      return;
    }

    Log.i(TAG, "Generated bid token: " + bidToken);
    signalCallbacks.onSuccess(bidToken);
  }

  @Override
  public void loadAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback) {
    if (isChildUser()) {
      callback.onFailure(getChildUserError());
      return;
    }

    AppLovinWaterfallAppOpenAd appOpenAd =
        new AppLovinWaterfallAppOpenAd(callback, appLovinInitializer, appLovinAdFactory);
    appOpenAd.loadAd(mediationAppOpenAdConfiguration);
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    if (isChildUser()) {
      callback.onFailure(getChildUserError());
      return;
    }

    bannerAd =
        AppLovinBannerAd.newInstance(
            callback, appLovinInitializer, appLovinAdFactory);
    bannerAd.loadAd(adConfiguration);
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    if (isChildUser()) {
      callback.onFailure(getChildUserError());
      return;
    }

    waterfallInterstitialAd =
        new AppLovinWaterfallInterstitialAd(
            callback, appLovinInitializer, appLovinAdFactory);
    waterfallInterstitialAd.loadAd(adConfiguration);
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    if (isChildUser()) {
      callback.onFailure(getChildUserError());
      return;
    }

    rtbInterstitialRenderer =
        new AppLovinRtbInterstitialRenderer(
            callback, appLovinInitializer, appLovinAdFactory);
    rtbInterstitialRenderer.loadAd(adConfiguration);
  }

  @Override
  public void loadRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    if (isChildUser()) {
      callback.onFailure(getChildUserError());
      return;
    }

    rewardedRenderer =
        new AppLovinWaterfallRewardedRenderer(callback, appLovinInitializer, appLovinAdFactory, appLovinSdkUtilsWrapper);
    rewardedRenderer.loadAd(adConfiguration);
  }

  @Override
  public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    if (isChildUser()) {
      callback.onFailure(getChildUserError());
      return;
    }

    rtbRewardedRenderer =
        new AppLovinRtbRewardedRenderer(callback, appLovinInitializer, appLovinAdFactory, appLovinSdkUtilsWrapper);
    rtbRewardedRenderer.loadAd(adConfiguration);
  }
}
