// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.pangle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PangleConstants {

  public static final String PLACEMENT_ID = "placementid";
  public static final String APP_ID = "appid";
  // The adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.pangle";
  public static final String PANGLE_SDK_ERROR_DOMAIN = "com.pangle.ads";

  /**
   * Error message when the user is a child.
   *
   * <p>See {@link ERROR_CHILD_USER} for more details.
   */
  public static final String ERROR_MSG_CHILD_USER =
      "MobileAds.getRequestConfiguration() indicates the user is a child. Pangle SDK V71 or"
          + " higher does not support child users.";

  /** Identifier used by Pangle to identify that the request is coming from Google's adapter. */
  public static final String ADX_ID = "207";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {ERROR_INVALID_SERVER_PARAMETERS, ERROR_BANNER_SIZE_MISMATCH, ERROR_CHILD_USER})
  public @interface AdapterError {}

  /**
   * Invalid server parameters (e.g. Missing app ID or placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Pangle supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * User is a child.
   *
   * <p>Shouldn't call Pangle SDK if the user is a child. Adapter will respond with this error code
   * if adapter is requested to load ad or collect signals when the user is a child.
   *
   * <p>Starting with Pangle SDK V71, Pangle no longer supports child user flags and you may not
   * initialize or use the Pangle SDK in connection with a "child" as defined under applicable laws.
   */
  public static final int ERROR_CHILD_USER = 103;

  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(int errorCode, @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, PANGLE_SDK_ERROR_DOMAIN);
  }

  /**
   * Returns error object that should be passed back through callback when a request is made for a
   * user who is a child.
   *
   * <p>See {@link PangleConstants.ERROR_CHILD_USER} for more details.
   */
  public static AdError createChildUserError() {
    return new AdError(ERROR_CHILD_USER, ERROR_MSG_CHILD_USER, ERROR_DOMAIN);
  }

  /** Returns whether the user has been tagged as a child or not. */
  public static boolean isChildUser() {
    RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration();
    return requestConfiguration.getTagForChildDirectedTreatment()
            == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        || requestConfiguration.getTagForUnderAgeOfConsent()
            == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
  }

  /** A private constructor since this is a utility class which should not be instantiated. */
  private PangleConstants() {}
}
