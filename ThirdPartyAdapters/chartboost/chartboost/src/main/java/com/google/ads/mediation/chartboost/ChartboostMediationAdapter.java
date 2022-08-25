package com.google.ads.mediation.chartboost;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.chartboost.sdk.Chartboost;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChartboostMediationAdapter extends Adapter {

  static final String TAG = ChartboostMediationAdapter.class.getSimpleName();

  // region Error codes
  // Chartboost adapter error domain.
  static final String ERROR_DOMAIN = "com.google.ads.mediation.chartboost";

  // Chartboost SDK error domain.
  static final String CHARTBOOST_SDK_ERROR_DOMAIN = "com.chartboost.sdk";

  /**
   * Chartboost adapter errors.
   */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_AD_ALREADY_LOADED,
          ERROR_INVALID_SERVER_PARAMETERS
      })
  public @interface AdapterError {

  }

  /**
   * The requested ad size does not match a Chartboost supported banner size.
   */
  static final int ERROR_BANNER_SIZE_MISMATCH = 100;

  /**
   * Chartboost can only load 1 ad per location at a time.
   */
  static final int ERROR_AD_ALREADY_LOADED = 101;

  /**
   * Invalid server parameters (e.g. Chartboost App ID is missing).
   */
  static final int ERROR_INVALID_SERVER_PARAMETERS = 102;

  /**
   * Chartboost ad is not ready to be shown.
   */
  static final int ERROR_AD_NOT_READY = 103;
  // endregion

  // Flag to check whether the Chartboost SDK has been initialized or not.
  static final AtomicBoolean isSdkInitialized = new AtomicBoolean();

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
    String versionString = Chartboost.getSDKVersion();
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
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {
    HashMap<String, Bundle> chartboostConfigs = new HashMap<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle params = configuration.getServerParameters();
      String serverAppID = params.getString(ChartboostAdapterUtils.KEY_APP_ID);

      if (!TextUtils.isEmpty(serverAppID)) {
        chartboostConfigs.put(serverAppID, params);
      }
    }

    String appID;
    Bundle serverParameters;
    int count = chartboostConfigs.size();
    if (count <= 0) {
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid App ID.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }

    appID = chartboostConfigs.keySet().iterator().next();
    serverParameters = chartboostConfigs.get(appID);

    // Multiple app IDs are not considered an error.
    if (count > 1) {
      String logMessage =
          String.format(
              "Multiple '%s' entries found: %s. Using '%s' to initialize the Chartboost SDK.",
              ChartboostAdapterUtils.KEY_APP_ID, chartboostConfigs.keySet(), appID);
      Log.w(TAG, logMessage);
    }

    if (serverParameters == null) {
      // Invalid server parameters, send initialization failed event.
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Invalid server parameters.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }

    /*
      A Chartboost extras object used to store optional information used when loading ads.
     */
    ChartboostParams mChartboostParams = ChartboostAdapterUtils.createChartboostParams(
        serverParameters, null);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send initialization failed event.
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Invalid server parameters.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }
    ChartboostInitializer.getInstance()
        .init(context, mChartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            isSdkInitialized.set(true);
            initializationCompleteCallback.onInitializationSucceeded();

          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            initializationCompleteCallback.onInitializationFailed(error.getMessage());
          }
        });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    // Callback listener
    ChartboostRewardedAd mChartboostRewarded = new ChartboostRewardedAd(
        mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    mChartboostRewarded.load();
  }
}
