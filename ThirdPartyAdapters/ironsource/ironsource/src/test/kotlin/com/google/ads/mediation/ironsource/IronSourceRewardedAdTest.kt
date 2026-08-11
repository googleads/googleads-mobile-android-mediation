package com.google.ads.mediation.ironsource

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.ads.mediation.ironsource.IronSourceRewardedAd.getFromAvailableInstances
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_CODE_DECRYPT_FAILED
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric

/** Tests for [IronSourceRewardedAd]. */
@RunWith(AndroidJUnit4::class)
class IronSourceRewardedAdTest {

  private lateinit var ironSourceRewardedAd: IronSourceRewardedAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )

  @After
  fun tearDown() {
    IronSourceRewardedAd.removeFromAvailableInstances(/* instanceId= */ "0")
  }

  @Test
  fun onRewardedVideoAdLoadSuccess_withRewardedAd_verifyOnSuccessCallback() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()

    ironSourceRewardedAdListener.onRewardedVideoAdLoadSuccess(/* instanceId= */ "0")

    assertThat(rewardedAdLoadCallback).hasSucceededWith(ironSourceRewardedAd)
  }

  @Test
  fun onRewardedVideoAdLoadSuccess_withoutRewardedAd_verifyNoCallbacks() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()

    ironSourceRewardedAdListener.onRewardedVideoAdLoadSuccess(/* instanceId= */ "1")

    assertThat(rewardedAdLoadCallback).hasNotSucceeded()
    assertThat(rewardedAdLoadCallback).hasNoFailure()
  }

  @Test
  fun onRewardedVideoAdLoadFailed_withRewardedAd_verifyOnFailureCallback() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceRewardedAdListener.onRewardedVideoAdLoadFailed(/* instanceId= */ "0", ironSourceError)

    val expectedAdError =
      AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onRewardedVideoAdLoadFailed_withoutRewardedAd_verifyNoCallbacks() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceRewardedAdListener.onRewardedVideoAdLoadFailed(/* instanceId= */ "1", ironSourceError)

    assertThat(rewardedAdLoadCallback).hasNotSucceeded()
    assertThat(rewardedAdLoadCallback).hasNoFailure()
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isEqualTo(ironSourceRewardedAd)
  }

  @Test
  fun showAd_verifyShowAdInvoked() {
    mockStatic(IronSource::class.java).use {
      loadRewardedAd()

      ironSourceRewardedAd.showAd(activity)

      it.verify { IronSource.showISDemandOnlyRewardedVideo("0") }
    }
  }

  @Test
  fun onRewardedAdShowFailed_withRewardedAd_verifyOnAdFailedToShow() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")
    ironSourceRewardedAdListener.onRewardedVideoAdLoadSuccess("0")

    ironSourceRewardedAdListener.onRewardedVideoAdShowFailed(/* instanceId= */ "0", ironSourceError)

    val expectedAdError =
      AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onRewardedAdShowFailed_withoutRewardedAd_verifyOnAdFailedToShow() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceRewardedAdListener.onRewardedVideoAdShowFailed(/* instanceId= */ "1", ironSourceError)

    assertThat(rewardedAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun onRewardedAdShowFailed_withoutRewardedAdCallbackInstance_verifyOnAdFailedToShow() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceRewardedAdListener.onRewardedVideoAdShowFailed(/* instanceId= */ "0", ironSourceError)

    assertThat(rewardedAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun onRewardedVideoAdOpened_withRewardedVideoAd_verifyOnRewardedVideoAdOpenedCallbacks() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    ironSourceRewardedAdListener.onRewardedVideoAdLoadSuccess(/* instanceId= */ "0")

    ironSourceRewardedAdListener.onRewardedVideoAdOpened(/* instanceId= */ "0")

    assertThat(rewardedAdCallback.isOpened).isTrue()
    assertThat(rewardedAdCallback.isVideoStarted).isTrue()
    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onRewardedVideoAdClosed_withRewardedAd_verifyOnAdClosedCallback() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    ironSourceRewardedAdListener.onRewardedVideoAdLoadSuccess(/* instanceId= */ "0")

    ironSourceRewardedAdListener.onRewardedVideoAdClosed(/* instanceId= */ "0")

    assertThat(rewardedAdCallback.isClosed).isTrue()
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onRewardedVideoAdRewarded_withRewardedAd_verifyOnRewardedCallbacks() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    ironSourceRewardedAdListener.onRewardedVideoAdLoadSuccess(/* instanceId= */ "0")

    ironSourceRewardedAdListener.onRewardedVideoAdRewarded(/* instanceId= */ "0")

    assertThat(rewardedAdCallback.isVideoCompleted).isTrue()
    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onRewardedVideoAdClicked_withRewardedAd_verifyReportAdClicked() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()
    ironSourceRewardedAdListener.onRewardedVideoAdLoadSuccess(/* instanceId= */ "0")

    ironSourceRewardedAdListener.onRewardedVideoAdClicked(/* instanceId= */ "0")

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdEvents_withoutRewardedAd_verifyNoCallbacks() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()

    ironSourceRewardedAdListener.onRewardedVideoAdOpened(/* instanceId= */ "1")
    ironSourceRewardedAdListener.onRewardedVideoAdClosed(/* instanceId= */ "1")
    ironSourceRewardedAdListener.onRewardedVideoAdRewarded(/* instanceId= */ "1")
    ironSourceRewardedAdListener.onRewardedVideoAdClicked(/* instanceId= */ "1")

    assertThat(rewardedAdCallback.isOpened).isFalse()
    assertThat(rewardedAdCallback.isVideoStarted).isFalse()
    assertThat(rewardedAdCallback.isImpressionReported).isFalse()
    assertThat(rewardedAdCallback.isClosed).isFalse()
    assertThat(rewardedAdCallback.isVideoCompleted).isFalse()
    assertThat(rewardedAdCallback.isUserEarnedReward).isFalse()
    assertThat(rewardedAdCallback.isClicked).isFalse()
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isEqualTo(ironSourceRewardedAd)
  }

  @Test
  fun onAdEvents_withoutRewardedAdCallbackInstance_verifyNoCallbacks() {
    loadRewardedAd()
    val ironSourceRewardedAdListener = IronSourceRewardedAd.getIronSourceRewardedListener()

    ironSourceRewardedAdListener.onRewardedVideoAdOpened(/* instanceId= */ "0")
    ironSourceRewardedAdListener.onRewardedVideoAdRewarded(/* instanceId= */ "0")
    ironSourceRewardedAdListener.onRewardedVideoAdClicked(/* instanceId= */ "0")
    ironSourceRewardedAdListener.onRewardedVideoAdClosed(/* instanceId= */ "0")

    assertThat(rewardedAdCallback.isOpened).isFalse()
    assertThat(rewardedAdCallback.isVideoStarted).isFalse()
    assertThat(rewardedAdCallback.isImpressionReported).isFalse()
    assertThat(rewardedAdCallback.isVideoCompleted).isFalse()
    assertThat(rewardedAdCallback.isUserEarnedReward).isFalse()
    assertThat(rewardedAdCallback.isClicked).isFalse()
    assertThat(rewardedAdCallback.isClosed).isFalse()
  }

  private fun loadRewardedAd() {
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)
    ironSourceRewardedAd = IronSourceRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)
    ironSourceRewardedAd.loadWaterfallAd(mediationAdConfiguration)
  }
}
