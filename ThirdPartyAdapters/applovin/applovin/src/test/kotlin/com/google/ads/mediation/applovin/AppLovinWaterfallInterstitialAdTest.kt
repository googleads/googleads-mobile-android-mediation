package com.google.ads.mediation.applovin

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.adview.AppLovinInterstitialAdDialog
import com.applovin.mediation.AppLovinUtils
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinErrorCodes
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_AD_ALREADY_REQUESTED
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppLovinWaterfallInterstitialAdTest {

  // Subject of tests
  private lateinit var appLovinMediationInterstitialAd: AppLovinWaterfallInterstitialAd
  private lateinit var appLovinInitializer: AppLovinInitializer

  private val appLovinAd: AppLovinAd = mock()
  private val interstitialAdConfiguration: MediationInterstitialAdConfiguration = mock()
  private val interstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val interstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn interstitialAdCallback
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
  private val appLovinInterstitialAdDialog: AppLovinInterstitialAdDialog = mock()
  private val appLovinAdFactory: AppLovinAdFactory = mock {
    on { createInterstitialAdDialog(any(), any()) } doReturn appLovinInterstitialAdDialog
  }
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    appLovinInitializer = spy(AppLovinInitializer(appLovinSdkWrapper))
    appLovinMediationInterstitialAd =
      AppLovinWaterfallInterstitialAd(
        interstitialAdLoadCallback,
        appLovinInitializer,
        appLovinAdFactory,
      )

    whenever(interstitialAdConfiguration.context) doReturn context
    val serverParameters =
      bundleOf(
        AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
        AppLovinUtils.ServerParameterKeys.ZONE_ID to TEST_ZONE_ID,
      )
    whenever(interstitialAdConfiguration.serverParameters) doReturn serverParameters
  }

  @After
  fun tearDown() {
    appLovinMediationInterstitialAd.unregister()
    AppLovinWaterfallInterstitialAd.appLovinWaterfallInterstitialAds.clear()
  }

  @Test
  fun failedToReceiveAd_invokesUnregisterToLetAnotherAdLoadWithSameZoneId() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    val errorCaptor = argumentCaptor<AdError>()
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)

    appLovinMediationInterstitialAd.failedToReceiveAd(AppLovinErrorCodes.NO_FILL)
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)

    verify(interstitialAdLoadCallback).onFailure(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isNotEqualTo(ERROR_AD_ALREADY_REQUESTED)
    verify(adService, times(2)).loadNextAdForZoneId(eq(TEST_ZONE_ID), any())
  }

  @Test
  fun adHidden_invokesUnregisterToLetAnotherAdLoadWithSameZoneId() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.adHidden(appLovinAd)
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)

    verify(interstitialAdLoadCallback, never()).onFailure(any<AdError>())
    verify(adService, times(2)).loadNextAdForZoneId(eq(TEST_ZONE_ID), any())
  }

  @Test
  fun showAd_withLoadedAd_invokesShowAndRender() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)
    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    appLovinMediationInterstitialAd.showAd(context)

    verify(appLovinInterstitialAdDialog).setAdDisplayListener(appLovinMediationInterstitialAd)
    verify(appLovinInterstitialAdDialog).setAdClickListener(appLovinMediationInterstitialAd)
    verify(appLovinInterstitialAdDialog).setAdVideoPlaybackListener(appLovinMediationInterstitialAd)
    verify(appLovinInterstitialAdDialog).showAndRender(any())
  }

  @Test
  fun showAd_withoutLoadedAd_doesNotInvokeShowAndRender() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)

    appLovinMediationInterstitialAd.showAd(context)

    verify(appLovinInterstitialAdDialog, never()).show()
    verify(appLovinInterstitialAdDialog, never()).showAndRender(any())
  }

  @Test
  fun showAd_withoutLoadedAdNorNoZoneId_invokeShow() {
    // Reinitializing serverParameters to remove the ZoneId
    val serverParameters = bundleOf(AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY)
    whenever(interstitialAdConfiguration.serverParameters) doReturn serverParameters
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)

    appLovinMediationInterstitialAd.showAd(context)

    verify(appLovinInterstitialAdDialog).show()
  }

  @Test
  fun loadAd_removesAdFromMap() {
    val serverParameters =
      bundleOf(
        AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
        AppLovinUtils.ServerParameterKeys.ZONE_ID to TEST_ZONE_ID,
      )
    whenever(interstitialAdConfiguration.serverParameters) doReturn serverParameters
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)

    appLovinMediationInterstitialAd.adReceived(appLovinAd)

    assertThat(
        AppLovinWaterfallInterstitialAd.appLovinWaterfallInterstitialAds.containsKey(TEST_ZONE_ID)
      )
      .isFalse()
  }

  @Test
  fun loadInterstitialAd_withCorrectParametersAndInitSuccess_invokesLoadNextAdForZoneId() {
    // Putting this test here instead of AppLovinMedaitionAdapterTest because it is essential to
    // have the unregister method remove the zoneID from the class' Map in between tests.
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())

    appLovinMediationInterstitialAd.loadAd(interstitialAdConfiguration)

    verify(adService).loadNextAdForZoneId(eq(TEST_ZONE_ID), any())
  }

  @Test
  fun appLovinWaterfallInterstitialAd_isASubclassOfAppLovinInterstitialRenderer() {
    assertThat(appLovinMediationInterstitialAd)
      .isInstanceOf(AppLovinInterstitialRenderer::class.java)
  }

  companion object {
    private const val TEST_SDK_KEY = "sdkKey"
    private const val TEST_ZONE_ID = "zoneId"
    private const val TEST_TRUE_VALUE = "true"
    private const val TEST_FALSE_VALUE = "false"
  }
}
