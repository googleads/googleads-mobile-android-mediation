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
  fun onClick_invokesReportAdClickedAndOnAdLeftApplication() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onClick(mockFiveAdInterstitial)

    verify(mockMediationAdCallback).reportAdClicked()
    verify(mockMediationAdCallback).onAdLeftApplication()
  }

  @Test
  fun onFullScreenClose_invokesOnAdClosed() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onFullScreenClose(mockFiveAdInterstitial)

    verify(mockMediationAdCallback).onAdClosed()
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onImpression(mockFiveAdInterstitial)

    verify(mockMediationAdCallback).reportAdImpression()
  }

  @Test
  fun onViewError_invokesOnAdFailedToShow() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR
    val adErrorCaptor = argumentCaptor<AdError>()

    lineInterstitialAd.onViewError(mockFiveAdInterstitial, dummyErrorCode)

    verify(mockMediationAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(dummyErrorCode.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK could not show ad with error with code INTERNAL_ERROR.")
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun onFullScreenOpen_invokesOnAdOpened() {
    lineInterstitialAd.loadAd(activity)
    lineInterstitialAd.onFiveAdLoad(mockFiveAdInterstitial)

    lineInterstitialAd.onFullScreenOpen(mockFiveAdInterstitial)

    verify(mockMediationAdCallback).onAdOpened()
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

  private fun createMediationInterstitialAdConfiguration(): MediationInterstitialAdConfiguration {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
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
