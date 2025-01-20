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
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdNative
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
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
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
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
        assertThat(headline).isEqualTo("FunnyTitle")
        assertThat(body).isEqualTo("DescriptionText")
        assertThat(callToAction).isEqualTo("ButtonText")
        verify(this).setMediaView(mockFiveAdNative.adMainView)
        assertThat(advertiser).isEqualTo(mockFiveAdNative.advertiserName)
        verify(mockFiveAdNative).loadIconImageAsync(any())
        assertThat(icon).isInstanceOf(LineNativeAd.LineNativeImage::class.java)
        verify(mockFiveAdNative).setEventListener(this)
        verify(mockFiveAdNative).loadInformationIconImageAsync(any())
        assertThat(adChoicesContent).isInstanceOf(ImageView::class.java)
        verify(mediationAdLoadCallback).onSuccess(this)
      }
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
      advanceUntilIdle()
      lineNativeAd.loadAd()
      lineNativeAd.onFiveAdLoad(mockFiveAdNative)

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

  @Test
  fun loadRtbNativeAd_onLoad_mapsAdAndThenLoadsFiveAdNative() {
    Mockito.mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      initiateImageLoadCallbacks()

      lineNativeAd.loadRtbAd()

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadNativeAdCallback>()
      verify(mockAdLoader).loadNativeAd(any(), loadCallbackCaptor.capture())
      val capturedCallback = loadCallbackCaptor.firstValue
      testCoroutineScope.runTest { capturedCallback.onLoad(mockFiveAdNative) }
      verify(mockFiveAdNative).setEventListener(lineNativeAd)
      verify(mediationAdLoadCallback).onSuccess(lineNativeAd)
    }
  }

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

  private fun createMediationNativeAdConfiguration(): MediationNativeAdConfiguration {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      )
    return MediationNativeAdConfiguration(
      context,
      /*bidresponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
      /*nativeAdOptions=*/ null,
    )
  }

  private companion object {
    const val TEST_APP_ID = "testAppId"
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_WATERMARK = "testWatermark"
  }
}
