package com.google.ads.mediation.applovin

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.mediation.AppLovinUtils.ERROR_MSG_REASON_PREFIX
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinErrorCodes
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
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

@RunWith(AndroidJUnit4::class)
class AppLovinInterstitialRendererTest {

  // Subject of tests
  private lateinit var appLovinMediationInterstitialAd: AppLovinInterstitialRenderer

  private val appLovinAd: AppLovinAd = mock()
  private val interstitialAdConfiguration: MediationInterstitialAdConfiguration = mock()
  private val interstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val interstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn interstitialAdCallback
    }
  private val appLovinSdkWrapper: AppLovinSdkWrapper = AppLovinSdkWrapper()
  private val appLovinInitializer: AppLovinInitializer = AppLovinInitializer(appLovinSdkWrapper)
  private val appLovinAdFactory: AppLovinAdFactory = AppLovinAdFactory()

  @Before
  fun setUp() {
    appLovinMediationInterstitialAd =
      object :
        AppLovinInterstitialRenderer(
          interstitialAdLoadCallback,
          appLovinInitializer,
          appLovinAdFactory,
        ) {
        override fun loadAd(interstitialAdConfiguration: MediationInterstitialAdConfiguration) {}

        override fun showAd(context: Context) {}
      }
  }

  @Test
  fun adReceived_invokesOnSuccess() {
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    verify(interstitialAdLoadCallback).onSuccess(appLovinMediationInterstitialAd)
  }

  @Test
  fun failedToReceiveAd_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    appLovinMediationInterstitialAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)

    verify(interstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(AppLovinErrorCodes.NO_FILL)
    assertThat(capturedError.message.startsWith(ERROR_MSG_REASON_PREFIX)).isTrue()
    assertThat(capturedError.domain).isEqualTo(APPLOVIN_SDK_ERROR_DOMAIN)
  }

  @Test
  fun adDisplayed_invokesOnAdOpened() {
    // Simulate successful ad loading
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.adDisplayed(appLovinAd)

    verify(interstitialAdCallback).onAdOpened()
  }

  @Test
  fun adHidden_invokesOnAdClosed() {
    // Simulate successful ad loading
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.adHidden(appLovinAd)

    verify(interstitialAdCallback).onAdClosed()
  }

  @Test
  fun adClicked_invokesReportAdClickedAndOnAdLeftApplication() {
    // Simulate successful ad loading
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.adClicked(appLovinAd)

    verify(interstitialAdCallback).reportAdClicked()
    verify(interstitialAdCallback).onAdLeftApplication()
  }
}
