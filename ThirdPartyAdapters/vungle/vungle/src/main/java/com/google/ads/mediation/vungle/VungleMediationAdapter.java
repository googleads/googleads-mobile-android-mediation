package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.rtb.VungleRtbBannerAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbInterstitialAd;
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
import com.vungle.mediation.BuildConfig;
import com.vungle.mediation.VungleNativeAdapter;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;

/**
 * Mediation network adapter for Vungle.
 */
public class VungleMediationAdapter extends RtbAdapter {

  private static final String TAG = VungleMediationAdapter.class.getSimpleName();
  public static final String KEY_APP_ID = "appid";

  /**
   * Vungle adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.vungle";

  /**
   * Vungle SDK error domain.
   */
  public static final String VUNGLE_SDK_ERROR_DOMAIN = "com.vungle.warren";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_REQUIRES_ACTIVITY_CONTEXT,
          ERROR_AD_ALREADY_LOADED,
          ERROR_VUNGLE_BANNER_NULL,
          ERROR_INITIALIZATION_FAILURE,
          ERROR_CANNOT_PLAY_AD,
      })

  public @interface AdapterError {

  }

  /**
   * Server parameters, such as app ID or placement ID, are invalid.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Vungle supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Vungle requires an {@link android.app.Activity} context to request ads.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /**
   * Vungle SDK cannot load multiple ads for the same placement ID.
   */
  public static final int ERROR_AD_ALREADY_LOADED = 104;

  /**
   * Vungle SDK failed to initialize.
   */
  public static final int ERROR_INITIALIZATION_FAILURE = 105;

  /**
   * Vungle SDk returned a successful load callback, but Banners.getBanner() or Vungle.getNativeAd()
   * returned null.
   */
  public static final int ERROR_VUNGLE_BANNER_NULL = 106;

  /**
   * Vungle SDK is not ready to play the ad.
   */
  public static final int ERROR_CANNOT_PLAY_AD = 107;

  /**
   * Convert the given Vungle exception into the appropriate custom error code.
   */
  @NonNull
  public static AdError getAdError(@NonNull VungleException exception) {
    return new AdError(exception.getExceptionCode(), exception.getLocalizedMessage(),
        VUNGLE_SDK_ERROR_DOMAIN);
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
    String versionString = com.vungle.warren.BuildConfig.VERSION_NAME;
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
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    String token = Vungle.getAvailableBidTokens(rtbSignalData.getContext());
    Log.d(TAG, "token=" + token);
    signalCallbacks.onSuccess(token);
  }

  @Override
  public void initialize(
      @NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (Vungle.isInitialized()) {
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
        Log.w(TAG, error.getMessage());
        initializationCompleteCallback.onInitializationFailed(error.getMessage());
      }
      return;
    }

    String appID = appIDs.iterator().next();
    if (count > 1) {
      String logMessage =
          String.format(
              "Multiple '%s' entries found: %s. Using '%s' to initialize the Vungle SDK.",
              KEY_APP_ID, appIDs.toString(), appID);
      Log.w(TAG, logMessage);
    }

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context.getApplicationContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                initializationCompleteCallback.onInitializationSucceeded();
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.getMessage());
                initializationCompleteCallback.onInitializationFailed(error.getMessage());
              }

            });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    Log.d(TAG, "loadRewardedAd()...");
    VungleRewardedAd vungleRewardedAd = new VungleRewardedAd(mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    vungleRewardedAd.render();
  }

  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    Log.d(TAG, "loadNativeAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationNativeAdConfiguration.taggedForChildDirectedTreatment());
    VungleNativeAdapter nativeAdapter = new VungleNativeAdapter(mediationNativeAdConfiguration,
        callback);
    nativeAdapter.render();
  }

  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbRewardedAd()...");
    VungleRewardedAd vungleRewardedAd = new VungleRewardedAd(mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    vungleRewardedAd.render();
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbInterstitialAd()...");
    VungleInitializer.getInstance()
            .updateCoppaStatus(mediationInterstitialAdConfiguration.taggedForChildDirectedTreatment());
    VungleRtbInterstitialAd rtbInterstitialAd = new VungleRtbInterstitialAd(
        mediationInterstitialAdConfiguration, mediationAdLoadCallback);
    rtbInterstitialAd.render();
  }

  @Override
  public void loadRtbBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbBannerAd()...");
    VungleInitializer.getInstance()
            .updateCoppaStatus(mediationBannerAdConfiguration.taggedForChildDirectedTreatment());
    VungleRtbBannerAd rtbBannerAd = new VungleRtbBannerAd(mediationBannerAdConfiguration,
        mediationAdLoadCallback);
    rtbBannerAd.render();
  }

  @Override
  public void loadRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRewardedInterstitialAd()...");
    VungleRewardedAd vungleRewardedAd = new VungleRewardedAd(mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    vungleRewardedAd.render();
  }

  @Override
  public void loadRtbRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    Log.d(TAG, "loadRtbRewardedInterstitialAd()...");
    VungleRewardedAd vungleRewardedAd = new VungleRewardedAd(adConfiguration, callback);
    vungleRewardedAd.render();
  }
}
