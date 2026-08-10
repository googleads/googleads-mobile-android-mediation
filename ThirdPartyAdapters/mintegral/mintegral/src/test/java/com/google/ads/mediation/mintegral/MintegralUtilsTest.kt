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

package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_BID_RESPONSE
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.common.truth.Truth.assertThat
import com.mbridge.msdk.MBridgeSDK
import com.mbridge.msdk.out.MBConfiguration
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

/** Tests for [MintegralUtils]. */
@RunWith(AndroidJUnit4::class)
class MintegralUtilsTest {

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockMBridgeSdk: MBridgeSDK = mock()

  @Before
  fun setUp() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfig)
  }

  @Test
  fun getAdapterVersion_returnsAdapterVersion() {
    assertThat(MintegralUtils.getAdapterVersion()).isEqualTo(BuildConfig.ADAPTER_VERSION)
  }

  @Test
  fun getSdkVersion_returnsMBConfigurationSdkVersion() {
    assertThat(MintegralUtils.getSdkVersion()).isEqualTo(MBConfiguration.SDK_VERSION)
  }

  @Test
  fun shouldMuteAudio_withMuteAudioTrue_returnsTrue() {
    val bundle = bundleOf(MintegralExtras.Keys.MUTE_AUDIO to true)

    assertThat(MintegralUtils.shouldMuteAudio(bundle)).isTrue()
  }

  @Test
  fun shouldMuteAudio_withMuteAudioFalse_returnsFalse() {
    val bundle = bundleOf(MintegralExtras.Keys.MUTE_AUDIO to false)

    assertThat(MintegralUtils.shouldMuteAudio(bundle)).isFalse()
  }

  @Test
  fun shouldMuteAudio_withEmptyBundle_returnsFalse() {
    assertThat(MintegralUtils.shouldMuteAudio(bundleOf())).isFalse()
  }

  @Test
  fun validateMintegralAdLoadParams_withNullAdUnitId_returnsAdError() {
    val error = MintegralUtils.validateMintegralAdLoadParams(null, TEST_PLACEMENT_ID)

    val nonNullError = assertNotNull(error)
    val expectedError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    assertThat(AdErrorMatcher(expectedError).matches(nonNullError)).isTrue()
  }

  @Test
  fun validateMintegralAdLoadParams_withEmptyAdUnitId_returnsAdError() {
    val error = MintegralUtils.validateMintegralAdLoadParams("", TEST_PLACEMENT_ID)

    val nonNullError = assertNotNull(error)
    val expectedError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    assertThat(AdErrorMatcher(expectedError).matches(nonNullError)).isTrue()
  }

  @Test
  fun validateMintegralAdLoadParams_withNullPlacementId_returnsAdError() {
    val error = MintegralUtils.validateMintegralAdLoadParams(TEST_AD_UNIT, null)

    val nonNullError = assertNotNull(error)
    val expectedError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    assertThat(AdErrorMatcher(expectedError).matches(nonNullError)).isTrue()
  }

  @Test
  fun validateMintegralAdLoadParams_withEmptyPlacementId_returnsAdError() {
    val error = MintegralUtils.validateMintegralAdLoadParams(TEST_AD_UNIT, "")

    val nonNullError = assertNotNull(error)
    val expectedError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    assertThat(AdErrorMatcher(expectedError).matches(nonNullError)).isTrue()
  }

  @Test
  fun validateMintegralAdLoadParams_withValidParams_returnsNull() {
    val error = MintegralUtils.validateMintegralAdLoadParams(TEST_AD_UNIT, TEST_PLACEMENT_ID)

    assertThat(error).isNull()
  }

  @Test
  fun validateMintegralAdLoadParams_withBidResponse_withInvalidServerParams_returnsAdError() {
    val error =
      MintegralUtils.validateMintegralAdLoadParams(null, TEST_PLACEMENT_ID, TEST_BID_RESPONSE)

    val nonNullError = assertNotNull(error)
    val expectedError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    assertThat(AdErrorMatcher(expectedError).matches(nonNullError)).isTrue()
  }

  @Test
  fun validateMintegralAdLoadParams_withBidResponse_withNullBidToken_returnsAdError() {
    val error = MintegralUtils.validateMintegralAdLoadParams(TEST_AD_UNIT, TEST_PLACEMENT_ID, null)

    val nonNullError = assertNotNull(error)
    val expectedError =
      AdError(
        ERROR_INVALID_BID_RESPONSE,
        "Missing or invalid Mintegral bidding signal in this ad request.",
        ERROR_DOMAIN,
      )
    assertThat(AdErrorMatcher(expectedError).matches(nonNullError)).isTrue()
  }

  @Test
  fun validateMintegralAdLoadParams_withBidResponse_withEmptyBidToken_returnsAdError() {
    val error = MintegralUtils.validateMintegralAdLoadParams(TEST_AD_UNIT, TEST_PLACEMENT_ID, "")

    val nonNullError = assertNotNull(error)
    val expectedError =
      AdError(
        ERROR_INVALID_BID_RESPONSE,
        "Missing or invalid Mintegral bidding signal in this ad request.",
        ERROR_DOMAIN,
      )
    assertThat(AdErrorMatcher(expectedError).matches(nonNullError)).isTrue()
  }

  @Test
  fun validateMintegralAdLoadParams_withBidResponse_withValidParams_returnsNull() {
    val error =
      MintegralUtils.validateMintegralAdLoadParams(
        TEST_AD_UNIT,
        TEST_PLACEMENT_ID,
        TEST_BID_RESPONSE,
      )

    assertThat(error).isNull()
  }

  @Test
  fun convertDipToPixel_returnsExpectedPixels() {
    val pixels = MintegralUtils.convertDipToPixel(context, 50f)

    assertThat(pixels).isGreaterThan(0)
  }

  @Test
  fun configureMintegralPrivacy_withChildDirectedTrue_setsCoppaTrue() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfig)

    MintegralUtils.configureMintegralPrivacy(context, mockMBridgeSdk)

    verify(mockMBridgeSdk).setCoppaStatus(eq(context), eq(true))
  }

  @Test
  fun configureMintegralPrivacy_withUnderAgeConsentTrue_setsCoppaTrue() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfig)

    MintegralUtils.configureMintegralPrivacy(context, mockMBridgeSdk)

    verify(mockMBridgeSdk).setCoppaStatus(eq(context), eq(true))
  }

  @Test
  fun configureMintegralPrivacy_withAgeRestrictedTreatmentChild_setsCoppaTrue() {
    val requestConfig =
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD).build()
    MobileAds.setRequestConfiguration(requestConfig)

    MintegralUtils.configureMintegralPrivacy(context, mockMBridgeSdk)

    verify(mockMBridgeSdk).setCoppaStatus(eq(context), eq(true))
  }

  @Test
  fun configureMintegralPrivacy_withChildDirectedFalse_setsCoppaFalse() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .build()
    MobileAds.setRequestConfiguration(requestConfig)

    MintegralUtils.configureMintegralPrivacy(context, mockMBridgeSdk)

    verify(mockMBridgeSdk).setCoppaStatus(eq(context), eq(false))
  }

  @Test
  fun configureMintegralPrivacy_withUnderAgeConsentFalse_setsCoppaFalse() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfig)

    MintegralUtils.configureMintegralPrivacy(context, mockMBridgeSdk)

    verify(mockMBridgeSdk).setCoppaStatus(eq(context), eq(false))
  }

  @Test
  fun configureMintegralPrivacy_withUnspecified_doesNotSetCoppa() {
    MintegralUtils.configureMintegralPrivacy(context, mockMBridgeSdk)

    verify(mockMBridgeSdk, never()).setCoppaStatus(eq(context), any())
  }

  @Test
  fun getMintegralSlotIdentifiers_withValidConfigurations_returnsSlotIdentifiers() {
    val serverParams1 = bundleOf(AD_UNIT_ID to "unit1", PLACEMENT_ID to "placement1")
    val serverParams2 = bundleOf(AD_UNIT_ID to "unit2", PLACEMENT_ID to "placement2")
    val config1 = createMediationConfiguration(AdFormat.BANNER, serverParams1)
    val config2 = createMediationConfiguration(AdFormat.INTERSTITIAL, serverParams2)
    val rtbSignalData = RtbSignalData(context, listOf(config1, config2), bundleOf(), null)

    val slotIdentifiers = MintegralUtils.getMintegralSlotIdentifiers(rtbSignalData)

    assertThat(slotIdentifiers).hasSize(2)
    assertThat(slotIdentifiers[0]).isEqualTo(MintegralSlotIdentifier("unit1", "placement1"))
    assertThat(slotIdentifiers[1]).isEqualTo(MintegralSlotIdentifier("unit2", "placement2"))
  }

  @Test
  fun getMintegralSlotIdentifiers_withInvalidConfigurations_filtersOutInvalid() {
    val validParams = bundleOf(AD_UNIT_ID to "unit1", PLACEMENT_ID to "placement1")
    val missingUnit = bundleOf(PLACEMENT_ID to "placement2")
    val emptyPlacement = bundleOf(AD_UNIT_ID to "unit3", PLACEMENT_ID to "")
    val emptyUnit = bundleOf(AD_UNIT_ID to "", PLACEMENT_ID to "placement4")
    val config1 = createMediationConfiguration(AdFormat.BANNER, validParams)
    val config2 = createMediationConfiguration(AdFormat.INTERSTITIAL, missingUnit)
    val config3 = createMediationConfiguration(AdFormat.REWARDED, emptyPlacement)
    val config4 = createMediationConfiguration(AdFormat.NATIVE, emptyUnit)
    val rtbSignalData =
      RtbSignalData(context, listOf(config1, config2, config3, config4), bundleOf(), null)

    val slotIdentifiers = MintegralUtils.getMintegralSlotIdentifiers(rtbSignalData)

    assertThat(slotIdentifiers).containsExactly(MintegralSlotIdentifier("unit1", "placement1"))
  }
}
