package com.google.ads.mediation.applovin

import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.adview.AppLovinAdView
import com.applovin.mediation.AppLovinUtils
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinAdSize
import com.applovin.sdk.AppLovinErrorCodes
import com.applovin.sdk.AppLovinSdk
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
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
    on { serverParameters } doReturn bundleOf(AppLovinUtils.ServerParameterKeys.SDK_KEY to SDK_KEY)
    on { adSize } doReturn AdSize.BANNER
  }
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
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
  private val mediationUtils: MediationUtilsWrapper = mock {
    on { findClosestSize(any(), eq(AdSize.BANNER), any()) } doReturn AdSize.BANNER
  }

  @Before
  fun setUp() {
    appLovinInitializer = spy(AppLovinInitializer(appLovinSdkWrapper))
    appLovinBannerAd =
      AppLovinBannerAd.newInstance(bannerAdLoadCallback, appLovinInitializer, appLovinAdFactory)
  }

  private fun loadAndReceiveAd() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinBannerAd.loadAd(bannerAdConfiguration, mediationUtils)
    appLovinBannerAd.adReceived(appLovinAd)
  }

  @Test
  fun adReceived_invokesOnSuccessAndRenderAd() {
    loadAndReceiveAd()

    verify(appLovinAdViewWrapper).renderAd(appLovinAd)
    assertThat(bannerAdLoadCallback).hasSucceededWith(appLovinBannerAd)
  }

  @Test
  fun failedToReceiveAd_invokesOnFailure() {
    appLovinBannerAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)

    val expectedError = AppLovinUtils.getAdError(AppLovinErrorCodes.NO_FILL)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun adDisplayed_invokesOnAdOpened() {
    loadAndReceiveAd()

    appLovinBannerAd.adDisplayed(appLovinAd)

    assertThat(bannerAdCallback.isOpened).isTrue()
  }

  @Test
  fun adClicked_invokesReportAdClicked() {
    loadAndReceiveAd()

    appLovinBannerAd.adClicked(appLovinAd)

    assertThat(bannerAdCallback.isClicked).isTrue()
  }

  @Test
  fun adOpenedFullscreen_invokesOnAdOpened() {
    loadAndReceiveAd()

    appLovinBannerAd.adOpenedFullscreen(appLovinAd, appLovinAdView)

    assertThat(bannerAdCallback.isOpened).isTrue()
  }

  @Test
  fun adClosedFullscreen_invokesOnAdClosed() {
    loadAndReceiveAd()

    appLovinBannerAd.adClosedFullscreen(appLovinAd, appLovinAdView)

    assertThat(bannerAdCallback.isClosed).isTrue()
  }

  @Test
  fun adLeftApplication_invokesOnAdLeftApplication() {
    loadAndReceiveAd()

    appLovinBannerAd.adLeftApplication(appLovinAd, appLovinAdView)

    assertThat(bannerAdCallback.isLeftApplication).isTrue()
  }

  companion object {
    private const val SDK_KEY = "sdkKey"
  }
}
