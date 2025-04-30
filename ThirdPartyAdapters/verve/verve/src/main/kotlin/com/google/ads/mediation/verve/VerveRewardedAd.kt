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

package com.google.ads.mediation.verve

import android.content.Context
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd

/**
 * Used to load Verve rewarded ads and mediate callbacks between Google Mobile Ads SDK and Verve
 * SDK.
 */
class VerveRewardedAd
private constructor(
  private val context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val bidResponse: String,
) : MediationRewardedAd, HyBidRewardedAd.Listener {
  private var hyBidRewardedAd: HyBidRewardedAd? = null
  private var rewardedAdCallback: MediationRewardedAdCallback? = null

  fun loadAd() {
    hyBidRewardedAd = VerveSdkFactory.delegate.createHyBidRewardedAd(context, this)
    hyBidRewardedAd?.prepareAd(bidResponse)
  }

  override fun showAd(context: Context) {
    if (hyBidRewardedAd == null) {
      val adError =
        AdError(
          ERROR_CODE_FULLSCREEN_AD_IS_NULL,
          ERROR_MSG_FULLSCREEN_AD_IS_NULL,
          ADAPTER_ERROR_DOMAIN,
        )
      rewardedAdCallback?.onAdFailedToShow(adError)
      return
    }
    hyBidRewardedAd?.show()
  }

  override fun onRewardedLoaded() {
    rewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onRewardedLoadFailed(error: Throwable?) {
    val errorMessage = if (error == null) "" else error.message.toString()
    val adError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "HyBid Error - Could not load rewarded ad: $errorMessage",
        SDK_ERROR_DOMAIN,
      )
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onRewardedOpened() {
    rewardedAdCallback?.apply {
      onAdOpened()
      reportAdImpression()
    }
  }

  override fun onRewardedClosed() {
    rewardedAdCallback?.onAdClosed()
  }

  override fun onRewardedClick() {
    rewardedAdCallback?.reportAdClicked()
  }

  override fun onReward() {
    rewardedAdCallback?.onUserEarnedReward()
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<VerveRewardedAd> {
      val context = mediationRewardedAdConfiguration.context
      val bidResponse = mediationRewardedAdConfiguration.bidResponse

      return Result.success(VerveRewardedAd(context, mediationAdLoadCallback, bidResponse))
    }
  }
}
