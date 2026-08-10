// Copyright 2023 Google LLC
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

package com.google.ads.mediation.line

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.NeedChildDirectedTreatment
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class LineInitializerTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockSdkWrapper = mock<SdkWrapper>()
  private val fiveAdConfig = FiveAdConfig(TEST_APP_ID)
  private val mockSdkFactory =
    mock<SdkFactory> { on { createFiveAdConfig(TEST_APP_ID) } doReturn fiveAdConfig }

  @Before
  fun setUp() {
    LineSdkWrapper.delegate = mockSdkWrapper
    LineSdkFactory.delegate = mockSdkFactory
    LineInitializer.resetFiveAdConfig()
    LineMediationAdapter.setTestMode(false)
    MobileAds.setRequestConfiguration(RequestConfiguration.Builder().build())
  }

  @Test
  fun initialize_whenNotInitialized_invokesLineSdkInitialize() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false

    LineInitializer.initialize(context, TEST_APP_ID)

    verify(mockSdkWrapper).initialize(eq(context), eq(fiveAdConfig))
  }

  @Test
  fun initialize_whenAlreadyInitialized_doesNotInvokeLineSdkInitialize() {
    whenever(mockSdkWrapper.isInitialized()) doReturn true

    LineInitializer.initialize(context, TEST_APP_ID)

    verify(mockSdkWrapper, never()).initialize(any(), any())
  }

  @Test
  fun getFiveAdConfig_withTagForChildDirectedTreatmentTrue_setsNeedChildDirectedTreatmentTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.TRUE)
  }

  @Test
  fun getFiveAdConfig_withTagForUnderAgeOfConsentTrue_setsNeedChildDirectedTreatmentTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.TRUE)
  }

  @Test
  fun getFiveAdConfig_withAgeRestrictedTreatmentChild_setsNeedChildDirectedTreatmentTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD).build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.TRUE)
  }

  @Test
  fun getFiveAdConfig_withTagForChildDirectedTreatmentFalse_setsNeedChildDirectedTreatmentFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.FALSE)
  }

  @Test
  fun getFiveAdConfig_withTagForUnderAgeOfConsentFalse_setsNeedChildDirectedTreatmentFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.FALSE)
  }

  @Test
  fun getFiveAdConfig_withUnspecifiedTreatment_setsNeedChildDirectedTreatmentUnspecified() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.UNSPECIFIED)
  }

  @Test
  fun getFiveAdConfig_withAgeRestrictedTreatmentTeen_setsNeedChildDirectedTreatmentUnspecified() {
    val requestConfiguration =
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN).build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.UNSPECIFIED)
  }

  @Test
  fun getFiveAdConfig_withTestModeTrue_setsIsTestTrue() {
    LineMediationAdapter.setTestMode(true)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.isTest).isTrue()
  }

  @Test
  fun getFiveAdConfig_withTestModeFalse_setsIsTestFalse() {
    LineMediationAdapter.setTestMode(false)

    val config = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config.isTest).isFalse()
  }

  @Test
  fun getFiveAdConfig_calledMultipleTimes_returnsCachedConfig() {
    val config1 = LineInitializer.getFiveAdConfig(TEST_APP_ID)
    val config2 = LineInitializer.getFiveAdConfig(TEST_APP_ID)

    assertThat(config1).isSameInstanceAs(config2)
    verify(mockSdkFactory, times(1)).createFiveAdConfig(TEST_APP_ID)
  }

  @Test
  fun resetFiveAdConfig_clearsCachedConfig() {
    LineInitializer.getFiveAdConfig(TEST_APP_ID)

    LineInitializer.resetFiveAdConfig()

    LineInitializer.getFiveAdConfig(TEST_APP_ID)
    verify(mockSdkFactory, times(2)).createFiveAdConfig(TEST_APP_ID)
  }

  private companion object {
    const val TEST_APP_ID = "testAppId"
  }
}
