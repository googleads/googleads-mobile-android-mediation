package com.google.ads.mediation.fyber;

import static com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_DOMAIN;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus;
import com.google.android.gms.ads.AdError;

/**
 * Utility class for the Fyber adapter.
 */
class FyberAdapterUtils {
  /**
   * Private constructor
   */
  private FyberAdapterUtils() {
  }

  /**
   * Gets the specific error code for the specified {@link FyberInitStatus}.
   *
   * @param initStatus the initialization state from Fyber.
   * @return specific error.
   */
  static AdError getAdError(@NonNull FyberInitStatus initStatus) {
    // Error '299' to indicate that the error is new and has not been supported by the adapter yet.
    int code = 299;
    switch (initStatus) {
      case SUCCESSFULLY:
        code = 200;
        break;
      case FAILED_NO_KITS_DETECTED:
        code = 201;
        break;
      case FAILED:
        code = 202;
        break;
      case INVALID_APP_ID:
        code = 203;
        break;
    }
    return new AdError(code,
        "Fyber failed to initialize with reason: " + initStatus.toString(),
        ERROR_DOMAIN);
  }

  /**
   * Gets the specific error code for the specified {@link InneractiveErrorCode}.
   *
   * @param inneractiveErrorCode the inneractive error code for ad request fail reason.
   * @return specific error.
   */

  static AdError getAdError(@NonNull InneractiveErrorCode inneractiveErrorCode) {
    // Error '399' to indicate that the error is new and has not been supported by the adapter yet.
    int code = 399;
    switch (inneractiveErrorCode) {
      case CONNECTION_ERROR:
        code = 300;
        break;
      case CONNECTION_TIMEOUT:
        code = 301;
        break;
      case NO_FILL:
        code = 302;
        break;
      case SERVER_INVALID_RESPONSE:
        code = 303;
        break;
      case SERVER_INTERNAL_ERROR:
        code = 304;
        break;
      case SDK_INTERNAL_ERROR:
        code = 305;
        break;
      case UNSPECIFIED:
        code = 306;
        break;
      case LOAD_TIMEOUT:
        code = 307;
        break;
      case INVALID_INPUT:
        code = 308;
        break;
      case SPOT_DISABLED:
        code = 309;
        break;
      case UNSUPPORTED_SPOT:
        code = 310;
        break;
      case IN_FLIGHT_TIMEOUT:
        code = 311;
        break;
      case SDK_NOT_INITIALIZED:
        code = 312;
        break;
      case NON_SECURE_CONTENT_DETECTED:
        code = 313;
        break;
      case ERROR_CONFIGURATION_MISMATCH:
        code = 314;
        break;
      case NATIVE_ADS_NOT_SUPPORTED_FOR_OS:
        code = 315;
        break;
      case ERROR_CONFIGURATION_NO_SUCH_SPOT:
        code = 316;
        break;
      case SDK_NOT_INITIALIZED_OR_CONFIG_ERROR:
        code = 317;
        break;
      case ERROR_CODE_NATIVE_VIDEO_NOT_SUPPORTED:
        break;
    }
    return new AdError(code,
        "Fyber failed to request ad with reason: " + inneractiveErrorCode.toString(), ERROR_DOMAIN);
  }

  /**
   * Extract age from mediation extras and add it to user params ad request.
   *
   * @param mediationExtras mediation extra bundle
   */
  static InneractiveUserConfig generateUserConfig(@Nullable Bundle mediationExtras) {
    InneractiveUserConfig userConfig = new InneractiveUserConfig();
    if (mediationExtras == null) {
      return userConfig;
    }

    if (mediationExtras.containsKey(InneractiveMediationDefs.KEY_AGE)) {
      int age = mediationExtras.getInt(InneractiveMediationDefs.KEY_AGE, 0);
      userConfig.setAge(age);
    }
    return userConfig;
  }
}
