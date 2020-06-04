package com.google.ads.mediation.unity;

import androidx.annotation.NonNull;
import com.google.ads.mediation.unity.UnityMediationAdapter.AdapterError;
import com.unity3d.ads.UnityAds.UnityAdsError;
import com.unity3d.services.banners.BannerErrorInfo;

/** Utility class for the Unity adapter. */
public class UnityAdsAdapterUtils {

  /** Private constructor */
  private UnityAdsAdapterUtils() {}

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
   * Creates a formatted SDK error message based on the specified {@link UnityAdsError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description the error message.
   * @return the error message.
   */
  @NonNull
  static String createSDKError(@NonNull UnityAdsError unityAdsError, @NonNull String description) {
    return String.format("%d: %s", getMediationErrorCode(unityAdsError), description);
  }

  /**
   * Creates a formatted adapter error string given a code and description.
   *
   * @param code the error code.
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
   * Gets the mediation specific error code for the specified {@link UnityAdsError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific error code.
   */
  static int getMediationErrorCode(@NonNull UnityAdsError unityAdsError) {
    int errorCode = 0;
    switch (unityAdsError) {
      case NOT_INITIALIZED:
        errorCode = 1;
        break;
      case INITIALIZE_FAILED:
        errorCode = 2;
        break;
      case INVALID_ARGUMENT:
        errorCode = 3;
        break;
      case VIDEO_PLAYER_ERROR:
        errorCode = 4;
        break;
      case INIT_SANITY_CHECK_FAIL:
        errorCode = 5;
        break;
      case AD_BLOCKER_DETECTED:
        errorCode = 6;
        break;
      case FILE_IO_ERROR:
        errorCode = 7;
        break;
      case DEVICE_ID_ERROR:
        errorCode = 8;
        break;
      case SHOW_ERROR:
        errorCode = 9;
        break;
      case INTERNAL_ERROR:
        errorCode = 10;
        break;
    }
    return errorCode;
  }
}
