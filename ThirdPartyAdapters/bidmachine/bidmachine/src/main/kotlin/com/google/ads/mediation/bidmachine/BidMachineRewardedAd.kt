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
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import io.bidmachine.AdPlacementConfig
import io.bidmachine.RendererConfiguration
import io.bidmachine.models.AuctionResult
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedListener
import io.bidmachine.rewarded.RewardedRequest
import io.bidmachine.utils.BMError

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
  adPlacementConfig: AdPlacementConfig
) : MediationRewardedAd, RewardedRequest.AdRequestListener, RewardedListener {
  @VisibleForTesting internal var rewardedRequestBuilder = RewardedRequest.Builder(adPlacementConfig)
  private lateinit var bidMachineRewardedAd: RewardedAd
  private var rewardedAdCallback: MediationRewardedAdCallback? = null

  fun loadWaterfallAd(rewardedAd: RewardedAd, context: Context) {
    val rewardedRequest =
      rewardedRequestBuilder.setListener(this).build()
    loadAd(rewardedAd, rewardedRequest, context)
  }

  fun loadRtbAd(rewardedAd: RewardedAd, context: Context) {
    val rewardedRequest =
      rewardedRequestBuilder.setBidPayload(bidResponse).setListener(this).build()
    loadAd(rewardedAd, rewardedRequest, context)
  }

  private fun loadAd(rewardedAd: RewardedAd, rewardedRequest: RewardedRequest, context: Context) {
    bidMachineRewardedAd = rewardedAd
    rewardedRequest.request(context)
  }

  override fun showAd(context: Context) {
    if (!bidMachineRewardedAd.canShow()) {
      val adError =
        AdError(
          ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD,
          ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD,
          SDK_ERROR_DOMAIN,
        )
      rewardedAdCallback?.onAdFailedToShow(adError)
      return
    }

    bidMachineRewardedAd.show()
  }

  override fun onRequestSuccess(rewardedRequest: RewardedRequest, auctionResult: AuctionResult) {
    if (rewardedRequest.isExpired) {
      val adError =
        AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
      mediationAdLoadCallback.onFailure(adError)
      rewardedRequest.destroy()
      return
    }
    bidMachineRewardedAd.setRendererConfiguration(
      RendererConfiguration(mapOf(WATERMARK_KEY to watermark))
    )
    bidMachineRewardedAd.setListener(this)
    bidMachineRewardedAd.load(rewardedRequest)
  }

  override fun onRequestFailed(rewardedRequest: RewardedRequest, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    rewardedRequest.destroy()
  }

  override fun onRequestExpired(rewardedRequest: RewardedRequest) {
    val adError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    rewardedRequest.destroy()
  }

  override fun onAdLoaded(rewardedAd: RewardedAd) {
    rewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(rewardedAd: RewardedAd, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    rewardedAd.destroy()
  }

  override fun onAdImpression(rewardedAd: RewardedAd) {
    rewardedAdCallback?.reportAdImpression()
    rewardedAdCallback?.onAdOpened()
  }

  override fun onAdShowFailed(rewardedAd: RewardedAd, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    rewardedAdCallback?.onAdFailedToShow(adError)
  }

  override fun onAdClicked(rewardedAd: RewardedAd) {
    rewardedAdCallback?.reportAdClicked()
  }

  override fun onAdExpired(rewardedAd: RewardedAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClosed(rewardedAd: RewardedAd, finished: Boolean) {
    rewardedAdCallback?.onAdClosed()
  }

  override fun onAdRewarded(rewardedAd: RewardedAd) {
    rewardedAdCallback?.onUserEarnedReward()
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<BidMachineRewardedAd> {
      val bidResponse = mediationRewardedAdConfiguration.bidResponse
      val watermark = mediationRewardedAdConfiguration.watermark
      val placementId =
        mediationRewardedAdConfiguration.serverParameters.getString(PLACEMENT_ID_KEY)
      val adPlacementConfig =
        AdPlacementConfig.rewardedBuilder().withPlacementId(placementId).build()

      return Result.success(
        BidMachineRewardedAd(mediationAdLoadCallback, bidResponse, watermark, adPlacementConfig)
      )
    }
  }
}
