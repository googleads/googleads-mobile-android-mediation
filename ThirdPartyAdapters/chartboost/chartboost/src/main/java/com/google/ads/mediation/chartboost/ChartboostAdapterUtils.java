package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostConstants.CHARTBOOST_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Mediation;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.StartError;
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
   * Default location for Chartboost ads.
   */
  static final String LOCATION_DEFAULT = "default";

  /**
   * Chartboost mediation object.
   */
  private static Mediation chartboostMediation;

  /**
   * Creates and return a new {@link ChartboostParams} object populated with the parameters obtained
   * from the server parameters and network extras bundles.
   *
   * @param serverParameters a {@link Bundle} containing server parameters used to initialize
   *                         Chartboost.
   * @return a {@link ChartboostParams} object populated with the params obtained from the bundles
   * provided.
   */
  static ChartboostParams createChartboostParams(@NonNull Bundle serverParameters) {
    ChartboostParams chartboostParams = new ChartboostParams();
    String appId = serverParameters.getString(KEY_APP_ID);
    String appSignature = serverParameters.getString(KEY_APP_SIGNATURE);
    if (appId != null && appSignature != null) {
      chartboostParams.setAppId(appId.trim());
      chartboostParams.setAppSignature(appSignature.trim());
    }

    String adLocation = serverParameters.getString(KEY_AD_LOCATION);
    if (TextUtils.isEmpty(adLocation)) {
      // Ad Location is empty, log a warning and use the default location.
      String logMessage =
          String.format(
              "Chartboost ad location is empty, defaulting to %s. "
                  + "Please set the Ad Location parameter in the AdMob UI.",
              LOCATION_DEFAULT);
      Log.w(TAG, logMessage);
      adLocation = LOCATION_DEFAULT;
    }
    chartboostParams.setLocation(adLocation.trim());
    return chartboostParams;
  }

  /**
   * Checks whether or not the provided {@link ChartboostParams} is valid.
   *
   * @param chartboostParams Chartboost params to be examined.
   * @return {@code true} if the given ChartboostParams' appId and appSignature are valid, false
   * otherwise.
   */
  static boolean isValidChartboostParams(@Nullable ChartboostParams chartboostParams) {
    if (chartboostParams == null) {
      return false;
    }

    if (TextUtils.isEmpty(chartboostParams.getAppId()) || TextUtils.isEmpty(
        chartboostParams.getAppSignature())) {
      Log.e(TAG,
          "Missing or invalid App ID or App Signature configured for this ad source instance"
              + "in the AdMob or Ad Manager UI.");
      return false;
    }
    return true;
  }

  /**
   * Returns the closest possible {@link Banner.BannerSize} format based on the provided {@link
   * AdSize}.
   *
   * @param context the context of requesting banner ad.
   * @param adSize  the requested banner ad size.
   * @return Chartboost {@link Banner.BannerSize} object.
   */
  @Nullable
  static Banner.BannerSize findClosestBannerSize(@NonNull Context context, @NonNull AdSize adSize) {
    AdSize standardSize =
        new AdSize(
            Banner.BannerSize.STANDARD.getWidth(), Banner.BannerSize.STANDARD.getHeight());
    AdSize mediumSize =
        new AdSize(Banner.BannerSize.MEDIUM.getWidth(), Banner.BannerSize.MEDIUM.getHeight());
    AdSize leaderboardSize =
        new AdSize(
            Banner.BannerSize.LEADERBOARD.getWidth(),
            Banner.BannerSize.LEADERBOARD.getHeight());

    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(standardSize);
    potentials.add(mediumSize);
    potentials.add(leaderboardSize);

    AdSize supportedAdSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (supportedAdSize == null) {
      return null;
    }

    if (supportedAdSize.equals(standardSize)) {
      return Banner.BannerSize.STANDARD;
    } else if (supportedAdSize.equals(mediumSize)) {
      return Banner.BannerSize.MEDIUM;
    } else if (supportedAdSize.equals(leaderboardSize)) {
      return Banner.BannerSize.LEADERBOARD;
    }
    return null;
  }

  /**
   * Returns a {@link Mediation} object that contains mediation information. This will be
   * called every time a Chartboost ad object is created.
   */
  static Mediation getChartboostMediation() {
    if (chartboostMediation == null) {
      chartboostMediation = new Mediation("AdMob", Chartboost.getSDKVersion(),
          BuildConfig.ADAPTER_VERSION);
    }
    return chartboostMediation;
  }
}
