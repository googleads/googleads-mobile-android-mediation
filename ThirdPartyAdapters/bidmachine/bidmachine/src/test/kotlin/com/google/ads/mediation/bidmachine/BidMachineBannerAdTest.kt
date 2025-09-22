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
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerSize
import io.bidmachine.banner.BannerView
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
class BidMachineBannerAdTest {
  // Subject of testing.
  private lateinit var bidMachineBannerAd: BidMachineBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockBannerAdCallback: MediationBannerAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }
  private val mockBannerRequest = mock<BannerRequest> { on { isExpired } doReturn false }
  private val mockBannerView = mock<BannerView>()

  @Before
  fun setUp() {
    val serverParams = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID)
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.BANNER,
        serverParameters = serverParams,
      )
    BidMachineBannerAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess {
      bidMachineBannerAd = it
    }
  }

  @Test
  fun loadWaterfallAd_OnBannerView_invokesBidMachineRequest() {
    val mockBannerRequestBuilder =
      mock<BannerRequest.Builder> {
        on { setSize(eq(BannerSize.Size_320x50)) } doReturn it
        on { setPlacementId(eq(TEST_PLACEMENT_ID)) } doReturn it
        on { setListener(any()) } doReturn it
        on { build() } doReturn mockBannerRequest
      }
    bidMachineBannerAd.bannerRequestBuilder = mockBannerRequestBuilder

    bidMachineBannerAd.loadWaterfallAd(mockBannerView)

    verify(mockBannerView).setListener(eq(bidMachineBannerAd))
    verify(mockBannerRequestBuilder).setSize(eq(BannerSize.Size_320x50))
    verify(mockBannerRequestBuilder).setPlacementId(eq(TEST_PLACEMENT_ID))
    verify(mockBannerRequestBuilder).setListener(eq(bidMachineBannerAd))
    verify(mockBannerRequest).request(eq(context))
  }

  @Test
  fun loadRtbAd_OnBannerView_invokesBidMachineRequest() {
    val mockBannerRequestBuilder =
      mock<BannerRequest.Builder> {
        on { setSize(eq(BannerSize.Size_320x50)) } doReturn it
        on { setBidPayload(eq(TEST_BID_RESPONSE)) } doReturn it
        on { setListener(any()) } doReturn it
        on { build() } doReturn mockBannerRequest
      }
    bidMachineBannerAd.bannerRequestBuilder = mockBannerRequestBuilder

    bidMachineBannerAd.loadRtbAd(mockBannerView)

    verify(mockBannerView).setListener(eq(bidMachineBannerAd))
    verify(mockBannerRequestBuilder).setSize(eq(BannerSize.Size_320x50))
    verify(mockBannerRequestBuilder).setBidPayload(eq(TEST_BID_RESPONSE))
    verify(mockBannerRequestBuilder).setListener(eq(bidMachineBannerAd))
    verify(mockBannerRequest).request(eq(context))
  }

  @Test
  fun getView_returnsBannerView() {
    bidMachineBannerAd.loadRtbAd(mockBannerView)

    val adView = bidMachineBannerAd.view

    assertThat(adView).isInstanceOf(BannerView::class.java)
  }

  @Test
  fun onRequestSuccess_invokesBannerViewLoad() {
    bidMachineBannerAd.loadRtbAd(mockBannerView)

    bidMachineBannerAd.onRequestSuccess(mockBannerRequest, mock())

    verify(mockBannerView).load(mockBannerRequest)
  }

  @Test
  fun onRequestSuccess_withExpiredBannerRequest_invokesOnFailure() {
    whenever(mockBannerRequest.isExpired) doReturn true
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    bidMachineBannerAd.loadRtbAd(mockBannerView)

    bidMachineBannerAd.onRequestSuccess(mockBannerRequest, mock())

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockBannerRequest).destroy()
    verify(mockBannerView, never()).load(mockBannerRequest)
  }

  @Test
  fun onRequestFailed_invokesOnFailure() {
    val bMError = BMError.BMServerNoFill
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)

    bidMachineBannerAd.onRequestFailed(mockBannerRequest, bMError)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockBannerRequest).destroy()
  }

  @Test
  fun onRequestExpired_invokesOnFailure() {
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)

    bidMachineBannerAd.onRequestExpired(mockBannerRequest)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockBannerRequest).destroy()
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    verify(mockAdLoadCallback).onSuccess(bidMachineBannerAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bMError = BMError.AlreadyShown
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineBannerAd.loadRtbAd(mockBannerView)

    bidMachineBannerAd.onAdLoadFailed(mockBannerView, bMError)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockBannerView).destroy()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    bidMachineBannerAd.onAdImpression(mockBannerView)

    verify(mockBannerAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_invokesOnAdOpenedOnAdLeftApplicationAndReportAdClicked() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    bidMachineBannerAd.onAdClicked(mockBannerView)

    verify(mockBannerAdCallback).onAdOpened()
    verify(mockBannerAdCallback).onAdLeftApplication()
    verify(mockBannerAdCallback).reportAdClicked()
  }

  @Test
  fun onAdShowFailed_throwsNoException() {
    bidMachineBannerAd.onAdShowFailed(mockBannerView, BMError.InternalUnknownError)
  }

  @Test
  fun onAdExpired_throwsNoException() {
    bidMachineBannerAd.onAdExpired(mockBannerView)
  }
}
