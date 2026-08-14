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

package com.google.ads.mediation.bidmachine

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import io.bidmachine.RendererConfiguration
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BidMachineInterstitialAdTest {
  // Subject of testing.
  private lateinit var bidMachineInterstitialAd: BidMachineInterstitialAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val mockInterstitialRequest =
    mock<InterstitialRequest> { on { isExpired } doReturn false }
  private val mockInterstitialAd = mock<InterstitialAd> { on { canShow() } doReturn true }

  @Before
  fun setUp() {
    val serverParams = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID)
    val adConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParams,
        watermark = TEST_WATERMARK,
      )
    BidMachineInterstitialAd.newInstance(adConfiguration, interstitialAdLoadCallback).onSuccess {
      bidMachineInterstitialAd = it
    }
  }

  @Test
  fun newInstance_correctlyCreatesAdPlacementConfig() {
    assertThat(bidMachineInterstitialAd.adPlacementConfig.placementId).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun loadWaterfallAd_invokesBidMachineRequest() {
    val mockInterstitialRequestBuilder =
      mock<InterstitialRequest.Builder> {
        on { setListener(any()) } doReturn it
        on { build() } doReturn mockInterstitialRequest
      }
    bidMachineInterstitialAd.interstitialRequestBuilder = mockInterstitialRequestBuilder

    bidMachineInterstitialAd.loadWaterfallAd(mockInterstitialAd, context)

    verify(mockInterstitialRequestBuilder, never()).setBidPayload(any())
    verify(mockInterstitialRequestBuilder).setListener(eq(bidMachineInterstitialAd))
    verify(mockInterstitialRequest).request(eq(context))
  }

  @Test
  fun loadRtbAd_invokesBidMachineRequest() {
    val mockInterstitialRequestBuilder =
      mock<InterstitialRequest.Builder> {
        on { setBidPayload(eq(TEST_BID_RESPONSE)) } doReturn it
        on { setListener(any()) } doReturn it
        on { build() } doReturn mockInterstitialRequest
      }
    bidMachineInterstitialAd.interstitialRequestBuilder = mockInterstitialRequestBuilder

    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd, context)

    verify(mockInterstitialRequestBuilder).setBidPayload(eq(TEST_BID_RESPONSE))
    verify(mockInterstitialRequestBuilder).setListener(eq(bidMachineInterstitialAd))
    verify(mockInterstitialRequest).request(eq(context))
  }

  @Test
  fun showAd_whenCanShowIsTrue_invokesShow() {
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd, context)

    bidMachineInterstitialAd.showAd(context)

    verify(mockInterstitialAd).show()
  }

  @Test
  fun showAd_whenCanShowIsFalse_invokesOnAdFailedToShowWithError105() {
    whenever(mockInterstitialAd.canShow()) doReturn false
    val expectedAdError =
      AdError(
        ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD,
        ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD,
        SDK_ERROR_DOMAIN,
      )
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd, context)
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.showAd(context)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
    verify(mockInterstitialAd, never()).show()
  }

  @Test
  fun onRequestSuccess_invokesLoad() {
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd, context)
    val rendererConfigCaptor = argumentCaptor<RendererConfiguration>()

    bidMachineInterstitialAd.onRequestSuccess(mockInterstitialRequest, mock())

    verify(mockInterstitialAd).setRendererConfiguration(rendererConfigCaptor.capture())
    assertThat(rendererConfigCaptor.firstValue.getWatermark()).isEqualTo(TEST_WATERMARK)
    verify(mockInterstitialAd).setListener(eq(bidMachineInterstitialAd))
    verify(mockInterstitialAd).load(mockInterstitialRequest)
  }

  @Test
  fun onRequestSuccess_withExpiredAdRequest_invokesOnFailure() {
    whenever(mockInterstitialRequest.isExpired) doReturn true
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd, context)

    bidMachineInterstitialAd.onRequestSuccess(mockInterstitialRequest, mock())

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockInterstitialRequest).destroy()
    verify(mockInterstitialAd, never()).load(mockInterstitialRequest)
  }

  @Test
  fun onRequestFailed_invokesOnFailure() {
    val bMError = BMError.BMServerNoFill
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)

    bidMachineInterstitialAd.onRequestFailed(mockInterstitialRequest, bMError)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockInterstitialRequest).destroy()
  }

  @Test
  fun onRequestExpired_invokesOnFailure() {
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)

    bidMachineInterstitialAd.onRequestExpired(mockInterstitialRequest)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockInterstitialRequest).destroy()
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    assertThat(interstitialAdLoadCallback).hasSucceededWith(bidMachineInterstitialAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bMError = BMError.AlreadyShown
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd, context)

    bidMachineInterstitialAd.onAdLoadFailed(mockInterstitialAd, bMError)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockInterstitialAd).destroy()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdImpression(mockInterstitialAd)

    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
    assertThat(interstitialAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdClicked(mockInterstitialAd)

    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdShowFailed_invokesOnAdShowFailed() {
    val bMError = BMError.InternalUnknownError
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdShowFailed(mockInterstitialAd, bMError)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdClosed(mockInterstitialAd, /* finished= */ true)

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdExpired_throwsNoException() {
    bidMachineInterstitialAd.onAdExpired(mockInterstitialAd)
  }
}
