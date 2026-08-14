// Copyright 2025 Google LLC
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

package com.google.ads.mediation.fyber

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.fyber.inneractive.sdk.external.MediaView
import com.fyber.inneractive.sdk.external.NativeAdContent
import com.fyber.inneractive.sdk.external.NativeAdUnitController
import com.fyber.inneractive.sdk.external.NativeAdVideoContentController
import com.fyber.inneractive.sdk.external.VideoContentListener
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdAssetNames
import com.google.common.truth.Truth.assertThat
import kotlin.use
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DTExchangeNativeAdMapperTest {
  // Subject of testing
  private lateinit var dtExchangeNativeAdMapper: DTExchangeNativeAdMapper

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val adConfiguration =
    createMediationNativeAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
  val mockContent =
    mock<NativeAdContent> { on { appIcon } doReturn Uri.parse("https://www.TestURL.com") }

  private val mockFactory: Factory = mock()
  private val defaultFactory = FyberFactory.delegate
  private val mockNativeAdController: NativeAdUnitController = mock()
  private val mockNativeAdVideoController: NativeAdVideoContentController = mock()

  @Before
  fun setUp() {
    FyberFactory.delegate = mockFactory
    whenever(mockFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(mockFactory.createNativeAdVideoContentController()) doReturn
      mockNativeAdVideoController
    dtExchangeNativeAdMapper = DTExchangeNativeAdMapper(nativeAdLoadCallback)
  }

  @After
  fun tearDown() {
    FyberFactory.delegate = defaultFactory
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_withNullContent_invokesOnFailure() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdSpot = mock<InneractiveAdSpot> { on { isReady } doReturn false }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val expectedAdError =
        AdError(
          305,
          "DT Exchange failed to request ad with reason: SDK Internal Error",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())

      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), null)

      assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
      verify(mockAdSpot).destroy()
    }
  }

  @Test
  fun onInneractiveFailedAdRequest_withInvalidUnitController_invokesOnFailure() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn null
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val expectedAdError =
        AdError(
          305,
          "DT Exchange failed to request ad with reason: SDK Internal Error",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())

      requestListenerCaptor.firstValue.onInneractiveFailedAdRequest(
        mock(),
        InneractiveErrorCode.SDK_INTERNAL_ERROR,
      )

      assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
      verify(mockAdSpot).destroy()
    }
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_invokesOnSuccess() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())

      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      assertThat(nativeAdLoadCallback).hasSucceededWith(dtExchangeNativeAdMapper)
    }
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val eventListenerCaptor = argumentCaptor<InneractiveUnitController.EventsListener>()
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      verify(mockNativeAdController).setEventsListener(eventListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      eventListenerCaptor.firstValue.onAdImpression(mock())

      assertThat(nativeAdCallback.isOpened).isTrue()
      assertThat(nativeAdCallback.isImpressionReported).isTrue()
    }
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val eventListenerCaptor = argumentCaptor<InneractiveUnitController.EventsListener>()
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      verify(mockNativeAdController).setEventsListener(eventListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      eventListenerCaptor.firstValue.onAdClicked(mock())

      assertThat(nativeAdCallback.reportAdClickedInvokeCount).isEqualTo(1)
      assertThat(nativeAdCallback.onAdOpenedInvokeCount).isEqualTo(1)
    }
  }

  @Test
  fun onAdWillCloseInternalBrowser_throwsNoException() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val eventListenerCaptor = argumentCaptor<InneractiveUnitController.EventsListener>()
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      verify(mockNativeAdController).setEventsListener(eventListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      eventListenerCaptor.firstValue.onAdWillCloseInternalBrowser(mock())
    }
  }

  @Test
  fun onAdWillOpenExternalApp_invokesOnAdLeftApplication() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val eventListenerCaptor = argumentCaptor<InneractiveUnitController.EventsListener>()
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      verify(mockNativeAdController).setEventsListener(eventListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      eventListenerCaptor.firstValue.onAdWillOpenExternalApp(mock())

      assertThat(nativeAdCallback.isLeftApplication).isTrue()
    }
  }

  @Test
  fun onCompleted_invokesOnVideoComplete() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val videoContentListenerCaptor = argumentCaptor<VideoContentListener>()
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      verify(mockNativeAdVideoController).setEventsListener(videoContentListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      videoContentListenerCaptor.firstValue.onCompleted()

      assertThat(nativeAdCallback.isVideoCompleted).isTrue()
    }
  }

  @Test
  fun onProgress_throwsNoException() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val videoContentListenerCaptor = argumentCaptor<VideoContentListener>()
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      verify(mockNativeAdVideoController).setEventsListener(videoContentListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      videoContentListenerCaptor.firstValue.onProgress(
        /* totalDurationInMsec = */ 0,
        /* positionInMsec = */ 0,
      )
    }
  }

  @Test
  fun trackViews_setsRootAndMediaTagsAndMapsClickableAssetTags() {
    val mockMediaView = MediaView(context)
    val mockContentWithMediaView =
      mock<NativeAdContent> {
        on { appIcon } doReturn Uri.parse("https://www.TestURL.com")
        on { mediaView } doReturn mockMediaView
      }
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(
        mock(),
        mockContentWithMediaView,
      )

      val containerView = FrameLayout(context)
      val ctaView = View(context)
      val headlineView = View(context)
      val bodyView = View(context)
      val iconView = View(context)
      val starRatingView = View(context)
      val unmappedView = View(context)
      val clickableAssetViews =
        mapOf(
          NativeAdAssetNames.ASSET_CALL_TO_ACTION to ctaView,
          NativeAdAssetNames.ASSET_HEADLINE to headlineView,
          NativeAdAssetNames.ASSET_BODY to bodyView,
          NativeAdAssetNames.ASSET_ICON to iconView,
          NativeAdAssetNames.ASSET_STAR_RATING to starRatingView,
          "unmapped_asset" to unmappedView,
        )

      dtExchangeNativeAdMapper.trackViews(containerView, clickableAssetViews, emptyMap())

      assertThat(containerView.tag).isEqualTo(NativeAdContent.ViewTag.ROOT)
      assertThat(mockMediaView.tag).isEqualTo(NativeAdContent.ViewTag.MEDIA_VIEW)
      assertThat(ctaView.tag).isEqualTo(NativeAdContent.ViewTag.CTA)
      assertThat(headlineView.tag).isEqualTo(NativeAdContent.ViewTag.AD_TITLE)
      assertThat(bodyView.tag).isEqualTo(NativeAdContent.ViewTag.AD_DESCRIPTION)
      assertThat(iconView.tag).isEqualTo(NativeAdContent.ViewTag.AD_ICON)
      assertThat(starRatingView.tag).isEqualTo(NativeAdContent.ViewTag.RATING)
      assertThat(unmappedView.tag).isEqualTo(NativeAdContent.ViewTag.OTHER)
      verify(mockContentWithMediaView)
        .registerViewsForInteraction(
          eq(containerView),
          eq(mockMediaView),
          isNull(),
          eq(clickableAssetViews.values),
        )
    }
  }

  @Test
  fun mapNativeAd_withAllAssetsPopulated_mapsAssetsCorrectly() {
    val spiedMapper = spy(dtExchangeNativeAdMapper)
    val mockMediaView = mock<MediaView>()
    val testIconUri = Uri.parse("https://example.com/icon.png")
    val mockContentWithAllAssets =
      mock<NativeAdContent> {
        on { adTitle } doReturn "Test Headline"
        on { adDescription } doReturn "Test Body"
        on { adCallToAction } doReturn "Test CTA"
        on { rating } doReturn 4.5f
        on { mediaAspectRatio } doReturn 1.77f
        on { mediaView } doReturn mockMediaView
        on { appIcon } doReturn testIconUri
      }
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      spiedMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(
        mock(),
        mockContentWithAllAssets,
      )

      assertThat(spiedMapper.headline).isEqualTo("Test Headline")
      assertThat(spiedMapper.body).isEqualTo("Test Body")
      assertThat(spiedMapper.callToAction).isEqualTo("Test CTA")
      assertThat(spiedMapper.starRating).isEqualTo(4.5)
      assertThat(spiedMapper.mediaContentAspectRatio).isEqualTo(1.77f)
      verify(spiedMapper).setMediaView(mockMediaView)
      assertThat(spiedMapper.overrideClickHandling).isTrue()
      assertThat(spiedMapper.overrideImpressionRecording).isTrue()

      val icon = checkNotNull(spiedMapper.icon)
      assertThat(icon.scale).isEqualTo(1.0)
      assertThat(icon.drawable).isNull()
      assertThat(icon.uri).isEqualTo(testIconUri)
    }
  }

  @Test
  fun mapNativeAd_withNullOptionalAssets_doesNotThrow() {
    val spiedMapper = spy(dtExchangeNativeAdMapper)
    val mockContentWithNullAssets =
      mock<NativeAdContent> {
        on { adTitle } doReturn null
        on { adDescription } doReturn null
        on { adCallToAction } doReturn null
        on { rating } doReturn null
        on { mediaAspectRatio } doReturn null
        on { mediaView } doReturn null
        on { appIcon } doReturn null
      }
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      spiedMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(
        mock(),
        mockContentWithNullAssets,
      )

      assertThat(spiedMapper.headline).isNull()
      assertThat(spiedMapper.body).isNull()
      assertThat(spiedMapper.callToAction).isNull()
      assertThat(spiedMapper.starRating).isNull()
      assertThat(spiedMapper.icon).isNull()
      verify(spiedMapper, never()).setMediaView(any())
      assertThat(spiedMapper.overrideClickHandling).isTrue()
      assertThat(spiedMapper.overrideImpressionRecording).isTrue()
    }
  }

  @Test
  fun destroy_destroysAdSpotAndAdContentAndNullsReferences() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val requestListenerCaptor = argumentCaptor<InneractiveAdSpot.NativeAdRequestListener>()
      dtExchangeNativeAdMapper.loadAd(adConfiguration)
      verify(mockAdSpot).setRequestListener(requestListenerCaptor.capture())
      requestListenerCaptor.firstValue.onInneractiveSuccessfulNativeAdRequest(mock(), mockContent)

      dtExchangeNativeAdMapper.destroy()

      verify(mockAdSpot).destroy()
      verify(mockContent).destroy()
    }
  }
}
