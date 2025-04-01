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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PangleConstants {

  public static final String PLACEMENT_ID = "placementid";
  public static final String APP_ID = "appid";
  // The adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.pangle";
  public static final String PANGLE_SDK_ERROR_DOMAIN = "com.pangle.ads";
  public static final String ADX_ID = "207";
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {ERROR_INVALID_SERVER_PARAMETERS, ERROR_BANNER_SIZE_MISMATCH,})
  public @interface AdapterError {

  }

  /**
   * Invalid server parameters (e.g. Missing app ID or placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Pangle supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(int errorCode, @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, PANGLE_SDK_ERROR_DOMAIN);
  }

  /** A private constructor since this is a utility class which should not be instantiated. */
  private PangleConstants() {}
}
