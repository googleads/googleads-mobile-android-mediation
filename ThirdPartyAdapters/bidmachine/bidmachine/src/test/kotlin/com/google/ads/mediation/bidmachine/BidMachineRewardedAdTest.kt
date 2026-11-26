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
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.common.truth.Truth.assertThat
import io.bidmachine.Ad
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.rewardedad.RewardedAd
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BidMachineRewardedAdTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
  private val mockBidMachineInstance = mock<BidMachineInstance>()
  private val mockRewardedAd = mock<RewardedAd>()

  // Subject of testing.
  private val bidMachineRewardedAd: BidMachineRewardedAd = createRewardedAd()

  // region newInstance tests
  @Test
  fun newInstance_createsPlacementWithPlacementId() {
    assertThat(bidMachineRewardedAd.placement?.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun newInstance_withoutPlacementId_createsNoPlacement() {
    assertThat(createRewardedAd(serverParameters = bundleOf()).placement).isNull()
  }

  // endregion

  // region load tests
  @Test
  fun loadWaterfallAd_loadsRewardedWithPlacement() {
    bidMachineRewardedAd.loadWaterfallAd(mockBidMachineInstance)

    verify(mockBidMachineInstance)
      .loadRewarded(same(bidMachineRewardedAd.placement), same(bidMachineRewardedAd))
  }

  @Test
  fun loadRtbAd_loadsRewardedWithBidResponse() {
    bidMachineRewardedAd.loadRtbAd(mockBidMachineInstance)

    verify(mockBidMachineInstance).loadRewarded(eq(TEST_BID_RESPONSE), same(bidMachineRewardedAd))
  }

  // endregion

  // region AdLoadCallback tests
  @Test
  fun onAdLoaded_setsWatermarkAndListenerAndInvokesOnSuccess() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    verify(mockRewardedAd as Ad).setExtras(eq(mapOf(WATERMARK_KEY to TEST_WATERMARK)))
    verify(mockRewardedAd).setListener(same(bidMachineRewardedAd))
    assertThat(rewardedAdLoadCallback).hasSucceededWith(bidMachineRewardedAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bmError = createBMError()

    bidMachineRewardedAd.onAdLoadFailed(bmError)

    assertThat(rewardedAdLoadCallback)
      .hasFailedWith(AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN))
  }

  // endregion

  // region showAd tests
  @Test
  fun showAd_whenAdCanBeShown_showsRewarded() {
    whenever((mockRewardedAd as Ad).canShow()) doReturn true
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.showAd(context)

    verify(mockRewardedAd).show()
    assertThat(rewardedAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun showAd_whenAdCanNotBeShown_invokesOnAdFailedToShow() {
    whenever((mockRewardedAd as Ad).canShow()) doReturn false
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.showAd(context)

    verify(mockRewardedAd, never()).show()
    assertThat(rewardedAdCallback.adFailedToShowError?.code)
      .isEqualTo(ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD)
    assertThat(rewardedAdCallback.adFailedToShowError?.message)
      .isEqualTo(ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD)
  }

  @Test
  fun showAd_beforeLoad_doesNotThrow() {
    bidMachineRewardedAd.showAd(context)

    assertThat(rewardedAdCallback.isFailedToShow).isFalse()
  }

  // endregion

  // region RewardedListener tests
  @Test
  fun onAdImpression_invokesReportAdImpressionAndOnAdOpened() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdImpression(mockRewardedAd)

    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
    assertThat(rewardedAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdShowFailed_invokesOnAdFailedToShow() {
    val bmError = createBMError()
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdShowFailed(mockRewardedAd, bmError)

    val adError = rewardedAdCallback.adFailedToShowError
    assertThat(adError?.code).isEqualTo(bmError.code)
    assertThat(adError?.message).isEqualTo(bmError.message)
    assertThat(adError?.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdClicked(mockRewardedAd)

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdRewarded_invokesOnUserEarnedReward() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdRewarded(mockRewardedAd)

    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdClosed(mockRewardedAd)

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdPresentedAndExpired_doNotInvokeCallbacks() {
    bidMachineRewardedAd.onAdLoaded(mockRewardedAd)

    bidMachineRewardedAd.onAdPresented(mockRewardedAd)
    bidMachineRewardedAd.onAdExpired(mockRewardedAd)

    assertThat(rewardedAdCallback.isOpened).isFalse()
    assertThat(rewardedAdCallback.isClosed).isFalse()
    assertThat(rewardedAdCallback.isFailedToShow).isFalse()
  }

  // endregion

  private fun createRewardedAd(
    serverParameters: Bundle = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID)
  ): BidMachineRewardedAd {
    val adConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParameters,
        watermark = TEST_WATERMARK,
      )
    return BidMachineRewardedAd.newInstance(adConfiguration, rewardedAdLoadCallback).getOrThrow()
  }
}
