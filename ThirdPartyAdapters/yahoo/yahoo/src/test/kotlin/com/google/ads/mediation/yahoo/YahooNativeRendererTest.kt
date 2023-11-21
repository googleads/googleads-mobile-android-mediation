package com.google.ads.mediation.yahoo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationNativeListener
import com.google.android.gms.ads.mediation.NativeMediationAdRequest
import com.google.common.truth.Truth.assertThat
import com.yahoo.ads.YASAds
import com.yahoo.ads.nativeplacement.NativeAd
import com.yahoo.ads.nativeplacement.NativePlacementConfig
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

/** Tests for [YahooNativeRenderer]. */
@RunWith(AndroidJUnit4::class)
class YahooNativeRendererTest {

  private lateinit var adapter: YahooMediationAdapter
  private lateinit var nativeRenderer: YahooNativeRenderer

  private val mockNativeListener = mock<MediationNativeListener>()
  private val mockMediationAdRequest = mock<NativeMediationAdRequest>()
  private val mockYahooFactory = mock<YahooFactory>()
  private val mockNativeAd = mock<NativeAd>()
  private val mockNativePlacementConfig = mock<NativePlacementConfig>()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val emptyBundle = Bundle()

  @Before
  fun setUp() {
    adapter = YahooMediationAdapter()
    nativeRenderer = YahooNativeRenderer(adapter, mockYahooFactory)
  }

  @Test
  fun render_invalidSiteId_onAdFailedToLoad() {
    val parameterError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Site ID.", ERROR_DOMAIN)

    nativeRenderer.render(context, mockNativeListener, emptyBundle, mockMediationAdRequest, null)

    verify(mockNativeListener)
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

    nativeRenderer.render(context, mockNativeListener, siteId, mockMediationAdRequest, emptyBundle)

    verify(mockNativeListener).onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(contextError)))
  }

  @Test
  fun render_invalidPlacementId_onAdFailedToLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      val siteId = bundleOf("site_id" to "foobar")
      val parameterError =
        AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Placement ID.", ERROR_DOMAIN)

      nativeRenderer.render(activity, mockNativeListener, siteId, mockMediationAdRequest, null)

      verify(mockNativeListener)
        .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(parameterError)))
    }
  }

  @Test
  fun render_validSetup_nativeLoad() {
    Mockito.mockStatic(YASAds::class.java).use {
      whenever(YASAds.isInitialized()) doReturn true
      whenever(mockYahooFactory.createNativeAd(any(), any(), any())) doReturn mockNativeAd
      whenever(mockYahooFactory.createNativePlacementConfig(any(), any(), any())) doReturn
        mockNativePlacementConfig
      val siteId = bundleOf("site_id" to "foobar", "placement_id" to "barfoo")
      val parameterError =
        AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Placement ID.", ERROR_DOMAIN)

      nativeRenderer.render(activity, mockNativeListener, siteId, mockMediationAdRequest, null)

      // taggedForChildDirectedTreatment is called within YahooAdapterUtils.setCoppaValue
      verify(mockMediationAdRequest).taggedForChildDirectedTreatment()
      verify(mockYahooFactory)
        .createNativePlacementConfig(
          eq("barfoo"),
          eq(mockMediationAdRequest),
          eq(arrayOf("100", "simpleImage"))
        )
      assertThat(mockNativePlacementConfig.skipAssets).isTrue()
      verify(mockYahooFactory).createNativeAd(eq(activity), eq("barfoo"), eq(nativeRenderer))
      verify(mockNativeAd).load(mockNativePlacementConfig)
      verify(mockNativeListener, never())
        .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(parameterError)))
    }
  }
}
