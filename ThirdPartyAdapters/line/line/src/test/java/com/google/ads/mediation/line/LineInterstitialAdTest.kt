package com.google.ads.mediation.line

import android.app.Activity
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterstitial
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
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
  private val mockMediationAdCallback = mock<MediationInterstitialAdCallback>()
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createFiveAdInterstitial(activity, TEST_SLOT_ID) } doReturn mockFiveAdInterstitial
    }
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    // Properly initialize lineInterstitialAd
    mediationAdConfiguration = createMediationInterstitialAdConfiguration()
    LineInterstitialAd.newInstance(mediationAdConfiguration, mediationAdLoadCallback).onSuccess {
      lineInterstitialAd = it
    }
    whenever(mediationAdLoadCallback.onSuccess(lineInterstitialAd)) doReturn mockMediationAdCallback
  }

  @Test
  fun showAd_withSuccessfulFullscreenResponse_invokesOnAdOpened() {
    whenever(mockFiveAdInterstitial.show(activity)) doReturn true
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.showAd(activity)

    verify(mockMediationAdCallback).onAdOpened()
  }

  @Test
  fun showAd_withFailedFullscreenResponse_invokesOnAdFailedToShow() {
    whenever(mockFiveAdInterstitial.show(activity)) doReturn false
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)
    val adErrorCaptor = argumentCaptor<AdError>()

    lineInterstitialAd.showAd(activity)

    verify(mockMediationAdCallback, never()).onAdOpened()
    verify(mockMediationAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code)
      .isEqualTo(LineMediationAdapter.ERROR_CODE_FAILED_TO_SHOW_FULLSCREEN)
    assertThat(capturedError.message)
      .isEqualTo(LineMediationAdapter.ERROR_MSG_FAILED_TO_SHOW_FULLSCREEN)
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun onFiveAdLoad_invokesOnSuccess() {
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    verify(mockFiveAdInterstitial).setViewEventListener(lineInterstitialAd)
    verify(mediationAdLoadCallback).onSuccess(lineInterstitialAd)
  }

  @Test
  fun onFiveAdLoadError_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    lineInterstitialAd.onFiveAdLoadError(mockFiveAdInterstitial, FiveAdErrorCode.INTERNAL_ERROR)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(FiveAdErrorCode.INTERNAL_ERROR.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK returned a load error with code INTERNAL_ERROR.")
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun onFiveAdClick_invokesReportAdClickedAndOnAdLeftApplication() {
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onFiveAdClick(mockFiveAdInterstitial)

    verify(mockMediationAdCallback).reportAdClicked()
    verify(mockMediationAdCallback).onAdLeftApplication()
  }

  @Test
  fun onFiveAdClose_invokesOnAdClosed() {
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onFiveAdClose(mockFiveAdInterstitial)

    verify(mockMediationAdCallback).onAdClosed()
  }

  @Test
  fun onFiveAdImpression_invokesReportAdImpression() {
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onFiveAdImpression(mockFiveAdInterstitial)

    verify(mockMediationAdCallback).reportAdImpression()
  }

  @Test
  fun onFiveAdViewError_invokesOnAdFailedToShow() {
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR
    val adErrorCaptor = argumentCaptor<AdError>()

    lineInterstitialAd.onFiveAdViewError(mockFiveAdInterstitial, dummyErrorCode)

    verify(mockMediationAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(dummyErrorCode.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK could not show ad with error with code INTERNAL_ERROR.")
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun onFiveAdStart_throwsNoException() {
    lineInterstitialAd.onFiveAdStart(mockFiveAdInterstitial)
  }

  @Test
  fun onFiveAdPause_throwsNoException() {
    lineInterstitialAd.onFiveAdPause(mockFiveAdInterstitial)
  }

  @Test
  fun onFiveAdResume_throwsNoException() {
    lineInterstitialAd.onFiveAdResume(mockFiveAdInterstitial)
  }

  @Test
  fun onFiveAdViewThrough_throwsNoException() {
    lineInterstitialAd.onFiveAdViewThrough(mockFiveAdInterstitial)
  }

  @Test
  fun onFiveAdReplay_throwsNoException() {
    lineInterstitialAd.onFiveAdReplay(mockFiveAdInterstitial)
  }

  @Test
  fun onFiveAdStall_throwsNoException() {
    lineInterstitialAd.onFiveAdStall(mockFiveAdInterstitial)
  }

  @Test
  fun onFiveAdRecover_throwsNoException() {
    lineInterstitialAd.onFiveAdRecover(mockFiveAdInterstitial)
  }

  private fun createMediationInterstitialAdConfiguration(): MediationInterstitialAdConfiguration {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID
      )
    return MediationInterstitialAdConfiguration(
      activity,
      /*bidresponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
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
