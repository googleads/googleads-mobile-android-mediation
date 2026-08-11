// Copyright 2026 Google LLC
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import com.google.ads.mediation.adaptertestkit.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DTExchangeErrorCodesTest {

  @Test
  fun getAdError_forFyberInitStatus_returnsExpectedAdError() {
    val statusToExpectedCode =
      mapOf(
        FyberInitStatus.SUCCESSFULLY to 200,
        FyberInitStatus.FAILED_NO_KITS_DETECTED to 201,
        FyberInitStatus.FAILED to 202,
        FyberInitStatus.INVALID_APP_ID to 203,
      )

    for ((status, expectedCode) in statusToExpectedCode) {
      val adError = DTExchangeErrorCodes.getAdError(status)
      assertThat(adError).hasCode(expectedCode)
      assertThat(adError).hasMessage("DT Exchange failed to initialize with reason: $status")
      assertThat(adError).hasDomain(DTExchangeErrorCodes.ERROR_DOMAIN)
    }
  }

  @Test
  fun getAdError_forInneractiveErrorCode_returnsExpectedAdError() {
    val inneractiveErrorCodeToExpectedCode =
      mapOf(
        InneractiveErrorCode.CONNECTION_ERROR to 300,
        InneractiveErrorCode.CONNECTION_TIMEOUT to 301,
        InneractiveErrorCode.NO_FILL to 302,
        InneractiveErrorCode.SERVER_INVALID_RESPONSE to 303,
        InneractiveErrorCode.SERVER_INTERNAL_ERROR to 304,
        InneractiveErrorCode.SDK_INTERNAL_ERROR to 305,
        InneractiveErrorCode.UNSPECIFIED to 306,
        InneractiveErrorCode.LOAD_TIMEOUT to 307,
        InneractiveErrorCode.INVALID_INPUT to 308,
        InneractiveErrorCode.SPOT_DISABLED to 309,
        InneractiveErrorCode.UNSUPPORTED_SPOT to 310,
        InneractiveErrorCode.IN_FLIGHT_TIMEOUT to 311,
        InneractiveErrorCode.SDK_NOT_INITIALIZED to 312,
        InneractiveErrorCode.NON_SECURE_CONTENT_DETECTED to 313,
        InneractiveErrorCode.ERROR_CONFIGURATION_MISMATCH to 314,
        InneractiveErrorCode.NATIVE_ADS_NOT_SUPPORTED_FOR_OS to 315,
        InneractiveErrorCode.ERROR_CONFIGURATION_NO_SUCH_SPOT to 316,
        InneractiveErrorCode.SDK_NOT_INITIALIZED_OR_CONFIG_ERROR to 317,
        InneractiveErrorCode.ERROR_CODE_NATIVE_VIDEO_NOT_SUPPORTED to 399,
      )

    for ((errorCode, expectedCode) in inneractiveErrorCodeToExpectedCode) {
      val adError = DTExchangeErrorCodes.getAdError(errorCode)
      assertThat(adError).hasCode(expectedCode)
      assertThat(adError).hasMessage("DT Exchange failed to request ad with reason: $errorCode")
      assertThat(adError).hasDomain(DTExchangeErrorCodes.ERROR_DOMAIN)
    }
  }

  @Test
  fun getAdError_forNullInneractiveErrorCode_returnsFallbackError() {
    val adError = DTExchangeErrorCodes.getAdError(null as InneractiveErrorCode?)
    assertThat(adError).hasCode(399)
    assertThat(adError).hasMessage("DT Exchange failed to request ad with reason: null")
    assertThat(adError).hasDomain(DTExchangeErrorCodes.ERROR_DOMAIN)
  }
}
