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
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.AdLoader
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdErrorCode
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
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

@RunWith(AndroidJUnit4::class)
class LineBannerAdTest {
  // Subject of tests
  private lateinit var lineBannerAd: LineBannerAd
  private lateinit var mediationAdConfiguration: MediationBannerAdConfiguration

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockFiveAdConfig = mock<FiveAdConfig>()
  private val mockFiveAdCustomLayout =
    mock<FiveAdCustomLayout> {
      on { logicalWidth } doReturn AdSize.BANNER.width
      on { logicalHeight } doReturn AdSize.BANNER.height
      on { context } doReturn context
    }
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createFiveAdCustomLayout(context, TEST_SLOT_ID, AdSize.BANNER.width) } doReturn
        mockFiveAdCustomLayout
    }
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    // Properly initialize lineBannerAd
    mediationAdConfiguration = createMediationBannerAdConfiguration()
    whenever(mediationUtils.findClosestSize(eq(context), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    LineBannerAd.newInstance(mediationAdConfiguration, mediationAdLoadCallback, mediationUtils)
      .onSuccess { lineBannerAd = it }
  }

  // region newInstance Tests
  @Test
  fun newInstance_withValidAppId_returnsSuccess() {
    val config = createMediationBannerAdConfiguration()

    val result = LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun newInstance_withMissingAppId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID)
    val config = createMediationBannerAdConfiguration(serverParameters = serverParameters)

    val result = LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils)

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
    val config = createMediationBannerAdConfiguration(serverParameters = serverParameters)

    val result = LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils)

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
    val config = createMediationBannerAdConfiguration(serverParameters = serverParameters)
    var ad: LineBannerAd? = null
    LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess { ad = it }

    ad?.loadAd(context)

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
    val config = createMediationBannerAdConfiguration(serverParameters = serverParameters)
    var ad: LineBannerAd? = null
    LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess { ad = it }

    ad?.loadAd(context)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
        LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadAd_withSoundExtrasTrue_enablesSound() {
    val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to true)
    val config = createMediationBannerAdConfiguration(mediationExtras = mediationExtras)
    var ad: LineBannerAd? = null
    LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess { ad = it }

    ad?.loadAd(context)

    verify(mockFiveAdCustomLayout).enableSound(true)
  }

  @Test
  fun loadAd_withSoundExtrasFalse_disablesSound() {
    val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to false)
    val config = createMediationBannerAdConfiguration(mediationExtras = mediationExtras)
    var ad: LineBannerAd? = null
    LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess { ad = it }

    ad?.loadAd(context)

    verify(mockFiveAdCustomLayout).enableSound(false)
  }

  // endregion

  // region RTB loadRtbAd Tests
  @Test
  fun loadRtbAd_success_invokesOnSuccess() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val config = createMediationBannerAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbBannerAd: LineBannerAd? = null
      LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess {
        rtbBannerAd = it
      }

      rtbBannerAd?.loadRtbAd(context)

      val callbackCaptor = argumentCaptor<AdLoader.LoadBannerAdCallback>()
      verify(mockAdLoader)
        .loadBannerAd(
          any<BidData>(),
          eq(AdSize.BANNER.getWidthInPixels(context)),
          callbackCaptor.capture(),
        )
      callbackCaptor.firstValue.onLoad(mockFiveAdCustomLayout)
      verify(mockFiveAdCustomLayout).setEventListener(rtbBannerAd!!)
      assertThat(mediationAdLoadCallback).hasSucceededWith(rtbBannerAd!!)
    }
  }

  @Test
  fun loadRtbAd_error_invokesOnFailure() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val config = createMediationBannerAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbBannerAd: LineBannerAd? = null
      LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess {
        rtbBannerAd = it
      }

      rtbBannerAd?.loadRtbAd(context)

      val callbackCaptor = argumentCaptor<AdLoader.LoadBannerAdCallback>()
      verify(mockAdLoader)
        .loadBannerAd(
          any<BidData>(),
          eq(AdSize.BANNER.getWidthInPixels(context)),
          callbackCaptor.capture(),
        )
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
      whenever(AdLoader.forConfig(eq(context), any())) doReturn null
      val config = createMediationBannerAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbBannerAd: LineBannerAd? = null
      LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess {
        rtbBannerAd = it
      }

      rtbBannerAd?.loadRtbAd(context)

      verify(mockFiveAdCustomLayout, never()).loadAdAsync()
    }
  }

  @Test
  fun loadRtbAd_withSoundExtrasTrue_enablesSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to true)
      val config =
        createMediationBannerAdConfiguration(
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )
      var rtbBannerAd: LineBannerAd? = null
      LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess {
        rtbBannerAd = it
      }

      rtbBannerAd?.loadRtbAd(context)

      val callbackCaptor = argumentCaptor<AdLoader.LoadBannerAdCallback>()
      verify(mockAdLoader)
        .loadBannerAd(
          any<BidData>(),
          eq(AdSize.BANNER.getWidthInPixels(context)),
          callbackCaptor.capture(),
        )
      callbackCaptor.firstValue.onLoad(mockFiveAdCustomLayout)
      verify(mockFiveAdCustomLayout).enableSound(true)
    }
  }

  @Test
  fun loadRtbAd_withSoundExtrasFalse_disablesSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val mediationExtras = bundleOf(LineExtras.KEY_ENABLE_AD_SOUND to false)
      val config =
        createMediationBannerAdConfiguration(
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )
      var rtbBannerAd: LineBannerAd? = null
      LineBannerAd.newInstance(config, mediationAdLoadCallback, mediationUtils).onSuccess {
        rtbBannerAd = it
      }

      rtbBannerAd?.loadRtbAd(context)

      val callbackCaptor = argumentCaptor<AdLoader.LoadBannerAdCallback>()
      verify(mockAdLoader)
        .loadBannerAd(
          any<BidData>(),
          eq(AdSize.BANNER.getWidthInPixels(context)),
          callbackCaptor.capture(),
        )
      callbackCaptor.firstValue.onLoad(mockFiveAdCustomLayout)
      verify(mockFiveAdCustomLayout).enableSound(false)
    }
  }

  // endregion

  @Test
  fun getView_returnsCreatedBannerAd() {
    lineBannerAd.loadAd(context)

    val createdAdView = lineBannerAd.view

    assertThat(createdAdView).isEqualTo(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdLoad_withUnexpectedAdSize_invokesOnFailure() {
    val differentBannerAd =
      mock<FiveAdCustomLayout> {
        on { logicalWidth } doReturn AdSize.LARGE_BANNER.width
        on { logicalHeight } doReturn AdSize.LARGE_BANNER.height
        on { context } doReturn context
      }
    whenever(mediationUtils.findClosestSize(eq(context), eq(AdSize.BANNER), any())) doReturn null

    lineBannerAd.onFiveAdLoad(differentBannerAd)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISMATCH_AD_SIZE,
        "Unexpected ad size loaded. Expected 320x50 but received 320x100.",
        LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun onFiveAdLoad_invokesOnSuccess() {
    lineBannerAd.loadAd(context)

    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    verify(mockFiveAdCustomLayout).setEventListener(lineBannerAd)
    assertThat(mediationAdLoadCallback).hasSucceededWith(lineBannerAd)
  }

  @Test
  fun onFiveAdLoadError_invokesOnFailure() {
    lineBannerAd.onFiveAdLoadError(mockFiveAdCustomLayout, FiveAdErrorCode.INTERNAL_ERROR)

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
    lineBannerAd.loadAd(context)
    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    lineBannerAd.onClick(mockFiveAdCustomLayout)

    assertThat(bannerAdCallback.isClicked).isTrue()
    assertThat(bannerAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    lineBannerAd.loadAd(context)
    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    lineBannerAd.onImpression(mockFiveAdCustomLayout)

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onRemove_throwsNoException() {
    lineBannerAd.onRemove(mockFiveAdCustomLayout)
  }

  @Test
  fun onViewError_throwsNoException() {
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR

    lineBannerAd.onViewError(mockFiveAdCustomLayout, dummyErrorCode)
  }

  @Test
  fun onPlay_throwsNoException() {
    lineBannerAd.onPlay(mockFiveAdCustomLayout)
  }

  @Test
  fun onPause_throwsNoException() {
    lineBannerAd.onPause(mockFiveAdCustomLayout)
  }

  @Test
  fun onViewThrough_throwsNoException() {
    lineBannerAd.onViewThrough(mockFiveAdCustomLayout)
  }

  private fun createMediationBannerAdConfiguration(
    serverParameters: Bundle =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      ),
    adSize: AdSize = AdSize.BANNER,
    mediationExtras: Bundle = Bundle(),
    bidResponse: String = "",
  ): MediationBannerAdConfiguration {
    return MediationBannerAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      mediationExtras,
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      adSize,
      TEST_WATERMARK,
    )
  }

  private companion object {
    const val TEST_APP_ID = "testAppId"
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_WATERMARK = "testWatermark"
  }
}
