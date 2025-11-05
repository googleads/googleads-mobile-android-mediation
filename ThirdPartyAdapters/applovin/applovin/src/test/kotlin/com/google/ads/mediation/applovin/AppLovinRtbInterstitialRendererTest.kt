package com.google.ads.mediation.applovin

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.adview.AppLovinInterstitialAdDialog
import com.applovin.mediation.AppLovinExtras
import com.applovin.mediation.AppLovinUtils
import com.applovin.mediation.rtb.AppLovinRtbInterstitialRenderer
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AppLovinRtbInterstitialRendererTest {

  // Subject of tests
  private lateinit var appLovinMediationInterstitialAd: AppLovinRtbInterstitialRenderer

  private val appLovinAd: AppLovinAd = mock()
  private val adService: AppLovinAdService = mock()
  private val appLovinSdkSettings: AppLovinSdkSettings = mock()
  private val appLovinSdk: AppLovinSdk = mock {
    on { adService } doReturn adService
    on { settings } doReturn appLovinSdkSettings
  }
  private val appLovinSdkWrapper: AppLovinSdkWrapper = mock {
    on { getInstance(any()) } doReturn appLovinSdk
  }
  private val appLovinInitializer: AppLovinInitializer = AppLovinInitializer(appLovinSdkWrapper)
  private val appLovinInterstitialAdDialog: AppLovinInterstitialAdDialog = mock()
  private val appLovinAdFactory: AppLovinAdFactory = mock {
    on { createInterstitialAdDialog(any(), any()) } doReturn appLovinInterstitialAdDialog
  }
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mediationExtras = bundleOf(AppLovinExtras.Keys.MUTE_AUDIO to true)
  private val serverParameters =
    bundleOf(
      AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
      AppLovinUtils.ServerParameterKeys.ZONE_ID to TEST_ZONE_ID,
    )
  private val interstitialAdConfiguration: MediationInterstitialAdConfiguration = mock {
    on { context } doReturn context
    on { serverParameters } doReturn serverParameters
    on { mediationExtras } doReturn mediationExtras
  }
  private val interstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val interstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()

  @Before
  fun setUp() {
    appLovinMediationInterstitialAd =
      AppLovinRtbInterstitialRenderer(
        interstitialAdLoadCallback,
        appLovinInitializer,
        appLovinAdFactory,
      )
  }

  @Test
  fun showAd_invokesShowAndRender() {
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.showAd(context)

    verify(appLovinInterstitialAdDialog).showAndRender(appLovinAd)
  }

  @Test
  fun showAd_setsSdkMuteSettings() {
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.showAd(context)

    verify(appLovinSdkSettings).setMuted(true)
  }

  @Test
  fun appLovinRtbInterstitialAd_isASubclassOfAppLovinInterstitialRenderer() {
    assertThat(appLovinMediationInterstitialAd)
      .isInstanceOf(AppLovinInterstitialRenderer::class.java)
  }

  companion object {
    private const val TEST_SDK_KEY = "sdkKey"
    private const val TEST_ZONE_ID = "zoneId"
  }
}
