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

package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.AdError;
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
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;

/**
 * The {@link UnityMediationAdapter} is used to initialize the Unity Ads SDK, load rewarded video
 * ads from Unity Ads and mediate the callbacks between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class UnityMediationAdapter extends RtbAdapter {

  /**
   * TAG used for logging messages.
   */
  static final String TAG = UnityMediationAdapter.class.getSimpleName();

  // region Error Codes
  // Unity Ads adapter error domain.
  public static final String ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.unity";

  // Unity Ads SDK error domain.
  public static final String SDK_ERROR_DOMAIN = "com.unity3d.ads";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_PLACEMENT_STATE_NO_FILL,
          ERROR_PLACEMENT_STATE_DISABLED,
          ERROR_NULL_CONTEXT,
          ERROR_CONTEXT_NOT_ACTIVITY,
          ERROR_AD_NOT_READY,
          ERROR_UNITY_ADS_NOT_SUPPORTED,
          ERROR_FINISH,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_INITIALIZATION_FAILURE
      })
  @interface AdapterError {

  }

  /**
   * Invalid server parameters.
   */
  static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * UnityAds returned a placement with no fill.
   */
  static final int ERROR_PLACEMENT_STATE_NO_FILL = 102;

  /**
   * UnityAds returned a disabled placement.
   */
  static final int ERROR_PLACEMENT_STATE_DISABLED = 103;

  /**
   * Tried to show an ad with a {@code null} context.
   */
  static final int ERROR_NULL_CONTEXT = 104;

  /**
   * Tried to show an ad with a non-Activity context.
   */
  static final int ERROR_CONTEXT_NOT_ACTIVITY = 105;

  /**
   * Tried to show an ad that's not ready to be shown.
   */
  static final int ERROR_AD_NOT_READY = 106;

  /**
   * UnityAds is not supported on the device.
   */
  static final int ERROR_UNITY_ADS_NOT_SUPPORTED = 107;

  /**
   * UnityAds finished with an error state.
   */
  static final int ERROR_FINISH = 109;

  /**
   * The requested ad size does not match a UnityAds supported banner size.
   */
  static final int ERROR_BANNER_SIZE_MISMATCH = 110;

  /**
   * UnityAds returned an initialization error.
   */
  static final int ERROR_INITIALIZATION_FAILURE = 111;

  static final String ERROR_MSG_MISSING_PARAMETERS = "Missing or invalid server parameters.";

  static final String ERROR_MSG_NON_ACTIVITY =
      "Unity Ads requires an Activity context to load ads.";

  static final String ERROR_MSG_CONTEXT_NULL = "Activity context is null.";

  static final String ERROR_MSG_INITIALIZATION_FAILURE = "Unity Ads initialization failed: [%s] %s";

  /**
   * Key to obtain Game ID, required for loading Unity Ads.
   */
  static final String KEY_GAME_ID = "gameId";

  /**
   * Key to obtain Placement ID, used to set the type of ad to be shown. Unity Ads has changed the
   * name from Zone ID to Placement ID in Unity Ads SDK 2.0.0. To maintain backwards compatibility
   * the key is not changed.
   */
  static final String KEY_PLACEMENT_ID = "zoneId";

  static final String KEY_WATERMARK = "watermark";
  private final UnityInitializer unityInitializer;

  private final UnityBannerViewFactory unityBannerViewFactory;

  private final UnityAdsLoader unityAdsLoader;

  /** UnityBannerAd instance. */
  private UnityMediationBannerAd bannerAd;

  /** UnityInterstitialAd instance. */
  private UnityInterstitialAd interstitialAd;

  /** UnityInterstitialAd instance used for RTB. */
  private UnityInterstitialAd interstitialRtbAd;

  /**
   * UnityRewardedAd instance.
   */
  private UnityRewardedAd rewardedAd;

  /** UnityRewardedAd instance used for RTB. */
  private UnityRewardedAd rewardedRtbAd;

  public UnityMediationAdapter() {
    unityInitializer = UnityInitializer.getInstance();
    unityBannerViewFactory = new UnityBannerViewFactory();
    this.unityAdsLoader = new UnityAdsLoader();
  }

  @Override
  public void collectSignals(
      @NonNull RtbSignalData rtbSignalData, @NonNull SignalCallbacks signalCallbacks) {
    UnityAds.getToken(
        token -> {
          if (token == null) {
            token = "";
          }
          signalCallbacks.onSuccess(token);
        });
  }

  @VisibleForTesting
  UnityMediationAdapter(
      UnityInitializer unityInitializer,
      UnityBannerViewFactory unityBannerViewFactory,
      UnityAdsLoader unityAdsLoader) {
    this.unityInitializer = unityInitializer;
    this.unityBannerViewFactory = unityBannerViewFactory;
    this.unityAdsLoader = unityAdsLoader;
  }

  /** {@link RtbAdapter} implementation */
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

    String logMessage = String
        .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = UnityAds.getVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String
        .format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {
    HashSet<String> gameIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String gameIDFromServer = serverParameters.getString(KEY_GAME_ID);

      if (!TextUtils.isEmpty(gameIDFromServer)) {
        gameIDs.add(gameIDFromServer);
      }
    }

    String gameID = "";
    int count = gameIDs.size();
    if (count > 0) {
      gameID = gameIDs.iterator().next();

      if (count > 1) {
        String message = String
            .format("Multiple '%s' entries found: %s. Using '%s' to initialize the UnityAds SDK",
                KEY_GAME_ID, gameIDs, gameID);
        Log.w(TAG, message);
      }
    }

    if (TextUtils.isEmpty(gameID)) {
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Game ID.", ADAPTER_ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }

    unityInitializer.initializeUnityAds(
        context,
        gameID,
        new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            Log.d(TAG, "Unity Ads initialized successfully.");
            initializationCompleteCallback.onInitializationSucceeded();
          }

          @Override
          public void onInitializationFailed(
              UnityAds.UnityAdsInitializationError unityAdsInitializationError,
              String errorMessage) {
            AdError adError =
                createSDKError(
                    unityAdsInitializationError,
                    String.format(
                        ERROR_MSG_INITIALIZATION_FAILURE,
                        unityAdsInitializationError,
                        errorMessage));
            Log.d(TAG, adError.toString());
            initializationCompleteCallback.onInitializationFailed(adError.toString());
          }
        });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    rewardedAd =
        new UnityRewardedAd(
            mediationRewardedAdConfiguration,
            mediationAdLoadCallback,
            unityInitializer,
            unityAdsLoader);
    rewardedAd.loadAd();
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    bannerAd =
        new UnityMediationBannerAd(
            mediationBannerAdConfiguration, callback, unityInitializer, unityBannerViewFactory);
    bannerAd.loadAd();
  }

  @Override
  public void loadInterstitialAd(
      MediationInterstitialAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    interstitialAd =
        new UnityInterstitialAd(adConfiguration, callback, unityInitializer, unityAdsLoader);
    interstitialAd.loadAd();
  }

  @Override
  public final void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    interstitialRtbAd =
        new UnityInterstitialAd(adConfiguration, callback, unityInitializer, unityAdsLoader);
    interstitialRtbAd.loadAd();
  }

  @Override
  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    rewardedRtbAd =
        new UnityRewardedAd(adConfiguration, callback, unityInitializer, unityAdsLoader);
    rewardedRtbAd.loadAd();
  }
}
