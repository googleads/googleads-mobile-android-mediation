package com.google.ads.mediation.applovin

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.mediation.AppLovinUtils.ERROR_MSG_REASON_PREFIX
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinErrorCodes
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AppLovinRewardedRendererTest {

  // Subject of tests
  private lateinit var appLovinMediationRewardedAd: AppLovinRewardedRenderer

  private val appLovinAd: AppLovinAd = mock()
  private val rewardedAdConfiguration: MediationRewardedAdConfiguration = mock()
  private val rewardedAdCallback: MediationRewardedAdCallback = mock()
  private val rewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn rewardedAdCallback
    }
  private val appLovinSdkWrapper: AppLovinSdkWrapper = AppLovinSdkWrapper()
  private val appLovinSdkUtilsWrapper: AppLovinSdkUtilsWrapper = mock {
    on { runOnUiThread(any()) } doAnswer
      { invocation ->
        val args = invocation.arguments
        (args[0] as Runnable).run()
      }
  }
  private val appLovinInitializer: AppLovinInitializer = AppLovinInitializer(appLovinSdkWrapper)
  private val appLovinAdFactory: AppLovinAdFactory = AppLovinAdFactory()

  @Before
  fun setUp() {
    appLovinMediationRewardedAd =
      object :
        AppLovinRewardedRenderer(
          rewardedAdLoadCallback,
          appLovinInitializer,
          appLovinAdFactory,
          appLovinSdkUtilsWrapper,
        ) {
        override fun loadAd(rewardedAdConfiguration: MediationRewardedAdConfiguration) {}

        override fun showAd(context: Context) {}
      }
  }

  @Test
  fun adReceived_invokesOnSuccess() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    verify(rewardedAdLoadCallback).onSuccess(appLovinMediationRewardedAd)
  }

  @Test
  fun failedToReceiveAd_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    appLovinMediationRewardedAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)

    verify(rewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(AppLovinErrorCodes.NO_FILL)
    assertThat(capturedError.message.startsWith(ERROR_MSG_REASON_PREFIX)).isTrue()
    assertThat(capturedError.domain).isEqualTo(APPLOVIN_SDK_ERROR_DOMAIN)
  }

  @Test
  fun adDisplayed_invokesOnAdOpened() {
    // Simulate successful ad loading
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.adDisplayed(appLovinAd)

    verify(rewardedAdCallback).onAdOpened()
    verify(rewardedAdCallback).reportAdImpression()
  }

  @Test
  fun adDisplayed_withNullAdCallback_invokesOnAdOpenedAndReportAdImpression() {
    appLovinMediationRewardedAd.adDisplayed(appLovinAd)

    verify(rewardedAdCallback, never()).onAdOpened()
    verify(rewardedAdCallback, never()).reportAdImpression()
  }

  @Test
  fun adHidden_invokesOnUserEarnedRewardAndOnAdClosed() {
    // Mocking successful ad load
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.adHidden(appLovinAd)

    verify(rewardedAdCallback).onAdClosed()
  }

  @Test
  fun adHidden_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    appLovinMediationRewardedAd.adHidden(appLovinAd)

    verify(rewardedAdCallback, never()).onUserEarnedReward()
    verify(rewardedAdCallback, never()).onAdClosed()
  }

  @Test
  fun adClicked_invokesReportAdClicked() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.adClicked(appLovinAd)

    verify(rewardedAdCallback).reportAdClicked()
  }

  @Test
  fun adClicked_withNullAdCallback_doesNotInvokeReportAdClicked() {
    appLovinMediationRewardedAd.adClicked(appLovinAd)

    verify(rewardedAdCallback, never()).reportAdClicked()
  }

  @Test
  fun videoPlaybackBegan_invokesOnVideoStart() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.videoPlaybackBegan(appLovinAd)

    verify(rewardedAdCallback).onVideoStart()
  }

  @Test
  fun videoPlaybackBegan_withNullAdCallback_doesNotInvokeOnVideoStart() {
    appLovinMediationRewardedAd.videoPlaybackBegan(appLovinAd)

    verify(rewardedAdCallback, never()).onVideoStart()
  }

  @Test
  fun videoPlaybackEnded_withFullyWatchedAd_invokesOnUserEarnedRewardAndOnVideoComplete() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.videoPlaybackEnded(
      appLovinAd,
      /*percentViewed=*/ 1.0,
      /*fullyWatched=*/ true,
    )

    verify(rewardedAdCallback).onUserEarnedReward()
    verify(rewardedAdCallback).onVideoComplete()
  }

  @Test
  fun videoPlaybackEnded_withoutFullyWatchedAd_doesNotInvokeAnyCallbackMethod() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.videoPlaybackEnded(
      appLovinAd,
      /*percentViewed=*/ 1.0,
      /*fullyWatched=*/ false,
    )

    verify(rewardedAdCallback, never()).onUserEarnedReward()
    verify(rewardedAdCallback, never()).onVideoComplete()
  }

  @Test
  fun videoPlaybackEnded_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    appLovinMediationRewardedAd.videoPlaybackEnded(
      appLovinAd,
      /*percentViewed=*/ 1.0,
      /*fullyWatched=*/ true,
    )

    verify(rewardedAdCallback, never()).onUserEarnedReward()
    verify(rewardedAdCallback, never()).onVideoComplete()
  }

  companion object {
    private const val TEST_CURRENCY = "TEST_CURRENCY"
    private const val TEST_AMOUNT = "10.0"
  }
}
