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

package com.google.ads.mediation.fyber;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.fyber.inneractive.sdk.external.BidTokenProvider;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AgeRestrictedTreatment;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
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
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.NativeAdMapper;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DT Exchange's official AdMob 3rd party adapter class. Implements Interstitials by implementing
 * the {@link MediationInterstitialAdapter} interface. Implements initialization and other ad
 * formats by extending the {@link RtbAdapter} class.
 */
public class FyberMediationAdapter extends RtbAdapter {

  /** Adapter class name for logging. */
  static final String TAG = FyberMediationAdapter.class.getSimpleName();

  /** DT Exchange requires to know the host mediation platform. */
  protected static final InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

  /** Key to obtain App id, required for initializing DT Exchange's SDK. */
  static final String KEY_APP_ID = "applicationId";

  /** Key to obtain a placement name or spot id. Required for creating a DT Exchange ad request. */
  static final String KEY_SPOT_ID = "spotId";

  /** Key to obtain the mute video state, which enables the publisher to mute interstitial ads */
  public static final String KEY_MUTE_VIDEO = "muteVideo";

  /** DT Exchange banner ad for sdk bidding. */
  private DTExchangeRtbBannerAd bannerRtbAd;

  /** DT Exchange interstitial ad for sdk bidding */
  private DTExchangeRtbInterstitialAd interstitialRtbAd;

  /** DT Exchange rewarded ad video renderer. */
  private FyberRewardedVideoRenderer rewardedRenderer;

  private DTExchangeNativeAdMapper nativeAdMapper;

  /** Default Constructor. */
  public FyberMediationAdapter() {}

  /** Only rewarded ads are implemented using the new Adapter interface. */
  public void loadRewardedAd(
      @NonNull final MediationRewardedAdConfiguration configuration,
      @NonNull
          final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              callback) {
    // Sometimes loadRewardedAd is called before initialize.
    String keyAppID = configuration.getServerParameters().getString(KEY_APP_ID);
    if (TextUtils.isEmpty(keyAppID)) {
      AdError error =
          new AdError(
              DTExchangeErrorCodes.ERROR_INVALID_SERVER_PARAMETERS,
              "App ID is null or empty.",
              DTExchangeErrorCodes.ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    InneractiveAdManager.setMediationName(MEDIATOR_NAME);
    InneractiveAdManager.setMediationVersion(MobileAds.getVersion().toString());
    InneractiveAdManager.initialize(
        configuration.getContext(),
        keyAppID,
        new OnFyberMarketplaceInitializedListener() {
          @Override
          public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
            if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
              AdError error = DTExchangeErrorCodes.getAdError(fyberInitStatus);
              Log.w(TAG, error.getMessage());
              callback.onFailure(error);
              return;
            }
            rewardedRenderer = new FyberRewardedVideoRenderer(callback);
            rewardedRenderer.loadWaterfallAd(configuration);
          }
        });
  }

  @Override
  public void initialize(
      @NonNull Context context,
      @NonNull final InitializationCompleteCallback completionCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {
    // Initialize only once.
    if (FyberSdkWrapper.getDelegate().isInitialized()) {
      completionCallback.onInitializationSucceeded();
      return;
    }

    Set<String> configuredAppIds = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appId = serverParameters.getString(KEY_APP_ID);
      if (!TextUtils.isEmpty(appId)) {
        configuredAppIds.add(appId);
      }
    }

    if (configuredAppIds.isEmpty()) {
      AdError error =
          new AdError(
              DTExchangeErrorCodes.ERROR_INVALID_SERVER_PARAMETERS,
              "DT Exchange SDK requires an appId to be configured on the AdMob UI.",
              DTExchangeErrorCodes.ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      completionCallback.onInitializationFailed(error.getMessage());
      return;
    }

    // We can only use one app id to initialize the DT Exchange SDK.
    String appIdForInitialization = configuredAppIds.iterator().next();
    if (configuredAppIds.size() > 1) {
      String logMessage =
          String.format(
              "Multiple '%s' entries found: %s. " + "Using '%s' to initialize the DT Exchange SDK.",
              KEY_APP_ID, configuredAppIds, appIdForInitialization);
      Log.w(TAG, logMessage);
    }

    configureDTExchangePrivacy();

    InneractiveAdManager.initialize(
        context,
        appIdForInitialization,
        new OnFyberMarketplaceInitializedListener() {
          @Override
          public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
            if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
              AdError error = DTExchangeErrorCodes.getAdError(fyberInitStatus);
              Log.w(TAG, error.getMessage());
              completionCallback.onInitializationFailed(error.getMessage());
              return;
            }
            completionCallback.onInitializationSucceeded();
          }
        });
  }

  @Override
  public void collectSignals(
      @NonNull RtbSignalData rtbSignalData, @NonNull SignalCallbacks signalCallbacks) {
    String bidToken = BidTokenProvider.getBidderToken();
    if (TextUtils.isEmpty(bidToken)) {
      bidToken = "";
    }
    signalCallbacks.onSuccess(bidToken);
  }

  @NonNull
  public VersionInfo getVersionInfo() {
    String versionString = FyberAdapterUtils.getAdapterVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected adapter version format: %s. " + "Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String sdkVersion = FyberAdapterUtils.getSdkVersion();
    String[] splits = sdkVersion.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected SDK version format: %s. " + "Returning 0.0.0 for SDK version.", sdkVersion);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    DTExchangeWaterfallBannerAd bannerWaterfallAd = new DTExchangeWaterfallBannerAd(callback);
    bannerWaterfallAd.loadAd(adConfiguration);
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              adLoadCallback) {
    DTExchangeWaterfallInterstitialAd waterfallInterstitialAd =
        new DTExchangeWaterfallInterstitialAd();
    waterfallInterstitialAd.loadAd(mediationInterstitialAdConfiguration, adLoadCallback);
  }

  @Override
  public void loadRtbBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    bannerRtbAd = new DTExchangeRtbBannerAd(callback);
    bannerRtbAd.loadAd(adConfiguration);
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    interstitialRtbAd = new DTExchangeRtbInterstitialAd(callback);
    interstitialRtbAd.loadAd(adConfiguration);
  }

  @Override
  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    rewardedRenderer = new FyberRewardedVideoRenderer(callback);
    InneractiveAdManager.setMediationName(MEDIATOR_NAME);
    InneractiveAdManager.setMediationVersion(MobileAds.getVersion().toString());
    rewardedRenderer.loadRtbAd(adConfiguration);
  }

  @Override
  public void loadRtbNativeAdMapper(
      @NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> callback) {
    if (nativeAdMapper != null) {
      nativeAdMapper.destroy();
      nativeAdMapper = null;
    }
    nativeAdMapper = new DTExchangeNativeAdMapper(callback);
    nativeAdMapper.loadAd(adConfiguration);
  }

  private void configureDTExchangePrivacy() {
    RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration();
    boolean isChildUser =
        (requestConfiguration.getTagForChildDirectedTreatment()
                == TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            || requestConfiguration.getTagForUnderAgeOfConsent()
                == TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
            || requestConfiguration.getAgeRestrictedTreatment() == AgeRestrictedTreatment.CHILD;
    if (isChildUser) {
      InneractiveAdManager.currentAudienceAppliesToCoppa();
    }
  }
}
