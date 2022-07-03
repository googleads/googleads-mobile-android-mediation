package com.google.ads.mediation.adcolony;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonySignalsListener;
import com.google.android.gms.ads.AdError;
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
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.jirbo.adcolony.AdColonyManager;
import com.jirbo.adcolony.AdColonyManager.InitializationListener;
import com.jirbo.adcolony.BuildConfig;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AdColonyMediationAdapter extends RtbAdapter {

  public static final String TAG = AdColonyMediationAdapter.class.getSimpleName();
  private static AdColonyAppOptions appOptions = new AdColonyAppOptions();

  // Keeps a strong reference to the banner ad renderer, which loads ads asynchronously.
  private AdColonyBannerRenderer adColonyBannerRenderer;

  // Keeps a strong reference to the interstitial ad renderer, which loads ads asynchronously.
  private AdColonyInterstitialRenderer adColonyInterstitialRenderer;

  // Keeps a strong reference to the rewarded ad renderer, which loads ads asynchronously.
  private AdColonyRewardedRenderer adColonyRewardedRenderer;

  // region Error codes
  // AdColony adapter error domain.
  private static final String ERROR_DOMAIN = "com.google.ads.mediation.adcolony";

  // AdColony SDK error domain.
  private static final String ADCOLONY_SDK_ERROR_DOMAIN = "com.jirbo.adcolony";

  @IntDef(value = {
      ERROR_ADCOLONY_SDK,
      ERROR_INVALID_SERVER_PARAMETERS,
      ERROR_AD_ALREADY_REQUESTED,
      ERROR_ADCOLONY_NOT_INITIALIZED,
      ERROR_BANNER_SIZE_MISMATCH,
      ERROR_PRESENTATION_AD_NOT_LOADED,
      ERROR_CONTEXT_NOT_ACTIVITY,
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface AdapterError {
  }

  /**
   * The AdColony SDK returned a failure callback.
   */
  public static final int ERROR_ADCOLONY_SDK = 100;

  /**
   * Missing server parameters.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The ad already was requested.
   */
  public static final int ERROR_AD_ALREADY_REQUESTED = 102;

  /**
   * The AdColony SDK returned an initialization error.
   */
  public static final int ERROR_ADCOLONY_NOT_INITIALIZED = 103;

  /**
   * The requested banner size does not map to a valid AdColony ad size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 104;

  /**
   * Presentation error due to ad not loaded.
   */
  public static final int ERROR_PRESENTATION_AD_NOT_LOADED = 105;

  /**
   * Context used to initialize the AdColony SDK was not an {@link Activity} instance.
   */
  public static final int ERROR_CONTEXT_NOT_ACTIVITY = 106;

  @NonNull
  public static AdError createAdapterError(@AdapterError int error, @NonNull String errorMessage) {
    return new AdError(error, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError() {
    return createSdkError(ERROR_ADCOLONY_SDK, "AdColony SDK returned a failure callback.");
  }

  @NonNull
  public static AdError createSdkError(@AdapterError int error, @NonNull String errorMessage) {
    return new AdError(error, errorMessage, ADCOLONY_SDK_ERROR_DOMAIN);
  }
  // endregion

  /**
   * {@link Adapter} implementation
   */
  @Override
  @NonNull
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

  @Override
  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String sdkVersion = AdColony.getSDKVersion();
    String[] splits = sdkVersion.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String
        .format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", sdkVersion);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(
          @NonNull Context context,
          @NonNull final InitializationCompleteCallback initializationCompleteCallback,
          @NonNull List<MediationConfiguration> mediationConfigurations
  ) {
    if (!(context instanceof Activity) && !(context instanceof Application)) {
      AdError error = createAdapterError(ERROR_CONTEXT_NOT_ACTIVITY,
              "AdColony SDK requires an Activity or Application context to initialize.");
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }

    HashSet<String> appIDs = new HashSet<>();
    ArrayList<String> zoneList = new ArrayList<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appIDFromServer = serverParameters.getString(AdColonyAdapterUtils.KEY_APP_ID);

      if (!TextUtils.isEmpty(appIDFromServer)) {
        appIDs.add(appIDFromServer);
      }

      // We need to include zone IDs from non-rewarded ads to configure the
      // AdColony SDK and avoid issues with Interstitial Ads.
      ArrayList<String> zoneIDs = AdColonyManager.getInstance()
          .parseZoneList(serverParameters);
      if (zoneIDs != null && zoneIDs.size() > 0) {
        zoneList.addAll(zoneIDs);
      }
    }

    String appID;
    int count = appIDs.size();
    if (count <= 0) {
      AdError error = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid AdColony app ID.");
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }

    appID = appIDs.iterator().next();
    if (count > 1) {
      String logMessage = String.format(
              "Multiple '%s' entries found: %s. Using '%s' to initialize the AdColony SDK.",
              AdColonyAdapterUtils.KEY_APP_ID,
              appIDs.toString(),
              appID
      );
      Log.w(TAG, logMessage);
    }

    // Always set mediation network info.
    appOptions.setMediationNetwork(AdColonyAppOptions.ADMOB, BuildConfig.ADAPTER_VERSION);
    AdColonyManager.getInstance().configureAdColony(context, appOptions, appID,
        zoneList, new InitializationListener() {
          @Override
          public void onInitializeSuccess() {
            initializationCompleteCallback.onInitializationSucceeded();
          }

          @Override
          public void onInitializeFailed(@NonNull AdError error) {
            // TODO: Forward the AdError object once available.
            initializationCompleteCallback.onInitializationFailed(error.toString());
          }
        });
  }

  @Override
  public void loadRtbBannerAd(
          @NonNull MediationBannerAdConfiguration bannerAdConfiguration,
          @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback
  ) {
    adColonyBannerRenderer = new AdColonyBannerRenderer(bannerAdConfiguration, mediationAdLoadCallback);
    adColonyBannerRenderer.render();
  }

  @Override
  public void loadRtbInterstitialAd(
          @NonNull MediationInterstitialAdConfiguration interstitialAdConfiguration,
          @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback
  ) {
    adColonyInterstitialRenderer = new AdColonyInterstitialRenderer(interstitialAdConfiguration, mediationAdLoadCallback);
    adColonyInterstitialRenderer.render();
  }

  @Override
  public void loadRtbRewardedAd(
          @NonNull MediationRewardedAdConfiguration rewardedAdConfiguration,
          @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback
  ) {
    loadRewardedAd(rewardedAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void loadRewardedAd(
          @NonNull MediationRewardedAdConfiguration rewardedAdConfiguration,
          @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback
  ) {
    adColonyRewardedRenderer = new AdColonyRewardedRenderer(rewardedAdConfiguration, mediationAdLoadCallback);
    adColonyRewardedRenderer.render();
  }

  public static AdColonyAppOptions getAppOptions() {
    return appOptions;
  }

  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData, @NonNull final SignalCallbacks signalCallbacks) {
    AdColony.collectSignals(new AdColonySignalsListener() {
      @Override
      public void onSuccess(String signals) {
        signalCallbacks.onSuccess(signals);
      }

      @Override
      public void onFailure() {
        AdError error = createSdkError(ERROR_ADCOLONY_SDK,
                "Failed to get signals from AdColony.");
        Log.e(TAG, error.getMessage());
        signalCallbacks.onFailure(error);
      }
    });
  }
}
