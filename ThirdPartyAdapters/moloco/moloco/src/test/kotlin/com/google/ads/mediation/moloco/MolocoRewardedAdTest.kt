// Copyright 2024 Google LLC
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

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.MEDIATION_PLATFORM_NAME
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.CreateRewardedInterstitialAdCallback
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.MolocoAdError.AdCreateError
import com.moloco.sdk.publisher.RewardedInterstitialAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MolocoRewardedAdTest {
  // Subject of tests
  private lateinit var molocoRewardedAd: MolocoRewardedAd
  private lateinit var mediationAdConfiguration: MediationRewardedAdConfiguration

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockRewardedAd = mock<RewardedInterstitialAd>()
  private val mockSdkFactory = mock<SdkFactory>()
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()
  private val mockMediationAdCallback = mock<MediationRewardedAdCallback>()

  @Before
  fun setUp() {
    MolocoSdkFactory.delegate = mockSdkFactory

    // Properly initialize molocoRewardedAd
    mediationAdConfiguration = createMediationRewardedAdConfiguration()
    MolocoRewardedAd.newInstance(mediationAdConfiguration, mockMediationAdLoadCallback).onSuccess {
      molocoRewardedAd = it
    }
    whenever(mockMediationAdLoadCallback.onSuccess(molocoRewardedAd)) doReturn
      mockMediationAdCallback
  }

  // region newInstance Tests
  @Test
  fun newInstance_withValidParameters_returnsSuccess() {
    val config = createMediationRewardedAdConfiguration()

    val result = MolocoRewardedAd.newInstance(config, mockMediationAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun newInstance_withMissingAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf()
    val config = createMediationRewardedAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = MolocoRewardedAd.newInstance(config, mockMediationAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    verify(mockMediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT)
    assertThat(capturedError.message).isEqualTo(MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT)
    assertThat(capturedError.domain).isEqualTo(MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun newInstance_withEmptyAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val config = createMediationRewardedAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = MolocoRewardedAd.newInstance(config, mockMediationAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    verify(mockMediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT)
    assertThat(capturedError.message).isEqualTo(MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT)
    assertThat(capturedError.domain).isEqualTo(MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  // endregion

  // region loadAd Tests
  @Test
  fun loadAd_createsRewardedWithCorrectParameters() {
    molocoRewardedAd.loadAd()

    verify(mockSdkFactory)
      .createRewarded(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        any(),
      )
  }

  @Test
  fun loadAd_whenAdCreationFailsWithMolocoError_invokesOnFailure() {
    val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()

    molocoRewardedAd.loadAd()

    verify(mockSdkFactory)
      .createRewarded(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createRewardedCaptor.capture(),
      )
    val capturedCallback = createRewardedCaptor.firstValue
    val molocoError = AdCreateError.SDK_INIT_FAILED
    capturedCallback.invoke(/* returnedAd= */ null, molocoError)

    val expectedAdError =
      AdError(
        AdCreateError.SDK_INIT_FAILED.errorCode,
        AdCreateError.SDK_INIT_FAILED.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenAdIsNullAndNoErrorReported_invokesOnFailure() {
    val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()

    molocoRewardedAd.loadAd()

    verify(mockSdkFactory)
      .createRewarded(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createRewardedCaptor.capture(),
      )
    val capturedCallback = createRewardedCaptor.firstValue
    capturedCallback.invoke(/* returnedAd= */ null, /* molocoError= */ null)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
        MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenAdCreated_loadsBidResponse() {
    loadRewardedAd()

    verify(mockRewardedAd).load(eq(TEST_BID_RESPONSE), eq(molocoRewardedAd))
  }

  // endregion

  // region showAd Tests
  @Test
  fun showAd_invokesMolocoShow() {
    loadRewardedAd()

    molocoRewardedAd.showAd(context)

    verify(mockRewardedAd).show(molocoRewardedAd)
  }

  // endregion

  // region Callback Tests
  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val testError =
      MolocoAdError("testNetwork", "testAdUnit", MolocoAdError.ErrorType.UNKNOWN, "testDesc")
    val expectedAdError =
      AdError(
        MolocoAdError.ErrorType.UNKNOWN.errorCode,
        MolocoAdError.ErrorType.UNKNOWN.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoRewardedAd.onAdLoadFailed(testError)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdLoadSuccess_invokesOnSuccess() {
    molocoRewardedAd.onAdLoadSuccess(mock())

    verify(mockMediationAdLoadCallback).onSuccess(molocoRewardedAd)
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    molocoRewardedAd.onAdLoadSuccess(mock())

    molocoRewardedAd.onAdClicked(mock())

    verify(mockMediationAdCallback).reportAdClicked()
  }

  @Test
  fun onAdHidden_invokesOnAdClosed() {
    molocoRewardedAd.onAdLoadSuccess(mock())

    molocoRewardedAd.onAdHidden(mock())

    verify(mockMediationAdCallback).onAdClosed()
  }

  @Test
  fun onAdShowFailed_invokesOnAdFailedToShow() {
    molocoRewardedAd.onAdLoadSuccess(mock())
    val testError =
      MolocoAdError("testNetwork", "testAdUnit", MolocoAdError.ErrorType.UNKNOWN, "testDesc")
    val expectedAdError =
      AdError(
        MolocoAdError.ErrorType.UNKNOWN.errorCode,
        MolocoAdError.ErrorType.UNKNOWN.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoRewardedAd.onAdShowFailed(testError)

    verify(mockMediationAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdShowSuccess_invokesOnAdOpenedAndReportAdImpression() {
    molocoRewardedAd.onAdLoadSuccess(mock())

    molocoRewardedAd.onAdShowSuccess(mock())

    verify(mockMediationAdCallback).onAdOpened()
    verify(mockMediationAdCallback).reportAdImpression()
  }

  @Test
  fun onRewardedVideoCompleted_invokesOnVideoComplete() {
    molocoRewardedAd.onAdLoadSuccess(mock())

    molocoRewardedAd.onRewardedVideoCompleted(mock())

    verify(mockMediationAdCallback).onVideoComplete()
  }

  @Test
  fun onRewardedVideoStarted_invokesOnVideoStart() {
    molocoRewardedAd.onAdLoadSuccess(mock())

    molocoRewardedAd.onRewardedVideoStarted(mock())

    verify(mockMediationAdCallback).onVideoStart()
  }

  @Test
  fun onUserRewarded_invokesOnUserEarnedReward() {
    molocoRewardedAd.onAdLoadSuccess(mock())

    molocoRewardedAd.onUserRewarded(mock())

    verify(mockMediationAdCallback).onUserEarnedReward()
  }

  // endregion

  private fun loadRewardedAd() {
    molocoRewardedAd.loadAd()
    val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()
    verify(mockSdkFactory)
      .createRewarded(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createRewardedCaptor.capture(),
      )
    val capturedCallback = createRewardedCaptor.firstValue
    capturedCallback.invoke(mockRewardedAd, /* molocoError= */ null)
  }

  private fun createMediationRewardedAdConfiguration(
    serverParameters: Bundle = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT),
    bidResponse: String = TEST_BID_RESPONSE,
    watermark: String = TEST_WATERMARK,
  ): MediationRewardedAdConfiguration {
    return MediationRewardedAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      watermark,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
