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

package com.google.ads.mediation.adsurge

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.adsurge.adn.ads.AdConfig
import com.adsurge.adn.ads.AdSurgeAd
import com.adsurge.adn.ads.AdSurgeAdError
import com.adsurge.adn.ads.rewarded.IRewardedAd
import com.adsurge.adn.ads.rewarded.RewardItem as AdSurgeRewardItem
import com.adsurge.adn.ads.rewarded.RewardedAdListener
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.rewarded.RewardItem

/**
 * Used to load AdSurge rewarded ads and mediate callbacks between Google Mobile Ads SDK and AdSurge
 * SDK.
 */
class AdSurgeRewardedAd
@VisibleForTesting
internal constructor(
  private val mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val adUnitId: String,
  private val bidResponse: String,
  private val rewardedAd: IRewardedAd,
) : MediationRewardedAd, RewardedAdListener {

  @VisibleForTesting internal var rewardedAdCallback: MediationRewardedAdCallback? = null

  fun loadAd() {
    AdSurgeUtils.applyPrivacyConfiguration(mediationRewardedAdConfiguration)

    val adConfig = AdConfig.Builder().payload(bidResponse).build()

    rewardedAd.loadAd(adConfig, this)
  }

  override fun showAd(context: Context) {
    if (context !is Activity) {
      val error =
        AdSurgeUtils.createAdapterError(
          AdSurgeUtils.ERROR_CONTEXT_NOT_ACTIVITY,
          "AdSurge rewarded ad presentation requires an Activity context.",
        )
      rewardedAdCallback?.onAdFailedToShow(error)
      return
    }

    if (!rewardedAd.isValid) {
      val error =
        AdSurgeUtils.createAdapterError(
          AdSurgeUtils.ERROR_AD_NOT_READY,
          "AdSurge rewarded ad is not ready to be presented.",
        )
      rewardedAdCallback?.onAdFailedToShow(error)
      return
    }

    rewardedAd.showAd(context)
  }

  override fun onAdLoaded(adSurgeAd: AdSurgeAd) {
    rewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdFailed(adSurgeAdError: AdSurgeAdError) {
    mediationAdLoadCallback.onFailure(AdSurgeUtils.createSdkError(adSurgeAdError))
  }

  override fun onAdImpression(adSurgeAd: AdSurgeAd) {
    rewardedAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(adSurgeAd: AdSurgeAd) {
    rewardedAdCallback?.reportAdClicked()
  }

  override fun onAdDismissed(adSurgeAd: AdSurgeAd) {
    rewardedAdCallback?.onAdClosed()
  }

  override fun onAdShowFailed(adSurgeAd: AdSurgeAd, adSurgeAdError: AdSurgeAdError) {
    rewardedAdCallback?.onAdFailedToShow(AdSurgeUtils.createSdkError(adSurgeAdError))
  }

  override fun onUserEarnedReward(adSurgeAd: AdSurgeAd, rewardItem: AdSurgeRewardItem?) {
    val gmaRewardItem =
      object : RewardItem {
        override fun getType(): String = rewardItem?.type ?: ""

        override fun getAmount(): Int = rewardItem?.amount ?: 1
      }
    rewardedAdCallback?.onUserEarnedReward(gmaRewardItem)
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<AdSurgeRewardedAd> {
      val context = mediationRewardedAdConfiguration.context
      val serverParameters = mediationRewardedAdConfiguration.serverParameters
      val adUnitId = serverParameters.getString(AdSurgeUtils.KEY_AD_UNIT_ID)

      if (adUnitId.isNullOrEmpty()) {
        val error =
          AdSurgeUtils.createAdapterError(
            AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS,
            "Missing or invalid ad_unit_id.",
          )
        mediationAdLoadCallback.onFailure(error)
        return Result.failure(IllegalArgumentException(error.message))
      }

      val bidResponse = mediationRewardedAdConfiguration.bidResponse
      if (bidResponse.isNullOrEmpty()) {
        val error =
          AdSurgeUtils.createAdapterError(
            AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS,
            "Missing or invalid bid response.",
          )
        mediationAdLoadCallback.onFailure(error)
        return Result.failure(IllegalArgumentException(error.message))
      }

      val rewardedAd = AdSurgeFactory.createRewardedAd(context, adUnitId)
      return Result.success(
        AdSurgeRewardedAd(
          mediationRewardedAdConfiguration = mediationRewardedAdConfiguration,
          mediationAdLoadCallback = mediationAdLoadCallback,
          adUnitId = adUnitId,
          bidResponse = bidResponse,
          rewardedAd = rewardedAd,
        )
      )
    }
  }
}
