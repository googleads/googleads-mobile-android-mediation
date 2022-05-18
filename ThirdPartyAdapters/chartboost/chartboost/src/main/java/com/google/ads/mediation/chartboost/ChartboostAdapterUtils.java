package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.CHARTBOOST_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_DOMAIN;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Banner.BannerSize;
import com.chartboost.sdk.CBLocation;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Events.ChartboostCacheError;
import com.chartboost.sdk.Events.ChartboostClickError;
import com.chartboost.sdk.Events.ChartboostShowError;
import com.chartboost.sdk.Model.CBError.CBImpressionError;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import java.util.ArrayList;

/**
 * Utility methods for the Chartboost Adapter.
 */
class ChartboostAdapterUtils {

  /**
   * Key to obtain App ID, required for initializing Chartboost SDK.
   */
  static final String KEY_APP_ID = "appId";

  /**
   * Key to obtain App Signature, required for initializing Charboost SDK.
   */
  static final String KEY_APP_SIGNATURE = "appSignature";

  /**
   * Key to obtain Ad Location. This is added in adapter version 1.1.0.
   */
  static final String KEY_AD_LOCATION = "adLocation";

  /**
   * Creates and return a new {@link ChartboostParams} object populated with the parameters obtained
   * from the server parameters and network extras bundles.
   *
   * @param serverParameters a {@link Bundle} containing server parameters used to initialize
   *                         Chartboost.
   * @param networkExtras    a {@link Bundle} containing optional information to be used by the
   *                         adapter.
   * @return a {@link ChartboostParams} object populated with the params obtained from the bundles
   * provided.
   */
  static ChartboostParams createChartboostParams(@NonNull Bundle serverParameters,
      @Nullable Bundle networkExtras) {
    ChartboostParams params = new ChartboostParams();
    String appId = serverParameters.getString(KEY_APP_ID);
    String appSignature = serverParameters.getString(KEY_APP_SIGNATURE);
    if (appId != null && appSignature != null) {
      params.setAppId(appId.trim());
      params.setAppSignature(appSignature.trim());
    }

    String adLocation = serverParameters.getString(KEY_AD_LOCATION);
    if (!isValidParam(adLocation)) {
      // Ad Location is empty, log a warning and use the default location.
      String logMessage =
          String.format(
              "Chartboost ad location is empty, defaulting to %s. "
                  + "Please set the Ad Location parameter in the AdMob UI.",
              CBLocation.LOCATION_DEFAULT);
      Log.w(ChartboostMediationAdapter.TAG, logMessage);
      adLocation = CBLocation.LOCATION_DEFAULT;
    }
    params.setLocation(adLocation.trim());

    if (networkExtras != null) {
      if (networkExtras.containsKey(ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK)
          && networkExtras.containsKey(
          ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK_VERSION)) {
        params.setFramework(
            (Chartboost.CBFramework)
                networkExtras.getSerializable(
                    ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK));
        params.setFrameworkVersion(
            networkExtras.getString(
                ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK_VERSION));
      }
    }
    return params;
  }

  /**
   * Checks whether or not the provided {@link ChartboostParams} is valid.
   *
   * @param params Chartboost params to be examined.
   * @return {@code true} if the given ChartboostParams' appId and appSignature are valid, false
   * otherwise.
   */
  static boolean isValidChartboostParams(ChartboostParams params) {
    String appId = params.getAppId();
    String appSignature = params.getAppSignature();
    if (!isValidParam(appId) || !isValidParam(appSignature)) {
      String log =
          !isValidParam(appId)
              ? (!isValidParam(appSignature) ? "App ID and App Signature" : "App ID")
              : "App Signature";
      Log.e(ChartboostMediationAdapter.TAG, log + " cannot be empty.");
      return false;
    }
    return true;
  }

  /**
   * Checks whether or not the Chartboost parameter string provided is valid.
   *
   * @param string the string to be examined.
   * @return {@code true} if the param string is not null and length when trimmed is not zero,
   * {@code false} otherwise.
   */
  static boolean isValidParam(String string) {
    return !(string == null || string.trim().length() == 0);
  }

