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

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_AD_FAILED_TO_DISPLAY
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_AD_NOT_READY
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_DOMAIN
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_WRONG_CONTROLLER_TYPE
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
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
import org.robolectric.Robolectric

/** Tests for [DTExchangeWaterfallInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class DTExchangeWaterfallInterstitialAdTest {

  private lateinit var waterfallInterstitialAd: DTExchangeWaterfallInterstitialAd

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }

  private lateinit var mockInneractiveAdManager: MockedStatic<InneractiveAdManager>

  private val serverParameters =
    bundleOf(
      FyberMediationAdapter.KEY_APP_ID to TEST_APP_ID,
      FyberMediationAdapter.KEY_SPOT_ID to TEST_SPOT_ID,
    )

  @Before
  fun setUp() {
    waterfallInterstitialAd = DTExchangeWaterfallInterstitialAd()
    mockInneractiveAdManager = mockStatic(InneractiveAdManager::class.java)
  }

  @After
  fun tearDown() {
    mockInneractiveAdManager.close()
  }

  // region MediationInterstitialAd implementation tests
  @Test
  fun showAd_withWrongControllerType_failsAndDestroysAdSpot() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot = mock<InneractiveAdSpot>()
    whenever(interstitialSpot.selectedUnitController)
      .thenReturn(mockFullscreenController) // For check in onInneractiveSuccessfulAdRequest
      .thenReturn(mockFullscreenController) // For cast in onInneractiveSuccessfulAdRequest
      .thenReturn(mock<InneractiveUnitController<*>>()) // For showAd
    whenever(interstitialSpot.isReady) doReturn true
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.showAd(context)

    val expectedAdError =
      AdError(
        ERROR_WRONG_CONTROLLER_TYPE,
        "showInterstitial called, but wrong spot has been used (should not happen).",
        ERROR_DOMAIN,
      )
    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    verify(interstitialSpot).destroy()
  }

  @Test
  fun showAd_whenAdNotReady_failsAndDestroysAdSpot() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> {
        on { selectedUnitController } doReturn mockFullscreenController
        on { isReady } doReturn false
      }
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.showAd(context)

    val expectedAdError =
      AdError(ERROR_AD_NOT_READY, "showInterstitial called, but the ad is not ready.", ERROR_DOMAIN)
    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    verify(interstitialSpot).destroy()
  }

  @Test
  fun showAd_succeeds() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> {
        on { selectedUnitController } doReturn mockFullscreenController
        on { isReady } doReturn true
      }
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.showAd(context)

    verify(mockFullscreenController).show(context)
  }

  // endregion

  // region InneractiveAdSpot.RequestListener implementation tests
  @Test
  fun onInneractiveSuccessfulAdRequest_withWrongControllerType_failsAndDestroysAdSpot() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val interstitialSpot =
        mock<InneractiveAdSpot> {
          on { selectedUnitController } doReturn mock<InneractiveUnitController<*>>()
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn interstitialSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val adConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )
      val listenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      waterfallInterstitialAd.loadAd(adConfiguration, mockAdLoadCallback)
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(eq(context), eq(TEST_APP_ID), listenerCaptor.capture())
      }
      listenerCaptor.firstValue.onFyberMarketplaceInitialized(FyberInitStatus.SUCCESSFULLY)

      waterfallInterstitialAd.onInneractiveSuccessfulAdRequest(interstitialSpot)

      verify(mockAdLoadCallback)
        .onFailure(
          argThat {
            this.code == ERROR_WRONG_CONTROLLER_TYPE &&
              this.domain == ERROR_DOMAIN &&
              this.message.startsWith("Unexpected controller type.")
          }
        )
      verify(interstitialSpot).destroy()
    }
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_succeeds() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
      val interstitialSpot =
        mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockFullscreenController }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn interstitialSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val adConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )
      val listenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      waterfallInterstitialAd.loadAd(adConfiguration, mockAdLoadCallback)
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(eq(context), eq(TEST_APP_ID), listenerCaptor.capture())
      }
      listenerCaptor.firstValue.onFyberMarketplaceInitialized(FyberInitStatus.SUCCESSFULLY)

      waterfallInterstitialAd.onInneractiveSuccessfulAdRequest(interstitialSpot)

      verify(mockFullscreenController).eventsListener = waterfallInterstitialAd
      verify(mockAdLoadCallback).onSuccess(waterfallInterstitialAd)
    }
  }

  @Test
  fun onInneractiveFailedAdRequest_invokesOnFailureAndDestroysAdSpot() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val interstitialSpot = mock<InneractiveAdSpot>()
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn interstitialSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val adConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )
      waterfallInterstitialAd.loadAd(adConfiguration, mockAdLoadCallback)
      val listenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(eq(context), eq(TEST_APP_ID), listenerCaptor.capture())
      }
      listenerCaptor.firstValue.onFyberMarketplaceInitialized(FyberInitStatus.SUCCESSFULLY)
      val errorCode = InneractiveErrorCode.LOAD_TIMEOUT

      waterfallInterstitialAd.onInneractiveFailedAdRequest(mock(), errorCode)

      val expectedAdError =
        AdError(
          307,
          "DT Exchange failed to request ad with reason: Failed Due To load timeout",
          ERROR_DOMAIN,
        )
      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(interstitialSpot).destroy()
    }
  }

  // endregion

  // region InneractiveFullscreenAdEventsListener implementation tests
  @Test
  fun onAdImpression_invokesCallbacks() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockFullscreenController }
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.onAdImpression(interstitialSpot)

    verify(mockInterstitialAdCallback).onAdOpened()
    verify(mockInterstitialAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_invokesCallback() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockFullscreenController }
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.onAdClicked(interstitialSpot)

    verify(mockInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAdWillOpenExternalApp_invokesCallback() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockFullscreenController }
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.onAdWillOpenExternalApp(interstitialSpot)

    verify(mockInterstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdEnteredErrorState_invokesCallbackAndDestroysAdSpot() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockFullscreenController }
    setupLoadedAd(interstitialSpot)
    val mockDisplayError =
      mock<InneractiveUnitController.AdDisplayError> {
        on { message } doReturn "Mock display error"
      }

    waterfallInterstitialAd.onAdEnteredErrorState(interstitialSpot, mockDisplayError)

    val expectedAdError = AdError(ERROR_AD_FAILED_TO_DISPLAY, "Mock display error", ERROR_DOMAIN)
    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    verify(interstitialSpot).destroy()
  }

  @Test
  fun onAdEnteredErrorState_withNullError_invokesCallbackWithEmptyMessageAndDestroysAdSpot() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockFullscreenController }
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.onAdEnteredErrorState(interstitialSpot, null)

    val expectedAdError = AdError(ERROR_AD_FAILED_TO_DISPLAY, "", ERROR_DOMAIN)
    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    verify(interstitialSpot).destroy()
  }

  @Test
  fun onAdDismissed_invokesCallbackAndDestroysAdSpot() {
    val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
    val interstitialSpot =
      mock<InneractiveAdSpot> { on { selectedUnitController } doReturn mockFullscreenController }
    setupLoadedAd(interstitialSpot)

    waterfallInterstitialAd.onAdDismissed(interstitialSpot)

    verify(mockInterstitialAdCallback).onAdClosed()
    verify(interstitialSpot).destroy()
  }

  // endregion

  private fun setupLoadedAd(interstitialSpot: InneractiveAdSpot) {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn interstitialSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager

      val adConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )
      val listenerCaptor = argumentCaptor<OnFyberMarketplaceInitializedListener>()
      waterfallInterstitialAd.loadAd(adConfiguration, mockAdLoadCallback)
      mockInneractiveAdManager.verify {
        InneractiveAdManager.initialize(eq(context), eq(TEST_APP_ID), listenerCaptor.capture())
      }
      listenerCaptor.firstValue.onFyberMarketplaceInitialized(FyberInitStatus.SUCCESSFULLY)

      waterfallInterstitialAd.onInneractiveSuccessfulAdRequest(interstitialSpot)
    }
  }

  private companion object {
    const val TEST_APP_ID = "testAppID"
    const val TEST_SPOT_ID = "testSpotId"
  }
}
