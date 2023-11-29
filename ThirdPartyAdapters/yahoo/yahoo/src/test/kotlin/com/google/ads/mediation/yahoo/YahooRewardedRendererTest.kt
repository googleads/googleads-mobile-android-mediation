package com.google.ads.mediation.yahoo

import android.app.Activity
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.yahoo.ads.YASAds
import com.yahoo.ads.interstitialplacement.InterstitialAd
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [YahooRewardedRenderer]. */
@RunWith(AndroidJUnit4::class)
class YahooRewardedRendererTest {

  private lateinit var adapter: YahooMediationAdapter
  private lateinit var rewardedRenderer: YahooRewardedRenderer

  private val mockMediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
  private val mockMediationRewardedAdConfiguration = mock<MediationRewardedAdConfiguration>()
  private val mockYahooFactory = mock<YahooFactory>()
  private val mockRewardedAd = mock<InterstitialAd>()
  private val mockRewardedPlacementConfig = mock<InterstitialPlacementConfig>()

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val emptyBundle = Bundle()

  @Before
  fun setUp() {
    adapter = YahooMediationAdapter()
    whenever(mockMediationRewardedAdConfiguration.mediationExtras) doReturn emptyBundle
  }

  @Test
  fun render_invalidSiteId_onFailure() {
    val parameterError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Site ID.", ERROR_DOMAIN)

    rewardedRenderer =
      YahooRewardedRenderer(
        mockMediationRewardedAdConfiguration,
        mockMediationAdLoadCallback,
        mockYahooFactory
      )
    rewardedRenderer.render()

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(parameterError)))
  }

  @Test
  fun render_invalidInit_onFailure() {
    val siteId = bundleOf("site_id" to "foobar")
    val contextError =
      AdError(
        ERROR_REQUIRES_ACTIVITY_CONTEXT,
        "Yahoo Mobile SDK requires an Activity context to initialize.",
        ERROR_DOMAIN
      )
    whenever(mockMediationRewardedAdConfiguration.serverParameters) doReturn siteId

    rewardedRenderer =
      YahooRewardedRenderer(
        mockMediationRewardedAdConfiguration,
        mockMediationAdLoadCallback,
        mockYahooFactory
      )
    rewardedRenderer.render()

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(contextError)))
  }

  @Test
  fun render_invalidPlacementId_onFailure() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      val siteId = bundleOf("site_id" to "foobar")
      val parameterError =
        AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Placement ID.", ERROR_DOMAIN)
      whenever(mockMediationRewardedAdConfiguration.serverParameters) doReturn siteId

      rewardedRenderer =
        YahooRewardedRenderer(
          mockMediationRewardedAdConfiguration,
          mockMediationAdLoadCallback,
          mockYahooFactory
        )
      rewardedRenderer.render()

      verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(parameterError)))
    }
  }

  @Test
  fun render_validSetup_rewardedLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      whenever(mockYahooFactory.createInterstitialAd(any(), any(), any())) doReturn mockRewardedAd
      whenever(mockYahooFactory.createRewardedPlacementConfig(any(), any())) doReturn
        mockRewardedPlacementConfig
      val siteId = bundleOf("site_id" to "foobar", "placement_id" to "barfoo")
      val parameterError =
        AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Placement ID.", ERROR_DOMAIN)
      whenever(mockMediationRewardedAdConfiguration.serverParameters) doReturn siteId
      whenever(mockMediationRewardedAdConfiguration.context) doReturn activity

      rewardedRenderer =
        YahooRewardedRenderer(
          mockMediationRewardedAdConfiguration,
          mockMediationAdLoadCallback,
          mockYahooFactory
        )
      rewardedRenderer.render()

      // taggedForChildDirectedTreatment is called within YahooAdapterUtils.setCoppaValue
      verify(mockMediationRewardedAdConfiguration).taggedForChildDirectedTreatment()
      verify(mockYahooFactory)
        .createRewardedPlacementConfig(eq("barfoo"), eq(mockMediationRewardedAdConfiguration))
      verify(mockYahooFactory)
        .createInterstitialAd(eq(activity), eq("barfoo"), eq(rewardedRenderer))
      verify(mockRewardedAd).load(mockRewardedPlacementConfig)
      verify(mockMediationAdLoadCallback, never())
        .onFailure(argThat(AdErrorMatcher(parameterError)))
    }
  }
}
