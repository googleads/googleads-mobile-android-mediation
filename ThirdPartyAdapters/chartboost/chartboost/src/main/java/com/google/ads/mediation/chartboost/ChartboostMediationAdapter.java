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

import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_DOMAIN;
import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Chartboost;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
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
import com.google.android.gms.ads.mediation.VersionInfo;
import java.util.HashMap;
import java.util.List;

/**
 * The {@link ChartboostMediationAdapter} class is used to load Chartboost rewarded-based video,
 * interstitial ads, banner ads and initialise Chartboost SDK.
 */
public class ChartboostMediationAdapter extends Adapter {

  static final String TAG = ChartboostMediationAdapter.class.getSimpleName();

  private ChartboostBannerAd bannerAd;
  private ChartboostInterstitialAd interstitialAd;
  private ChartboostRewardedAd rewardedAd;

  /**
   * Preferred Chartboost App ID.
   */
  @Nullable
  private static String preferredAppID;

  /**
   * Preferred Chartboost App Signature.
   */
  @Nullable
  private static String preferredAppSignature;

  /**
   * {@link Adapter} implementation
   */
  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = Chartboost.getSDKVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    ChartboostParams chartboostParams = null;

    // Initialize with the preferred parameters if set.
    if (!TextUtils.isEmpty(preferredAppID) && !TextUtils.isEmpty(preferredAppSignature)) {
      String logMessage = String.format("Preferred parameters have been set. "
              + "Initializing Chartboost SDK with App ID: '%s', App Signature: '%s'",
          preferredAppID, preferredAppSignature);
      Log.d(TAG, logMessage);

      chartboostParams = new ChartboostParams();
      chartboostParams.setAppId(preferredAppID);
      chartboostParams.setAppSignature(preferredAppSignature);
    } else {
      HashMap<String, Bundle> chartboostConfigs = new HashMap<>();
      for (MediationConfiguration configuration : mediationConfigurations) {
        Bundle serverParameters = configuration.getServerParameters();
        String appId = serverParameters.getString(ChartboostAdapterUtils.KEY_APP_ID);

        if (!TextUtils.isEmpty(appId)) {
          chartboostConfigs.put(appId, serverParameters);
        }
      }

      int count = chartboostConfigs.size();
      if (count <= 0) {
        AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
            "Missing or invalid App ID.", ERROR_DOMAIN);
        initializationCompleteCallback.onInitializationFailed(initializationError.toString());
        Log.e(TAG, initializationError.toString());
        return;
      }

      String appId = chartboostConfigs.keySet().iterator().next();
      Bundle serverParameters = chartboostConfigs.get(appId);

      // Multiple app IDs are not considered an error.
      if (count > 1) {
        String logMessage =
            String.format(
                "Multiple '%s' entries found: %s. Using '%s' to initialize the Chartboost SDK.",
                ChartboostAdapterUtils.KEY_APP_ID, chartboostConfigs.keySet(), appId);
        Log.w(TAG, logMessage);
      }

      if (serverParameters == null) {
        // Invalid server parameters, send initialization failed event.
        AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
            "Invalid server parameters.", ERROR_DOMAIN);
        initializationCompleteCallback.onInitializationFailed(initializationError.toString());
        Log.e(TAG, initializationError.toString());
        return;
      }

    /*
      A Chartboost extras object used to store optional information used when loading ads.
     */
      chartboostParams = ChartboostAdapterUtils.createChartboostParams(
          serverParameters);
    }

    if (!ChartboostAdapterUtils.isValidChartboostParams(chartboostParams)) {
      // Invalid server parameters, send initialization failed event.
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Invalid server parameters.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      Log.e(TAG, initializationError.toString());
      return;
    }

    ChartboostInitializer.getInstance()
        .init(context, chartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            initializationCompleteCallback.onInitializationSucceeded();

          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            initializationCompleteCallback.onInitializationFailed(error.getMessage());
          }
        });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    // Callback listener
    rewardedAd = new ChartboostRewardedAd(
        mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    rewardedAd.loadAd();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    interstitialAd = new ChartboostInterstitialAd(
        mediationInterstitialAdConfiguration,
        mediationAdLoadCallback);
    interstitialAd.loadAd();
  }

  @Override
  public void loadBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    bannerAd = new ChartboostBannerAd(
        mediationBannerAdConfiguration,
        mediationAdLoadCallback);
    bannerAd.loadAd();
  }

  // region Public utility methods

  /**
   * Indicates which Chartboost App ID and App Signature to use to initialize the Chartboost SDK.
   *
   * @param appId        Chartboost App ID
   * @param appSignature Chartboost App Signature
   */
  public static void setAppParams(@NonNull String appId, @NonNull String appSignature) {
    preferredAppID = appId;
    preferredAppSignature = appSignature;
  }
  // endregion
}
