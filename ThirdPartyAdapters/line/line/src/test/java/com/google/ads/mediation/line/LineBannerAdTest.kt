package com.google.ads.mediation.line

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdErrorCode
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
  private val mockMediationAdCallback = mock<MediationBannerAdCallback>()
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createFiveAdCustomLayout(context, TEST_SLOT_ID, AdSize.BANNER.width) } doReturn
        mockFiveAdCustomLayout
    }
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    // Properly initialize lineBannerAd
    mediationAdConfiguration = createMediationBannerAdConfiguration()
    LineBannerAd.newInstance(mediationAdConfiguration, mediationAdLoadCallback).onSuccess {
      lineBannerAd = it
    }
    whenever(mediationAdLoadCallback.onSuccess(lineBannerAd)) doReturn mockMediationAdCallback
  }

  @Test
  fun getView_returnsCreatedBannerAd() {
    lineBannerAd.loadAd(context)

    val createdAdView = lineBannerAd.view

    assertThat(createdAdView).isEqualTo(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdLoad_withUnexpectedAdSize_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()
    val differentBannerAd =
      mock<FiveAdCustomLayout> {
        on { logicalWidth } doReturn AdSize.LARGE_BANNER.width
        on { logicalHeight } doReturn AdSize.LARGE_BANNER.height
        on { context } doReturn context
      }

    lineBannerAd.onFiveAdLoad(differentBannerAd)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(LineBannerAd.ERROR_CODE_MISMATCH_AD_SIZE)
    assertThat(capturedError.message).startsWith("Unexpected ad size loaded.")
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun onFiveAdLoad_invokesOnSuccess() {
    lineBannerAd.loadAd(context)

    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    verify(mockFiveAdCustomLayout).setEventListener(lineBannerAd)
    verify(mediationAdLoadCallback).onSuccess(lineBannerAd)
  }

  @Test
  fun onFiveAdLoadError_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    lineBannerAd.onFiveAdLoadError(mockFiveAdCustomLayout, FiveAdErrorCode.INTERNAL_ERROR)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(FiveAdErrorCode.INTERNAL_ERROR.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK returned a load error with code INTERNAL_ERROR.")
  }

  @Test
  fun onClick_invokesReportAdClickedAndOnAdLeftApplication() {
    lineBannerAd.loadAd(context)
    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    lineBannerAd.onClick(mockFiveAdCustomLayout)

    verify(mockMediationAdCallback).reportAdClicked()
    verify(mockMediationAdCallback).onAdLeftApplication()
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    lineBannerAd.loadAd(context)
    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    lineBannerAd.onImpression(mockFiveAdCustomLayout)

    verify(mockMediationAdCallback).reportAdImpression()
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

  private fun createMediationBannerAdConfiguration(): MediationBannerAdConfiguration {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      )
    return MediationBannerAdConfiguration(
      context,
      /*bidresponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      AdSize.BANNER,
      TEST_WATERMARK,
    )
  }

  companion object {
    private const val TEST_APP_ID = "testAppId"
    private const val TEST_SLOT_ID = "testSlotId"
    private const val TEST_WATERMARK = "testWatermark"
  }
}
