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
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class DTExchangeRtbInterstitialAdTest {
  // Subject of testing.
  private lateinit var dtExchangeRtbInterstitialAd: DTExchangeRtbInterstitialAd

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val adConfiguration =
    createMediationInterstitialAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)

  @Before
  fun setUp() {
    dtExchangeRtbInterstitialAd = DTExchangeRtbInterstitialAd(interstitialAdLoadCallback)
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

      assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
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

      assertThat(interstitialAdLoadCallback).hasSucceededWith(dtExchangeRtbInterstitialAd)
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

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
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

      assertThat(interstitialAdCallback.isOpened).isTrue()
      assertThat(interstitialAdCallback.isClosed).isTrue()
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

      assertThat(interstitialAdCallback.isImpressionReported).isTrue()
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

      assertThat(interstitialAdCallback.isClicked).isTrue()
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

      assertThat(interstitialAdCallback.isLeftApplication).isTrue()
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

  @Test
  fun onAdDismissed_invokesOnAdClosed() {
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

      dtExchangeRtbInterstitialAd.onAdDismissed(mock())

      assertThat(interstitialAdCallback.isClosed).isTrue()
    }
  }
}
