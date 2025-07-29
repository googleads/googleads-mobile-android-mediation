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

package com.google.ads.mediation.vungle;

import static com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_USER_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.rtb.VungleRtbAppOpenAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbBannerAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbInterstitialAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbNativeAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbRewardedAd;
import com.google.ads.mediation.vungle.waterfall.VungleWaterfallAppOpenAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
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
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleError;
import com.vungle.mediation.BuildConfig;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;

/**
 * Mediation network adapter for Liftoff Monetize.
 */
public class VungleMediationAdapter extends RtbAdapter implements MediationRewardedAd,
    RewardedAdListener {

  public static final String TAG = VungleMediationAdapter.class.getSimpleName();

  private VungleWaterfallAppOpenAd waterfallAppOpenAd;

  private VungleRtbBannerAd rtbBannerAd;
  private VungleRtbInterstitialAd rtbInterstitialAd;
  private VungleRtbRewardedAd rtbRewardedAd;
  private VungleRtbRewardedAd rtbRewardedInterstitialAd;
  private VungleRtbNativeAd rtbNativeAd;
  private VungleRtbAppOpenAd rtbAppOpenAd;

  private AdConfig adConfig;
  private String userId;
  private RewardedAd rewardedAd;
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback;
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  private final VungleFactory vungleFactory;

  /**
   * Liftoff Monetize adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.vungle";

  /**
   * Vungle SDK error domain.
   */
  public static final String VUNGLE_SDK_ERROR_DOMAIN = "com.vungle.ads";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
        ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_INITIALIZATION_FAILURE,
        ERROR_CANNOT_PLAY_AD,
        ERROR_CANNOT_GET_BID_TOKEN
      })
  public @interface AdapterError {}

  /**
   * Server parameters, such as app ID or placement ID, are invalid.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  // Commenting this out since this error code is not returned by this adapter anymore. But, still
  // having this as a comment so that, if we look at error code 103 in the logs, we have this as a
  // reference to understand what it means.
  // Also, shouldn't reuse 103 for any new error type since that could cause confusion when looking
  // at the logs.
  /** Liftoff Monetize requires an {@link android.app.Activity} context to request ads. */
  // public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /** Vungle SDK failed to initialize. */
  public static final int ERROR_INITIALIZATION_FAILURE = 105;

  /**
   * Vungle SDK is not ready to play the ad.
   */
  public static final int ERROR_CANNOT_PLAY_AD = 107;

  /**
   * Vungle SDK returned invalid bidding token.
   */
  public static final int ERROR_CANNOT_GET_BID_TOKEN = 108;

  public VungleMediationAdapter() {
    vungleFactory = new VungleFactory();
  }

  @VisibleForTesting
  VungleMediationAdapter(VungleFactory vungleFactory) {
    this.vungleFactory = vungleFactory;
  }

  /**
   * Convert the given Liftoff Monetize exception into the appropriate custom error code.
   */
  @NonNull
  public static AdError getAdError(@NonNull VungleError vungleError) {
    return new AdError(vungleError.getCode(), vungleError.getErrorMessage(),
        VUNGLE_SDK_ERROR_DOMAIN);
  }

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = getAdapterVersion();
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
    String versionString = VungleSdkWrapper.delegate.getSdkVersion();
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
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    String token = VungleSdkWrapper.delegate.getBiddingToken(rtbSignalData.getContext());
    if (TextUtils.isEmpty(token)) {
      AdError error = new AdError(ERROR_CANNOT_GET_BID_TOKEN,
          "Liftoff Monetize returned an empty bid token.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      signalCallbacks.onFailure(error);
    } else {
      Log.d(TAG, "Liftoff Monetize bidding token=" + token);
      signalCallbacks.onSuccess(token);
    }
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (VungleSdkWrapper.delegate.isInitialized()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    HashSet<String> appIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appIDFromServer = serverParameters.getString(KEY_APP_ID);

      if (!TextUtils.isEmpty(appIDFromServer)) {
        appIDs.add(appIDFromServer);
      }
    }

    int count = appIDs.size();
    if (count <= 0) {
      if (initializationCompleteCallback != null) {
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.",
            ERROR_DOMAIN);
        Log.w(TAG, error.toString());
        initializationCompleteCallback.onInitializationFailed(error.toString());
      }
      return;
    }

    String appID = appIDs.iterator().next();
    if (count > 1) {
      String logMessage = String.format(
          "Multiple '%s' entries found: %s. Using '%s' to initialize the Vungle SDK.", KEY_APP_ID,
          appIDs, appID);
      Log.w(TAG, logMessage);
    }

    VungleInitializer.getInstance().initialize(appID, context, new VungleInitializationListener() {
      @Override
      public void onInitializeSuccess() {
        initializationCompleteCallback.onInitializationSucceeded();
      }

      @Override
      public void onInitializeError(AdError error) {
        Log.w(TAG, error.toString());
        initializationCompleteCallback.onInitializationFailed(error.toString());
      }

    });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;

    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    if (mediationExtras != null) {
      userId = mediationExtras.getString(KEY_USER_ID);
    }

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load waterfall rewarded ad from Liftoff Monetize. "
              + "Missing or invalid App ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String placement = serverParameters.getString(KEY_PLACEMENT_ID);
    if (TextUtils.isEmpty(placement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load waterfall rewarded ad from Liftoff Monetize. "
              + "Missing or Invalid Placement ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    adConfig = vungleFactory.createAdConfig();
    if (mediationExtras != null && mediationExtras.containsKey(KEY_ORIENTATION)) {
      adConfig.setAdOrientation(mediationExtras.getInt(KEY_ORIENTATION, AdConfig.AUTO_ROTATE));
    }

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());

    Context context = mediationRewardedAdConfiguration.getContext();

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context,
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                rewardedAd = vungleFactory.createRewardedAd(context, placement, adConfig);
                rewardedAd.setAdListener(VungleMediationAdapter.this);
                if (!TextUtils.isEmpty(userId)) {
                  rewardedAd.setUserId(userId);
                }

                rewardedAd.load(null);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                VungleMediationAdapter.this.mediationAdLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (rewardedAd != null) {
      rewardedAd.play(context);
    } else if (mediationRewardedAdCallback != null) {
      AdError error = new AdError(ERROR_CANNOT_PLAY_AD,
          "Failed to show waterfall rewarded" + " ad from Liftoff Monetize.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  /**
   * {@link RewardedAdListener} implementation from Liftoff Monetize.
   */
  @Override
  public void onAdLoaded(@NonNull BaseAd baseAd) {
    Log.d(TAG, "Loaded waterfall rewarded ad from Liftoff Monetize.");
    if (mediationAdLoadCallback != null) {
      mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
    }
  }

  @Override
  public void onAdStart(@NonNull BaseAd baseAd) {
    Log.d(TAG, "Liftoff Monetize waterfall rewarded ad has started.");
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdEnd(@NonNull BaseAd baseAd) {
    Log.d(TAG, "Liftoff Monetize waterfall rewarded ad has ended.");
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onAdClicked(@NonNull BaseAd baseAd) {
    Log.d(TAG, "Liftoff Monetize waterfall rewarded ad was clicked.");
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdRewarded(@NonNull BaseAd baseAd) {
    Log.d(TAG, "Received reward from Liftoff Monetize waterfall rewarded ad.");
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onVideoComplete();
      mediationRewardedAdCallback.onUserEarnedReward();
    }
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdFailedToPlay(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = getAdError(vungleError);
    Log.w(TAG, "Failed to play waterfall rewarded ad from Liftoff Monetize with error: "
        + error.toString());
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdFailedToLoad(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = getAdError(vungleError);
    Log.w(TAG, "Failed to load waterfall rewarded ad from Liftoff Monetize with error: "
        + error.toString());
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdImpression(@NonNull BaseAd baseAd) {
    mediationRewardedAdCallback.onVideoStart();
    mediationRewardedAdCallback.reportAdImpression();
  }

  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    Log.d(TAG, "loadNativeAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationNativeAdConfiguration.taggedForChildDirectedTreatment());
    // Vungle waterfall and bidding Native ads use the same API.
    rtbNativeAd = new VungleRtbNativeAd(mediationNativeAdConfiguration, callback, vungleFactory);
    rtbNativeAd.render();
  }

  @Override
  public void loadRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    Log.d(TAG, "loadRewardedInterstitialAd()...");
    Log.d(TAG, "Liftoff Monetize adapter was asked to load a rewarded interstitial ad. "
        + "Using the rewarded ad request flow to load the ad to attempt to load a "
        + "rewarded interstitial ad from Liftoff Monetize.");
    // Liftoff Monetize Rewarded Interstitial ads use the same Rewarded Video API.
    loadRewardedAd(mediationRewardedAdConfiguration, callback);
  }

  @Override
  public void loadAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback) {
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAppOpenAdConfiguration.taggedForChildDirectedTreatment());
    waterfallAppOpenAd =
        new VungleWaterfallAppOpenAd(mediationAppOpenAdConfiguration, callback, vungleFactory);
    waterfallAppOpenAd.render();
  }

  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbRewardedAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());
    rtbRewardedAd =
        new VungleRtbRewardedAd(
            mediationRewardedAdConfiguration, mediationAdLoadCallback, vungleFactory);
    rtbRewardedAd.render();
  }

  @Override
  public void loadRtbBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbBannerAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationBannerAdConfiguration.taggedForChildDirectedTreatment());
    rtbBannerAd =
        new VungleRtbBannerAd(
            mediationBannerAdConfiguration, mediationAdLoadCallback, vungleFactory);
    rtbBannerAd.render();
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbInterstitialAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationInterstitialAdConfiguration.taggedForChildDirectedTreatment());
    rtbInterstitialAd =
        new VungleRtbInterstitialAd(
            mediationInterstitialAdConfiguration, mediationAdLoadCallback, vungleFactory);
    rtbInterstitialAd.render();
  }

  @Override
  public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    Log.d(TAG, "loadRtbNativeAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment());
    rtbNativeAd = new VungleRtbNativeAd(adConfiguration, callback, vungleFactory);
    rtbNativeAd.render();
  }

  @Override
  public void loadRtbRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    Log.d(TAG, "loadRtbRewardedInterstitialAd()...");
    Log.d(TAG, "Liftoff Monetize adapter was asked to load a rewarded interstitial ad. "
        + "Using the rewarded ad request flow to load the ad to attempt to load a "
        + "rewarded interstitial ad from Liftoff Monetize.");
    VungleInitializer.getInstance()
        .updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment());
    // Vungle Rewarded Interstitial ads use the same Rewarded Video API.
    rtbRewardedInterstitialAd = new VungleRtbRewardedAd(adConfiguration, callback, vungleFactory);
    rtbRewardedInterstitialAd.render();
  }

  @Override
  public void loadRtbAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback) {
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAppOpenAdConfiguration.taggedForChildDirectedTreatment());
    rtbAppOpenAd = new VungleRtbAppOpenAd(mediationAppOpenAdConfiguration, callback, vungleFactory);
    rtbAppOpenAd.render();
  }

  static String getAdapterVersion() {
    return BuildConfig.ADAPTER_VERSION;
  }

  /**
   * Checks whether the runtime GMA SDK is a version of GMA SDK that listens to adapter-reported
   * native ad impressions.
   *
   * <p>GMA SDK versions >= 24.4.0 or < 6.5.0 listen to adapter-reported native ad impressions.
   */
  public static boolean runtimeGmaSdkListensToAdapterReportedImpressions() {
    VersionInfo gmaSdkVersion = MobileAds.getVersion();
    if (isVersionLowerThan(
            gmaSdkVersion,
            new VersionInfo(/* majorVersion= */ 6, /* minorVersion= */ 5, /* microVersion= */ 0))
        && isVersionGreaterThanOrEqualTo(
            gmaSdkVersion,
            new VersionInfo(
                /* majorVersion= */ 0, /* minorVersion= */ 18, /* microVersion= */ 0))) {
      return true;
    }
    return isVersionGreaterThanOrEqualTo(
        gmaSdkVersion,
        new VersionInfo(/* majorVersion= */ 24, /* minorVersion= */ 4, /* microVersion= */ 0));
  }

  /** Returns true iff version1 is greater than or equal to version2. */
  private static boolean isVersionGreaterThanOrEqualTo(VersionInfo version1, VersionInfo version2) {
    if (version1.getMajorVersion() > version2.getMajorVersion()) {
      return true;
    } else if (version1.getMajorVersion() == version2.getMajorVersion()) {
      if (version1.getMinorVersion() > version2.getMinorVersion()) {
        return true;
      } else if (version1.getMinorVersion() == version2.getMinorVersion()) {
        return version1.getMicroVersion() >= version2.getMicroVersion();
      }
    }
    return false;
  }

  /** Returns true iff version1 is less than version2. */
  private static boolean isVersionLowerThan(VersionInfo version1, VersionInfo version2) {
    if (version1.getMajorVersion() < version2.getMajorVersion()) {
      return true;
    } else if (version1.getMajorVersion() == version2.getMajorVersion()) {
      if (version1.getMinorVersion() < version2.getMinorVersion()) {
        return true;
      } else if (version1.getMinorVersion() == version2.getMinorVersion()) {
        return version1.getMicroVersion() < version2.getMicroVersion();
      }
    }
    return false;
  }
}
