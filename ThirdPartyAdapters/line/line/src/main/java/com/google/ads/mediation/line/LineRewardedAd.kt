// Copyright 2023 Google LLC
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

package com.google.ads.mediation.line

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.five_corp.ad.AdLoader
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterface
import com.five_corp.ad.FiveAdLoadListener
import com.five_corp.ad.FiveAdVideoReward
import com.five_corp.ad.FiveAdVideoRewardEventListener
import com.google.ads.mediation.line.LineExtras.Companion.KEY_ENABLE_AD_SOUND
import com.google.ads.mediation.line.LineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration

/**
 * Used to load Line rewarded ads and mediate callbacks between Google Mobile Ads SDK and FiveAd
 * SDK.
 */
class LineRewardedAd
private constructor(
  private val appId: String,
  private val slotId: String?,
  private val bidResponse: String,
  private val watermark: String,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  private val networkExtras: Bundle?,
) : MediationRewardedAd, FiveAdLoadListener, FiveAdVideoRewardEventListener {

  private var mediationRewardedAdCallback: MediationRewardedAdCallback? = null
  private lateinit var rewardedAd: FiveAdVideoReward

  fun loadAd(context: Context) {
    if (slotId.isNullOrEmpty()) {
      val adError =
        AdError(
          LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
          LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
          LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
        )
      mediationAdLoadCallback.onFailure(adError)
      return
    }
    LineInitializer.initialize(context, appId)
    rewardedAd = LineSdkFactory.delegate.createFiveVideoRewarded(context, slotId)
    rewardedAd.setLoadListener(this)
    if (networkExtras != null) {
      rewardedAd.enableSound(networkExtras.getBoolean(KEY_ENABLE_AD_SOUND, true))
    }
    rewardedAd.loadAdAsync()
  }

  fun loadRtbAd(context: Context) {
    val fiveAdConfig = LineInitializer.getFiveAdConfig(appId)
    val adLoader = AdLoader.forConfig(context, fiveAdConfig) ?: return
    val bidData = BidData(bidResponse, watermark)
    adLoader.loadRewardAd(
      bidData,
      object : AdLoader.LoadRewardAdCallback {
        override fun onLoad(fiveAdRewarded: FiveAdVideoReward) {
          rewardedAd = fiveAdRewarded
          if (networkExtras != null) {
            rewardedAd.enableSound(networkExtras.getBoolean(KEY_ENABLE_AD_SOUND, true))
          }
          mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(this@LineRewardedAd)
          rewardedAd.setEventListener(this@LineRewardedAd)
        }

        override fun onError(adErrorCode: FiveAdErrorCode) {
          val adError = AdError(adErrorCode.value, adErrorCode.name, SDK_ERROR_DOMAIN)
          mediationAdLoadCallback.onFailure(adError)
        }
      },
    )
  }

  override fun showAd(context: Context) {
    rewardedAd.showAd()
  }

  override fun onFiveAdLoad(ad: FiveAdInterface) {
    Log.d(TAG, "Finished loading Line Rewarded Ad for slotId: ${ad.slotId}")
    mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
    rewardedAd.setEventListener(this)
  }

  override fun onFiveAdLoadError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    val adError =
      AdError(
        errorCode.value,
        String.format(LineMediationAdapter.ERROR_MSG_AD_LOADING, errorCode.name),
        SDK_ERROR_DOMAIN,
      )
    Log.w(TAG, adError.message)
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onViewError(fiveAdVideoReward: FiveAdVideoReward, fiveAdErrorCode: FiveAdErrorCode) {
    val adError =
      AdError(
        fiveAdErrorCode.value,
        String.format(LineMediationAdapter.ERROR_MSG_AD_SHOWING, fiveAdErrorCode.name),
        SDK_ERROR_DOMAIN,
      )
    Log.w(TAG, adError.message)
    mediationRewardedAdCallback?.onAdFailedToShow(adError)
  }

  override fun onClick(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded ad did record a click.")
    mediationRewardedAdCallback?.reportAdClicked()
  }

  override fun onFullScreenClose(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded ad closed")
    mediationRewardedAdCallback?.onAdClosed()
  }

  override fun onPlay(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded ad played")
    mediationRewardedAdCallback?.onVideoStart()
  }

  override fun onPause(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded ad paused")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onReward(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded ad user earned reward")

    mediationRewardedAdCallback?.onUserEarnedReward()
  }

  override fun onViewThrough(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded video ad viewed")
    mediationRewardedAdCallback?.onVideoComplete()
  }

  override fun onImpression(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded ad recorded an impression.")
    mediationRewardedAdCallback?.reportAdImpression()
  }

  override fun onFullScreenOpen(fiveAdVideoReward: FiveAdVideoReward) {
    Log.d(TAG, "Line rewarded ad opened")
    mediationRewardedAdCallback?.onAdOpened()
  }

  companion object {
    private val TAG = LineRewardedAd::class.simpleName

    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<LineRewardedAd> {
      val serverParameters = mediationRewardedAdConfiguration.serverParameters
      val appId = serverParameters.getString(LineMediationAdapter.KEY_APP_ID)
      if (appId.isNullOrEmpty()) {
        val adError =
          AdError(
            LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
            LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val slotId = serverParameters.getString(LineMediationAdapter.KEY_SLOT_ID)
      val bidResponse = mediationRewardedAdConfiguration.bidResponse
      val watermark = mediationRewardedAdConfiguration.watermark

      return Result.success(
        LineRewardedAd(
          appId,
          slotId,
          bidResponse,
          watermark,
          mediationAdLoadCallback,
          mediationRewardedAdConfiguration.mediationExtras,
        )
      )
    }
  }
}
