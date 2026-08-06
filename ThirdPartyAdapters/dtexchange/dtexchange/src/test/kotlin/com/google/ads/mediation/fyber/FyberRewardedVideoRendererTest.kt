// Copyright 2024 Google LLC
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
import android.app.Application
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveContentController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class FyberRewardedVideoRendererTest {

  // Subject of testing.
  private lateinit var fyberRewardedAd: FyberRewardedVideoRenderer

  private val activity = Robolectric.buildActivity(Activity::class.java).get()
  private val serverParameters =
    bundleOf(FyberMediationAdapter.KEY_SPOT_ID to AdapterTestKitConstants.TEST_AD_UNIT)

  private val mockRewardedAdCallback: MediationRewardedAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockRewardedAdCallback
    }
  private val mockRewardedAdSpot: InneractiveAdSpot = mock()
  private val mockUnitController: InneractiveFullscreenUnitController = mock()

  private val mockFactory: Factory = mock()
  private val defaultFactory = FyberFactory.delegate

  private val adConfiguration =
    createMediationRewardedAdConfiguration(context = activity, serverParameters = serverParameters)

  // region Setup
  @Before
  fun setUp() {
    FyberFactory.delegate = mockFactory
    whenever(mockFactory.createRewardedAdSpot()) doReturn mockRewardedAdSpot
    whenever(mockFactory.createInneractiveFullscreenUnitController()) doReturn mockUnitController
    fyberRewardedAd = FyberRewardedVideoRenderer(mockAdLoadCallback)
  }

  @After
  fun tearDown() {
    FyberFactory.delegate = defaultFactory
  }

  // endregion

  // region Ad Load Tests
  @Test
  fun onInneractiveSuccessfulAdRequest_invokesOnSuccess() {
    loadAndRenderAdSuccessfully()

    verify(mockAdLoadCallback).onSuccess(fyberRewardedAd)
  }

  @Test
  fun onInneractiveFailedAdRequest_invokesOnFailedToLoad() {
    val fyberErrorCodeMessage = InneractiveErrorCode.NO_FILL.toString()
    val expectedAdError =
      AdError(
        302,
        "DT Exchange failed to request ad with reason: $fyberErrorCodeMessage",
        DTExchangeErrorCodes.ERROR_DOMAIN,
      )

    fyberRewardedAd.onInneractiveFailedAdRequest(mockRewardedAdSpot, InneractiveErrorCode.NO_FILL)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadWaterfallAd_withValidSpotId_requestsFyberAd() {
    fyberRewardedAd.loadWaterfallAd(adConfiguration)

    verify(mockRewardedAdSpot).requestAd(any())
  }

  @Test
  fun loadWaterfallAd_whenInvalidSpotId_invokesOnAdFailedToLoad() {
    val invalidServerParameters = bundleOf(FyberMediationAdapter.KEY_SPOT_ID to "")
    val adConfiguration =
      createMediationRewardedAdConfiguration(
        context = activity,
        serverParameters = invalidServerParameters,
      )
    val invalidFyberRewardedAd = FyberRewardedVideoRenderer(mockAdLoadCallback)
    val expectedAdError =
      AdError(
        DTExchangeErrorCodes.ERROR_INVALID_SERVER_PARAMETERS,
        "Spot ID is null or empty.",
        DTExchangeErrorCodes.ERROR_DOMAIN,
      )

    invalidFyberRewardedAd.loadWaterfallAd(adConfiguration)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Show Ad Tests
  @Test
  fun showAd_invokesShowAd() {
    whenever(mockRewardedAdSpot.isReady) doReturn true

    loadAndRenderAdSuccessfully()
    fyberRewardedAd.showAd(activity)

    verify(mockUnitController).show(activity)
  }

  @Test
  fun showAd_whenInvalidContext_invokesFailedToShow() {
    loadAndRenderAdSuccessfully()
    val context: Application = mock()
    val expectedAdError =
      AdError(
        DTExchangeErrorCodes.ERROR_CONTEXT_NOT_ACTIVITY_INSTANCE,
        "Cannot show a rewarded ad without an activity context.",
        DTExchangeErrorCodes.ERROR_DOMAIN,
      )

    fyberRewardedAd.showAd(context)

    verify(mockRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun showAd_whenAdNotReady_invokesFailedToShow() {
    whenever(mockRewardedAdSpot.isReady) doReturn false
    val expectedAdError =
      AdError(
        DTExchangeErrorCodes.ERROR_AD_NOT_READY,
        "DT Exchange's rewarded spot is not ready.",
        DTExchangeErrorCodes.ERROR_DOMAIN,
      )

    loadAndRenderAdSuccessfully()
    fyberRewardedAd.showAd(activity)

    verify(mockRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun showAd_viaRtb_invokesShowAd() {
    whenever(mockRewardedAdSpot.isReady) doReturn true

    loadRtbAdSuccessfully()
    fyberRewardedAd.showAd(activity)

    verify(mockUnitController).show(activity)
  }

  // endregion

  // region Ad Event Tests
  @Test
  fun onAdImpression_invokesOnAdOpenedAndOnVideoStartAndReportAdImpression() {
    val mockVideoContentController: InneractiveFullscreenVideoContentController = mock()
    whenever(mockUnitController.selectedContentController) doReturn mockVideoContentController

    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdImpression(mockRewardedAdSpot)

    verify(mockRewardedAdCallback).onAdOpened()
    verify(mockRewardedAdCallback).onVideoStart()
    verify(mockRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onAdImpression_whenNonVideoDisplayAd_invokesOnAdOpenedAndReportImpressionWithoutVideoStart() {
    val mockDisplayContentController = mock<InneractiveContentController<*>>()
    whenever(mockUnitController.selectedContentController) doReturn mockDisplayContentController

    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdImpression(mockRewardedAdSpot)

    verify(mockRewardedAdCallback).onAdOpened()
    verify(mockRewardedAdCallback, never()).onVideoStart()
    verify(mockRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdClicked(mockRewardedAdSpot)

    verify(mockRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosed() {
    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdDismissed(mockRewardedAdSpot)

    verify(mockRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onAdRewarded_invokesOnUserEarnedRewardAndVideoCompleted() {
    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdRewarded(mockRewardedAdSpot)

    verify(mockRewardedAdCallback).onUserEarnedReward()
    verify(mockRewardedAdCallback).onVideoComplete()
  }

  @Test
  fun onAdWillOpenExternalApp_doesNotInvokeAnyAdEvents() {
    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdWillOpenExternalApp(mockRewardedAdSpot)

    verifyNoInteractions(mockRewardedAdCallback)
  }

  @Test
  fun onAdEnteredErrorState_doesNotInvokeAnyAdEvents() {
    val mockAdDisplayError: AdDisplayError = mock()

    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdEnteredErrorState(mockRewardedAdSpot, mockAdDisplayError)

    verifyNoInteractions(mockRewardedAdCallback)
  }

  @Test
  fun onAdWillCloseInternalBrowser_doesNotInvokeAnyAdEvents() {
    loadAndRenderAdSuccessfully()
    fyberRewardedAd.onAdWillCloseInternalBrowser(mockRewardedAdSpot)

    verifyNoInteractions(mockRewardedAdCallback)
  }

  // endregion

  // region Utility methods
  private fun loadAndRenderAdSuccessfully() {
    fyberRewardedAd.loadWaterfallAd(adConfiguration)
    fyberRewardedAd.onInneractiveSuccessfulAdRequest(mockRewardedAdSpot)
  }

  private fun loadRtbAdSuccessfully() {
    fyberRewardedAd.loadRtbAd(adConfiguration)
    fyberRewardedAd.onInneractiveSuccessfulAdRequest(mockRewardedAdSpot)
  }
  // endregion
}
