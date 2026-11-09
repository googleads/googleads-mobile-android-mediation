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

import android.app.Activity
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.AdLoader
import com.five_corp.ad.AdSlotConfig
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdVideoReward
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class LineRewardedAdTest {
  // Subject of tests
  private lateinit var lineRewardedAd: LineRewardedAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mockFiveAdConfig = mock<FiveAdConfig>()
  private val mockAdLoader = mock<AdLoader>()
  private val slotConfig = AdSlotConfig(TEST_SLOT_ID)
  private val mockFiveAdVideoReward = mock<FiveAdVideoReward>()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createAdLoader(any(), any()) } doReturn mockAdLoader
      on { createAdSlotConfig(TEST_SLOT_ID) } doReturn slotConfig
    }
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    // Properly initialize lineRewardedAd
    val mediationAdConfiguration = createMediationRewardedAdConfiguration()
    LineRewardedAd.newInstance(mediationAdConfiguration, mediationAdLoadCallback).onSuccess {
      lineRewardedAd = it
    }
  }

  // region newInstance Tests
  @Test
  fun newInstance_withValidAppId_returnsSuccess() {
    val config = createMediationRewardedAdConfiguration()

    val result = LineRewardedAd.newInstance(config, mediationAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun newInstance_withMissingAppId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID)
    val config = createMediationRewardedAdConfiguration(serverParameters = serverParameters)

    val result = LineRewardedAd.newInstance(config, mediationAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    assertThat(result.isFailure).isTrue()
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun newInstance_withEmptyAppId_invokesOnFailureAndReturnsFailure() {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_APP_ID to "",
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
      )
    val config = createMediationRewardedAdConfiguration(serverParameters = serverParameters)

    val result = LineRewardedAd.newInstance(config, mediationAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    assertThat(result.isFailure).isTrue()
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  // endregion

  // region Waterfall loadAd Tests
  @Test
  fun loadAd_withNullSlotId_invokesOnFailure() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID)
    val config = createMediationRewardedAdConfiguration(serverParameters = serverParameters)
    var ad: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

    ad?.loadAd(activity)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
        LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadAd_withEmptySlotId_invokesOnFailure() {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
        LineMediationAdapter.KEY_SLOT_ID to "",
      )
    val config = createMediationRewardedAdConfiguration(serverParameters = serverParameters)
    var ad: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

    ad?.loadAd(activity)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
        LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadAd_withSoundExtrasFalse_disablesSound() {
    val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to false)
    val config = createMediationRewardedAdConfiguration(mediationExtras = mediationExtras)
    var ad: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

    ad?.loadAd(activity)

    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(eq(slotConfig), callbackCaptor.capture())
    callbackCaptor.firstValue.onLoad(mockFiveAdVideoReward)
    verify(mockFiveAdVideoReward).enableSound(false)
  }

  @Test
  fun loadAd_withSoundExtrasTrue_enablesSound() {
    val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to true)
    val config = createMediationRewardedAdConfiguration(mediationExtras = mediationExtras)
    var ad: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

    ad?.loadAd(activity)

    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(eq(slotConfig), callbackCaptor.capture())
    callbackCaptor.firstValue.onLoad(mockFiveAdVideoReward)
    verify(mockFiveAdVideoReward).enableSound(true)
  }

  @Test
  fun loadAd_whenNullAdLoader_returnsEarlySafely() {
    whenever(sdkFactory.createAdLoader(eq(activity), any())) doReturn null

    lineRewardedAd.loadAd(activity)

    verify(sdkFactory, never()).createAdSlotConfig(any())
  }

  // endregion

  // region RTB loadRtbAd Tests
  @Test
  fun loadRtbAd_success_invokesOnSuccess() {
    val mockAdLoader = mock<AdLoader>()
    whenever(sdkFactory.createAdLoader(eq(activity), any())) doReturn mockAdLoader
    val config = createMediationRewardedAdConfiguration(bidResponse = TEST_BID_RESPONSE)
    var rtbAd: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

    rtbAd?.loadRtbAd(activity)

    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(any<BidData>(), callbackCaptor.capture())
    callbackCaptor.firstValue.onLoad(mockFiveAdVideoReward)
    verify(mockFiveAdVideoReward).setEventListener(rtbAd!!)
    assertThat(mediationAdLoadCallback).hasSucceededWith(rtbAd!!)
  }

  @Test
  fun loadRtbAd_error_invokesOnFailure() {
    val mockAdLoader = mock<AdLoader>()
    whenever(sdkFactory.createAdLoader(eq(activity), any())) doReturn mockAdLoader
    val config = createMediationRewardedAdConfiguration(bidResponse = TEST_BID_RESPONSE)
    var rtbAd: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

    rtbAd?.loadRtbAd(activity)

    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(any<BidData>(), callbackCaptor.capture())
    callbackCaptor.firstValue.onError(FiveAdErrorCode.INTERNAL_ERROR)
    val expectedError =
      AdError(
        FiveAdErrorCode.INTERNAL_ERROR.value,
        FiveAdErrorCode.INTERNAL_ERROR.name,
        LineMediationAdapter.SDK_ERROR_DOMAIN,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRtbAd_whenNullAdLoader_returnsEarlySafely() {
    whenever(sdkFactory.createAdLoader(eq(activity), any())) doReturn null
    val config = createMediationRewardedAdConfiguration(bidResponse = TEST_BID_RESPONSE)
    var rtbAd: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

    rtbAd?.loadRtbAd(activity)

    verify(mockFiveAdVideoReward, never()).loadAdAsync()
  }

  @Test
  fun loadRtbAd_withSoundExtrasFalse_disablesSound() {
    val mockAdLoader = mock<AdLoader>()
    whenever(sdkFactory.createAdLoader(eq(activity), any())) doReturn mockAdLoader
    val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to false)
    val config =
      createMediationRewardedAdConfiguration(
        mediationExtras = mediationExtras,
        bidResponse = TEST_BID_RESPONSE,
      )
    var rtbAd: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

    rtbAd?.loadRtbAd(activity)

    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(any<BidData>(), callbackCaptor.capture())
    callbackCaptor.firstValue.onLoad(mockFiveAdVideoReward)
    verify(mockFiveAdVideoReward).enableSound(false)
  }

  @Test
  fun loadRtbAd_withSoundExtrasTrue_enablesSound() {
    val mockAdLoader = mock<AdLoader>()
    whenever(sdkFactory.createAdLoader(eq(activity), any())) doReturn mockAdLoader
    val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to true)
    val config =
      createMediationRewardedAdConfiguration(
        mediationExtras = mediationExtras,
        bidResponse = TEST_BID_RESPONSE,
      )
    var rtbAd: LineRewardedAd? = null
    LineRewardedAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

    rtbAd?.loadRtbAd(activity)

    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(any<BidData>(), callbackCaptor.capture())
    callbackCaptor.firstValue.onLoad(mockFiveAdVideoReward)
    verify(mockFiveAdVideoReward).enableSound(true)
  }

  // endregion

  @Test
  fun showAd_invokesFiveAdShowAd() {
    loadWaterfallRewardedAd()

    lineRewardedAd.showAd(activity)

    verify(mockFiveAdVideoReward).showAd()
  }

  @Test
  fun loadAd_onLoad_invokesOnSuccess() {
    loadWaterfallRewardedAd()

    verify(mockFiveAdVideoReward).setEventListener(lineRewardedAd)
    assertThat(mediationAdLoadCallback).hasSucceededWith(lineRewardedAd)
  }

  @Test
  fun loadAd_onError_invokesOnFailure() {
    lineRewardedAd.loadAd(activity)
    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(eq(slotConfig), callbackCaptor.capture())
    callbackCaptor.firstValue.onError(FiveAdErrorCode.INTERNAL_ERROR)

    val expectedError =
      AdError(
        FiveAdErrorCode.INTERNAL_ERROR.value,
        "FiveAd SDK returned a load error with code INTERNAL_ERROR.",
        LineMediationAdapter.SDK_ERROR_DOMAIN,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun onClick_invokesReportAdClicked() {
    loadWaterfallRewardedAd()

    lineRewardedAd.onClick(mockFiveAdVideoReward)

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onFullScreenClose_invokesOnAdClosed() {
    loadWaterfallRewardedAd()

    lineRewardedAd.onFullScreenClose(mockFiveAdVideoReward)

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onReward_invokesOnUserEarnedReward() {
    loadWaterfallRewardedAd()

    lineRewardedAd.onReward(mockFiveAdVideoReward)

    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    loadWaterfallRewardedAd()

    lineRewardedAd.onImpression(mockFiveAdVideoReward)

    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onViewError_invokesOnAdFailedToShow() {
    loadWaterfallRewardedAd()
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR

    lineRewardedAd.onViewError(mockFiveAdVideoReward, dummyErrorCode)

    val expectedError =
      AdError(
        dummyErrorCode.value,
        "FiveAd SDK could not show ad with error with code INTERNAL_ERROR.",
        LineMediationAdapter.SDK_ERROR_DOMAIN,
      )
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedError)
  }

  @Test
  fun onPlay_invokesOnVideoStart() {
    loadWaterfallRewardedAd()

    lineRewardedAd.onPlay(mockFiveAdVideoReward)

    assertThat(rewardedAdCallback.isVideoStarted).isTrue()
  }

  @Test
  fun onFullScreenOpen_invokesOnAdOpened() {
    loadWaterfallRewardedAd()

    lineRewardedAd.onFullScreenOpen(mockFiveAdVideoReward)

    assertThat(rewardedAdCallback.isOpened).isTrue()
  }

  @Test
  fun onPause_throwsNoException() {
    lineRewardedAd.onPause(mockFiveAdVideoReward)
  }

  @Test
  fun onViewThrough_invokesOnVideoComplete() {
    loadWaterfallRewardedAd()

    lineRewardedAd.onViewThrough(mockFiveAdVideoReward)

    assertThat(rewardedAdCallback.isVideoCompleted).isTrue()
  }

  private fun loadWaterfallRewardedAd() {
    lineRewardedAd.loadAd(activity)
    val callbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
    verify(mockAdLoader).loadRewardAd(eq(slotConfig), callbackCaptor.capture())
    callbackCaptor.firstValue.onLoad(mockFiveAdVideoReward)
  }

  private fun createMediationRewardedAdConfiguration(
    serverParameters: Bundle =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      ),
    mediationExtras: Bundle = Bundle(),
    bidResponse: String = "",
  ): MediationRewardedAdConfiguration {
    return MediationRewardedAdConfiguration(
      activity,
      bidResponse,
      serverParameters,
      mediationExtras,
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )
  }

  private companion object {
    const val TEST_APP_ID = "testAppId"
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_WATERMARK = "testWatermark"
  }
}
