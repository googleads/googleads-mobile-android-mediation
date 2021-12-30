package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

  /**
   * Enumeration of ad events that get forwarded to AdMob
   */
  public enum AdEvent {
    LOADED,
    OPENED,
    CLICKED,
    CLOSED,
    LEFT_APPLICATION,
    IMPRESSION,
    VIDEO_START,
    REWARD,
    VIDEO_COMPLETE
  }

  /**
   * Private constructor
   */
  private UnityAdsAdapterUtils() {
  }

  /**
   * Creates an {@link AdError} object based on the specified
   * {@link UnityAds.UnityAdsInitializationError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsInitializationError unityAdsError,
      @NonNull String description) {
    return createAdError(getMediationErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified {@link UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsLoadError unityAdsError,
      @NonNull String description) {
    return createAdError(getMediationErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified {@link UnityAds.UnityAdsShowError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsShowError unityAdsError,
      @NonNull String description) {
    return createAdError(getMediationErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified error code and description
   *
   * @param errorCode   the mediation error code.
   * @param description the error message.
   * @return the error.
   */
  @NonNull
  static AdError createAdError(int errorCode, @NonNull String description) {
    return new AdError(errorCode, description, SDK_ERROR_DOMAIN);
  }

  /**
   * Gets the mediation specific error code for the specified {@link BannerErrorInfo}.
   *
   * @param errorInfo error object from Unity.
   * @return mediation specific banner error code.
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
   * Gets the mediation specific error code for the specified
   * {@link UnityAds.UnityAdsInitializationError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific initialization error code.
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
   * @return mediation specific load error code.
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