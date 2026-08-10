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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.AdLoader
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdNative
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LineNativeAdTest {
  // Subject of tests
  private lateinit var lineNativeAd: LineNativeAd
  private lateinit var mediationAdConfiguration: MediationNativeAdConfiguration

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockIconBitmap =
    mock<Bitmap> {
      on { getScaledHeight(any<Int>()) } doReturn 1
      on { getScaledHeight(any<Canvas>()) } doReturn 1
      on { getScaledHeight(any<DisplayMetrics>()) } doReturn 1
      on { getScaledWidth(any<Int>()) } doReturn 1
      on { getScaledWidth(any<Canvas>()) } doReturn 1
      on { getScaledWidth(any<DisplayMetrics>()) } doReturn 1
    }
  private val mockInfoBitmap = mock<Bitmap>()
  private val mockFiveAdConfig = mock<FiveAdConfig>()
  private val mockFiveAdNative =
    mock<FiveAdNative> {
      on { adTitle } doReturn "FunnyTitle"
      on { descriptionText } doReturn "DescriptionText"
      on { buttonText } doReturn "ButtonText"
    }
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createFiveAdNative(context, TEST_SLOT_ID) } doReturn mockFiveAdNative
    }
  private val mockMediationAdCallback = mock<MediationNativeAdCallback>()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val testCoroutineScope = TestScope(UnconfinedTestDispatcher())

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    mediationAdConfiguration = createMediationNativeAdConfiguration()
    LineNativeAd.newInstance(
        mediationAdConfiguration,
        mediationAdLoadCallback,
        Dispatchers.Unconfined,
      )
      .onSuccess { lineNativeAd = it }
    whenever(mediationAdLoadCallback.onSuccess(lineNativeAd)) doReturn mockMediationAdCallback
  }

  // region newInstance Tests
  @Test
  fun newInstance_withValidAppId_returnsSuccess() {
    val config = createMediationNativeAdConfiguration()

    val result = LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun newInstance_withMissingAppId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID)
    val config = createMediationNativeAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined)

    assertThat(result.isFailure).isTrue()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(LineMediationAdapter.ERROR_CODE_MISSING_APP_ID)
    assertThat(capturedError.message).isEqualTo(LineMediationAdapter.ERROR_MSG_MISSING_APP_ID)
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun newInstance_withEmptyAppId_invokesOnFailureAndReturnsFailure() {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_APP_ID to "",
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
      )
    val config = createMediationNativeAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined)

    assertThat(result.isFailure).isTrue()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(LineMediationAdapter.ERROR_CODE_MISSING_APP_ID)
    assertThat(capturedError.message).isEqualTo(LineMediationAdapter.ERROR_MSG_MISSING_APP_ID)
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  // endregion

  // region Waterfall loadAd Tests
  @Test
  fun loadAd_withNullSlotId_invokesOnFailure() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID)
    val config = createMediationNativeAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()
    var ad: LineNativeAd? = null
    LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
      ad = it
    }

    ad?.loadAd()

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID)
    assertThat(capturedError.message).isEqualTo(LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID)
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadAd_withEmptySlotId_invokesOnFailure() {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
        LineMediationAdapter.KEY_SLOT_ID to "",
      )
    val config = createMediationNativeAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()
    var ad: LineNativeAd? = null
    LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
      ad = it
    }

    ad?.loadAd()

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID)
    assertThat(capturedError.message).isEqualTo(LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID)
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadAd_withStartMutedTrue_disablesSound() {
    val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
    val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
    val config = createMediationNativeAdConfiguration(nativeAdOptions = nativeAdOptions)
    var ad: LineNativeAd? = null
    LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
      ad = it
    }

    ad?.loadAd()

    verify(mockFiveAdNative).enableSound(false)
  }

  @Test
  fun loadAd_withStartMutedFalse_enablesSound() {
    val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
    val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
    val config = createMediationNativeAdConfiguration(nativeAdOptions = nativeAdOptions)
    var ad: LineNativeAd? = null
    LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
      ad = it
    }

    ad?.loadAd()

    verify(mockFiveAdNative).enableSound(true)
  }

  // endregion

  // region RTB loadRtbAd Tests
  @Test
  fun loadRtbAd_success_invokesOnSuccess() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val config = createMediationNativeAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbNativeAd: LineNativeAd? = null
      LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
        rtbNativeAd = it
      }
      initiateImageLoadCallbacks()

      rtbNativeAd?.loadRtbAd()

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadNativeAdCallback>()
      verify(mockAdLoader).loadNativeAd(any<BidData>(), loadCallbackCaptor.capture())
      val capturedCallback = loadCallbackCaptor.firstValue
      testCoroutineScope.runTest { capturedCallback.onLoad(mockFiveAdNative) }
      verify(mockFiveAdNative).setEventListener(rtbNativeAd!!)
      verify(mediationAdLoadCallback).onSuccess(rtbNativeAd!!)
    }
  }

  @Test
  fun loadRtbAd_error_invokesOnFailure() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val config = createMediationNativeAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbNativeAd: LineNativeAd? = null
      LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
        rtbNativeAd = it
      }

      rtbNativeAd?.loadRtbAd()

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadNativeAdCallback>()
      verify(mockAdLoader).loadNativeAd(any<BidData>(), loadCallbackCaptor.capture())
      loadCallbackCaptor.firstValue.onError(FiveAdErrorCode.INTERNAL_ERROR)

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
      val capturedError = adErrorCaptor.firstValue
      assertThat(capturedError.code).isEqualTo(FiveAdErrorCode.INTERNAL_ERROR.value)
      assertThat(capturedError.message).isEqualTo(FiveAdErrorCode.INTERNAL_ERROR.name)
      assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
    }
  }

  @Test
  fun loadRtbAd_whenNullAdLoader_returnsEarlySafely() {
    mockStatic(AdLoader::class.java).use {
      whenever(AdLoader.forConfig(eq(context), any())) doReturn null
      val config = createMediationNativeAdConfiguration(bidResponse = TEST_BID_RESPONSE)
      var rtbNativeAd: LineNativeAd? = null
      LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
        rtbNativeAd = it
      }

      rtbNativeAd?.loadRtbAd()

      verify(mediationAdLoadCallback, never()).onSuccess(any())
      verify(mediationAdLoadCallback, never()).onFailure(any())
    }
  }

  @Test
  fun loadRtbAd_withStartMutedTrue_disablesSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
      val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
      val config =
        createMediationNativeAdConfiguration(
          bidResponse = TEST_BID_RESPONSE,
          nativeAdOptions = nativeAdOptions,
        )
      var ad: LineNativeAd? = null
      LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
        ad = it
      }

      ad?.loadRtbAd()

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadNativeAdCallback>()
      verify(mockAdLoader).loadNativeAd(any<BidData>(), loadCallbackCaptor.capture())
      loadCallbackCaptor.firstValue.onLoad(mockFiveAdNative)

      verify(mockFiveAdNative).enableSound(false)
    }
  }

  @Test
  fun loadRtbAd_withStartMutedFalse_enablesSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
      val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
      val config =
        createMediationNativeAdConfiguration(
          bidResponse = TEST_BID_RESPONSE,
          nativeAdOptions = nativeAdOptions,
        )
      var ad: LineNativeAd? = null
      LineNativeAd.newInstance(config, mediationAdLoadCallback, Dispatchers.Unconfined).onSuccess {
        ad = it
      }

      ad?.loadRtbAd()

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadNativeAdCallback>()
      verify(mockAdLoader).loadNativeAd(any<BidData>(), loadCallbackCaptor.capture())
      loadCallbackCaptor.firstValue.onLoad(mockFiveAdNative)

      verify(mockFiveAdNative).enableSound(true)
    }
  }

  // endregion

  // region Native Ad Mapping & Callbacks Tests
  @Test
  fun onFiveAdLoad_mapsNativeAdAndInvokesOnSuccess() {
    testCoroutineScope.runTest {
      // Required to verify mediaView assignment
      val spiedLineNativeAd = spy(lineNativeAd)
      initiateImageLoadCallbacks()
      spiedLineNativeAd.loadAd()

      spiedLineNativeAd.onFiveAdLoad(mockFiveAdNative)
      advanceUntilIdle()

      with(spiedLineNativeAd) {
        verify(this).overrideClickHandling = true
        verify(this).overrideImpressionRecording = true
        assertThat(headline).isEqualTo("FunnyTitle")
        assertThat(body).isEqualTo("DescriptionText")
        assertThat(callToAction).isEqualTo("ButtonText")
        verify(this).setMediaView(mockFiveAdNative.adMainView)
        assertThat(advertiser).isEqualTo(mockFiveAdNative.advertiserName)
        verify(mockFiveAdNative).loadIconImageAsync(any())
        assertIs<LineNativeAd.LineNativeImage>(icon)
        verify(mockFiveAdNative).setEventListener(this)
        verify(mockFiveAdNative).loadInformationIconImageAsync(any())
        assertIs<ImageView>(adChoicesContent)
        verify(mediationAdLoadCallback).onSuccess(this)
      }
    }
  }

  @Test
  fun onFiveAdLoad_whenInformationIconFails_invokesOnFailure() {
    testCoroutineScope.runTest {
      val spiedLineNativeAd = spy(lineNativeAd)
      initiateImageLoadCallbacks(returnValidIconImage = true, returnValidInformationImage = false)
      spiedLineNativeAd.loadAd()

      spiedLineNativeAd.onFiveAdLoad(mockFiveAdNative)
      advanceUntilIdle()

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
      val capturedError = adErrorCaptor.firstValue
      assertThat(capturedError.code)
        .isEqualTo(LineMediationAdapter.ERROR_CODE_MINIMUM_NATIVE_INFO_NOT_RECEIVED)
      assertThat(capturedError.message)
        .isEqualTo(LineMediationAdapter.ERROR_MSG_MINIMUM_NATIVE_INFO_NOT_RECEIVED)
      assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
    }
  }

  @Test
  fun onFiveAdLoad_whenAppIconFailsButInformationIconSucceeds_completesLoadWithNullIcon() {
    testCoroutineScope.runTest {
      val spiedLineNativeAd = spy(lineNativeAd)
      initiateImageLoadCallbacks(returnValidIconImage = false, returnValidInformationImage = true)
      spiedLineNativeAd.loadAd()

      spiedLineNativeAd.onFiveAdLoad(mockFiveAdNative)
      advanceUntilIdle()

      with(spiedLineNativeAd) {
        assertThat(icon).isNull()
        assertIs<ImageView>(adChoicesContent)
        verify(mediationAdLoadCallback).onSuccess(this)
      }
    }
  }

  @Test
  fun onFiveAdLoad_inLowerGMASdkVersions_doesNotInvokeOverrideImpressionRecording() {
    testCoroutineScope.runTest {
      // Required to verify mediaView assignment
      val spiedLineNativeAd = spy(lineNativeAd)
      initiateImageLoadCallbacks()
      spiedLineNativeAd.loadAd()

      Mockito.mockStatic(MobileAds::class.java).use {
        // Return a version of GMA SDK that doesn't listen to adapter-reported impressions.
        whenever(MobileAds.getVersion()) doReturn VersionInfo(24, 0, 0)
        spiedLineNativeAd.onFiveAdLoad(mockFiveAdNative)
        advanceUntilIdle()
      }

      verify(spiedLineNativeAd, never()).overrideImpressionRecording = true
    }
  }

  @Test
  fun onFiveAdLoad_ifGmaSdkVersionBetween018And650_setsOverrideImpressionRecordingAsTrue() {
    testCoroutineScope.runTest {
      // Required to verify mediaView assignment
      val spiedLineNativeAd = spy(lineNativeAd)
      initiateImageLoadCallbacks()
      spiedLineNativeAd.loadAd()

      Mockito.mockStatic(MobileAds::class.java).use {
        // Return a version of GMA SDK that does listens to adapter-reported impressions.
        whenever(MobileAds.getVersion()) doReturn VersionInfo(4, 0, 0)
        spiedLineNativeAd.onFiveAdLoad(mockFiveAdNative)
        advanceUntilIdle()
      }

      verify(spiedLineNativeAd).overrideImpressionRecording = true
    }
  }

  @Test
  fun onFiveAdLoadError_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    lineNativeAd.onFiveAdLoadError(mockFiveAdNative, FiveAdErrorCode.INTERNAL_ERROR)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(FiveAdErrorCode.INTERNAL_ERROR.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK returned a load error with code INTERNAL_ERROR.")
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun onClick_invokesReportAdClickedAndOnAdLeftApplication() {
    initiateImageLoadCallbacks()
    testCoroutineScope.runTest {
      lineNativeAd.loadAd()
      lineNativeAd.onFiveAdLoad(mockFiveAdNative)
      advanceUntilIdle()
    }

    lineNativeAd.onClick(mockFiveAdNative)

    verify(mockMediationAdCallback).reportAdClicked()
    verify(mockMediationAdCallback).onAdLeftApplication()
  }

  @Test
  fun onRemove_throwsNoException() {
    lineNativeAd.onRemove(mockFiveAdNative)
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    testCoroutineScope.runTest {
      initiateImageLoadCallbacks()
      lineNativeAd.loadAd()
      lineNativeAd.onFiveAdLoad(mockFiveAdNative)
      advanceUntilIdle()

      lineNativeAd.onImpression(mockFiveAdNative)

      verify(mockMediationAdCallback).reportAdImpression()
    }
  }

  @Test
  fun onViewError_throwsNoException() {
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR

    lineNativeAd.onViewError(mockFiveAdNative, dummyErrorCode)
  }

  @Test
  fun onPlay_throwsNoException() {
    lineNativeAd.onPlay(mockFiveAdNative)
  }

  @Test
  fun onPause_throwsNoException() {
    lineNativeAd.onPause(mockFiveAdNative)
  }

  @Test
  fun onViewThrough_throwsNoException() {
    lineNativeAd.onViewThrough(mockFiveAdNative)
  }

  @Test
  fun trackViews_invokesRegisterViews() {
    lineNativeAd.loadAd()
    lineNativeAd.adChoicesContent = View(context)
    val viewContainer = View(context)

    lineNativeAd.trackViews(
      viewContainer,
      /* clickableAssetViews= */ mock(),
      /* nonClickableAssetViews= */ mock(),
    )

    verify(mockFiveAdNative)
      .registerViews(eq(viewContainer), eq(lineNativeAd.adChoicesContent), any())
  }

  // endregion

  private fun initiateImageLoadCallbacks(
    returnValidIconImage: Boolean = true,
    returnValidInformationImage: Boolean = true,
  ) {
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[0] as FiveAdNative.LoadImageCallback).onImageLoad(
          if (returnValidIconImage) mockIconBitmap else null
        )
      }
      .whenever(mockFiveAdNative)
      .loadIconImageAsync(any())
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[0] as FiveAdNative.LoadImageCallback).onImageLoad(
          if (returnValidInformationImage) mockInfoBitmap else null
        )
      }
      .whenever(mockFiveAdNative)
      .loadInformationIconImageAsync(any())
  }

  private fun createMediationNativeAdConfiguration(
    serverParameters: Bundle =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      ),
    mediationExtras: Bundle = Bundle(),
    bidResponse: String = "",
    nativeAdOptions: NativeAdOptions? = null,
  ): MediationNativeAdConfiguration {
    val config =
      MediationNativeAdConfiguration(
        context,
        bidResponse,
        serverParameters,
        mediationExtras,
        /*isTesting=*/ true,
        /*location=*/ null,
        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        TEST_WATERMARK,
        /*nativeAdOptions=*/ null,
      )
    if (nativeAdOptions != null) {
      val spiedConfig = spy(config)
      whenever(spiedConfig.nativeAdOptions) doReturn nativeAdOptions
      return spiedConfig
    }
    return config
  }

  private companion object {
    const val TEST_APP_ID = "testAppId"
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_WATERMARK = "testWatermark"
    const val TEST_BID_RESPONSE = "testBidResponse"
  }
}
