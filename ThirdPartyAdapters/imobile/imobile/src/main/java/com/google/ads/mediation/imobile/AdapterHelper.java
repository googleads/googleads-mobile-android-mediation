package com.google.ads.mediation.imobile;

import static com.google.ads.mediation.imobile.IMobileMediationAdapter.IMOBILE_SDK_ERROR_DOMAIN;

import com.google.android.gms.ads.AdError;
import jp.co.imobile.sdkads.android.FailNotificationReason;

/**
 * Helper of mediation adapter.
 */
public final class AdapterHelper {

  /**
   * Convert i-mobile fail reason to error code.
   *
   * @param reason i-mobile fail reason
   * @return error code
   */
  public static AdError getAdError(FailNotificationReason reason) {
    // Error '99' to indicate that the error is new and has not been supported by the adapter yet.
    int code = 99;
    switch (reason) {
      case RESPONSE:
        code = 0;
        break;
      case PARAM:
        code = 1;
        break;
      case AUTHORITY:
        code = 2;
        break;
      case PERMISSION:
        code = 3;
        break;
      case NETWORK_NOT_READY:
        code = 4;
        break;
      case NETWORK:
        code = 5;
        break;
      case AD_NOT_READY:
        code = 6;
        break;
      case NOT_DELIVERY_AD:
        code = 7;
        break;
      case SHOW_TIMEOUT:
        code = 8;
        break;
    }
    return new AdError(code,
        "Failed to request ad from Imobile: " + reason.toString(),
        IMOBILE_SDK_ERROR_DOMAIN);
  }
}