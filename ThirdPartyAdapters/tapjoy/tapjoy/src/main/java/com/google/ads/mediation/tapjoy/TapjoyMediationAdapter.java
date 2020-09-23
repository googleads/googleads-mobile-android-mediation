package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.ads.mediation.tapjoy.rtb.TapjoyRtbInterstitialRenderer;
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
import com.tapjoy.BuildConfig;
import com.tapjoy.TJError;
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

  /**
   * TapJoy adapter errors.
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

  public @interface Error {

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


  /**
   * Creates a formatted adapter error string given a code and description.
   */
  public static String createAdapterError(@NonNull @TapjoyMediationAdapter.Error int code,
      String description) {
    return String.format("%d: %s", code, description);
  }

  public static String createSDKError(@NonNull TJError error) {
    return String.format("%d: %s", error.code, error.message);
  }


  /**
   * {@link Adapter} implementation
   */
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.VERSION_NAME;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format("Unexpected adapter version format: %s." +
        "Returning 0.0.0 for adapter version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

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

    String logMessage = String.format("Unexpected SDK version format: %s." +
        "Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(Context context,
      final InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {

    if (!(context instanceof Activity)) {
      String errorMessage = createAdapterError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Initialization Failed: "
              + "Tapjoy SDK requires an Activity context to initialize");
      initializationCompleteCallback.onInitializationFailed(errorMessage);
      return;
    }

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
    if (count > 0) {
      sdkKey = sdkKeys.iterator().next();

      if (count > 1) {
        String message = String.format("Multiple '%s' entries found: %s. " +
                "Using '%s' to initialize the Tapjoy SDK.",
            SDK_KEY_SERVER_PARAMETER_KEY, sdkKeys.toString(), sdkKey);
        Log.w(TAG, message);
      }
    } else {
      String errorMessage = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Initialization failed: Missing or Invalid SDK key.");
      initializationCompleteCallback.onInitializationFailed(errorMessage);
      return;
    }

    Tapjoy.setActivity((Activity) context);

    Hashtable<String, Object> connectFlags = new Hashtable<>();
    // TODO: Get Debug flag from publisher at init time. Currently not possible.
    // connectFlags.put("TJC_OPTION_ENABLE_LOGGING", true);

    TapjoyInitializer.getInstance().initialize((Activity) context, sdkKey, connectFlags,
        new TapjoyInitializer.Listener() {
          @Override
          public void onInitializeSucceeded() {
            initializationCompleteCallback.onInitializationSucceeded();
          }

          @Override
          public void onInitializeFailed(String message) {
            String errorMessage = createAdapterError(ERROR_TAPJOY_INITIALIZATION,
                "Initialization failed: " + message);
            initializationCompleteCallback.onInitializationFailed(errorMessage);
          }
        });
  }

  @Override
  public void collectSignals(RtbSignalData rtbSignalData, SignalCallbacks signalCallbacks) {
    signalCallbacks.onSuccess(Tapjoy.getUserToken());
  }

  @Override
  public void loadInterstitialAd(
      MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd,
          MediationInterstitialAdCallback> mediationAdLoadCallback) {
    TapjoyRtbInterstitialRenderer interstitialRenderer =
        new TapjoyRtbInterstitialRenderer(mediationInterstitialAdConfiguration,
            mediationAdLoadCallback);
    interstitialRenderer.render();
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {
    TapjoyRewardedRenderer rewardedRenderer =
        new TapjoyRewardedRenderer(mediationRewardedAdConfiguration, mediationAdLoadCallback);
    rewardedRenderer.render();
  }

}
