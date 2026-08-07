package com.google.ads.mediation.applovin

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.mediation.AppLovinUtils
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinErrorCodes
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class AppLovinInterstitialRendererTest {

  // Subject of tests
  private lateinit var appLovinMediationInterstitialAd: AppLovinInterstitialRenderer

  private val appLovinAd: AppLovinAd = mock()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
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

    assertThat(interstitialAdLoadCallback).hasSucceededWith(appLovinMediationInterstitialAd)
  }

  @Test
  fun failedToReceiveAd_invokesOnFailure() {
    appLovinMediationInterstitialAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)

    val expectedError = AppLovinUtils.getAdError(AppLovinErrorCodes.NO_FILL)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun adDisplayed_invokesOnAdOpenedAndReportsAdImpression() {
    // Simulate successful ad loading
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.adDisplayed(appLovinAd)

    assertThat(interstitialAdCallback.isOpened).isTrue()
    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun adHidden_invokesOnAdClosed() {
    // Simulate successful ad loading
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.adHidden(appLovinAd)

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun adClicked_invokesReportAdClickedAndOnAdLeftApplication() {
    // Simulate successful ad loading
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.adClicked(appLovinAd)

    assertThat(interstitialAdCallback.isClicked).isTrue()
    assertThat(interstitialAdCallback.isLeftApplication).isTrue()
  }
}
