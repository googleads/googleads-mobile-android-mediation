package com.google.ads.mediation.yahoo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.MediationBannerListener
import com.yahoo.ads.YASAds
import com.yahoo.ads.inlineplacement.InlineAdView
import com.yahoo.ads.inlineplacement.InlinePlacementConfig
import java.lang.String
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

/** Tests for [YahooBannerRenderer]. */
@RunWith(AndroidJUnit4::class)
class YahooBannerRendererTest {

  private lateinit var adapter: YahooMediationAdapter
  private lateinit var bannerRenderer: YahooBannerRenderer

  private val mockBannerListener = mock<MediationBannerListener>()
  private val mockMediationAdRequest = mock<MediationAdRequest>()
  private val mockYahooAdSize = mock<com.yahoo.ads.inlineplacement.AdSize>()
  private val mockYahooFactory = mock<YahooFactory>()
  private val mockBannerAd = mock<InlineAdView>()
  private val mockInlinePlacementConfig = mock<InlinePlacementConfig>()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val emptyBundle = Bundle()

  @Before
  fun setUp() {
    adapter = YahooMediationAdapter()
    bannerRenderer = YahooBannerRenderer(adapter, mockYahooFactory)
  }

  @Test
  fun render_invalidSiteId_onAdFailedToLoad() {
    val parameterError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Site ID.", ERROR_DOMAIN)

    bannerRenderer.render(
      context,
      mockBannerListener,
      emptyBundle,
      AdSize.BANNER,
      mockMediationAdRequest,
      null
    )

    verify(mockBannerListener)
      .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(parameterError)))
  }

  @Test
  fun render_invalidInit_onAdFailedToLoad() {
    val siteId = bundleOf("site_id" to "foobar")
    val contextError =
      AdError(
        ERROR_REQUIRES_ACTIVITY_CONTEXT,
        "Yahoo Mobile SDK requires an Activity context to initialize.",
        ERROR_DOMAIN
      )

    bannerRenderer.render(
      context,
      mockBannerListener,
      siteId,
      AdSize.BANNER,
      mockMediationAdRequest,
      emptyBundle
    )

    verify(mockBannerListener).onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(contextError)))
  }

  @Test
  fun render_invalidPlacementId_onAdFailedToLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      val siteId = bundleOf("site_id" to "foobar")
      val parameterError =
        AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Placement ID.", ERROR_DOMAIN)

      bannerRenderer.render(
        activity,
        mockBannerListener,
        siteId,
        AdSize.BANNER,
        mockMediationAdRequest,
        null
      )

      verify(mockBannerListener)
        .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(parameterError)))
    }
  }

  @Test
  fun render_invalidAdSize_onAdFailedToLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      val siteId = bundleOf("site_id" to "foobar", "placement_id" to "barfoo")
      val message =
        String.format(
          "The requested banner size is not supported by Yahoo Mobile SDK: %s",
          AdSize.WIDE_SKYSCRAPER
        )
      val adSizeError = AdError(ERROR_BANNER_SIZE_MISMATCH, message, ERROR_DOMAIN)

      bannerRenderer.render(
        activity,
        mockBannerListener,
        siteId,
        AdSize.WIDE_SKYSCRAPER,
        mockMediationAdRequest,
        null
      )

      verify(mockBannerListener).onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(adSizeError)))
    }
  }

  @Test
  fun render_valid_bannerLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      whenever(mockYahooFactory.createInlineAd(any(), any(), any())) doReturn mockBannerAd
      whenever(mockYahooFactory.createInlinePlacementConfig(any(), any(), any())) doReturn
        mockInlinePlacementConfig
      whenever(mockYahooFactory.createYahooAdSize(eq(AdSize.BANNER))) doReturn mockYahooAdSize
      val siteId = bundleOf("site_id" to "foobar", "placement_id" to "barfoo")
      val message =
        String.format(
          "The requested banner size is not supported by Yahoo Mobile SDK: %s",
          AdSize.BANNER
        )
      val adSizeError = AdError(ERROR_BANNER_SIZE_MISMATCH, message, ERROR_DOMAIN)

      bannerRenderer.render(
        activity,
        mockBannerListener,
        siteId,
        AdSize.BANNER,
        mockMediationAdRequest,
        null
      )

      // taggedForChildDirectedTreatment is called within YahooAdapterUtils.setCoppaValue
      verify(mockMediationAdRequest).taggedForChildDirectedTreatment()
      verify(mockYahooFactory).createYahooAdSize(AdSize.BANNER)
      verify(mockYahooFactory)
        .createInlinePlacementConfig(eq("barfoo"), eq(mockMediationAdRequest), eq(mockYahooAdSize))
      verify(mockYahooFactory).createInlineAd(eq(activity), eq("barfoo"), eq(bannerRenderer))
      verify(mockBannerAd).load(mockInlinePlacementConfig)
      verify(mockBannerListener, never())
        .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(adSizeError)))
    }
  }
}
