package com.google.ads.mediation.ironsource

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.ironsource.IronSourceInterstitialAd.getFromAvailableInstances
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_CODE_DECRYPT_FAILED
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [IronSourceInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class IronSourceInterstitialAdTest {

  private lateinit var ironSourceInterstitialAd: IronSourceInterstitialAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val interstitialAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
  private val mockInterstitialAdCallback = mock<MediationInterstitialAdCallback>()

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

    verify(interstitialAdLoadCallback).onSuccess(ironSourceInterstitialAd)
  }

  @Test
  fun onInterstitialAdReady_withoutInterstitialAd_verifyNoCallbacks() {
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()

    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")

    verifyNoInteractions(interstitialAdLoadCallback)
  }

  @Test
  fun onInterstitialAdLoadFailed_withInterstitialAd_verifyOnFailureCallback() {
    loadInterstitialAd()
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdLoadFailed(
      /* instanceId= */ "0",
      ironSourceError
    )

    val expectedAdError =
      AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
    verify(interstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onInterstitialAdLoadFailed_withoutInterstitialAd_verifyNoCallbacks() {
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdLoadFailed(
      /* instanceId= */ "0",
      ironSourceError
    )

    verifyNoInteractions(interstitialAdLoadCallback)
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
    whenever(interstitialAdLoadCallback.onSuccess(ironSourceInterstitialAd)) doReturn
      mockInterstitialAdCallback
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    ironSourceInterstitialAdListener.onInterstitialAdReady(/* instanceId= */ "0")
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdShowFailed(
      /* instanceId= */ "0",
      ironSourceError
    )

    val expectedAdError =
      AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
  }

  @Test
  fun onInterstitialAdShowFailed_withoutInterstitialAd_verifyOnAdFailedToShow() {
    loadInterstitialAd()
    whenever(interstitialAdLoadCallback.onSuccess(ironSourceInterstitialAd)) doReturn
      mockInterstitialAdCallback
    val ironSourceInterstitialAdListener =
      IronSourceInterstitialAd.getIronSourceInterstitialListener()
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    ironSourceInterstitialAdListener.onInterstitialAdShowFailed(
      /* instanceId= */ "1",
      ironSourceError
    )

    verifyNoInteractions(mockInterstitialAdCallback)
  }

  private fun loadInterstitialAd() {
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(activity)
    ironSourceInterstitialAd =
      IronSourceInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)
    ironSourceInterstitialAd.loadAd()
  }
}
