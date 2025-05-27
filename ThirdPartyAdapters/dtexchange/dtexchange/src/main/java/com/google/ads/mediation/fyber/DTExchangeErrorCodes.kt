// Copyright 2020 Google LLC
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

package com.google.ads.mediation.fyber

import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import com.google.android.gms.ads.AdError

/** Error codes for the DT Exchange adapter. */
object DTExchangeErrorCodes {

  /** DT Exchange adapter error domain. */
  const val ERROR_DOMAIN = "com.google.ads.mediation.dtexchange"

  /** Server parameters, such as app ID or spot ID, are invalid. */
  const val ERROR_INVALID_SERVER_PARAMETERS = 101

  /** The requested ad size does not match a DT Exchange supported banner size. */
  const val ERROR_BANNER_SIZE_MISMATCH = 103

  /** DT Exchange SDK loaded an ad but returned an unexpected controller. */
  const val ERROR_WRONG_CONTROLLER_TYPE = 105

  /** Ad not ready. */
  const val ERROR_AD_NOT_READY = 106

  /** Context is not an activity instance. */
  const val ERROR_CONTEXT_NOT_ACTIVITY_INSTANCE = 107

  /**
   * Gets the specific AdError for the specified [FyberInitStatus].
   *
   * @param initStatus The Fyber initialization status.
   * @return The corresponding AdError.
   */
  @JvmStatic
  fun getAdError(initStatus: FyberInitStatus): AdError {
    var code =
      when (initStatus) {
        FyberInitStatus.SUCCESSFULLY -> 200
        FyberInitStatus.FAILED_NO_KITS_DETECTED -> 201
        FyberInitStatus.FAILED -> 202
        FyberInitStatus.INVALID_APP_ID -> 203
        // Error '299' to indicate that the error is new and has not been supported by the adapter
        // yet.
        else -> 299
      }
    return AdError(code, "DT Exchange failed to initialize with reason: $initStatus", ERROR_DOMAIN)
  }

  /**
   * Gets the specific AdError for the specified [InneractiveErrorCode].
   *
   * @param inneractiveErrorCode The Inneractive error code for ad request fail reason.
   * @return The corresponding AdError.
   */
  @JvmStatic // Optional: if you need to call this as a static method from Java
  fun getAdError(inneractiveErrorCode: InneractiveErrorCode): AdError {
    val code =
      when (inneractiveErrorCode) {
        InneractiveErrorCode.CONNECTION_ERROR -> 300
        InneractiveErrorCode.CONNECTION_TIMEOUT -> 301
        InneractiveErrorCode.NO_FILL -> 302
        InneractiveErrorCode.SERVER_INVALID_RESPONSE -> 303
        InneractiveErrorCode.SERVER_INTERNAL_ERROR -> 304
        InneractiveErrorCode.SDK_INTERNAL_ERROR -> 305
        InneractiveErrorCode.UNSPECIFIED -> 306
        InneractiveErrorCode.LOAD_TIMEOUT -> 307
        InneractiveErrorCode.INVALID_INPUT -> 308
        InneractiveErrorCode.SPOT_DISABLED -> 309
        InneractiveErrorCode.UNSUPPORTED_SPOT -> 310
        InneractiveErrorCode.IN_FLIGHT_TIMEOUT -> 311
        InneractiveErrorCode.SDK_NOT_INITIALIZED -> 312
        InneractiveErrorCode.NON_SECURE_CONTENT_DETECTED -> 313
        InneractiveErrorCode.ERROR_CONFIGURATION_MISMATCH -> 314
        InneractiveErrorCode.NATIVE_ADS_NOT_SUPPORTED_FOR_OS -> 315
        InneractiveErrorCode.ERROR_CONFIGURATION_NO_SUCH_SPOT -> 316
        InneractiveErrorCode.SDK_NOT_INITIALIZED_OR_CONFIG_ERROR -> 317
        InneractiveErrorCode.ERROR_CODE_NATIVE_VIDEO_NOT_SUPPORTED -> 399
        // Error '399' to indicate that the error is new and has not been supported by the adapter
        // yet.
        else -> 399
      }
    return AdError(
      code,
      "DT Exchange failed to request ad with reason: $inneractiveErrorCode",
      ERROR_DOMAIN,
    )
  }
}
