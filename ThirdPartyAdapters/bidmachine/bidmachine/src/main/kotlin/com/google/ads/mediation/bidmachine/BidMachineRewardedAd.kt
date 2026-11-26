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
import androidx.annotation.VisibleForTesting
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.createPlacement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import io.bidmachine.Ad
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.rewardedad.RewardedAd
import io.bidmachine.format.rewardedad.RewardedListener
import io.bidmachine.model.Placement

/**
 * Used to load BidMachine rewarded ads and mediate callbacks between Google Mobile Ads SDK and
 * BidMachine SDK.
 */
class BidMachineRewardedAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  @get:VisibleForTesting internal val placement: Placement?,
) : MediationRewardedAd, AdLoadCallback<RewardedAd>, RewardedListener {
  private var bidMachineRewardedAd: RewardedAd? = null
  private var rewardedAdCallback: MediationRewardedAdCallback? = null

  fun loadWaterfallAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadRewarded(placement, this)
  }

  fun loadRtbAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadRewarded(bidResponse, this)
  }

  override fun showAd(context: Context) {
    val rewardedAd = bidMachineRewardedAd
    if (rewardedAd == null || !canShow(rewardedAd)) {
      val adError =
        AdError(
          ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD,
          ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD,
          SDK_ERROR_DOMAIN,
        )
      rewardedAdCallback?.onAdFailedToShow(adError)
      return
    }

    rewardedAd.show()
  }

  override fun onAdLoaded(ad: RewardedAd) {
    bidMachineRewardedAd = ad
    setWatermark(ad)
    ad.setListener(this)
    rewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(bmError: BMError) {
    val adError = AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onAdPresented(ad: RewardedAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdImpression(ad: RewardedAd) {
    rewardedAdCallback?.reportAdImpression()
    rewardedAdCallback?.onAdOpened()
  }

  override fun onAdShowFailed(ad: RewardedAd, bmError: BMError) {
    val adError = AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN)
    rewardedAdCallback?.onAdFailedToShow(adError)
  }

  override fun onAdClicked(ad: RewardedAd) {
    rewardedAdCallback?.reportAdClicked()
  }

  override fun onAdExpired(ad: RewardedAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClosed(ad: RewardedAd) {
    rewardedAdCallback?.onAdClosed()
  }

  override fun onAdRewarded(ad: RewardedAd) {
    rewardedAdCallback?.onUserEarnedReward()
  }

  private fun canShow(ad: Ad): Boolean = ad.canShow()

  private fun setWatermark(ad: Ad) {
    ad.setExtras(mapOf(WATERMARK_KEY to watermark))
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<BidMachineRewardedAd> {
      val bidResponse = mediationRewardedAdConfiguration.bidResponse
      val watermark = mediationRewardedAdConfiguration.watermark
      val placement =
        createPlacement(
          mediationRewardedAdConfiguration.serverParameters.getString(PLACEMENT_ID_KEY)
        )

      return Result.success(
        BidMachineRewardedAd(mediationAdLoadCallback, bidResponse, watermark, placement)
      )
    }
  }
}
