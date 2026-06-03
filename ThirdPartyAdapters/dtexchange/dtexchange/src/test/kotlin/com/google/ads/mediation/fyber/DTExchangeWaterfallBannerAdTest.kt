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

package com.google.ads.mediation.fyber

import android.content.Context
import android.widget.RelativeLayout
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.NativeAdUnitController
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MediationUtils
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import kotlin.test.assertIs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [DTExchangeWaterfallBannerAd]. */
@RunWith(AndroidJUnit4::class)
class DTExchangeWaterfallBannerAdTest {

  // Subject of testing.
  private lateinit var dtExchangeWaterfallBannerAd: DTExchangeWaterfallBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockBannerAdCallback: MediationBannerAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }
  private val adConfiguration =
    createMediationBannerAdConfiguration(
      context = context,
      serverParameters =
        bundleOf(
          FyberMediationAdapter.KEY_APP_ID to "test_app_id",
          FyberMediationAdapter.KEY_SPOT_ID to "test_spot_id",
        ),
    )
  private lateinit var mockInneractiveAdManager: MockedStatic<InneractiveAdManager>
  private lateinit var mockFyberFactory: MockedStatic<FyberFactory>
  private lateinit var mockMediationUtils: MockedStatic<MediationUtils>

  @Before
  fun setUp() {
    dtExchangeWaterfallBannerAd = DTExchangeWaterfallBannerAd(mockAdLoadCallback)
    mockInneractiveAdManager = mockStatic(InneractiveAdManager::class.java)
    mockFyberFactory = mockStatic(FyberFactory::class.java)
    mockMediationUtils = mockStatic(MediationUtils::class.java)
  }

  @After
  fun tearDown() {
    mockInneractiveAdManager.close()
    mockFyberFactory.close()
    mockMediationUtils.close()
  }

  // region InneractiveAdSpot.RequestListener implementation tests
  @Test
  fun onInneractiveSuccessfulAdRequest_withInvalidUnitController_invokesOnFailureAndDestroysAdSpot() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val bannerSpot =
        mock<InneractiveAdSpot> {
          // Return a unit controller that's invalid for banner ads.
          on { selectedUnitController } doReturn mock<NativeAdUnitController>()
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )

      dtExchangeWaterfallBannerAd.onInneractiveSuccessfulAdRequest(mock())

      val expectedAdError =
        AdError(
          DTExchangeErrorCodes.ERROR_WRONG_CONTROLLER_TYPE,
          /* message = */ "",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(bannerSpot).destroy()
    }
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_ifLoadedSizeIsCloseToRequestedSize_invokesOnSuccess() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockAdViewController }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )
      // Assume the loaded size is banner.
      whenever(MediationUtils.findClosestSize(any(), any(), any())) doReturn AdSize.BANNER

      dtExchangeWaterfallBannerAd.onInneractiveSuccessfulAdRequest(mock())

      verify(mockAdViewController).bindView(any<RelativeLayout>())
      verify(mockAdLoadCallback).onSuccess(eq(dtExchangeWaterfallBannerAd))
      assertIs<RelativeLayout>(dtExchangeWaterfallBannerAd.view)
    }
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_ifLoadedSizeIsNotCloseToRequestedSize_invokesFailureAndDestroysAdSpot() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockAdViewController }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )
      // Assume the loaded size is not close to requested size.
      whenever(MediationUtils.findClosestSize(any(), any(), any())) doReturn null

      dtExchangeWaterfallBannerAd.onInneractiveSuccessfulAdRequest(mock())

      val expectedAdError =
        AdError(
          DTExchangeErrorCodes.ERROR_BANNER_SIZE_MISMATCH,
          /* message = */ "",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(bannerSpot).destroy()
    }
  }

  @Test
  fun onInneractiveFailedAdRequest_invokesOnFailureAndDestroysAdSpot() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockAdViewController }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )
      val iErrorCode = InneractiveErrorCode.LOAD_TIMEOUT
      val expectedAdError =
        AdError(
          307,
          "DT Exchange failed to request ad with reason: Failed Due To load timeout",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )

      dtExchangeWaterfallBannerAd.onInneractiveFailedAdRequest(mock(), iErrorCode)

      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(bannerSpot).destroy()
    }
  }

  // endregion

  // region InneractiveAdViewEventsListener implementation tests
  @Test
  fun onAdImpression_invokesReportAdImpression() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )
      whenever(MediationUtils.findClosestSize(any(), any(), any())) doReturn AdSize.BANNER
      dtExchangeWaterfallBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeWaterfallBannerAd.onAdImpression(mock())

      verify(mockBannerAdCallback).reportAdImpression()
    }
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )
      whenever(MediationUtils.findClosestSize(any(), any(), any())) doReturn AdSize.BANNER
      dtExchangeWaterfallBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeWaterfallBannerAd.onAdClicked(mock())

      verify(mockBannerAdCallback).reportAdClicked()
      verify(mockBannerAdCallback).onAdOpened()
    }
  }

  @Test
  fun onAdWillCloseInternalBrowser_invokesOnAdClosed() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )
      whenever(MediationUtils.findClosestSize(any(), any(), any())) doReturn AdSize.BANNER
      dtExchangeWaterfallBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeWaterfallBannerAd.onAdWillCloseInternalBrowser(mock())

      verify(mockBannerAdCallback).onAdClosed()
    }
  }

  @Test
  fun onAdWillOpenExternalApp_invokesOnAdOpenedAndOnAdLeftApplication() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )
      whenever(MediationUtils.findClosestSize(any(), any(), any())) doReturn AdSize.BANNER
      dtExchangeWaterfallBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeWaterfallBannerAd.onAdWillOpenExternalApp(mock())

      verify(mockBannerAdCallback).onAdLeftApplication()
    }
  }

  @Test
  fun onAdEnteredErrorState_destroysBannerAdSpot() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val bannerSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn bannerSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeWaterfallBannerAd.loadAd(adConfiguration)
      val initListenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(any(), any(), initListenerCaptor.capture())
      }
      initListenerCaptor.firstValue.onFyberMarketplaceInitialized(
        OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
      )

      dtExchangeWaterfallBannerAd.onAdEnteredErrorState(mock(), mock())

      verify(bannerSpot).destroy()
    }
  }

  @Test
  fun onAdExpanded_throwsNoException() {
    dtExchangeWaterfallBannerAd.onAdExpanded(mock())
  }

  @Test
  fun onAdResized_throwsNoException() {
    dtExchangeWaterfallBannerAd.onAdResized(mock())
  }

  @Test
  fun onAdCollapsed_throwsNoException() {
    dtExchangeWaterfallBannerAd.onAdCollapsed(mock())
  }
  // endregion
}
