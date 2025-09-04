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
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_NULL_REWARDED_AD
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_NULL_REWARDED_AD_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_AD_UNIT
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PUBLISHER_ID
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
import java.lang.NullPointerException

/**
 * Used to load PubMatic rewarded ads and mediate callbacks between Google Mobile Ads SDK and
 * PubMatic SDK.
 */
class PubMaticRewardedAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  private val pobRewardedAd: POBRewardedAd,
  private val isRtb: Boolean,
) : MediationRewardedAd, POBRewardedAd.POBRewardedAdListener() {

  private var mediationRewardedAdCallback: MediationRewardedAdCallback? = null

  fun loadAd() {
    pobRewardedAd.setListener(this)
    if (isRtb) {
      pobRewardedAd.addExtraInfo(KEY_POB_ADMOB_WATERMARK, watermark)
      pobRewardedAd.loadAd(bidResponse, POBBiddingHost.ADMOB)
      return
    }
    pobRewardedAd.loadAd()
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
      isRtb: Boolean,
    ): Result<PubMaticRewardedAd> {
      val context = mediationRewardedAdConfiguration.context
      val pobRewardedAd =
        if (isRtb) {
          pubMaticAdFactory.createPOBRewardedAd(context)
        } else {
          val serverParameters = mediationRewardedAdConfiguration.serverParameters
          val pubId = serverParameters.getString(KEY_PUBLISHER_ID)
          val profileId = serverParameters.getString(KEY_PROFILE_ID)?.toIntOrNull()
          val adUnit = serverParameters.getString(KEY_AD_UNIT)
          if (pubId.isNullOrEmpty()) {
            val adError =
              AdError(
                ERROR_MISSING_PUBLISHER_ID,
                ERROR_MISSING_PUBLISHER_ID_MSG,
                ADAPTER_ERROR_DOMAIN,
              )
            mediationAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          if (profileId == null) {
            val adError =
              AdError(
                ERROR_MISSING_OR_INVALID_PROFILE_ID,
                ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG,
                ADAPTER_ERROR_DOMAIN,
              )
            mediationAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          if (adUnit.isNullOrEmpty()) {
            val adError =
              AdError(ERROR_MISSING_AD_UNIT_ID, ERROR_MISSING_AD_UNIT_ID_MSG, ADAPTER_ERROR_DOMAIN)
            mediationAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          pubMaticAdFactory.createPOBRewardedAd(context, pubId, profileId, adUnit)
        }

      if (pobRewardedAd == null) {
        val adError = AdError(ERROR_NULL_REWARDED_AD, ERROR_NULL_REWARDED_AD_MSG, SDK_ERROR_DOMAIN)
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NullPointerException())
      }

      return Result.success(
        PubMaticRewardedAd(
          mediationAdLoadCallback,
          mediationRewardedAdConfiguration.bidResponse,
          mediationRewardedAdConfiguration.watermark,
          pobRewardedAd,
          isRtb,
        )
      )
    }
  }
}
