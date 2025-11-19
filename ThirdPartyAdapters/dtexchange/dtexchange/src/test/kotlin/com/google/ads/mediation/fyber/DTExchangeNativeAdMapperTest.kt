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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.fyber.inneractive.sdk.external.NativeAdContent
import com.fyber.inneractive.sdk.external.NativeAdUnitController
import com.fyber.inneractive.sdk.external.NativeAdVideoContentController
import com.fyber.inneractive.sdk.external.VideoContentListener
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import kotlin.use
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DTExchangeNativeAdMapperTest {
  // Subject of testing
  private lateinit var dtExchangeNativeAdMapper: DTExchangeNativeAdMapper

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockNativeAdCallback: MediationNativeAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockNativeAdCallback
    }
  private val adConfiguration =
    createMediationNativeAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
  val mockContent =
    mock<NativeAdContent> { on { appIcon } doReturn Uri.parse("https://www.TestURL.com") }

  @Before
  fun setUp() {
    dtExchangeNativeAdMapper = DTExchangeNativeAdMapper(mockAdLoadCallback)
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_withNullContent_invokesOnFailure() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdController: NativeAdUnitController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn mock()
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

      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(mockAdSpot).destroy()
    }
    mockFactory.close()
  }

  @Test
  fun onInneractiveFailedAdRequest_withInvalidUnitController_invokesOnFailure() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdController: NativeAdUnitController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn mock()
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

      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(mockAdSpot).destroy()
    }
    mockFactory.close()
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_invokesOnSuccess() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdController: NativeAdUnitController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn mock()
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

      verify(mockAdLoadCallback).onSuccess(eq(dtExchangeNativeAdMapper))
    }
    mockFactory.close()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdController: NativeAdUnitController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn mock()
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

      verify(mockNativeAdCallback).reportAdImpression()
    }
    mockFactory.close()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdController: NativeAdUnitController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn mock()
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

      verify(mockNativeAdCallback).reportAdClicked()
      verify(mockNativeAdCallback).onAdOpened()
    }
    mockFactory.close()
  }

  @Test
  fun onAdWillCloseInternalBrowser_throwsNoException() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdController: NativeAdUnitController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn mock()
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
    mockFactory.close()
  }

  @Test
  fun onAdWillOpenExternalApp_invokesOnAdOpenedAndOnAdLeftApplication() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdController: NativeAdUnitController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mockNativeAdController
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn mock()
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

      verify(mockNativeAdCallback).onAdLeftApplication()
    }
    mockFactory.close()
  }

  @Test
  fun onCompleted_invokesOnVideoComplete() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdVideoController: NativeAdVideoContentController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mock()
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn
      mockNativeAdVideoController
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

      verify(mockNativeAdCallback).onVideoComplete()
    }
    mockFactory.close()
  }

  @Test
  fun onProgress_throwsNoException() {
    val mockFactory = mockStatic(FyberFactory::class.java)
    val mockNativeAdVideoController: NativeAdVideoContentController = mock()
    whenever(FyberFactory.createNativeAdUnitController()) doReturn mock()
    whenever(FyberFactory.createNativeAdVideoContentController()) doReturn
      mockNativeAdVideoController
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
    mockFactory.close()
  }
}
