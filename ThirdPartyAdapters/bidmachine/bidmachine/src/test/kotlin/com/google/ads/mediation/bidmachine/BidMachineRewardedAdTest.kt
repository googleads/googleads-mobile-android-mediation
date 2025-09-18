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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedRequest
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
class BidMachineRewardedAdTest {
  // Subject of testing.
  private lateinit var bidMachineRewardedAd: BidMachineRewardedAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockRewardedAdCallback: MediationRewardedAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockRewardedAdCallback
    }
  private val mockRewardedRequest = mock<RewardedRequest> { on { isExpired } doReturn false }
  private val mockRewardedAd = mock<RewardedAd> { on { canShow() } doReturn true }

  @Before
  fun setUp() {
    val adConfiguration =
      createMediationRewardedAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
    BidMachineRewardedAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess {
      bidMachineRewardedAd = it
    }
  }

  @Test
  fun loadAd_invokesBidMachineRequest() {
    val mockRewardedRequestBuilder =
      mock<RewardedRequest.Builder> {
        on { setBidPayload(eq(TEST_BID_RESPONSE)) } doReturn it
        on { setListener(any()) } doReturn it
        on { build() } doReturn mockRewardedRequest
      }
    bidMachineRewardedAd.rewardedRequestBuilder = mockRewardedRequestBuilder

    bidMachineRewardedAd.loadAd(mockRewardedAd)

    verify(mockRewardedRequestBuilder).setBidPayload(eq(TEST_BID_RESPONSE))
    verify(mockRewardedRequestBuilder).setListener(eq(bidMachineRewardedAd))
    verify(mockRewardedRequest).request(eq(context))
  }

  @Test
  fun showAd_invokesBidMachineShow() {
    bidMachineRewardedAd.loadAd(mockRewardedAd)

    bidMachineRewardedAd.showAd(context)

    verify(mockRewardedAd).show()
  }

  @Test
  fun onRequestSuccess_invokesBannerViewLoad() {
    bidMachineRewardedAd.loadAd(mockRewardedAd)

    bidMachineRewardedAd.onRequestSuccess(mockRewardedRequest, mock())

    verify(mockRewardedAd).setListener(eq(bidMachineRewardedAd))
    verify(mockRewardedAd).load(mockRewardedRequest)
  }

  @Test
  fun onRequestSuccess_withExpiredBannerRequest_invokesOnFailure() {
    whenever(mockRewardedRequest.isExpired) doReturn true
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    bidMachineRewardedAd.loadAd(mockRewardedAd)

    bidMachineRewardedAd.onRequestSuccess(mockRewardedRequest, mock())

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockRewardedRequest).destroy()
    verify(mockRewardedAd, never()).load(mockRewardedRequest)
  }

  @Test
  fun onRequestFailed_invokesOnFailure() {
    val bMError = BMError.BMServerNoFill
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)

    bidMachineRewardedAd.onRequestFailed(mockRewardedRequest, bMError)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockRewardedRequest).destroy()
  }

  @Test
  fun onRequestExpired_invokesOnFailure() {
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)

    bidMachineRewardedAd.onRequestExpired(mockRewardedRequest)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockRewardedRequest).destroy()
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    verify(mockAdLoadCallback).onSuccess(bidMachineRewardedAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bMError = BMError.AlreadyShown
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineRewardedAd.loadAd(mockRewardedAd)

    bidMachineRewardedAd.onAdLoadFailed(mockRewardedAd, bMError)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockRewardedAd).destroy()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdImpression(mockRewardedAd)

    verify(mockRewardedAdCallback).reportAdImpression()
    verify(mockRewardedAdCallback).onAdOpened()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdClicked(mockRewardedAd)

    verify(mockRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onAdShowFailed_invokesOnAdShowFailed() {
    val bMError = BMError.InternalUnknownError
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdShowFailed(mockRewardedAd, bMError)

    verify(mockRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdClosed(mockRewardedAd, /* finished= */ true)

    verify(mockRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onAdExpired_throwsNoException() {
    bidMachineRewardedAd.onAdExpired(mockRewardedAd)
  }

  @Test
  fun onAdRewarded_invokesOnUserEarnedReward() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdRewarded(mockRewardedAd)

    verify(mockRewardedAdCallback).onUserEarnedReward()
  }
}
