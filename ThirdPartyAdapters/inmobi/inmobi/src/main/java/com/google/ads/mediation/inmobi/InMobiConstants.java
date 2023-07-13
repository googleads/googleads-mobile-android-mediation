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

package com.google.ads.mediation.inmobi;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class InMobiConstants {

  // region Error codes
  // InMobi adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.inmobi";

  // InMobi SDK error domain.
  public static final String INMOBI_SDK_ERROR_DOMAIN = "com.inmobi.sdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {ERROR_INVALID_SERVER_PARAMETERS, ERROR_INMOBI_FAILED_INITIALIZATION,
      ERROR_BANNER_SIZE_MISMATCH, ERROR_NON_UNIFIED_NATIVE_REQUEST, ERROR_INMOBI_NOT_INITIALIZED,
      ERROR_AD_NOT_READY, ERROR_AD_DISPLAY_FAILED, ERROR_MISSING_NATIVE_ASSETS,
      ERROR_MALFORMED_IMAGE_URL, ERROR_NATIVE_ASSET_DOWNLOAD_FAILED,})
  public @interface AdapterError {

  }

  /**
   * Invalid server parameters (e.g. InMobi Account ID is missing).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 100;

  /**
   * InMobi SDK failed to initialize.
   */
  public static final int ERROR_INMOBI_FAILED_INITIALIZATION = 101;

  /**
   * The requested ad size does not match an InMobi supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Native ad request is not a Unified native ad request.
   */
  public static final int ERROR_NON_UNIFIED_NATIVE_REQUEST = 103;

  /**
   * InMobi SDK isn't initialized yet.
   */
  public static final int ERROR_INMOBI_NOT_INITIALIZED = 104;

  /**
   * InMobi's ad is not yet ready to be shown.
   */
  public static final int ERROR_AD_NOT_READY = 105;

  /**
   * InMobi failed to display an ad.
   */
  public static final int ERROR_AD_DISPLAY_FAILED = 106;

  /**
   * InMobi returned a native ad with a missing required asset.
   */
  public static final int ERROR_MISSING_NATIVE_ASSETS = 107;

  /**
   * InMobi's native ad image assets contain a malformed URL.
   */
  public static final int ERROR_MALFORMED_IMAGE_URL = 108;

  /**
   * The adapter failed to download InMobi's native ad image assets.
   */
  public static final int ERROR_NATIVE_ASSET_DOWNLOAD_FAILED = 109;

  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(int errorCode, @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, INMOBI_SDK_ERROR_DOMAIN);
  }

  /** A private constructor since this is a utility class which should not be instantiated. */
  private InMobiConstants() {}
}
