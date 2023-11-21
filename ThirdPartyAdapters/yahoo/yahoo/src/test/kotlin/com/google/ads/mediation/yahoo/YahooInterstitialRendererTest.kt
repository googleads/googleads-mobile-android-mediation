package com.google.ads.mediation.yahoo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.MediationInterstitialListener
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

/** Tests for [YahooInterstitialRenderer]. */
@RunWith(AndroidJUnit4::class)
class YahooInterstitialRendererTest {

  private lateinit var adapter: YahooMediationAdapter
  private lateinit var interstitalRenderer: YahooInterstitialRenderer

  private val mockIntersititalListener = mock<MediationInterstitialListener>()
  private val mockMediationAdRequest = mock<MediationAdRequest>()
  private val mockYahooFactory = mock<YahooFactory>()
  private val mockInterstitialAd = mock<InterstitialAd>()
  private val mockInterStitialPlacementConfig = mock<InterstitialPlacementConfig>()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val emptyBundle = Bundle()

  @Before
  fun setUp() {
    adapter = YahooMediationAdapter()
    interstitalRenderer = YahooInterstitialRenderer(adapter, mockYahooFactory)
  }

  @Test
  fun render_invalidSiteId_onAdFailedToLoad() {
    val parameterError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Site ID.", ERROR_DOMAIN)

    interstitalRenderer.render(
      context,
      mockIntersititalListener,
      mockMediationAdRequest,
      emptyBundle,
      null
    )

    verify(mockIntersititalListener)
      .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(parameterError)))
  }

  @Test
  fun render_invalidInit_onAdFailedToLoad() {
    val siteId = Bundle()
    siteId.putString("site_id", "foobar")
    val contextError =
      AdError(
        ERROR_REQUIRES_ACTIVITY_CONTEXT,
        "Yahoo Mobile SDK requires an Activity context to initialize.",
        ERROR_DOMAIN
      )

    interstitalRenderer.render(
      context,
      mockIntersititalListener,
      mockMediationAdRequest,
      siteId,
      emptyBundle
    )

    verify(mockIntersititalListener)
      .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(contextError)))
  }

  @Test
  fun render_invalidPlacementId_onAdFailedToLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      val siteId = Bundle()
      siteId.putString("site_id", "foobar")
      val parameterError =
        AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Placement ID.", ERROR_DOMAIN)

      interstitalRenderer.render(
        activity,
        mockIntersititalListener,
        mockMediationAdRequest,
        siteId,
        null
      )

      verify(mockIntersititalListener)
        .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(parameterError)))
    }
  }

  @Test
  fun render_validSetup_interstitialLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      whenever(mockYahooFactory.createInterstitialAd(any(), any(), any())) doReturn
        mockInterstitialAd
      whenever(mockYahooFactory.createInterstitialPlacementConfig(any(), any())) doReturn
        mockInterStitialPlacementConfig
      val siteId = Bundle()
      siteId.putString("site_id", "foobar")
      siteId.putString("placement_id", "barfoo")
      val parameterError =
        AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Placement ID.", ERROR_DOMAIN)

      interstitalRenderer.render(
        activity,
        mockIntersititalListener,
        mockMediationAdRequest,
        siteId,
        null
      )

      // taggedForChildDirectedTreatment is called within YahooAdapterUtils.setCoppaValue
      verify(mockMediationAdRequest).taggedForChildDirectedTreatment()
      verify(mockYahooFactory)
        .createInterstitialPlacementConfig(eq("barfoo"), eq(mockMediationAdRequest))
      verify(mockYahooFactory)
        .createInterstitialAd(eq(activity), eq("barfoo"), eq(interstitalRenderer))
      verify(mockInterstitialAd).load(mockInterStitialPlacementConfig)
      verify(mockIntersititalListener, never())
        .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(parameterError)))
    }
  }
}
