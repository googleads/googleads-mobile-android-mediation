package com.google.ads.mediation.ironsource

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.ironsource.IronSourceInterstitialAd.getFromAvailableInstances
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_CODE_DECRYPT_FAILED
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric

/** Tests for [IronSourceInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class IronSourceInterstitialAdTest {

  private lateinit var ironSourceInterstitialAd: IronSourceInterstitialAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )

  @After
  fun tearDown() {
    IronSourceInterstitialAd.removeFromAvailableInstances(/* instanceId= */ "0")
  }

  @Test
  fun onInterstitialAdReady_withInterstitialAd_verifyOnSuccessCallback() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()

    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")

    assertThat(interstitialAdLoadCallback).hasSucceededWith(ironSourceInterstitialAd)
  }

  @Test
  fun onInterstitialAdReady_withoutInterstitialAd_verifyNoCallbacks() {
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()

    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")

    assertThat(interstitialAdLoadCallback).hasNotSucceeded()
    assertThat(interstitialAdLoadCallback).hasNoFailure()
  }

  @Test
  fun onInterstitialAdLoadFailed_withInterstitialAd_verifyOnFailureCallback() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdLoadFailed(
      /* instanceId= */ "0",
      ironSourceError,
    )

    val expectedAdError =
      AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onInterstitialAdLoadFailed_withoutInterstitialAd_verifyNoCallbacks() {
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdLoadFailed(
      /* instanceId= */ "0",
      ironSourceError,
    )

    assertThat(interstitialAdLoadCallback).hasNotSucceeded()
    assertThat(interstitialAdLoadCallback).hasNoFailure()
  }

  @Test
  fun showAd_verifyShowAdInvoked() {
    mockStatic(IronSource::class.java).use {
      loadInterstitialAd()

      ironSourceInterstitialAd.showAd(activity)

      it.verify { IronSource.showISDemandOnlyInterstitial("0") }
    }
  }

  @Test
  fun onInterstitialAdShowFailed_withInterstitialAd_verifyOnAdFailedToShow() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdShowFailed(
      /* instanceId= */ "0",
      ironSourceError,
    )

    val expectedAdError =
      AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onInterstitialAdShowFailed_withoutInterstitialAd_verifyOnAdFailedToShow() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdShowFailed(
      /* instanceId= */ "1",
      ironSourceError,
    )

    assertThat(interstitialAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun onInterstitialAdShowFailed_withoutInterstitialAdCallbackInstance_verifyOnAdFailedToShow() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdShowFailed(
      /* instanceId= */ "0",
      ironSourceError,
    )

    assertThat(interstitialAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun onInterstitialAdOpened_withInterstitialAd_verifyOnInterstitialAdOpenedCallbacks() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")

    ironSourceInterstitialAdListener.onInterstitialAdOpened(/* instanceId= */ "0")

    assertThat(interstitialAdCallback.isOpened).isTrue()
    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onInterstitialAdClosed_withInterstitialAd_verifyOnAdClosedCallback() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")

    ironSourceInterstitialAdListener.onInterstitialAdClosed(/* instanceId= */ "0")

    assertThat(interstitialAdCallback.isClosed).isTrue()
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onInterstitialAdClicked_withInterstitialAd_verifyReportAdClickedCallback() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")

    ironSourceInterstitialAdListener.onInterstitialAdClicked(/* instanceId= */ "0")

    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdEvents_withoutInterstitialAd_verifyNoCallbacks() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()

    ironSourceInterstitialAdListener.onInterstitialAdOpened(/* instanceId= */ "1")
    ironSourceInterstitialAdListener.onInterstitialAdClosed(/* instanceId= */ "1")
    ironSourceInterstitialAdListener.onInterstitialAdClicked(/* instanceId= */ "1")

    assertThat(interstitialAdCallback.isOpened).isFalse()
    assertThat(interstitialAdCallback.isImpressionReported).isFalse()
    assertThat(interstitialAdCallback.isClosed).isFalse()
    assertThat(interstitialAdCallback.isClicked).isFalse()
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isEqualTo(ironSourceInterstitialAd)
  }

  @Test
  fun onAdEvents_withoutInterstitialAdCallbackInstance_verifyNoCallbacks() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()

    ironSourceInterstitialAdListener.onInterstitialAdOpened(/* instanceId= */ "0")
    ironSourceInterstitialAdListener.onInterstitialAdClicked(/* instanceId= */ "0")
    ironSourceInterstitialAdListener.onInterstitialAdClosed(/* instanceId= */ "0")

    assertThat(interstitialAdCallback.isOpened).isFalse()
    assertThat(interstitialAdCallback.isImpressionReported).isFalse()
    assertThat(interstitialAdCallback.isClicked).isFalse()
    assertThat(interstitialAdCallback.isClosed).isFalse()
  }

  private fun loadInterstitialAd() {
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(activity)
    ironSourceInterstitialAd =
      IronSourceInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)
    ironSourceInterstitialAd.loadWaterfallAd(mediationAdConfiguration)
  }
}
