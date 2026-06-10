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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class DTExchangeRtbInterstitialAdTest {
  // Subject of testing.
  private lateinit var dtExchangeRtbInterstitialAd: DTExchangeRtbInterstitialAd

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }
  private val adConfiguration =
    createMediationInterstitialAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)

  @Before
  fun setUp() {
    dtExchangeRtbInterstitialAd = DTExchangeRtbInterstitialAd(mockAdLoadCallback)
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_withAdSpotNotReady_invokesOnFailure() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdSpot = mock<InneractiveAdSpot> { on { isReady } doReturn false }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val expectedAdError =
        AdError(
          DTExchangeErrorCodes.ERROR_AD_NOT_READY,
          "DT Exchange's interstitial ad spot is not ready.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      dtExchangeRtbInterstitialAd.loadAd(adConfiguration)

      dtExchangeRtbInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
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
      dtExchangeRtbInterstitialAd.loadAd(adConfiguration)

      dtExchangeRtbInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      verify(mockAdLoadCallback).onSuccess(eq(dtExchangeRtbInterstitialAd))
    }
  }

  @Test
  fun onInneractiveFailedAdRequest_invokesOnFailure() {
    val mockAdSpot = mock<InneractiveAdSpot>()
    val iErrorCode = InneractiveErrorCode.LOAD_TIMEOUT
    val expectedAdError =
      AdError(
        307,
        "DT Exchange failed to request ad with reason: Failed Due To load timeout",
        DTExchangeErrorCodes.ERROR_DOMAIN,
      )

    dtExchangeRtbInterstitialAd.onInneractiveFailedAdRequest(mockAdSpot, iErrorCode)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockAdSpot).destroy()
  }

  @Test
  fun showAd_withInvalidUnitController_invokesOnFailure() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn null
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeRtbInterstitialAd.loadAd(adConfiguration)
      dtExchangeRtbInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbInterstitialAd.showAd(context)

      verify(mockInterstitialAdCallback).onAdOpened()
      verify(mockInterstitialAdCallback).onAdClosed()
      verify(mockAdSpot).destroy()
    }
  }

  @Test
  fun showAd_invokesShow() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockFullscreenController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeRtbInterstitialAd.loadAd(adConfiguration)
      dtExchangeRtbInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbInterstitialAd.showAd(context)

      verify(mockFullscreenController).show(context)
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
      dtExchangeRtbInterstitialAd.loadAd(adConfiguration)
      dtExchangeRtbInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbInterstitialAd.onAdImpression(mock())

      verify(mockInterstitialAdCallback).reportAdImpression()
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
      dtExchangeRtbInterstitialAd.loadAd(adConfiguration)
      dtExchangeRtbInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbInterstitialAd.onAdClicked(mock())

      verify(mockInterstitialAdCallback).reportAdClicked()
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
      dtExchangeRtbInterstitialAd.loadAd(adConfiguration)
      dtExchangeRtbInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbInterstitialAd.onAdWillOpenExternalApp(mock())

      verify(mockInterstitialAdCallback).onAdLeftApplication()
    }
  }

  @Test
  fun onAdEnteredErrorState_throwsNoException() {
    dtExchangeRtbInterstitialAd.onAdEnteredErrorState(mock(), mock())
  }

  @Test
  fun onAdWillCloseInternalBrowser_throwsNoException() {
    dtExchangeRtbInterstitialAd.onAdWillCloseInternalBrowser(mock())
  }
}
