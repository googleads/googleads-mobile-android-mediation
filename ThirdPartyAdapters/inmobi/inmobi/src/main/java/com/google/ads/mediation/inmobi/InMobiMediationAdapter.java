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
package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.ads.mediation.inmobi.rtb.InMobiRtbBannerAd;
import com.google.ads.mediation.inmobi.rtb.InMobiRtbInterstitialAd;
import com.google.ads.mediation.inmobi.rtb.InMobiRtbNativeAd;
import com.google.ads.mediation.inmobi.rtb.InMobiRtbRewardedAd;
import com.google.ads.mediation.inmobi.waterfall.InMobiWaterfallBannerAd;
import com.google.ads.mediation.inmobi.waterfall.InMobiWaterfallInterstitialAd;
import com.google.ads.mediation.inmobi.waterfall.InMobiWaterfallNativeAd;
import com.google.ads.mediation.inmobi.waterfall.InMobiWaterfallRewardedAd;
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
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.inmobi.sdk.InMobiSdk;
import java.util.HashSet;
import java.util.List;

/**
 * InMobi Adapter for AdMob Mediation used to load and show rewarded video ads. This class should
 * not be used directly by publishers.
 */
public class InMobiMediationAdapter extends RtbAdapter {

  @VisibleForTesting
  public static final String ERROR_MESSAGE_FOR_INVALID_ACCOUNTID =
      "Missing or invalid Account ID, configured for this ad source instance in the AdMob or Ad"
          + " Manager UI";

  public static final String TAG = InMobiMediationAdapter.class.getSimpleName();

  private InMobiWaterfallRewardedAd inMobiWaterfallRewardedAd;

  private InMobiWaterfallBannerAd inMobiWaterfallBannerAd;

  private InMobiWaterfallInterstitialAd inMobiWaterfallInterstitialAd;

  private InMobiWaterfallNativeAd inMobiWaterfallNativeAd;

  private InMobiRtbRewardedAd inMobiRtbRewardedAd;

  private InMobiRtbBannerAd inMobiRtbBannerAd;

  private InMobiRtbInterstitialAd inMobiRtbInterstitialAd;

  private InMobiRtbNativeAd inMobiRtbNativeAd;

  private InMobiInitializer inMobiInitializer;

  private InMobiAdFactory inMobiAdFactory;

  private InMobiSdkWrapper inMobiSdkWrapper;

  /** {@link Adapter} implementation */
  @VisibleForTesting
  InMobiMediationAdapter(
      InMobiInitializer inMobiInitializer,
      InMobiAdFactory inMobiAdFactory,
      InMobiSdkWrapper inMobiSdkWrapper) {
    this.inMobiInitializer = inMobiInitializer;
    this.inMobiAdFactory = inMobiAdFactory;
    this.inMobiSdkWrapper = inMobiSdkWrapper;
  }

  InMobiMediationAdapter() {
    super();
    this.inMobiInitializer = InMobiInitializer.getInstance();
    this.inMobiAdFactory = new InMobiAdFactory();
    this.inMobiSdkWrapper = new InMobiSdkWrapper();
  }

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    return getVersionInfo(BuildConfig.ADAPTER_VERSION);
  }

  @VisibleForTesting
  @NonNull
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

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = inMobiSdkWrapper.getVersion();
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
      final @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (inMobiSdkWrapper.isSDKInitialized()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    HashSet<String> accountIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      String serverAccountID = configuration.getServerParameters()
          .getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);

      if (!TextUtils.isEmpty(serverAccountID)) {
        accountIDs.add(serverAccountID);
      }
    }

    int count = accountIDs.size();
    if (count <= 0) {
      AdError error = InMobiConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_MESSAGE_FOR_INVALID_ACCOUNTID);
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }

    String accountID = accountIDs.iterator().next();

    if (count > 1) {
      String message = String.format(
          "Multiple '%s' entries found: %s. " + "Using '%s' to initialize the InMobi SDK",
          InMobiAdapterUtils.KEY_ACCOUNT_ID, accountIDs, accountID);
      Log.w(TAG, message);
    }

    inMobiInitializer.init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        initializationCompleteCallback.onInitializationSucceeded();
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        initializationCompleteCallback.onInitializationFailed(error.toString());
      }
    });
  }

  @Override
  public void collectSignals(
      @NonNull RtbSignalData rtbSignalData, @NonNull SignalCallbacks signalCallbacks) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(rtbSignalData.getContext(),
            rtbSignalData.getNetworkExtras(), InMobiAdapterUtils.PROTOCOL_RTB);
    String token =
        inMobiSdkWrapper.getToken(inMobiExtras.getParameterMap(), inMobiExtras.getKeywords());
    signalCallbacks.onSuccess(token);
  }

  @Override
  public void loadRtbBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    inMobiRtbBannerAd =
        new InMobiRtbBannerAd(adConfiguration, callback, inMobiInitializer, inMobiAdFactory);
    inMobiRtbBannerAd.loadAd();
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    inMobiRtbInterstitialAd =
        new InMobiRtbInterstitialAd(adConfiguration, callback, inMobiInitializer, inMobiAdFactory);
    inMobiRtbInterstitialAd.loadAd();
  }

  @Override
  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    inMobiRtbRewardedAd =
        new InMobiRtbRewardedAd(adConfiguration, callback, inMobiInitializer, inMobiAdFactory);
    inMobiRtbRewardedAd.loadAd();
  }

  @Override
  public void loadRtbNativeAd(
      @NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    inMobiRtbNativeAd =
        new InMobiRtbNativeAd(adConfiguration, callback, inMobiInitializer, inMobiAdFactory);
    inMobiRtbNativeAd.loadAd();
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      final @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    inMobiWaterfallRewardedAd =
        new InMobiWaterfallRewardedAd(
            mediationRewardedAdConfiguration,
            mediationAdLoadCallback,
            inMobiInitializer,
            inMobiAdFactory);
    inMobiWaterfallRewardedAd.loadAd();
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    inMobiWaterfallBannerAd =
        new InMobiWaterfallBannerAd(
            mediationBannerAdConfiguration, callback, inMobiInitializer, inMobiAdFactory);
    inMobiWaterfallBannerAd.loadAd();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    inMobiWaterfallInterstitialAd =
        new InMobiWaterfallInterstitialAd(
            mediationInterstitialAdConfiguration, callback, inMobiInitializer, inMobiAdFactory);
    inMobiWaterfallInterstitialAd.loadAd();
  }

  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    inMobiWaterfallNativeAd =
        new InMobiWaterfallNativeAd(
            mediationNativeAdConfiguration, callback, inMobiInitializer, inMobiAdFactory);
    inMobiWaterfallNativeAd.loadAd();
  }

}
