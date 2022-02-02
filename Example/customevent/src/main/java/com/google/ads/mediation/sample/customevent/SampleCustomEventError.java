/*
 * Copyright (C) 2022 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.customevent;

import androidx.annotation.IntDef;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.android.gms.ads.AdError;

/** Convenience factory class to create AdError objects for custom events. */
public class SampleCustomEventError {

  private SampleCustomEventError() {}

  public static final String SAMPLE_SDK_DOMAIN = "com.google.ads.mediation.sample.sdk";
  public static final String CUSTOM_EVENT_ERROR_DOMAIN =
      "com.google.ads.mediation.sample.customevent";

  @IntDef(value = {ERROR_NO_AD_UNIT_ID, ERROR_AD_NOT_AVAILABLE, ERROR_NO_ACTIVITY_CONTEXT})
  public @interface SampleCustomEventErrorCode {}

  /** Error raised when the custom event adapter cannot obtain the ad unit id. */
  public static final int ERROR_NO_AD_UNIT_ID = 101;

  /**
   * Error raised when the custom event adapter does not have an ad available when trying to show
   * the ad.
   */
  public static final int ERROR_AD_NOT_AVAILABLE = 102;

  /** Error raised when the custom event adapter cannot obtain the activity context. */
  public static final int ERROR_NO_ACTIVITY_CONTEXT = 103;

  public static AdError createCustomEventNoAdIdError() {
    return new AdError(ERROR_NO_AD_UNIT_ID, "Ad unit id is empty", CUSTOM_EVENT_ERROR_DOMAIN);
  }

  public static AdError createCustomEventAdNotAvailableError() {
    return new AdError(ERROR_AD_NOT_AVAILABLE, "No ads to show", CUSTOM_EVENT_ERROR_DOMAIN);
  }

  public static AdError createCustomEventNoActivityContextError() {
    return new AdError(
        ERROR_NO_ACTIVITY_CONTEXT,
        "An activity context is required to show the sample ad",
        CUSTOM_EVENT_ERROR_DOMAIN);
  }
  /**
   * Creates a custom event {@code AdError}. This error wraps the underlying error thrown by the
   * sample SDK.
   *
   * @param errorCode A {@code SampleErrorCode} to be reported.
   */
  public static AdError createSampleSdkError(SampleErrorCode errorCode) {
    String message = errorCode.toString();
    return new AdError(getMediationErrorCode(errorCode), message, SAMPLE_SDK_DOMAIN);
  }

  /**
   * Converts the SampleErrorCode to an integer in the range 0-99. This range is distinct from the
   * SampleCustomEventErrorCode's range which is 100-199.
   *
   * @param errorCode the error code returned by the sample SDK
   * @return an integer in the range 0-99
   */
  private static int getMediationErrorCode(SampleErrorCode errorCode) {
    switch (errorCode) {
      case UNKNOWN:
        return 0;
      case BAD_REQUEST:
        return 1;
      case NO_INVENTORY:
        return 2;
      case NETWORK_ERROR:
        return 3;
    }

    return 99;
  }
}
