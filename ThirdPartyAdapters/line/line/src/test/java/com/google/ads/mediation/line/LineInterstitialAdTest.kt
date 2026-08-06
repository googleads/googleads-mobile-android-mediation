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
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterstitial
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
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
class LineInterstitialAdTest {
  // Subject of tests
  private lateinit var lineInterstitialAd: LineInterstitialAd
  private lateinit var mediationAdConfiguration: MediationInterstitialAdConfiguration

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mockFiveAdConfig = mock<FiveAdConfig>()
  private val mockFiveAdInterstitial = mock<FiveAdInterstitial>()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createFiveAdInterstitial(activity, TEST_SLOT_ID) } doReturn mockFiveAdInterstitial
    }
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    // Properly initialize lineInterstitialAd
    mediationAdConfiguration = createMediationInterstitialAdConfiguration()
    LineInterstitialAd.newInstance(mediationAdConfiguration, mediationAdLoadCallback).onSuccess {
      lineInterstitialAd = it
    }
  }

  // region newInstance Tests
  @Test
  fun newInstance_withValidAppId_returnsSuccess() {
    val config = createMediationInterstitialAdConfiguration()

    val result = LineInterstitialAd.newInstance(config, mediationAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun newInstance_withMissingAppId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID)
    val config = createMediationInterstitialAdConfiguration(serverParameters = serverParameters)

    val result = LineInterstitialAd.newInstance(config, mediationAdLoadCallback)

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
    val config = createMediationInterstitialAdConfiguration(serverParameters = serverParameters)

    val result = LineInterstitialAd.newInstance(config, mediationAdLoadCallback)

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
    val config = createMediationInterstitialAdConfiguration(serverParameters = serverParameters)
    var ad: LineInterstitialAd? = null
    LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

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
    val config = createMediationInterstitialAdConfiguration(serverParameters = serverParameters)
    var ad: LineInterstitialAd? = null
    LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

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
    val config = createMediationInterstitialAdConfiguration(mediationExtras = mediationExtras)
    var ad: LineInterstitialAd? = null
    LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

    ad?.loadAd(activity)

    verify(mockFiveAdInterstitial).enableSound(false)
  }

  @Test
  fun loadAd_withSoundExtrasTrue_enablesSound() {
    val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to true)
    val config = createMediationInterstitialAdConfiguration(mediationExtras = mediationExtras)
    var ad: LineInterstitialAd? = null
    LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { ad = it }

    ad?.loadAd(activity)

    verify(mockFiveAdInterstitial).enableSound(true)
  }

  // endregion

  // region RTB loadRtbAd Tests
  @Test
  fun loadRtbAd_success_invokesOnSuccess() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val config = createMediationInterstitialAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbAd: LineInterstitialAd? = null
      LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

      rtbAd?.loadRtbAd(activity)

      val callbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), callbackCaptor.capture())
      callbackCaptor.firstValue.onLoad(mockFiveAdInterstitial)
      verify(mockFiveAdInterstitial).setEventListener(rtbAd!!)
      assertThat(mediationAdLoadCallback).hasSucceededWith(rtbAd!!)
    }
  }

  @Test
  fun loadRtbAd_error_invokesOnFailure() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val config = createMediationInterstitialAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbAd: LineInterstitialAd? = null
      LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

      rtbAd?.loadRtbAd(activity)

      val callbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), callbackCaptor.capture())
      callbackCaptor.firstValue.onError(FiveAdErrorCode.INTERNAL_ERROR)
      val expectedError =
        AdError(
          FiveAdErrorCode.INTERNAL_ERROR.value,
          FiveAdErrorCode.INTERNAL_ERROR.name,
          LineMediationAdapter.SDK_ERROR_DOMAIN,
        )
      assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
    }
  }

  @Test
  fun loadRtbAd_whenNullAdLoader_returnsEarlySafely() {
    mockStatic(AdLoader::class.java).use {
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn null
      val config = createMediationInterstitialAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbAd: LineInterstitialAd? = null
      LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

      rtbAd?.loadRtbAd(activity)

      verify(mockFiveAdInterstitial, never()).loadAdAsync()
    }
  }

  @Test
  fun loadRtbAd_withSoundExtrasFalse_disablesSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to false)
      val config =
        createMediationInterstitialAdConfiguration(
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )
      var rtbAd: LineInterstitialAd? = null
      LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

      rtbAd?.loadRtbAd(activity)

      val callbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), callbackCaptor.capture())
      callbackCaptor.firstValue.onLoad(mockFiveAdInterstitial)
      verify(mockFiveAdInterstitial).enableSound(false)
    }
  }

  @Test
  fun loadRtbAd_withSoundExtrasTrue_enablesSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to true)
      val config =
        createMediationInterstitialAdConfiguration(
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )
      var rtbAd: LineInterstitialAd? = null
      LineInterstitialAd.newInstance(config, mediationAdLoadCallback).onSuccess { rtbAd = it }

      rtbAd?.loadRtbAd(activity)

      val callbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), callbackCaptor.capture())
      callbackCaptor.firstValue.onLoad(mockFiveAdInterstitial)
      verify(mockFiveAdInterstitial).enableSound(true)
    }
  }

  // endregion

  @Test
  fun showAd_invokesFiveAdShowAd() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.showAd(activity)

    verify(mockFiveAdInterstitial).showAd()
  }

  @Test
  fun onFiveAdLoad_invokesOnSuccess() {
    lineInterstitialAd.loadAd(activity)

    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    verify(mockFiveAdInterstitial).setEventListener(lineInterstitialAd)
    assertThat(mediationAdLoadCallback).hasSucceededWith(lineInterstitialAd)
  }

  @Test
  fun onFiveAdLoadError_invokesOnFailure() {
    lineInterstitialAd.onFiveAdLoadError(mockFiveAdInterstitial, FiveAdErrorCode.INTERNAL_ERROR)

    val expectedError =
      AdError(
        FiveAdErrorCode.INTERNAL_ERROR.value,
        "FiveAd SDK returned a load error with code INTERNAL_ERROR.",
        LineMediationAdapter.SDK_ERROR_DOMAIN,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun onClick_invokesReportAdClickedAndOnAdLeftApplication() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onClick(mockFiveAdInterstitial)

    assertThat(interstitialAdCallback.isClicked).isTrue()
    assertThat(interstitialAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onFullScreenClose_invokesOnAdClosed() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onFullScreenClose(mockFiveAdInterstitial)

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onImpression(mockFiveAdInterstitial)

    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onViewError_invokesOnAdFailedToShow() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR

    lineInterstitialAd.onViewError(mockFiveAdInterstitial, dummyErrorCode)

    val expectedError =
      AdError(
        dummyErrorCode.value,
        "FiveAd SDK could not show ad with error with code INTERNAL_ERROR.",
        LineMediationAdapter.SDK_ERROR_DOMAIN,
      )
    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedError)
  }

  @Test
  fun onFullScreenOpen_invokesOnAdOpened() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onFullScreenOpen(mockFiveAdInterstitial)

    assertThat(interstitialAdCallback.isOpened).isTrue()
  }

  @Test
  fun onPlay_throwsNoException() {
    lineInterstitialAd.onPlay(mockFiveAdInterstitial)
  }

  @Test
  fun onPause_throwsNoException() {
    lineInterstitialAd.onPause(mockFiveAdInterstitial)
  }

  @Test
  fun onViewThrough_throwsNoException() {
    lineInterstitialAd.onViewThrough(mockFiveAdInterstitial)
  }

  private fun createMediationInterstitialAdConfiguration(
    serverParameters: Bundle =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      ),
    mediationExtras: Bundle = Bundle(),
    bidResponse: String = "",
  ): MediationInterstitialAdConfiguration {
    return MediationInterstitialAdConfiguration(
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
