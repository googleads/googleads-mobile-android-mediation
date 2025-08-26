// Copyright 2024 Google LLC
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

package com.google.ads.mediation.moloco

import android.content.Context
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.MEDIATION_PLATFORM_NAME
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.RewardedInterstitialAd
import com.moloco.sdk.publisher.RewardedInterstitialAdShowListener

/**
 * Used to load Moloco rewarded ads and mediate callbacks between Google Mobile Ads SDK and Moloco
 * SDK.
 */
class MolocoRewardedAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val adUnitId: String,
  private val bidResponse: String,
  private val watermark: String,
) : MediationRewardedAd, AdLoad.Listener, RewardedInterstitialAdShowListener {

  private lateinit var molocoAd: RewardedInterstitialAd
  private var rewardedAdCallback: MediationRewardedAdCallback? = null

  fun loadAd() {
    val mediationInfo = MediationInfo(MEDIATION_PLATFORM_NAME)
    Moloco.createRewardedInterstitial(
      mediationInfo = mediationInfo,
      adUnitId = adUnitId,
      watermarkString = watermark,
    ) { returnedAd, molocoError ->
      if (molocoError != null) {
        val adError = AdError(molocoError.errorCode, molocoError.description, SDK_ERROR_DOMAIN)
        mediationAdLoadCallback.onFailure(adError)
        return@createRewardedInterstitial
      }
      // Gracefully handle the scenario where ad object is null even if no error is reported.
      if (returnedAd == null) {
        val adError =
          AdError(
            MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
            MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
            MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(adError)
        return@createRewardedInterstitial
      }
      molocoAd = returnedAd
      molocoAd.load(bidResponse, this)
    }
  }

  override fun showAd(context: Context) {
    molocoAd.show(this)
  }

  override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
    val adError =
      AdError(
        molocoAdError.errorType.errorCode,
        molocoAdError.errorType.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onAdLoadSuccess(molocoAd: MolocoAd) {
    rewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdClicked(molocoAd: MolocoAd) {
    rewardedAdCallback?.reportAdClicked()
  }

  override fun onAdHidden(molocoAd: MolocoAd) {
    rewardedAdCallback?.onAdClosed()
  }

  override fun onAdShowFailed(molocoAdError: MolocoAdError) {
    val adError =
      AdError(
        molocoAdError.errorType.errorCode,
        molocoAdError.errorType.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    rewardedAdCallback?.onAdFailedToShow(adError)
  }

  override fun onAdShowSuccess(molocoAd: MolocoAd) {
    rewardedAdCallback?.apply {
      onAdOpened()
      reportAdImpression()
    }
  }

  override fun onRewardedVideoCompleted(molocoAd: MolocoAd) {
    rewardedAdCallback?.onVideoComplete()
  }

  override fun onRewardedVideoStarted(molocoAd: MolocoAd) {
    rewardedAdCallback?.onVideoStart()
  }

  override fun onUserRewarded(molocoAd: MolocoAd) {
    rewardedAdCallback?.onUserEarnedReward()
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<MolocoRewardedAd> {
      val serverParameters = mediationRewardedAdConfiguration.serverParameters

      val adUnitId = serverParameters.getString(MolocoMediationAdapter.KEY_AD_UNIT_ID)
      if (adUnitId.isNullOrEmpty()) {
        val adError =
          AdError(
            MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
            MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
            MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val bidResponse = mediationRewardedAdConfiguration.bidResponse
      val watermark = mediationRewardedAdConfiguration.watermark

      return Result.success(
        MolocoRewardedAd(mediationAdLoadCallback, adUnitId, bidResponse, watermark)
      )
    }
  }
}
