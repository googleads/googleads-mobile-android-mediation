package com.google.ads.mediation.applovin

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.mediation.AppLovinUtils
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinErrorCodes
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_AD_ALREADY_REQUESTED
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_PRESENTATION_AD_NOT_READY
import com.google.ads.mediation.applovin.AppLovinRewardedRenderer.ERROR_MSG_AD_NOT_READY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppLovinWaterfallRewardedRendererTest {

  // Subject of tests
  private lateinit var appLovinRewardedAd: AppLovinWaterfallRewardedRenderer
  private lateinit var appLovinInitializer: AppLovinInitializer

  private val appLovinAd: AppLovinAd = mock()
  private val rewardedAdConfiguration: MediationRewardedAdConfiguration = mock()
  private val rewardedAdCallback: MediationRewardedAdCallback = mock()
  private val rewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn rewardedAdCallback
    }
  private val appLovinSdkSettings: AppLovinSdkSettings = mock()
  private val adService: AppLovinAdService = mock()
  private val appLovinSdk: AppLovinSdk = mock {
    on { getAdService() } doReturn adService
    on { getSettings() } doReturn appLovinSdkSettings
  }
  private val appLovinSdkWrapper: AppLovinSdkWrapper = mock {
    on { getInstance(any()) } doReturn appLovinSdk
  }
  private val appLovinSdkUtilsWrapper: AppLovinSdkUtilsWrapper = mock {
    on { runOnUiThread(any()) } doAnswer
      { invocation ->
        val args = invocation.arguments
        (args[0] as Runnable).run()
      }
  }
  private val appLovinIncentivizedInterstitial: AppLovinIncentivizedInterstitial = mock()
  private val appLovinAdFactory: AppLovinAdFactory = mock {
    on { createIncentivizedInterstitial(any(), any()) } doReturn appLovinIncentivizedInterstitial
    on { createIncentivizedInterstitial(any()) } doReturn appLovinIncentivizedInterstitial
  }
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    appLovinInitializer = spy(AppLovinInitializer(appLovinSdkWrapper))
    appLovinRewardedAd =
      AppLovinWaterfallRewardedRenderer(
        rewardedAdLoadCallback,
        appLovinInitializer,
        appLovinAdFactory,
        appLovinSdkUtilsWrapper,
      )

    whenever(rewardedAdConfiguration.context) doReturn context
    val serverParameters =
      bundleOf(
        AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
        AppLovinUtils.ServerParameterKeys.ZONE_ID to TEST_ZONE_ID,
      )
    whenever(rewardedAdConfiguration.serverParameters) doReturn serverParameters
  }

  @After
  fun tearDown() {
    AppLovinWaterfallRewardedRenderer.incentivizedAdsMap.clear()
  }

  @Test
  fun failedToReceiveAd_removesZoneIdToLetAnotherAdLoadWithSameZoneId() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    val errorCaptor = argumentCaptor<AdError>()
    appLovinRewardedAd.loadAd(rewardedAdConfiguration)

    appLovinRewardedAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)
    appLovinRewardedAd.loadAd(rewardedAdConfiguration)

    verify(rewardedAdLoadCallback).onFailure(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isNotEqualTo(ERROR_AD_ALREADY_REQUESTED)
    verify(appLovinIncentivizedInterstitial, times(2)).preload(appLovinRewardedAd)
  }

  @Test
  fun adHidden_invokesUnregisterToLetAnotherAdLoadWithSameZoneId() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinRewardedAd.loadAd(rewardedAdConfiguration)
    appLovinRewardedAd.adReceived(appLovinAd)

    appLovinRewardedAd.adHidden(appLovinAd)
    appLovinRewardedAd.loadAd(rewardedAdConfiguration)

    verify(rewardedAdLoadCallback, never()).onFailure(any<AdError>())
    verify(appLovinIncentivizedInterstitial, times(2)).preload(appLovinRewardedAd)
  }

  @Test
  fun showAd_withLoadedAd_invokesShow() {
    doAnswer { true }.whenever(appLovinIncentivizedInterstitial).isAdReadyToDisplay()
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinRewardedAd.loadAd(rewardedAdConfiguration)
    appLovinRewardedAd.adReceived(appLovinAd)

    appLovinRewardedAd.showAd(context)

    verify(appLovinIncentivizedInterstitial)
      .show(context, appLovinRewardedAd, appLovinRewardedAd, appLovinRewardedAd, appLovinRewardedAd)
  }

  @Test
  fun showAd_withoutLoadedAd_invokesOnAdFailedToShow() {
    doAnswer { false }.whenever(appLovinIncentivizedInterstitial).isAdReadyToDisplay()
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinRewardedAd.loadAd(rewardedAdConfiguration)
    appLovinRewardedAd.adReceived(appLovinAd)
    val errorCaptor = argumentCaptor<AdError>()

    appLovinRewardedAd.showAd(context)

    verify(rewardedAdCallback).onAdFailedToShow(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_PRESENTATION_AD_NOT_READY)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_AD_NOT_READY)
    assertThat(capturedError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadAd_removesAdFromMap() {
    val serverParameters =
      bundleOf(
        AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
        AppLovinUtils.ServerParameterKeys.ZONE_ID to TEST_ZONE_ID,
      )
    whenever(rewardedAdConfiguration.serverParameters) doReturn serverParameters
    doAnswer { true }.whenever(appLovinIncentivizedInterstitial).isAdReadyToDisplay()
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinRewardedAd.loadAd(rewardedAdConfiguration)

    appLovinRewardedAd.adReceived(appLovinAd)

    assertThat(AppLovinWaterfallRewardedRenderer.incentivizedAdsMap.containsKey(TEST_ZONE_ID))
      .isFalse()
  }

  @Test
  fun loadRewardedAd_withCorrectParametersAndInitSuccess_invokesPreload() {
    // Putting this test here instead of AppLovinMedaitionAdapterTest because it is essential to
    // have the adHidden method remove the zoneID from the class' Map in between tests.
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())

    appLovinRewardedAd.loadAd(rewardedAdConfiguration)

    verify(appLovinIncentivizedInterstitial).preload(appLovinRewardedAd)
  }

  @Test
  fun appLovinWaterfallRewardedAd_isASubclassOfAppLovinRewardedRenderer() {
    assertThat(appLovinRewardedAd).isInstanceOf(AppLovinRewardedRenderer::class.java)
  }

  companion object {
    private const val TEST_SDK_KEY = "sdkKey"
    private const val TEST_ZONE_ID = "zoneId"
    private const val TEST_TRUE_VALUE = "true"
    private const val TEST_FALSE_VALUE = "false"
  }
}
