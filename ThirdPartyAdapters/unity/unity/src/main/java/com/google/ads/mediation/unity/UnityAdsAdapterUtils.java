package com.google.ads.mediation.unity;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.unity.UnityMediationAdapter.AdapterError;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.UnityAdsInitializationError;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.UnityBannerSize;
import java.util.ArrayList;

/**
 * Utility class for the Unity adapter.
 */
public class UnityAdsAdapterUtils {

  private static final String SDK_ERROR_DOMAIN = "com.unity3d.ads";

  /**
   * Private constructor
   */
  private UnityAdsAdapterUtils() {
  }

  /**
   * Creates a formatted SDK error message based on the specified {@link
   * UnityAds.UnityAdsInitializationError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the error.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsInitializationError unityAdsError,
      @NonNull String description) {
    return new AdError(getMediationErrorCode(unityAdsError), description, SDK_ERROR_DOMAIN);
  }

  /**
   * Creates a formatted SDK error message based on the specified {@link BannerErrorInfo}.
   *
   * @param errorInfo error object from Unity.
   * @return the error message.
   */
  @NonNull
  static String createSDKError(@NonNull BannerErrorInfo errorInfo) {
    return String.format("%d: %s", getMediationErrorCode(errorInfo), errorInfo.errorMessage);
  }

  /**
   * Creates a formatted SDK error message based on the specified {@link
   * UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the error.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsLoadError unityAdsError,
      @NonNull String description) {
    return new AdError(getMediationErrorCode(unityAdsError), description, SDK_ERROR_DOMAIN);
  }

  /**
   * Creates a formatted SDK error message based on the specified {@link
   * UnityAds.UnityAdsShowError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the error.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsShowError unityAdsError,
      @NonNull String description) {
    return new AdError(getMediationErrorCode(unityAdsError), description, SDK_ERROR_DOMAIN);
  }

  /**
   * Creates a formatted adapter error string given a code and description.
   *
   * @param code        the error code.
   * @param description the error message.
   * @return the error message.
   */
  @NonNull
  static String createAdapterError(@AdapterError int code, String description) {
    return String.format("%d: %s", code, description);
  }

  /**
   * Gets the mediation specific error code for the specified {@link BannerErrorInfo}.
   *
   * @param errorInfo error object from Unity.
   * @return mediation specific error code.
   */
  static int getMediationErrorCode(@NonNull BannerErrorInfo errorInfo) {
    int errorCode = 200;
    switch (errorInfo.errorCode) {
      case UNKNOWN:
        errorCode = 201;
        break;
      case NATIVE_ERROR:
        errorCode = 202;
        break;
      case WEBVIEW_ERROR:
        errorCode = 203;
        break;
      case NO_FILL:
        errorCode = 204;
        break;
    }
    return errorCode;
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsInitializationError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific show error code.
   */
  static int getMediationErrorCode(@NonNull UnityAdsInitializationError unityAdsError) {
    switch (unityAdsError) {
      case INTERNAL_ERROR:
        return 301;
      case INVALID_ARGUMENT:
        return 302;
      case AD_BLOCKER_DETECTED:
        return 303;
      // Excluding default to allow for compile warnings if UnityAdsInitializationError is expanded
      // in the future.
    }
    return 300;
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific show error code.
   */
  static int getMediationErrorCode(@NonNull UnityAds.UnityAdsLoadError unityAdsError) {
    switch (unityAdsError) {
      case INITIALIZE_FAILED:
        return 401;
      case INTERNAL_ERROR:
        return 402;
      case INVALID_ARGUMENT:
        return 403;
      case NO_FILL:
        return 404;
      case TIMEOUT:
        return 405;
      // Excluding default to allow for compile warnings if UnityAdsLoadError is expanded
      // in the future.
    }
    return 400;
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsShowError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific show error code.
   */
  static int getMediationErrorCode(@NonNull UnityAds.UnityAdsShowError unityAdsError) {
    switch (unityAdsError) {
      case NOT_INITIALIZED:
        return 501;
      case NOT_READY:
        return 502;
      case VIDEO_PLAYER_ERROR:
        return 503;
      case INVALID_ARGUMENT:
        return 504;
      case NO_CONNECTION:
        return 505;
      case ALREADY_SHOWING:
        return 506;
      case INTERNAL_ERROR:
        return 507;
      // Excluding default to allow for compile warnings if UnityAdsShowError is expanded
      // in the future.
    }
    return 500;
  }

  @Nullable
  public static UnityBannerSize getUnityBannerSize(@NonNull Context context,
      @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LEADERBOARD);

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize != null) {
      return new UnityBannerSize(closestSize.getWidth(), closestSize.getHeight());
    }

    return null;
  }
}