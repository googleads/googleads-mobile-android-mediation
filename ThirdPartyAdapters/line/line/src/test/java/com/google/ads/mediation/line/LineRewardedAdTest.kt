package com.google.ads.mediation.line

import android.app.Activity
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdVideoReward
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class LineRewardedAdTest {
  // Subject of tests
  private lateinit var lineRewardedAd: LineRewardedAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mockFiveAdConfig = mock<FiveAdConfig>()
  private val mockFiveAdVideoReward = mock<FiveAdVideoReward>()
  private val mockMediationAdCallback = mock<MediationRewardedAdCallback>()
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createFiveVideoRewarded(activity, TEST_SLOT_ID) } doReturn mockFiveAdVideoReward
    }
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    // Properly initialize lineRewardedAd
    val mediationAdConfiguration = createMediationRewardedAdConfiguration()
    LineRewardedAd.newInstance(mediationAdConfiguration, mediationAdLoadCallback).onSuccess {
      lineRewardedAd = it
    }
    whenever(mediationAdLoadCallback.onSuccess(lineRewardedAd)) doReturn mockMediationAdCallback
  }

  @Test
  fun showAd_invokesFiveAdShowAd() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.showAd(activity)

    verify(mockFiveAdVideoReward).showAd()
  }

  @Test
  fun onFiveAdLoad_invokesOnSuccess() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    verify(mockFiveAdVideoReward).setEventListener(lineRewardedAd)
    verify(mediationAdLoadCallback).onSuccess(lineRewardedAd)
  }

  @Test
  fun onFiveAdLoadError_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    lineRewardedAd.onFiveAdLoadError(mockFiveAdVideoReward, FiveAdErrorCode.INTERNAL_ERROR)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(FiveAdErrorCode.INTERNAL_ERROR.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK returned a load error with code INTERNAL_ERROR.")
  }

  @Test
  fun onClick_invokesReportAdClicked() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.onClick(mockFiveAdVideoReward)

    verify(mockMediationAdCallback).reportAdClicked()
  }

  @Test
  fun onFullScreenClose_invokesOnAdClosed() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.onFullScreenClose(mockFiveAdVideoReward)

    verify(mockMediationAdCallback).onAdClosed()
  }

  @Test
  fun onReward_invokesOnUserEarnedReward() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.onReward(mockFiveAdVideoReward)

    verify(mockMediationAdCallback).onUserEarnedReward()
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.onImpression(mockFiveAdVideoReward)

    verify(mockMediationAdCallback).reportAdImpression()
  }

  @Test
  fun onViewError_invokesOnAdFailedToShow() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR
    val adErrorCaptor = argumentCaptor<AdError>()

    lineRewardedAd.onViewError(mockFiveAdVideoReward, dummyErrorCode)

    verify(mockMediationAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(dummyErrorCode.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK could not show ad with error with code INTERNAL_ERROR.")
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun onPlay_invokesOnVideoStart() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.onPlay(mockFiveAdVideoReward)

    verify(mockMediationAdCallback).onVideoStart()
  }

  @Test
  fun onFullScreenOpen_invokesOnAdOpened() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.onFullScreenOpen(mockFiveAdVideoReward)

    verify(mockMediationAdCallback).onAdOpened()
  }

  @Test
  fun onPause_throwsNoException() {
    lineRewardedAd.onPause(mockFiveAdVideoReward)
  }

  @Test
  fun onViewThrough_invokesOnVideoComplete() {
    lineRewardedAd.loadAd()
    lineRewardedAd.onFiveAdLoad(mockFiveAdVideoReward)

    lineRewardedAd.onViewThrough(mockFiveAdVideoReward)

    verify(mockMediationAdCallback).onVideoComplete()
  }

  private fun createMediationRewardedAdConfiguration(): MediationRewardedAdConfiguration {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      )
    return MediationRewardedAdConfiguration(
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
