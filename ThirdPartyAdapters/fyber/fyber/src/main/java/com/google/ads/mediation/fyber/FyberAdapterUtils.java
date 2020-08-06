package com.google.ads.mediation.fyber;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus;

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
   * Creates a formatted SDK error message based on the specified {@link FyberInitStatus}.
   *
   * @param initStatus  the initialization state from Fyber.
   * @param description the error message.
   * @return the formatted error message.
   */
  @NonNull
  static String createSDKError(@NonNull FyberInitStatus initStatus, @NonNull String description) {
    return String.format("%d: %s", getMediationErrorCode(initStatus), description);
  }

  /**
   * Gets the mediation specific error code for the specified {@link FyberInitStatus}.
   *
   * @param initStatus the initialization state from Fyber.
   * @return mediation specific error code.
   */
  static int getMediationErrorCode(@NonNull FyberInitStatus initStatus) {
    switch (initStatus) {
      case SUCCESSFULLY:
        return 200;
      case FAILED_NO_KITS_DETECTED:
        return 201;
      case FAILED:
        return 202;
      case INVALID_APP_ID:
        return 203;
    }
    // Error '299' to indicate that the error is new and has not been supported by the adapter yet.
    return 299;
  }

  /**
   * extract age from mediation extras and add it to user params ad request
   *
   * @param request Fyber ad request
   * @param mediationExtras mediation extra bundle
   */
  static void addExtrasToAdRequest(InneractiveAdRequest request, Bundle mediationExtras) {
    if (mediationExtras != null && mediationExtras.containsKey(InneractiveMediationDefs.KEY_AGE)) {
      int age = mediationExtras.getInt(InneractiveMediationDefs.KEY_AGE, 0);
      request.setUserParams(new InneractiveUserConfig().setAge(age));
    }
  }
}
