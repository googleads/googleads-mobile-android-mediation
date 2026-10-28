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

package com.google.ads.mediation.adsurge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adsurge.adn.ads.AdSurgeAdError
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdSurgeUtilsTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var fakeSdkWrapper: FakeSdkWrapper

  @Before
  fun setUp() {
    fakeSdkWrapper = FakeSdkWrapper()
    AdSurgeFactory.delegate = fakeSdkWrapper
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    )
  }

  @After
  fun tearDown() {
    AdSurgeFactory.reset()
  }

  @Test
  fun createAdapterError_returnsExpectedAdError() {
    val error =
      AdSurgeUtils.createAdapterError(
        AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS,
        "Invalid server parameters.",
      )

    assertThat(error.code).isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(error.message).isEqualTo("Invalid server parameters.")
    assertThat(error.domain).isEqualTo(AdSurgeMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun createSdkError_withValidAdSurgeAdError_returnsExpectedAdError() {
    val sdkError = AdSurgeAdError(201, "No fill")

    val error = AdSurgeUtils.createSdkError(sdkError)

    assertThat(error.code).isEqualTo(201)
    assertThat(error.message).isEqualTo("No fill")
    assertThat(error.domain).isEqualTo(AdSurgeMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun createSdkError_withNullOrEmptyMessage_returnsDefaultMessage() {
    val sdkError = AdSurgeAdError(500, "")

    val error = AdSurgeUtils.createSdkError(sdkError)

    assertThat(error.code).isEqualTo(500)
    assertThat(error.message).isEqualTo("AdSurge SDK error.")
    assertThat(error.domain).isEqualTo(AdSurgeMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun applyPrivacyConfiguration_whenChildDirectedTrue_setsAgeRestrictedUserTrue() {
    AdSurgeUtils.applyPrivacyConfiguration(
      childDirected = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE,
      underAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
    )

    assertThat(fakeSdkWrapper.ageRestrictedUser).isTrue()
  }

  @Test
  fun applyPrivacyConfiguration_whenUnderAgeOfConsentTrue_setsAgeRestrictedUserTrue() {
    AdSurgeUtils.applyPrivacyConfiguration(
      childDirected = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      underAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE,
    )

    assertThat(fakeSdkWrapper.ageRestrictedUser).isTrue()
  }

  @Test
  fun applyPrivacyConfiguration_whenBothFalse_setsAgeRestrictedUserFalse() {
    AdSurgeUtils.applyPrivacyConfiguration(
      childDirected = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
      underAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE,
    )

    assertThat(fakeSdkWrapper.ageRestrictedUser).isFalse()
  }

  @Test
  fun applyPrivacyConfiguration_whenBothUnspecified_doesNotSetAgeRestrictedUser() {
    AdSurgeUtils.applyPrivacyConfiguration(
      childDirected = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      underAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
    )

    assertThat(fakeSdkWrapper.ageRestrictedUser).isNull()
  }

  @Test
  fun applyPrivacyConfiguration_whenAgeRestrictedTreatmentChild_setsAgeRestrictedUserTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD).build()
    )

    AdSurgeUtils.applyPrivacyConfiguration(
      childDirected = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      underAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
    )

    assertThat(fakeSdkWrapper.ageRestrictedUser).isTrue()
  }

  @Test
  fun applyPrivacyConfiguration_whenAgeRestrictedTreatmentTeen_doesNotSetAgeRestrictedUser() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN).build()
    )

    AdSurgeUtils.applyPrivacyConfiguration(
      childDirected = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      underAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
    )

    assertThat(fakeSdkWrapper.ageRestrictedUser).isNull()
  }

  @Test
  fun applyPrivacyConfiguration_withMediationAdConfiguration_delegatesCorrectly() {
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        taggedForChildDirectedTreatment = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE,
      )

    AdSurgeUtils.applyPrivacyConfiguration(adConfiguration)

    assertThat(fakeSdkWrapper.ageRestrictedUser).isTrue()
  }
}
