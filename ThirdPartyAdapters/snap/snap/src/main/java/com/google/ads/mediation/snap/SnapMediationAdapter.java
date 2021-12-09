package com.google.ads.mediation.snap;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
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
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.snap.adkit.external.AdKitAudienceAdsNetwork;
import com.snap.adkit.external.AudienceNetworkAdsApi;
import com.snap.adkit.external.NetworkInitSettings;
import java.util.HashSet;
import java.util.List;

public class SnapMediationAdapter extends RtbAdapter {

  static final String TAG = SnapMediationAdapter.class.getSimpleName();

  // Snap SDK error domain.
  public static final String SNAP_AD_SDK_ERROR_DOMAIN = "com.snap.ads";

  public static final String APP_ID_PARAMETER = "snapAppId";

  public static final String SLOT_ID_KEY = "adSlotId";

  private SnapBannerAd bannerAd;
  private SnapInterstitialAd interstitialAd;
  private SnapRewardedAd rewardedAd;

  @Override
  public void collectSignals(
      @NonNull RtbSignalData rtbSignalData, @NonNull SignalCallbacks signalCallbacks) {
    String bidToken = AdKitAudienceAdsNetwork.getAdsNetwork().requestBidToken();
    if (TextUtils.isEmpty(bidToken)) {
      AdError error =
          new AdError(
              0, "Failed to generate bid token.", SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      signalCallbacks.onFailure(error);
      return;
    }

    signalCallbacks.onSuccess(bidToken);
  }

  @Override
  public void initialize(
      @NonNull Context context,
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> configurations) {
    if (context == null) {
      AdError error =
          new AdError(
              0,
              "Failed to initialize. Context is null.",
              SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    HashSet<String> appIds = new HashSet<>();
    for (MediationConfiguration configuration : configurations) {
      Bundle serverParameters = configuration.getServerParameters();

      String appIdConfig = getAppID(serverParameters);
      if (!TextUtils.isEmpty(appIdConfig)) {
        appIds.add(appIdConfig);
      }
    }

    if (appIds.isEmpty()) {
      AdError error =
          new AdError(
              0,
              "Initialization failed. Missing or invalid App ID.",
              SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    String appId = appIds.iterator().next();
    if (appIds.size() > 1) {
      String logMessage =
          String.format(
              "Multiple App IDs found: %s. Using '%s' to initialize the Snap SDK.",
              appIds.toString(), appId);
      Log.w(TAG, logMessage);
    }
    NetworkInitSettings initSettings =
        AdKitAudienceAdsNetwork.buildNetworkInitSettings(context).withAppId(appId).build();
    AudienceNetworkAdsApi adsNetworkApi = AdKitAudienceAdsNetwork.init(initSettings);
    if (adsNetworkApi == null) {
      AdError error =
          new AdError(
              0,
              "Initialization failed. Snap Audience Network failed to initialize.",
              SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    initializationCompleteCallback.onInitializationSucceeded();
  }

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
    String versionString = com.snap.adkit.BuildConfig.VERSION_NAME;
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
  public void loadRtbBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    bannerAd = new SnapBannerAd(adConfiguration, callback);
    bannerAd.loadAd();
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    interstitialAd = new SnapInterstitialAd(adConfiguration, callback);
    interstitialAd.loadAd();
  }

  @Override
  public void loadRtbNativeAd(
      @NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    AdError error =
        new AdError(
            0,
            "Native Ad is not supported in Snap Ad Network.",
            SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
    Log.w(TAG, error.getMessage());
    callback.onFailure(error);
  }

  @Override
  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    rewardedAd = new SnapRewardedAd(adConfiguration, callback);
    rewardedAd.loadAd();
  }

  public static @Nullable
  String getAppID(@NonNull Bundle serverParameters) {
    return serverParameters.getString(APP_ID_PARAMETER);
  }
}
