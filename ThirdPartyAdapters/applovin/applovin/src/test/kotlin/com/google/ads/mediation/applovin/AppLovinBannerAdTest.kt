package com.google.ads.mediation.applovin

import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.adview.AppLovinAdView
import com.applovin.mediation.AppLovinUtils
import com.applovin.mediation.AppLovinUtils.ERROR_MSG_REASON_PREFIX
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinAdSize
import com.applovin.sdk.AppLovinErrorCodes
import com.applovin.sdk.AppLovinSdk
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppLovinBannerAdTest {

  // Subject of tests
  private lateinit var appLovinBannerAd: AppLovinBannerAd
  private lateinit var appLovinInitializer: AppLovinInitializer

  private val appLovinAd: AppLovinAd = mock()
  private val bannerAdConfiguration: MediationBannerAdConfiguration = mock {
    on { context } doReturn ApplicationProvider.getApplicationContext()
  }
  private val bannerAdCallback: MediationBannerAdCallback = mock()
  private val bannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val appLovinAdService: AppLovinAdService = mock()
  private val appLovinSdk: AppLovinSdk = mock { on { getAdService() } doReturn appLovinAdService }
  private val appLovinSdkWrapper: AppLovinSdkWrapper = mock {
    on { getInstance(any()) } doReturn appLovinSdk
  }
  private val appLovinAdView: AppLovinAdView =
    AppLovinAdView(
      /* sdk= */ null,
      AppLovinAdSize.BANNER,
      ApplicationProvider.getApplicationContext(),
    )
  private val appLovinAdViewWrapper: AppLovinAdViewWrapper = mock()
  private val appLovinAdFactory: AppLovinAdFactory = mock {
    on { createAdView(any(), any(), any(), any()) } doReturn appLovinAdViewWrapper
  }

  @Before
  fun setUp() {
    appLovinInitializer = spy(AppLovinInitializer(appLovinSdkWrapper))
    appLovinBannerAd =
      AppLovinBannerAd.newInstance(bannerAdLoadCallback, appLovinInitializer, appLovinAdFactory)

    val serverParameters = bundleOf(AppLovinUtils.ServerParameterKeys.SDK_KEY to SDK_KEY)
    whenever(bannerAdConfiguration.serverParameters) doReturn serverParameters
    whenever(bannerAdConfiguration.adSize) doReturn AdSize.BANNER
    whenever(bannerAdLoadCallback.onSuccess(appLovinBannerAd)) doReturn bannerAdCallback
  }

  @Test
  fun adReceived_invokesOnSuccessAndRenderAd() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinBannerAd.loadAd(bannerAdConfiguration)

    appLovinBannerAd.adReceived(appLovinAd)

    verify(appLovinAdViewWrapper).renderAd(appLovinAd)
    verify(bannerAdLoadCallback).onSuccess(appLovinBannerAd)
  }

  @Test
  fun failedToReceiveAd_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    appLovinBannerAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)

    verify(bannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(AppLovinErrorCodes.NO_FILL)
    assertThat(capturedError.message.startsWith(ERROR_MSG_REASON_PREFIX)).isTrue()
    assertThat(capturedError.domain).isEqualTo(APPLOVIN_SDK_ERROR_DOMAIN)
  }

  @Test
  fun adDisplayed_invokesOnAdOpened() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinBannerAd.loadAd(bannerAdConfiguration)
    appLovinBannerAd.adReceived(appLovinAd)

    appLovinBannerAd.adDisplayed(appLovinAd)

    verify(bannerAdCallback).onAdOpened()
  }

  @Test
  fun adClicked_invokesReportAdClicked() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinBannerAd.loadAd(bannerAdConfiguration)
    appLovinBannerAd.adReceived(appLovinAd)

    appLovinBannerAd.adClicked(appLovinAd)

    verify(bannerAdCallback).reportAdClicked()
  }

  @Test
  fun adOpenedFullscreen_invokesOnAdOpened() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinBannerAd.loadAd(bannerAdConfiguration)
    appLovinBannerAd.adReceived(appLovinAd)

    appLovinBannerAd.adOpenedFullscreen(appLovinAd, appLovinAdView)

    verify(bannerAdCallback).onAdOpened()
  }

  @Test
  fun adClosedFullscreen_invokesOnAdClosed() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinBannerAd.loadAd(bannerAdConfiguration)
    appLovinBannerAd.adReceived(appLovinAd)

    appLovinBannerAd.adClosedFullscreen(appLovinAd, appLovinAdView)

    verify(bannerAdCallback).onAdClosed()
  }

  @Test
  fun adLeftApplication_invokesOnAdLeftApplication() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinBannerAd.loadAd(bannerAdConfiguration)
    appLovinBannerAd.adReceived(appLovinAd)

    appLovinBannerAd.adLeftApplication(appLovinAd, appLovinAdView)

    verify(bannerAdCallback).onAdLeftApplication()
  }

  companion object {
    private const val SDK_KEY = "sdkKey"
  }
}
