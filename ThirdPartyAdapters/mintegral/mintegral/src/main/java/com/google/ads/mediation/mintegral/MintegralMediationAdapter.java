// Copyright 2022 Google LLC
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

package com.google.ads.mediation.mintegral;

import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.mintegral.MintegralConstants.createSdkError;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.rtb.MintegralRtbAppOpenAd;
import com.google.ads.mediation.mintegral.rtb.MintegralRtbBannerAd;
import com.google.ads.mediation.mintegral.rtb.MintegralRtbInterstitialAd;
import com.google.ads.mediation.mintegral.rtb.MintegralRtbNativeAd;
import com.google.ads.mediation.mintegral.rtb.MintegralRtbRewardedAd;
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallAppOpenAd;
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallBannerAd;
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallInterstitialAd;
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallNativeAd;
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallRewardedAd;
import com.google.android.gms.ads.AdError;
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
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.foundation.same.net.Aa;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.SDKInitStatusListener;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MintegralMediationAdapter extends RtbAdapter {

  public static final String TAG = MintegralMediationAdapter.class.getSimpleName();
  private static MBridgeSDK mBridgeSDK;
  private MintegralWaterfallBannerAd mintegralWaterfallBannerAd;
  private MintegralWaterfallInterstitialAd mintegralInterstitialAd;
  private MintegralWaterfallRewardedAd mintegralRewardedAd;
  private MintegralWaterfallNativeAd mintegralNativeAd;
  private MintegralWaterfallAppOpenAd mintegralWaterfallAppOpenAd;
  private MintegralRtbBannerAd mintegralRtbBannerAd;
  private MintegralRtbInterstitialAd mintegralRtbInterstitialAd;
  private MintegralRtbRewardedAd mintegralRtbRewardedAd;
  private MintegralRtbNativeAd mintegralRtbNativeAd;
  private MintegralRtbAppOpenAd mintegralRtbAppOpenAd;

  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    String buyerUid = BidManager.getBuyerUid(rtbSignalData.getContext());
    signalCallbacks.onSuccess(buyerUid);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    // Mintegral SDK returns the SDK version in "MAL_x.y.z" format.
    String versionString = MintegralUtils.getSdkVersion();
    String[] versionSplits = versionString.split("_");
    if (versionSplits.length > 1) {
      String[] splits = versionSplits[1].split("\\.");

      if (splits.length >= 3) {
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
      }
    }

    String logMessage = String.format(
        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = MintegralUtils.getAdapterVersion();
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
  public void initialize(@NonNull Context context,
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> list) {
    HashSet<String> appIds = new HashSet<>();
    HashSet<String> appKeys = new HashSet<>();
    for (MediationConfiguration mediationConfiguration : list) {
      Bundle serverParameters = mediationConfiguration.getServerParameters();
      String appId = serverParameters.getString(MintegralConstants.APP_ID);
      String appKey = serverParameters.getString(MintegralConstants.APP_KEY);
      if (!TextUtils.isEmpty(appId)) {
        appIds.add(appId);
      }
      if (!TextUtils.isEmpty(appKey)) {
        appKeys.add(appKey);
      }
    }
    int appIdCount = appIds.size();
    int appKeyCount = appKeys.size();
    if (appIdCount <= 0 || appKeyCount <= 0) {
      AdError error = MintegralConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid App ID or App Key"
              + " configured for this ad source instance in the AdMob or Ad Manager UI");
      Log.e(TAG, error.toString());
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }
    String appId = appIds.iterator().next();
    String appKey = appKeys.iterator().next();
    if (appIdCount > 1) {
      String message = String.format(
          "Found multiple app IDs in %s. Using %s to initialize Mintegral SDK.", appIds, appId);
      Log.w(TAG, message);
    }
    if (appKeyCount > 1) {
      String message = String.format(
          "Found multiple App Keys in %s. Using %s to initialize Mintegral SDK.", appKeys, appKey);
      Log.w(TAG, message);
    }
    mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK();
    Map<String, String> configurationMap = mBridgeSDK.getMBConfigurationMap(appId, appKey);
    // Mintegral sdk modified the channel acquisition plan and needs to adjust the setting time to
    // before init
    try {
      Aa channelManagerInstance = new Aa();
      Class channelManagerClass = channelManagerInstance.getClass();
      Method setChannelMethod = channelManagerClass.getDeclaredMethod("b", String.class);
      setChannelMethod.setAccessible(true);
      // Set the channel flag to "Y+H6DFttYrPQYcIBiQKwJQKQYrN=" to indicate an AdMob channel.
      setChannelMethod.invoke(channelManagerInstance, "Y+H6DFttYrPQYcIBiQKwJQKQYrN=");
    } catch (Throwable e) {
      e.printStackTrace();
    }
    mBridgeSDK.init(configurationMap, context, new SDKInitStatusListener() {
      @Override
      public void onInitSuccess() {
        initializationCompleteCallback.onInitializationSucceeded();
      }

      @Override
      public void onInitFail(String errorMessage) {
        AdError initError = createSdkError(MintegralConstants.ERROR_CODE_SDK_INIT_FAILED,
            errorMessage);
        initializationCompleteCallback.onInitializationFailed(initError.getMessage());
        Log.w(TAG, initError.toString());
      }
    });

  }

  @Override
  public void loadRtbBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    mintegralRtbBannerAd = new MintegralRtbBannerAd(adConfiguration, callback);
    mintegralRtbBannerAd.loadAd();
  }

  @Override
  public void loadRtbInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    mintegralRtbInterstitialAd = new MintegralRtbInterstitialAd(adConfiguration, callback);
    mintegralRtbInterstitialAd.loadAd();
  }

  @Override
  public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    mintegralRtbNativeAd = new MintegralRtbNativeAd(adConfiguration, callback);
    mintegralRtbNativeAd.loadAd();
  }

  @Override
  public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    mintegralRtbRewardedAd = new MintegralRtbRewardedAd(adConfiguration, callback);
    mintegralRtbRewardedAd.loadAd();
  }

  @Override
  public void loadRtbAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback) {
    mintegralRtbAppOpenAd =
        new MintegralRtbAppOpenAd(mediationAppOpenAdConfiguration, callback);
    mintegralRtbAppOpenAd.loadAd();
  }

  @Override
  public void loadBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    mintegralWaterfallBannerAd = new MintegralWaterfallBannerAd(adConfiguration, callback);
    mintegralWaterfallBannerAd.loadAd();
  }

  @Override
  public void loadInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    mintegralInterstitialAd = new MintegralWaterfallInterstitialAd(adConfiguration, callback);
    mintegralInterstitialAd.loadAd();
  }

  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    mintegralNativeAd = new MintegralWaterfallNativeAd(adConfiguration, callback);
    mintegralNativeAd.loadAd();
  }

  @Override
  public void loadRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    mintegralRewardedAd = new MintegralWaterfallRewardedAd(adConfiguration, callback);
    mintegralRewardedAd.loadAd();
  }

  @Override
  public void loadAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback) {
    mintegralWaterfallAppOpenAd =
        new MintegralWaterfallAppOpenAd(mediationAppOpenAdConfiguration, callback);
    mintegralWaterfallAppOpenAd.loadAd();
  }
}
