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

package com.google.ads.mediation.pangle

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.pangle.PangleConstants.ERROR_CHILD_USER
import com.google.ads.mediation.pangle.PangleConstants.ERROR_DOMAIN
import com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.pangle.PangleConstants.ERROR_MSG_CHILD_USER
import com.google.ads.mediation.pangle.PangleConstants.PANGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [PangleConstants]. */
@RunWith(AndroidJUnit4::class)
class PangleConstantsTest {

  @Before
  fun setUp() {
    MobileAds.setRequestConfiguration(RequestConfiguration.Builder().build())
  }

  @Test
  fun createAdapterError_returnsAdErrorWithAdapterDomain() {
    val error = PangleConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS, ERROR_MESSAGE)

    assertThat(error)
      .hasCode(ERROR_INVALID_SERVER_PARAMETERS)
      .hasMessage(ERROR_MESSAGE)
      .hasDomain(ERROR_DOMAIN)
      .hasNoCause()
  }

  @Test
  fun createSdkError_returnsAdErrorWithPangleDomain() {
    val error = PangleConstants.createSdkError(SDK_ERROR_CODE, ERROR_MESSAGE)

    assertThat(error)
      .hasCode(SDK_ERROR_CODE)
      .hasMessage(ERROR_MESSAGE)
      .hasDomain(PANGLE_SDK_ERROR_DOMAIN)
      .hasNoCause()
  }

  @Test
  fun createChildUserError_returnsAdErrorWithChildUserCodeAndDomain() {
    val error = PangleConstants.createChildUserError()

    assertThat(error)
      .hasCode(ERROR_CHILD_USER)
      .hasMessage(ERROR_MSG_CHILD_USER)
      .hasDomain(ERROR_DOMAIN)
      .hasNoCause()
  }

  @Test
  fun isChildUser_whenTaggedChildDirected_returnsTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )

    assertThat(PangleConstants.isChildUser()).isTrue()
  }

  @Test
  fun isChildUser_whenTaggedUnderAgeOfConsent_returnsTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )

    assertThat(PangleConstants.isChildUser()).isTrue()
  }

  @Test
  fun isChildUser_whenAgeRestrictedTreatmentChild_returnsTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD).build()
    )

    assertThat(PangleConstants.isChildUser()).isTrue()
  }

  @Test
  fun isChildUser_whenExplicitlyNotChild_returnsFalse() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    )

    assertThat(PangleConstants.isChildUser()).isFalse()
  }

  @Test
  fun isChildUser_whenNotTagged_returnsFalse() {
    MobileAds.setRequestConfiguration(RequestConfiguration.Builder().build())

    assertThat(PangleConstants.isChildUser()).isFalse()
  }

  private companion object {
    const val ERROR_MESSAGE = "test_error_message"
    const val SDK_ERROR_CODE = 1005
  }
}
