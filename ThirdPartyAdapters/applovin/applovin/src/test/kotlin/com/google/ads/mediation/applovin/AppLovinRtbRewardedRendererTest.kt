package com.google.ads.mediation.applovin

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.mediation.AppLovinExtras
import com.applovin.mediation.AppLovinUtils
import com.applovin.mediation.rtb.AppLovinRtbRewardedRenderer
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AppLovinRtbRewardedRendererTest {

  // Subject of tests
  private lateinit var appLovinMediationRewardedAd: AppLovinRtbRewardedRenderer

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
  private val appLovinRewardedAd: AppLovinIncentivizedInterstitial = mock()
  private val appLovinAdFactory: AppLovinAdFactory = mock {
    on { createIncentivizedInterstitial(appLovinSdk) } doReturn appLovinRewardedAd
  }
  private val appLovinSdkUtilsWrapper: AppLovinSdkUtilsWrapper = mock()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mediationExtras = bundleOf(AppLovinExtras.Keys.MUTE_AUDIO to true)
  private val serverParameters =
    bundleOf(
      AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
      AppLovinUtils.ServerParameterKeys.ZONE_ID to TEST_ZONE_ID,
    )
  private val rewardedAdConfiguration: MediationRewardedAdConfiguration = mock {
    on { context } doReturn context
    on { serverParameters } doReturn serverParameters
    on { mediationExtras } doReturn mediationExtras
  }
  private val rewardedAdCallback: MediationRewardedAdCallback = mock()
  private val rewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()

  @Before
  fun setUp() {
    appLovinMediationRewardedAd =
      AppLovinRtbRewardedRenderer(
        rewardedAdLoadCallback,
        appLovinInitializer,
        appLovinAdFactory,
        appLovinSdkUtilsWrapper,
      )
  }

  @Test
  fun showAd_invokesShow() {
    appLovinMediationRewardedAd.loadAd(rewardedAdConfiguration)
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.showAd(context)

    verify(appLovinRewardedAd)
      .show(
        appLovinAd,
        context,
        appLovinMediationRewardedAd,
        appLovinMediationRewardedAd,
        appLovinMediationRewardedAd,
        appLovinMediationRewardedAd,
      )
  }

  @Test
  fun showAd_setsSdkMuteSettings() {
    appLovinMediationRewardedAd.loadAd(rewardedAdConfiguration)
    appLovinMediationRewardedAd.adReceived(appLovinAd)

    appLovinMediationRewardedAd.showAd(context)

    verify(appLovinSdkSettings).setMuted(true)
  }

  @Test
  fun appLovinRtbRewardedAd_isASubclassOfAppLovinRewardedRenderer() {
    assertThat(appLovinMediationRewardedAd).isInstanceOf(AppLovinRewardedRenderer::class.java)
  }

  companion object {
    private const val TEST_SDK_KEY = "sdkKey"
    private const val TEST_ZONE_ID = "zoneId"
  }
}
