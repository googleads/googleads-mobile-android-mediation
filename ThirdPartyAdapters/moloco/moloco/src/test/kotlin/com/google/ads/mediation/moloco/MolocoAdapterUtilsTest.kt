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

package com.google.ads.mediation.moloco

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.MolocoAdError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/** Unit tests for [MolocoAdapterUtils]. */
@RunWith(AndroidJUnit4::class)
class MolocoAdapterUtilsTest {

  private val mockSdkWrapper = mock<SdkWrapper>()

  @Before
  fun setUp() {
    MolocoSdkWrapper.delegate = mockSdkWrapper
  }

  @Test
  fun adapterVersion_returnsBuildConfigAdapterVersion() {
    assertThat(MolocoAdapterUtils.adapterVersion).isEqualTo(BuildConfig.ADAPTER_VERSION)
  }

  @Test
  fun setMolocoIsAgeRestricted_delegatesToSdkWrapper() {
    MolocoAdapterUtils.setMolocoIsAgeRestricted(true)
    verify(mockSdkWrapper).setAgeRestricted(true)

    MolocoAdapterUtils.setMolocoIsAgeRestricted(false)
    verify(mockSdkWrapper).setAgeRestricted(false)
  }

  @Test
  fun getAdError_forMolocoAdError_mapsCorrectly() {
    for (errorType in MolocoAdError.ErrorType.entries) {
      val molocoAdError =
        MolocoAdError(
          networkName = "testNetwork",
          adUnitId = "testUnit",
          errorType = errorType,
          description = errorType.description,
        )

      val adError = MolocoAdapterUtils.getAdError(molocoAdError)

      assertThat(adError.code).isEqualTo(errorType.errorCode)
      assertThat(adError.message).isEqualTo(errorType.description)
      assertThat(adError.domain).isEqualTo(MolocoMediationAdapter.SDK_ERROR_DOMAIN)
    }
  }

  @Test
  fun getAdError_forErrorType_mapsCorrectly() {
    for (errorType in MolocoAdError.ErrorType.entries) {
      val adError = MolocoAdapterUtils.getAdError(errorType)

      assertThat(adError.code).isEqualTo(errorType.errorCode)
      assertThat(adError.message).isEqualTo(errorType.description)
      assertThat(adError.domain).isEqualTo(MolocoMediationAdapter.SDK_ERROR_DOMAIN)
    }
  }

  @Test
  fun getAdError_forAdCreateError_mapsCorrectly() {
    for (adCreateError in MolocoAdError.AdCreateError.entries) {
      val adError = MolocoAdapterUtils.getAdError(adCreateError)

      assertThat(adError.code).isEqualTo(adCreateError.errorCode)
      assertThat(adError.message).isEqualTo(adCreateError.description)
      assertThat(adError.domain).isEqualTo(MolocoMediationAdapter.SDK_ERROR_DOMAIN)
    }
  }
}
