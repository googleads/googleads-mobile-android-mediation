package com.google.ads.mediation.mintegral;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;

public class MintegralUtils {

  public static final String TAG = MintegralUtils.class.getSimpleName();

  /**
   * Determines whether the ad should be muted based on the provided network extras.
   */
  public static boolean shouldMuteAudio(@NonNull Bundle networkExtras) {
    return networkExtras != null && networkExtras.getBoolean(MintegralExtras.Keys.MUTE_AUDIO);
  }

  @Nullable
  public static AdError validateMintegralAdLoadParams(
      @Nullable String adUnitId,
      @Nullable String placementId) {
    if (TextUtils.isEmpty(adUnitId)) {
      AdError parameterError =
          MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid ad Unit ID configured for this ad source instance in the"
                  + " AdMob or Ad Manager UI.");
      Log.e(TAG, parameterError.toString());
      return parameterError;
    }
    if (TextUtils.isEmpty(placementId)) {
      AdError parameterError =
          MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid Placement ID configured for this ad source instance in the"
                  + " AdMob or Ad Manager UI.");
      Log.e(TAG, parameterError.toString());
      return parameterError;
    }
    return null;
  }

  @Nullable
  public static AdError validateMintegralAdLoadParams(
      @Nullable String adUnitId,
      @Nullable String placementId,
      @Nullable String bidToken) {
    AdError parameterError = validateMintegralAdLoadParams(adUnitId, placementId);
    if (parameterError != null) {
      return parameterError;
    }
    if (TextUtils.isEmpty(bidToken)) {
      parameterError = MintegralConstants.createAdapterError(
          MintegralConstants.ERROR_INVALID_BID_RESPONSE,
          "Missing or invalid Mintegral bidding signal in this ad request.");
      Log.w(TAG, parameterError.toString());
      return parameterError;
    }
    return null;
  }

  public static int getCodeByMsg(@NonNull String errorMessage){
    int code  = MintegralConstants.ERROR_MINTEGRAL_SDK;
    if(!TextUtils.isEmpty(errorMessage)){
      if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_APP_INSTALLED) || errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_SHOW_LIST_IS_NULL) ){
        code = MintegralConstants.ERROR_CODE_APP_FILTER;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_TPL_PRELOAD_FAILED) ){
        code = MintegralConstants.ERROR_CODE_H5_TEMPLATE_ERROR;
      }else if (errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_ZIP_OR_HTML_FAILED)){
        code = MintegralConstants.ERROR_CODE_ZIP_OR_HTML_FAILED;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_201_LOAD_FAILED)){
        code = MintegralConstants.ERROR_CODE_LOAD_201_TIMEOUT;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_101_LOAD_FAILED)){
        code = MintegralConstants.ERROR_CODE_LOAD_101_TIMEOUT;
      }else if(errorMessage.contains(MintegralConstants.ERROR_MESSAGE_RV_ZIP_ERROR)){
        code = MintegralConstants.ERROR_CODE_RV_ZIP_ERROR;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_MRAID_ERROR)){
        code = MintegralConstants.ERROR_CODE_MRAID_ERROR;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_DISCONNECTED_EXCEPTION)){
        code = MintegralConstants.ERROR_CODE_DISCONNECTED_EXCEPTION;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_V3_ERROR)){
        code = MintegralConstants.ERROR_CODE_V3_ERROR;
      }else if(errorMessage.contains(MintegralConstants.ERROR_MESSAGE_EXCEPTION_RETURN_EMPTY)){
        code = MintegralConstants.ERROR_CODE_EXCEPTION_RETURN_EMPTY;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_UNKNOWN_HOST)){
        code = MintegralConstants.ERROR_CODE_UNKNOWN_HOST;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_VIDEO_UNABLE_RESOLVE_HOST)){
        code = MintegralConstants.ERROR_CODE_VIDEO_UNABLE_RESOLVE_HOST;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_CONNECT_EXCEPTION)){
        code = MintegralConstants.ERROR_CODE_CONNECT_EXCEPTION;
      }else if(errorMessage.contains(MintegralConstants.ERROR_MESSAGE_VIDEO_NO_SPACE)){
        code = MintegralConstants.ERROR_CODE_VIDEO_NO_SPACE;
      }else if(errorMessage.startsWith(MintegralConstants.ERROR_MESSAGE_TEMP_DOWNLOAD_FAILED)){
        code = MintegralConstants.ERROR_CODE_TEMP_DOWNLOAD_FAILED;
      }
    }
    return code;
  }
}
