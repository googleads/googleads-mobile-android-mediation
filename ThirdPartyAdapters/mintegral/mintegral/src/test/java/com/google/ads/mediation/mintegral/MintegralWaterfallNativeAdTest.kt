// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.mintegral

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallNativeAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdAssetNames
import com.google.common.truth.Truth.assertThat
import com.mbridge.msdk.nativex.view.MBMediaView
import com.mbridge.msdk.out.MBBidNativeHandler
import com.mbridge.msdk.out.MBNativeHandler
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [MintegralWaterfallNativeAd]. */
@RunWith(AndroidJUnit4::class)
class MintegralWaterfallNativeAdTest {

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockNativeAdCallback: MediationNativeAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockNativeAdCallback
    }

  private lateinit var mockBidNativeHandlerStatic: MockedStatic<MBBidNativeHandler>
  private lateinit var mockNativeHandlerConstruction: MockedConstruction<MBNativeHandler>
  private lateinit var waterfallNativeAd: MintegralWaterfallNativeAd

  @Before
  fun setUp() {
    mockBidNativeHandlerStatic = mockStatic(MBBidNativeHandler::class.java)
    whenever(MBBidNativeHandler.getNativeProperties(any(), any())) doReturn HashMap()

    mockNativeHandlerConstruction = mockConstruction(MBNativeHandler::class.java)

    val adConfig = createDefaultNativeAdConfiguration()
    waterfallNativeAd = MintegralWaterfallNativeAd(adConfig, mockAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockBidNativeHandlerStatic.close()
    mockNativeHandlerConstruction.close()
  }

  @Test
  fun loadAd_withNullAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(PLACEMENT_ID to TEST_PLACEMENT_ID)
    val config =
      createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallNativeAd.loadAd(config)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "", PLACEMENT_ID to TEST_PLACEMENT_ID)
    val config =
      createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallNativeAd.loadAd(config)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_withNullPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val config =
      createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallNativeAd.loadAd(config)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val config =
      createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallNativeAd.loadAd(config)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_withValidParameters_initializesMBNativeHandlerAndLoads() {
    val config = createDefaultNativeAdConfiguration()

    waterfallNativeAd.loadAd(config)

    assertThat(mockNativeHandlerConstruction.constructed()).hasSize(1)
    val mockHandler = mockNativeHandlerConstruction.constructed().first()
    verify(mockHandler).setAdListener(any())
    verify(mockHandler).load()
  }

  @Test
  fun trackViews_registersViewsWithMBNativeHandler() {
    val config = createDefaultNativeAdConfiguration()
    waterfallNativeAd.loadAd(config)

    val containerView = FrameLayout(context)
    val clickableAssetViews =
      mutableMapOf<String, View>(
        NativeAdAssetNames.ASSET_HEADLINE to View(context),
        NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW to View(context),
        "3012" to View(context),
      )

    waterfallNativeAd.trackViews(containerView, clickableAssetViews, mapOf())

    val mockHandler = mockNativeHandlerConstruction.constructed().first()
    verify(mockHandler).registerView(eq(containerView), any(), anyOrNull())
  }

  @Test
  fun trackViews_withMediaView_configuresMediaViewListener() {
    val config = createDefaultNativeAdConfiguration()
    waterfallNativeAd.loadAd(config)

    val containerView = FrameLayout(context)
    val mediaView = MediaView(context)
    val mbMediaView = mock<MBMediaView>()
    mediaView.addView(mbMediaView)

    val clickableAssetViews = mutableMapOf<String, View>("media_view" to mediaView)

    waterfallNativeAd.trackViews(containerView, clickableAssetViews, mapOf())

    verify(mbMediaView).setOnMediaViewListener(waterfallNativeAd)
  }

  @Test
  fun untrackView_unregistersViewsWithMBNativeHandler() {
    val config = createDefaultNativeAdConfiguration()
    waterfallNativeAd.loadAd(config)

    val containerView = FrameLayout(context)
    waterfallNativeAd.untrackView(containerView)

    val mockHandler = mockNativeHandlerConstruction.constructed().first()
    verify(mockHandler).unregisterView(eq(containerView), any(), anyOrNull())
  }

  @Test
  fun trackViews_beforeLoadAd_doesNotCrash() {
    val containerView = FrameLayout(context)
    waterfallNativeAd.trackViews(containerView, mapOf(), mapOf())
  }

  @Test
  fun untrackView_beforeLoadAd_doesNotCrash() {
    val containerView = FrameLayout(context)
    waterfallNativeAd.untrackView(containerView)
  }

  private fun createDefaultNativeAdConfiguration(): MediationNativeAdConfiguration {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    return createMediationNativeAdConfiguration(
      context = context,
      serverParameters = serverParameters,
    )
  }
}
