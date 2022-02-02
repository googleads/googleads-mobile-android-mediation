package com.applovin.mediation;

import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.google.ads.mediation.applovin.AppLovinMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import java.util.ArrayList;

/**
 * A helper class used by {@link ApplovinAdapter}.
 */
public class AppLovinUtils {

  private static final String DEFAULT_ZONE = "";

  /**
   * Keys for retrieving values from the server parameters.
   */
  public static class ServerParameterKeys {

    public static final String SDK_KEY = "sdkKey";
    public static final String ZONE_ID = "zone_id";

    // Private constructor
    private ServerParameterKeys() {
    }
  }

  /**
   * Retrieves the appropriate instance of AppLovin's SDK from the SDK key given in the server
   * parameters, or Android Manifest.
   */
  public static AppLovinSdk retrieveSdk(Bundle serverParameters, Context context) {
    final String sdkKey =
        (serverParameters != null) ? serverParameters.getString(ServerParameterKeys.SDK_KEY) : null;
    final AppLovinSdk sdk;

    AppLovinSdkSettings sdkSettings = AppLovinMediationAdapter.getSdkSettings(context);
    if (!TextUtils.isEmpty(sdkKey)) {
      sdk = AppLovinSdk.getInstance(sdkKey, sdkSettings, context);
    } else {
      sdk = AppLovinSdk.getInstance(sdkSettings, context);
    }

    sdk.setPluginVersion(BuildConfig.ADAPTER_VERSION);
    sdk.setMediationProvider(AppLovinMediationProvider.ADMOB);
    return sdk;
  }

  /**
   * Retrieves the AppLovin SDK key.
   *
   * @param context          the context object.
   * @param serverParameters the ad request's server parameters.
   * @return the AppLovin SDK key.
   */
  @Nullable
  public static String retrieveSdkKey(@NonNull Context context, @Nullable Bundle serverParameters) {
    String sdkKey = null;

    // Prioritize the SDK Key contained in the server parameters, if any.
    if (serverParameters != null) {
      sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY);
    }

    // Fetch the SDK key in the AndroidManifest.xml file if no SDK key is found in the server
    // parameters.
    if (TextUtils.isEmpty(sdkKey)) {
      final Bundle metaData = retrieveMetadata(context);
      if (metaData != null) {
        sdkKey = metaData.getString("applovin.sdk.key");
      }
    }

    return sdkKey;
  }

  private static Bundle retrieveMetadata(Context context) {
    try {
      final PackageManager pm = context.getPackageManager();
      final ApplicationInfo ai =
          pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

      return ai.metaData;
    } catch (PackageManager.NameNotFoundException ignored) {
      // Metadata not found. Just continue and return null.
    }

    return null;
  }

  /**
   * Retrieves the zone identifier from an appropriate connector object. Will use empty string if
   * none exists.
   */
  public static String retrieveZoneId(Bundle serverParameters) {
    if (serverParameters.containsKey(ServerParameterKeys.ZONE_ID)) {
      return serverParameters.getString(ServerParameterKeys.ZONE_ID);
    } else {
      return DEFAULT_ZONE;
    }
  }

  /**
   * Retrieves whether or not to mute the ad that is about to be rendered.
   */
  public static boolean shouldMuteAudio(Bundle networkExtras) {
    return networkExtras != null && networkExtras.getBoolean(AppLovinExtras.Keys.MUTE_AUDIO);
  }

  /**
   * Convert the given AppLovin SDK error code into a Google AdError.
   */
  public static AdError getAdError(int applovinErrorCode) {
    String reason = "AppLovin error code " + applovinErrorCode;
    switch (applovinErrorCode) {
      case AppLovinErrorCodes.NO_FILL:
        reason = "NO_FILL";
        break;
      case AppLovinErrorCodes.FETCH_AD_TIMEOUT:
        reason = "FETCH_AD_TIMEOUT";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_NO_AD_PRELOADED:
        reason = "INCENTIVIZED_NO_AD_PRELOADED";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_SERVER_TIMEOUT:
        reason = "INCENTIVIZED_SERVER_TIMEOUT";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_UNKNOWN_SERVER_ERROR:
        reason = "INCENTIVIZED_UNKNOWN_SERVER_ERROR";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_USER_CLOSED_VIDEO:
        reason = "INCENTIVIZED_USER_CLOSED_VIDEO";
        break;
      case AppLovinErrorCodes.INVALID_AD_TOKEN:
        reason = "INVALID_AD_TOKEN";
        break;
      case AppLovinErrorCodes.INVALID_RESPONSE:
        reason = "INVALID_RESPONSE";
        break;
      case AppLovinErrorCodes.INVALID_URL:
        reason = "INVALID_URL";
        break;
      case AppLovinErrorCodes.INVALID_ZONE:
        reason = "INVALID_ZONE";
        break;
      case AppLovinErrorCodes.NO_NETWORK:
        reason = "NO_NETWORK";
        break;
      case AppLovinErrorCodes.SDK_DISABLED:
        reason = "SDK_DISABLED";
        break;
      case AppLovinErrorCodes.UNABLE_TO_PRECACHE_IMAGE_RESOURCES:
        reason = "UNABLE_TO_PRECACHE_IMAGE_RESOURCES";
        break;
      case AppLovinErrorCodes.UNABLE_TO_PRECACHE_RESOURCES:
        reason = "UNABLE_TO_PRECACHE_RESOURCES";
        break;
      case AppLovinErrorCodes.UNABLE_TO_PRECACHE_VIDEO_RESOURCES:
        reason = "UNABLE_TO_PRECACHE_VIDEO_RESOURCES";
        break;
      case AppLovinErrorCodes.UNABLE_TO_RENDER_AD:
        reason = "UNABLE_TO_RENDER_AD";
        break;
      case AppLovinErrorCodes.UNSPECIFIED_ERROR:
        reason = "UNSPECIFIED_ERROR";
        break;
      default: // fall out
    }

    return new AdError(applovinErrorCode,
        "AppLovin SDK returned a load failure callback with reason: " + reason, ERROR_DOMAIN);
  }

  /**
   * Get the {@link AppLovinAdSize} from a given {@link AdSize} from AdMob.
   */
  @Nullable
  public static AppLovinAdSize appLovinAdSizeFromAdMobAdSize(@NonNull Context context,
      @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LEADERBOARD);

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (AdSize.BANNER.equals(closestSize)) {
      return AppLovinAdSize.BANNER;
    } else if (AdSize.LEADERBOARD.equals(closestSize)) {
      return AppLovinAdSize.LEADER;
    }
    return null;
  }
}