  /**
   * Convert Chartboost's {@link CBImpressionError} to a mediation specific error code.
   *
   * @param impressionError Chartboost's error.
   * @return the mediation specific error code.
   */
  public static int getMediationErrorCode(@NonNull CBImpressionError impressionError) {
    switch (impressionError) {
      case INTERNAL:
        return 0;
      case INTERNET_UNAVAILABLE:
        return 1;
      case TOO_MANY_CONNECTIONS:
        return 2;
      case WRONG_ORIENTATION:
        return 3;
      case FIRST_SESSION_INTERSTITIALS_DISABLED:
        return 4;
      case NETWORK_FAILURE:
        return 5;
      case NO_AD_FOUND:
        return 6;
      case SESSION_NOT_STARTED:
        return 7;
      case IMPRESSION_ALREADY_VISIBLE:
        return 8;
      case NO_HOST_ACTIVITY:
        return 9;
      case USER_CANCELLATION:
        return 10;
      case INVALID_LOCATION:
        return 11;
      case VIDEO_UNAVAILABLE:
        return 12;
      case VIDEO_ID_MISSING:
        return 13;
      case ERROR_PLAYING_VIDEO:
        return 14;
      case INVALID_RESPONSE:
        return 15;
      case ASSETS_DOWNLOAD_FAILURE:
        return 16;
      case ERROR_CREATING_VIEW:
        return 17;
      case ERROR_DISPLAYING_VIEW:
        return 18;
      case INCOMPATIBLE_API_VERSION:
        return 19;
      case ERROR_LOADING_WEB_VIEW:
        return 20;
      case ASSET_PREFETCH_IN_PROGRESS:
        return 21;
      case ACTIVITY_MISSING_IN_MANIFEST:
        return 22;
      case EMPTY_LOCAL_VIDEO_LIST:
        return 23;
      case END_POINT_DISABLED:
        return 24;
      case HARDWARE_ACCELERATION_DISABLED:
        return 25;
      case PENDING_IMPRESSION_ERROR:
        return 26;
      case VIDEO_UNAVAILABLE_FOR_CURRENT_ORIENTATION:
        return 27;
      case ASSET_MISSING:
        return 28;
      case WEB_VIEW_PAGE_LOAD_TIMEOUT:
        return 29;
      case WEB_VIEW_CLIENT_RECEIVED_ERROR:
        return 30;
      case INTERNET_UNAVAILABLE_AT_SHOW:
        return 31;
    }
    // Error '99' to indicate that the error is new and has not been supported by the adapter yet.
    return 99;
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link CBImpressionError}.
   *
   * @param impressionError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull CBImpressionError impressionError) {
    return new AdError(getMediationErrorCode(impressionError), impressionError.toString(),
        ERROR_DOMAIN);
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link ChartboostCacheError}.
   *
   * @param cacheError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull ChartboostCacheError cacheError) {
    // Use the error's code as opposed to getting the mediation error code due to Chartboost not
    // having an organized enum for cache errors.
    return new AdError(cacheError.code.getErrorCode(), cacheError.toString(),
        CHARTBOOST_SDK_ERROR_DOMAIN);
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link ChartboostShowError}.
   *
   * @param showError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull ChartboostShowError showError) {
    // Use the error's code as opposed to getting the mediation error code due to Chartboost not
    // having an organized enum for show errors.
    return new AdError(showError.code.getErrorCode(), showError.toString(),
        CHARTBOOST_SDK_ERROR_DOMAIN);
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link ChartboostClickError}.
   *
   * @param clickError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull ChartboostClickError clickError) {
    // Use the error's code as opposed to getting the mediation error code due to Chartboost not
    // having an organized enum for click errors.
    return new AdError(clickError.code.getErrorCode(), clickError.toString(),
        CHARTBOOST_SDK_ERROR_DOMAIN);
  }

  /**
   * Find the closest possible {@link BannerSize} format based on the provided {@link AdSize}.
   *
   * @param context the context of requesting banner ad.
   * @param adSize  the requested banner ad size.
   * @return Chartboost {@link BannerSize} object.
   */
  @Nullable
  static BannerSize findClosestBannerSize(@NonNull Context context, @NonNull AdSize adSize) {
    AdSize standardSize =
        new AdSize(
            BannerSize.getWidth(BannerSize.STANDARD), BannerSize.getHeight(BannerSize.STANDARD));
    AdSize mediumSize =
        new AdSize(BannerSize.getWidth(BannerSize.MEDIUM), BannerSize.getHeight(BannerSize.MEDIUM));
    AdSize leaderboardSize =
        new AdSize(
            BannerSize.getWidth(BannerSize.LEADERBOARD),
            BannerSize.getHeight(BannerSize.LEADERBOARD));

    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(standardSize);
    potentials.add(mediumSize);
    potentials.add(leaderboardSize);

    AdSize supportedAdSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (supportedAdSize == null) {
      return null;
    }

    if (supportedAdSize.equals(standardSize)) {
      return BannerSize.STANDARD;
    } else if (supportedAdSize.equals(mediumSize)) {
      return BannerSize.MEDIUM;
    } else if (supportedAdSize.equals(leaderboardSize)) {
      return BannerSize.LEADERBOARD;
    }
    return null;
  }
}
