package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.ads.mediation.tapjoy.rtb.TapjoyRtbInterstitialRenderer;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.tapjoy.Tapjoy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class TapjoyMediationAdapter extends RtbAdapter {

  static final String TAG = TapjoyMediationAdapter.class.getSimpleName();

  static final String SDK_KEY_SERVER_PARAMETER_KEY = "sdkKey";
  static final String PLACEMENT_NAME_SERVER_PARAMETER_KEY = "placementName";
  static final String MEDIATION_AGENT = "admob";
  // only used internally for Tapjoy SDK
  static final String TAPJOY_INTERNAL_ADAPTER_VERSION = "1.0.0";

  // region Error codes.
  // Tapjoy adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.tapjoy";

  // Tapjoy SDK error domain.
  public static final String TAPJOY_SDK_ERROR_DOMAIN = "com.tapjoy";

  /**
   * Tapjoy adapter errors.
   */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {
      ERROR_INVALID_SERVER_PARAMETERS,
      ERROR_BANNER_SIZE_MISMATCH,
      ERROR_REQUIRES_ACTIVITY_CONTEXT,
      ERROR_TAPJOY_INITIALIZATION,
      ERROR_PRESENTATION_VIDEO_PLAYBACK,
      ERROR_AD_ALREADY_REQUESTED,
      ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
      ERROR_NO_CONTENT_AVAILABLE
  })
  public @interface AdapterError {

  }

  /**
   * Invalid server parameters.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * Banner size mismatch.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Adapter requires an activity context to load ads.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /**
   * Tapjoy failed to initialize.
   */
  public static final int ERROR_TAPJOY_INITIALIZATION = 104;

  /**
   * Presentation error occurred during video playback.
   */
  public static final int ERROR_PRESENTATION_VIDEO_PLAYBACK = 105;

  /**
   * Tapjoy SDK can't load two ads for the same placement ID at once.
   */
  public static final int ERROR_AD_ALREADY_REQUESTED = 106;

  /**
   * App did not request unified native ads.
   */
  public static final int ERROR_REQUIRES_UNIFIED_NATIVE_ADS = 107;

  /**
   * Tapjoy SDK has no content available.
   */
  public static final int ERROR_NO_CONTENT_AVAILABLE = 108;
  // endregion

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

    String logMessage = String
        .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = Tapjoy.getVersion();
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

    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Tapjoy SDK requires an Activity context to initialize.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }
    Activity activity = (Activity) context;

    HashSet<String> sdkKeys = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String sdkKeyFromServer = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);

      if (!TextUtils.isEmpty(sdkKeyFromServer)) {
        sdkKeys.add(sdkKeyFromServer);
      }
    }

    String sdkKey;
    int count = sdkKeys.size();
    if (count <= 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid SDK key.",
          ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    sdkKey = sdkKeys.iterator().next();
    if (count > 1) {
      String message = String
          .format("Multiple '%s' entries found: %s. Using '%s' to initialize the Tapjoy SDK.",
              SDK_KEY_SERVER_PARAMETER_KEY, sdkKeys, sdkKey);
      Log.w(TAG, message);
    }

    Tapjoy.setActivity(activity);

    Hashtable<String, Object> connectFlags = new Hashtable<>();
    // TODO: Get Debug flag from publisher at init time. Currently not possible.
    // connectFlags.put("TJC_OPTION_ENABLE_LOGGING", true);

    TapjoyInitializer.getInstance().initialize(activity, sdkKey, connectFlags,
        new TapjoyInitializer.Listener() {
          @Override
          public void onInitializeSucceeded() {
            initializationCompleteCallback.onInitializationSucceeded();
          }

          @Override
          public void onInitializeFailed(String message) {
            AdError error = new AdError(ERROR_TAPJOY_INITIALIZATION, message, ERROR_DOMAIN);
            initializationCompleteCallback.onInitializationFailed(error.getMessage());
          }
        });
  }

  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    signalCallbacks.onSuccess(Tapjoy.getUserToken());
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback) {
    TapjoyRtbInterstitialRenderer interstitialRenderer = new TapjoyRtbInterstitialRenderer(
        mediationInterstitialAdConfiguration, mediationAdLoadCallback);
    interstitialRenderer.render();
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    TapjoyRewardedRenderer rewardedRenderer = new TapjoyRewardedRenderer(
        mediationRewardedAdConfiguration, mediationAdLoadCallback);
    rewardedRenderer.render();
  }

}
