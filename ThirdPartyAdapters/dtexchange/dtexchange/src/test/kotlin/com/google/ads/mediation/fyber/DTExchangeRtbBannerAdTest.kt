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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [DTExchangeRtbBannerAd]. */
@RunWith(AndroidJUnit4::class)
class DTExchangeRtbBannerAdTest {
  // Subject of testing.
  private lateinit var dtExchangeRtbBannerAd: DTExchangeRtbBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private val adConfiguration =
    createMediationBannerAdConfiguration(
      context = context,
      bidResponse = TEST_BID_RESPONSE,
      watermark = TEST_WATERMARK,
    )

  @Before
  fun setUp() {
    dtExchangeRtbBannerAd = DTExchangeRtbBannerAd(bannerAdLoadCallback)
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
          "DT Exchange's banner ad spot is not ready.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      dtExchangeRtbBannerAd.loadAd(adConfiguration)

      dtExchangeRtbBannerAd.onInneractiveSuccessfulAdRequest(mock())

      assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
      verify(mockAdSpot).destroy()
    }
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_withInvalidUnitController_invokesOnFailure() {
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
          DTExchangeErrorCodes.ERROR_WRONG_CONTROLLER_TYPE,
          "Unexpected controller type.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      dtExchangeRtbBannerAd.loadAd(adConfiguration)

      dtExchangeRtbBannerAd.onInneractiveSuccessfulAdRequest(mock())

      assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
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
      dtExchangeRtbBannerAd.loadAd(adConfiguration)

      dtExchangeRtbBannerAd.onInneractiveSuccessfulAdRequest(mock())
      val bannerView = dtExchangeRtbBannerAd.view

      verify(mockAdViewController).bindView(any<RelativeLayout>())
      assertThat(bannerAdLoadCallback).hasSucceededWith(dtExchangeRtbBannerAd)
      assertIs<RelativeLayout>(bannerView)
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

    dtExchangeRtbBannerAd.onInneractiveFailedAdRequest(mockAdSpot, iErrorCode)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockAdSpot).destroy()
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
      dtExchangeRtbBannerAd.loadAd(adConfiguration)
      dtExchangeRtbBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbBannerAd.onAdImpression(mock())

      assertThat(bannerAdCallback.isImpressionReported).isTrue()
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
      dtExchangeRtbBannerAd.loadAd(adConfiguration)
      dtExchangeRtbBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbBannerAd.onAdClicked(mock())

      assertThat(bannerAdCallback.isClicked).isTrue()
    }
  }

  @Test
  fun onAdWillCloseInternalBrowser_invokesOnAdClosed() {
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
      dtExchangeRtbBannerAd.loadAd(adConfiguration)
      dtExchangeRtbBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbBannerAd.onAdWillCloseInternalBrowser(mock())

      assertThat(bannerAdCallback.isClosed).isTrue()
    }
  }

  @Test
  fun onAdWillOpenExternalApp_invokesOnAdOpenedAndOnAdLeftApplication() {
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
      dtExchangeRtbBannerAd.loadAd(adConfiguration)
      dtExchangeRtbBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeRtbBannerAd.onAdWillOpenExternalApp(mock())

      assertThat(bannerAdCallback.isOpened).isTrue()
      assertThat(bannerAdCallback.isLeftApplication).isTrue()
    }
  }

  @Test
  fun onAdEnteredErrorState_throwsNoException() {
    dtExchangeRtbBannerAd.onAdEnteredErrorState(mock(), mock())
  }

  @Test
  fun onAdExpanded_throwsNoException() {
    dtExchangeRtbBannerAd.onAdExpanded(mock())
  }

  @Test
  fun onAdResized_throwsNoException() {
    dtExchangeRtbBannerAd.onAdResized(mock())
  }

  @Test
  fun onAdCollapsed_throwsNoException() {
    dtExchangeRtbBannerAd.onAdCollapsed(mock())
  }
}
