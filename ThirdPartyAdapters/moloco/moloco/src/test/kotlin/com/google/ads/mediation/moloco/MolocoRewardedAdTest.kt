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
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.RewardedInterstitialAd
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
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

  private lateinit var molocoRewardedAd: MolocoRewardedAd
  private lateinit var mediationAdConfiguration: MediationRewardedAdConfiguration
  private lateinit var mockMoloco: MockedStatic<Moloco>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockRewardedAd = mock<RewardedInterstitialAd>()
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()
  private val mockMediationAdCallback = mock<MediationRewardedAdCallback>()

  @Before
  fun setUp() {
    mockMoloco = mockStatic(Moloco::class.java)
    mediationAdConfiguration = createMediationRewardedAdConfiguration()
    MolocoRewardedAd.newInstance(mediationAdConfiguration, mockMediationAdLoadCallback).onSuccess {
      molocoRewardedAd = it
    }
    whenever(mockMediationAdLoadCallback.onSuccess(molocoRewardedAd)) doReturn
      mockMediationAdCallback
  }

  @After
  fun tearDown() {
    mockMoloco.close()
  }

  // region newInstance Tests
  @Test
  fun newInstance_emptyAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val configuration = createMediationRewardedAdConfiguration(serverParameters = serverParameters)
    val callback = mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )

    val result = MolocoRewardedAd.newInstance(configuration, callback)

    assertThat(result.isFailure).isTrue()
    verify(callback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun newInstance_validConfiguration_returnsSuccess() {
    val configuration = createMediationRewardedAdConfiguration()
    val callback = mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    val result = MolocoRewardedAd.newInstance(configuration, callback)

    assertThat(result.isSuccess).isTrue()
  }

  // endregion

  // region loadAd Tests
  @Test
  fun loadAd_createsRewardedInterstitialAndLoadsAd() {
    val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()
    val mediationInfoCaptor = argumentCaptor<MediationInfo>()

    molocoRewardedAd.loadAd()

    mockMoloco.verify {
      Moloco.createRewardedInterstitial(
        mediationInfoCaptor.capture(),
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createRewardedCaptor.capture(),
      )
    }
    assertThat(mediationInfoCaptor.firstValue.name).isEqualTo(MEDIATION_PLATFORM_NAME)

    val capturedCallback = createRewardedCaptor.firstValue
    capturedCallback.invoke(mockRewardedAd, /* molocoError= */ null)

    verify(mockRewardedAd).load(eq(TEST_BID_RESPONSE), eq(molocoRewardedAd))
  }

  @Test
  fun loadAd_whenMolocoErrorIsReported_invokesOnFailure() {
    val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()
    val molocoError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
    val expectedAdError =
      AdError(
        MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
        MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoRewardedAd.loadAd()

    mockMoloco.verify {
      Moloco.createRewardedInterstitial(
        any(),
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createRewardedCaptor.capture(),
      )
    }
    val capturedCallback = createRewardedCaptor.firstValue
    capturedCallback.invoke(/* returnedAd= */ null, molocoError)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenRewardedIsNullAndNoMolocoErrorIsReported_invokesOnFailure() {
    val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()
    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
        MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )

    molocoRewardedAd.loadAd()

    mockMoloco.verify {
      Moloco.createRewardedInterstitial(
        any(),
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createRewardedCaptor.capture(),
      )
    }
    val capturedCallback = createRewardedCaptor.firstValue
    capturedCallback.invoke(/* returnedAd= */ null, /* molocoError= */ null)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
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
    mockMoloco.verify {
      Moloco.createRewardedInterstitial(
        any(),
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createRewardedCaptor.capture(),
      )
    }
    val capturedCallback = createRewardedCaptor.firstValue
    capturedCallback.invoke(mockRewardedAd, /* error= */ null)
  }

  private fun createMediationRewardedAdConfiguration(
    serverParameters: android.os.Bundle =
      bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
  ): MediationRewardedAdConfiguration {
    return MediationRewardedAdConfiguration(
      context,
      TEST_BID_RESPONSE,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
