package com.google.ads.mediation.applovin

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.mediation.AppLovinUtils
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinErrorCodes
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class AppLovinRewardedRendererTest {

  // Subject of tests
  private lateinit var appLovinMediationRewardedAd: AppLovinRewardedRenderer

  private val appLovinAd: AppLovinAd = mock()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
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

    assertThat(rewardedAdLoadCallback).hasSucceededWith(appLovinMediationRewardedAd)
  }

  @Test
  fun failedToReceiveAd_invokesOnFailure() {
    appLovinMediationRewardedAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)

    val expectedError = AppLovinUtils.getAdError(AppLovinErrorCodes.NO_FILL)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun adDisplayed_invokesOnAdOpened() {
    // Simulate successful ad loading
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.adDisplayed(appLovinAd)

    assertThat(rewardedAdCallback.isOpened).isTrue()
    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun adDisplayed_withNullAdCallback_invokesOnAdOpenedAndReportAdImpression() {
    appLovinMediationRewardedAd.adDisplayed(appLovinAd)

    assertThat(rewardedAdCallback.isOpened).isFalse()
    assertThat(rewardedAdCallback.isImpressionReported).isFalse()
  }

  @Test
  fun adHidden_invokesOnUserEarnedRewardAndOnAdClosed() {
    // Mocking successful ad load
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.adHidden(appLovinAd)

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun adHidden_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    appLovinMediationRewardedAd.adHidden(appLovinAd)

    assertThat(rewardedAdCallback.isUserEarnedReward).isFalse()
    assertThat(rewardedAdCallback.isClosed).isFalse()
  }

  @Test
  fun adClicked_invokesReportAdClicked() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.adClicked(appLovinAd)

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun adClicked_withNullAdCallback_doesNotInvokeReportAdClicked() {
    appLovinMediationRewardedAd.adClicked(appLovinAd)

    assertThat(rewardedAdCallback.isClicked).isFalse()
  }

  @Test
  fun videoPlaybackBegan_invokesOnVideoStart() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.videoPlaybackBegan(appLovinAd)

    assertThat(rewardedAdCallback.isVideoStarted).isTrue()
  }

  @Test
  fun videoPlaybackBegan_withNullAdCallback_doesNotInvokeOnVideoStart() {
    appLovinMediationRewardedAd.videoPlaybackBegan(appLovinAd)

    assertThat(rewardedAdCallback.isVideoStarted).isFalse()
  }

  @Test
  fun videoPlaybackEnded_withFullyWatchedAd_invokesOnUserEarnedRewardAndOnVideoComplete() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.videoPlaybackEnded(
      appLovinAd,
      /*percentViewed=*/ 1.0,
      /*fullyWatched=*/ true,
    )

    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
    assertThat(rewardedAdCallback.isVideoCompleted).isTrue()
  }

  @Test
  fun videoPlaybackEnded_withoutFullyWatchedAd_doesNotInvokeAnyCallbackMethod() {
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.videoPlaybackEnded(
      appLovinAd,
      /*percentViewed=*/ 1.0,
      /*fullyWatched=*/ false,
    )

    assertThat(rewardedAdCallback.isUserEarnedReward).isFalse()
    assertThat(rewardedAdCallback.isVideoCompleted).isFalse()
  }

  @Test
  fun videoPlaybackEnded_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    appLovinMediationRewardedAd.videoPlaybackEnded(
      appLovinAd,
      /*percentViewed=*/ 1.0,
      /*fullyWatched=*/ true,
    )

    assertThat(rewardedAdCallback.isUserEarnedReward).isFalse()
    assertThat(rewardedAdCallback.isVideoCompleted).isFalse()
  }
}
