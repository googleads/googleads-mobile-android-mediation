package com.google.ads.mediation.nend;

import com.google.android.gms.ads.AdRequest;
import java.net.HttpURLConnection;

/** The {@link ErrorUtil} contains constants and static utility methods for error conversion. */
class ErrorUtil {

  // Invalid network
  private static final int NEND_SDK_NETWORK_ERROR_CODE = 603;
  // https://github.com/fan-ADN/nendSDK-Android/wiki/Implementation-for-video-ads#acquire-error-information)

  static int convertErrorCodeFromNendVideoToAdMob(int errorCode) {
    switch (errorCode) {
      case HttpURLConnection.HTTP_NO_CONTENT:
        return AdRequest.ERROR_CODE_NO_FILL;

      case HttpURLConnection.HTTP_BAD_REQUEST:
        return AdRequest.ERROR_CODE_INVALID_REQUEST;

      case NEND_SDK_NETWORK_ERROR_CODE:
        return AdRequest.ERROR_CODE_NETWORK_ERROR;

      case HttpURLConnection.HTTP_INTERNAL_ERROR:
      default:
        return AdRequest.ERROR_CODE_INTERNAL_ERROR;
    }
  }
}
