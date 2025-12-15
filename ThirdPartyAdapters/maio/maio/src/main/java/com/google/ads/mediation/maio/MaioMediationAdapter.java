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

package com.google.ads.mediation.maio;

import android.content.Context;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.VersionInfo;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager;
import jp.maio.sdk.android.v2.banner.MaioBannerView;

public class MaioMediationAdapter extends Adapter {

  public static final String TAG = MaioMediationAdapter.class.getSimpleName();

  private MaioInterstitialAd interstitialAd;
  private MaioRewardedAd rewardedAd;
  private MaioBannerAd bannerAd;

  /**
   * Maio adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.maio";

  /**
   * Maio sdk error domain.
   */
  public static final String MAIO_SDK_ERROR_DOMAIN = "jp.maio.sdk.android";

  @IntDef(value = {ERROR_AD_NOT_AVAILABLE,
      ERROR_INVALID_SERVER_PARAMETERS,
  })

  @Retention(RetentionPolicy.SOURCE)
  public @interface AdapterError {

  }

  @NonNull
  public static AdError getAdError(int reason) {
    //     0: unknown
    // 10100: noNetwork
    // 10200: networkTimeout
    // 10300: abortedDownload
    // 10400: invalidResponse
    // 10500: zoneNotFound
    // 10600: unavailableZone
    // 10700: noFill
    // 10800: nullArgMaioRequest
    // 10900: diskSpaceNotEnough
    // 11000: unsupportedOsVer
    // 20100: expired
    // 20200: notReadyYet
    // 20300: alreadyShown
    // 20400: failedPlayback
    // 20500: nullArgViewContext
    return new AdError(reason, "Failed to request ad from Maio: " + reason, MAIO_SDK_ERROR_DOMAIN);
  }

  /**
   * Maio does not have an ad available.
   */
  public static final int ERROR_AD_NOT_AVAILABLE = 101;

  /**
   * Invalid or missing server parameters.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 102;

  /**
   * Activity context is required.
   */
  // Commented out since adapter no longer reports this error code. But, leaving it as comment for
  // reference.
  // public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /**
   * {@link Adapter} implementation
   */
  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = MaioUtils.getVersionInfo();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = MaioAdsManager.getSdkVersion().toString();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {
    // maio does not have an initialization API.
    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    interstitialAd = new MaioInterstitialAd(callback);
    interstitialAd.loadAd(mediationInterstitialAdConfiguration);
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    rewardedAd = new MaioRewardedAd(mediationAdLoadCallback);
    rewardedAd.loadAd(mediationRewardedAdConfiguration);
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          callback) {
    bannerAd = new MaioBannerAd(callback);
    bannerAd.loadAd(adConfiguration);
  }
}