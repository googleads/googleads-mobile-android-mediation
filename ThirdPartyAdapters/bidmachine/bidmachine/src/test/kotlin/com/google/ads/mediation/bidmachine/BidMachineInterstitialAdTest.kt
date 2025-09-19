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
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
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
  private val mockInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }
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
      )
    BidMachineInterstitialAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess {
      bidMachineInterstitialAd = it
    }
  }

  @Test
  fun loadWaterfallAd_invokesBidMachineRequest() {
    val mockInterstitialRequestBuilder =
      mock<InterstitialRequest.Builder> {
        on { setPlacementId(eq(TEST_PLACEMENT_ID)) } doReturn it
        on { setListener(any()) } doReturn it
        on { build() } doReturn mockInterstitialRequest
      }
    bidMachineInterstitialAd.interstitialRequestBuilder = mockInterstitialRequestBuilder

    bidMachineInterstitialAd.loadWaterfallAd(mockInterstitialAd)

    verify(mockInterstitialRequestBuilder).setPlacementId(eq(TEST_PLACEMENT_ID))
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

    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd)

    verify(mockInterstitialRequestBuilder).setBidPayload(eq(TEST_BID_RESPONSE))
    verify(mockInterstitialRequestBuilder).setListener(eq(bidMachineInterstitialAd))
    verify(mockInterstitialRequest).request(eq(context))
  }

  @Test
  fun showAd_invokesBidMachineShow() {
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd)

    bidMachineInterstitialAd.showAd(context)

    verify(mockInterstitialAd).show()
  }

  @Test
  fun onRequestSuccess_invokesBannerViewLoad() {
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd)

    bidMachineInterstitialAd.onRequestSuccess(mockInterstitialRequest, mock())

    verify(mockInterstitialAd).setListener(eq(bidMachineInterstitialAd))
    verify(mockInterstitialAd).load(mockInterstitialRequest)
  }

  @Test
  fun onRequestSuccess_withExpiredBannerRequest_invokesOnFailure() {
    whenever(mockInterstitialRequest.isExpired) doReturn true
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd)

    bidMachineInterstitialAd.onRequestSuccess(mockInterstitialRequest, mock())

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockInterstitialRequest).destroy()
    verify(mockInterstitialAd, never()).load(mockInterstitialRequest)
  }

  @Test
  fun onRequestFailed_invokesOnFailure() {
    val bMError = BMError.BMServerNoFill
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)

    bidMachineInterstitialAd.onRequestFailed(mockInterstitialRequest, bMError)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockInterstitialRequest).destroy()
  }

  @Test
  fun onRequestExpired_invokesOnFailure() {
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)

    bidMachineInterstitialAd.onRequestExpired(mockInterstitialRequest)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockInterstitialRequest).destroy()
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    verify(mockAdLoadCallback).onSuccess(bidMachineInterstitialAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bMError = BMError.AlreadyShown
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineInterstitialAd.loadRtbAd(mockInterstitialAd)

    bidMachineInterstitialAd.onAdLoadFailed(mockInterstitialAd, bMError)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockInterstitialAd).destroy()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdImpression(mockInterstitialAd)

    verify(mockInterstitialAdCallback).reportAdImpression()
    verify(mockInterstitialAdCallback).onAdOpened()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdClicked(mockInterstitialAd)

    verify(mockInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAdShowFailed_invokesOnAdShowFailed() {
    val bMError = BMError.InternalUnknownError
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdShowFailed(mockInterstitialAd, bMError)

    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdClosed(mockInterstitialAd, /* finished= */ true)

    verify(mockInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun onAdExpired_throwsNoException() {
    bidMachineInterstitialAd.onAdExpired(mockInterstitialAd)
  }
}
