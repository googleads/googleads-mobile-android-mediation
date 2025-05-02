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

package com.google.ads.mediation.pubmatic

import android.content.Context
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_AD_NOT_READY
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.POBConstants.KEY_POB_ADMOB_WATERMARK
import com.pubmatic.sdk.openwrap.core.POBReward
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.rewardedad.POBRewardedAd

/**
 * Used to load PubMatic rewarded ads and mediate callbacks between Google Mobile Ads SDK and
 * PubMatic SDK.
 */
class PubMaticRewardedAd
private constructor(
  context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  pubMaticAdFactory: PubMaticAdFactory,
) : MediationRewardedAd, POBRewardedAd.POBRewardedAdListener() {

  /** PubMatic SDK's rewarded ad object. */
  private val pobRewardedAd: POBRewardedAd = pubMaticAdFactory.createPOBRewardedAd(context)

  private var mediationRewardedAdCallback: MediationRewardedAdCallback? = null

  fun loadAd() {
    pobRewardedAd.setListener(this)
    pobRewardedAd.addExtraInfo(KEY_POB_ADMOB_WATERMARK, watermark)
    pobRewardedAd.loadAd(bidResponse, POBBiddingHost.ADMOB)
  }

  override fun onAdReceived(pobRewardedAd: POBRewardedAd) {
    mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdFailedToLoad(pobRewardedAd: POBRewardedAd, pobError: POBError) {
    mediationAdLoadCallback.onFailure(
      AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    )
  }

  override fun showAd(context: Context) {
    if (pobRewardedAd.isReady) {
      pobRewardedAd.show()
    } else {
      mediationRewardedAdCallback?.onAdFailedToShow(
        AdError(ERROR_AD_NOT_READY, "Ad not ready", ADAPTER_ERROR_DOMAIN)
      )
    }
  }

  override fun onAdFailedToShow(pobRewardedAd: POBRewardedAd, pobError: POBError) {
    mediationRewardedAdCallback?.onAdFailedToShow(
      AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    )
  }

  override fun onAdImpression(pobRewardedAd: POBRewardedAd) {
    mediationRewardedAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(pobRewardedAd: POBRewardedAd) {
    mediationRewardedAdCallback?.reportAdClicked()
  }

  override fun onAdOpened(pobRewardedAd: POBRewardedAd) {
    mediationRewardedAdCallback?.onAdOpened()
  }

  override fun onAdClosed(pobRewardedAd: POBRewardedAd) {
    mediationRewardedAdCallback?.onAdClosed()
  }

  override fun onReceiveReward(pobRewardedAd: POBRewardedAd, pobReward: POBReward) {
    mediationRewardedAdCallback?.onUserEarnedReward()
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
      pubMaticAdFactory: PubMaticAdFactory,
    ) =
      Result.success(
        PubMaticRewardedAd(
          mediationRewardedAdConfiguration.context,
          mediationAdLoadCallback,
          mediationRewardedAdConfiguration.bidResponse,
          mediationRewardedAdConfiguration.watermark,
          pubMaticAdFactory,
        )
      )
  }
}
