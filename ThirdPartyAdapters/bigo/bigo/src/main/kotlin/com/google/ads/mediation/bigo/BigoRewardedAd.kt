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

package com.google.ads.mediation.bigo

import android.content.Context
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_CODE_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_MSG_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import java.lang.IllegalArgumentException
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.RewardAdInteractionListener
import sg.bigo.ads.api.RewardVideoAd

/**
 * Used to load Bigo rewarded ads and mediate callbacks between Google Mobile Ads SDK and Bigo SDK.
 */
class BigoRewardedAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val bidResponse: String,
  private val slotId: String,
  private val watermark: String,
) : MediationRewardedAd, AdLoadListener<RewardVideoAd>, RewardAdInteractionListener {
  private var rewardedAdCallback: MediationRewardedAdCallback? = null
  private var rewardVideoAd: RewardVideoAd? = null

  fun loadAd(versionString: String) {
    val adRequest = BigoFactory.delegate.createRewardVideoAdRequest(bidResponse, slotId, watermark)
    val rewardVideoAdLoader = BigoFactory.delegate.createRewardVideoAdLoader()
    rewardVideoAdLoader.initializeAdLoader(loadListener = this, versionString)
    rewardVideoAdLoader.loadAd(adRequest)
  }

  override fun showAd(context: Context) {
    rewardVideoAd?.show()
  }

  override fun onError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(gmaAdError)
  }

  override fun onAdLoaded(rewardVideoAd: RewardVideoAd) {
    rewardVideoAd.setAdInteractionListener(this)
    this.rewardVideoAd = rewardVideoAd
    rewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdRewarded() {
    rewardedAdCallback?.onUserEarnedReward()
  }

  override fun onAdError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    rewardedAdCallback?.onAdFailedToShow(gmaAdError)
  }

  override fun onAdImpression() {
    rewardedAdCallback?.reportAdImpression()
  }

  override fun onAdClicked() {
    rewardedAdCallback?.reportAdClicked()
  }

  override fun onAdOpened() {
    rewardedAdCallback?.onAdOpened()
  }

  override fun onAdClosed() {
    rewardedAdCallback?.onAdClosed()
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<BigoRewardedAd> {
      val serverParameters = mediationRewardedAdConfiguration.serverParameters
      val bidResponse = mediationRewardedAdConfiguration.bidResponse
      val slotId = serverParameters.getString(SLOT_ID_KEY)
      val watermark = mediationRewardedAdConfiguration.watermark

      if (slotId.isNullOrEmpty()) {
        val gmaAdError =
          BigoUtils.getGmaAdError(
            ERROR_CODE_MISSING_SLOT_ID,
            ERROR_MSG_MISSING_SLOT_ID,
            ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(gmaAdError)
        return Result.failure(IllegalArgumentException(gmaAdError.toString()))
      }

      return Result.success(BigoRewardedAd(mediationAdLoadCallback, bidResponse, slotId, watermark))
    }
  }
}
