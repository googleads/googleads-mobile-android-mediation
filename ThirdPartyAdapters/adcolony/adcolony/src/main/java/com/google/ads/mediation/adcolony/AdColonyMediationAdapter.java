package com.google.ads.mediation.adcolony;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyCustomMessage;
import com.adcolony.sdk.AdColonyCustomMessageListener;
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
import com.jirbo.adcolony.AdColonyManager;
import com.jirbo.adcolony.BuildConfig;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

public class AdColonyMediationAdapter extends RtbAdapter {

  public static final String TAG = AdColonyMediationAdapter.class.getSimpleName();
  private static AdColonyAppOptions appOptions = new AdColonyAppOptions();
  private static HashMap<String, String> bidResponseDetailsHashMap = new HashMap<>();

  // Keeps a strong reference to the interstitial ad renderer, which loads ads asynchronously.
  private AdColonyInterstitialRenderer adColonyInterstitialRenderer;

  // Keeps a strong reference to the rewarded ad renderer, which loads ads asynchronously.
  private AdColonyRewardedRenderer adColonyRewardedRenderer;

  /**
   * AdColony adapter errors.
   */
  @IntDef(value = {
      ERROR_ADCOLONY_SDK,
      ERROR_REQUEST_INVALID,
      ERROR_AD_ALREADY_REQUESTED,
      ERROR_ADCOLONY_NOT_INITIALIZED,
      ERROR_BANNER_SIZE_MISMATCH,
      ERROR_PRESENTATION_AD_NOT_LOADED

  })

  @Retention(RetentionPolicy.SOURCE)
  public @interface Error {

  }

  /**
   * The AdColony SDK returned a failure callback.
   */
  public static final int ERROR_ADCOLONY_SDK = 100;
  /**
   * Missing server parameters.
   */
  public static final int ERROR_REQUEST_INVALID = 101;
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

  public static String createAdapterError(@NonNull @Error int error, String errorMessage) {
    return String.format(Locale.US, "%d: %s", error, errorMessage);
  }

  public static String createSdkError() {
    return String.format(Locale.US, "%d: %s", ERROR_ADCOLONY_SDK,
        "AdColony SDK returned a failure callback");
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
    String sdkVersion = AdColony.getSDKVersion();
    String[] splits = sdkVersion.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format("Unexpected SDK version format: %s." +
        "Returning 0.0.0 for SDK version.", sdkVersion);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {

    if (!(context instanceof Activity)) {
      initializationCompleteCallback.onInitializationFailed("AdColony SDK requires an " +
          "Activity context to initialize");
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
    if (count > 0) {
      appID = appIDs.iterator().next();

      if (count > 1) {
        String logMessage = String.format("Multiple '%s' entries found: %s. " +
                "Using '%s' to initialize the AdColony SDK.",
            AdColonyAdapterUtils.KEY_APP_ID, appIDs.toString(), appID);
        Log.w(TAG, logMessage);
      }
    } else {
      initializationCompleteCallback.onInitializationFailed("Initialization Failed: " +
          "Missing or Invalid App ID.");
      return;
    }

    if (zoneList.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed("Initialization Failed: " +
          "No zones provided to initialize the AdColony SDK.");
      return;
    }

    // Always set mediation network info.
    appOptions.setMediationNetwork(AdColonyAppOptions.ADMOB, BuildConfig.VERSION_NAME);
    boolean success = AdColony.configure((Activity) context, appOptions, appID,
        zoneList.toArray(new String[0]));

    if (success) {
      initializationCompleteCallback.onInitializationSucceeded();
    } else {
      initializationCompleteCallback.onInitializationFailed("Initialization Failed: " +
          "Internal Error on Configuration");
    }
  }

  @Override
  public void loadInterstitialAd(
      MediationInterstitialAdConfiguration interstitialAdConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd,
          MediationInterstitialAdCallback> mediationAdLoadCallback) {
    String requestedZone = interstitialAdConfiguration.getServerParameters()
        .getString(AdColonyAdapterUtils.KEY_ZONE_ID);
    adColonyInterstitialRenderer = new AdColonyInterstitialRenderer(requestedZone);
    adColonyInterstitialRenderer.requestInterstitial(mediationAdLoadCallback);
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration rewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {
    adColonyRewardedRenderer =
        new AdColonyRewardedRenderer(rewardedAdConfiguration, mediationAdLoadCallback);
    adColonyRewardedRenderer.render();
  }

  public static AdColonyAppOptions getAppOptions() {
    return appOptions;
  }

  @Override
  public void collectSignals(RtbSignalData rtbSignalData, SignalCallbacks signalCallbacks) {
    String zone, signals = "";
    AdColony.addCustomMessageListener(new AdColonyCustomMessageListener() {
      @Override
      public void onAdColonyCustomMessage(AdColonyCustomMessage adColonyCustomMessage) {
        try {
          JSONObject jsonObject = new JSONObject(adColonyCustomMessage.getMessage());
          String zoneID = jsonObject.getString("zone");
          bidResponseDetailsHashMap.put(zoneID, adColonyCustomMessage.getMessage());
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }, "bid");
    if (bidResponseDetailsHashMap != null && bidResponseDetailsHashMap.size() > 0) {
      Bundle serverParameters = rtbSignalData.getConfiguration().getServerParameters();
      zone = serverParameters.getString(AdColonyAdapterUtils.KEY_ZONE_ID);
      signals = bidResponseDetailsHashMap.get(zone);
    }
    signalCallbacks.onSuccess(signals);
  }
  //endregion
}
