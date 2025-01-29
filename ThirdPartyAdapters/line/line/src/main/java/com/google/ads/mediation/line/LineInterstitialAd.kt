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
import com.five_corp.ad.FiveAdInterstitial
import com.five_corp.ad.FiveAdInterstitialEventListener
import com.five_corp.ad.FiveAdLoadListener
import com.google.ads.mediation.line.LineExtras.Companion.KEY_ENABLE_AD_SOUND
import com.google.ads.mediation.line.LineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration

/**
 * Used to load Line interstitial ads and mediate callbacks between Google Mobile Ads SDK and Line
 * (aka FiveAd) SDK.
 */
class LineInterstitialAd
private constructor(
  private val context: Context,
  private val appId: String,
  private val slotId: String?,
  private val bidResponse: String,
  private val watermark: String,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val networkExtras: Bundle?,
) : MediationInterstitialAd, FiveAdLoadListener, FiveAdInterstitialEventListener {

  private var mediationInterstitialAdCallback: MediationInterstitialAdCallback? = null
  private lateinit var interstitialAd: FiveAdInterstitial

  fun loadAd() {
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
    interstitialAd = LineSdkFactory.delegate.createFiveAdInterstitial(context, slotId)
    interstitialAd.setLoadListener(this)
    if (networkExtras != null) {
      interstitialAd.enableSound(networkExtras.getBoolean(KEY_ENABLE_AD_SOUND, true))
    }
    interstitialAd.loadAdAsync()
  }

  fun loadRtbAd() {
    val fiveAdConfig = LineInitializer.getFiveAdConfig(appId)
    val adLoader = AdLoader.forConfig(context, fiveAdConfig) ?: return
    val bidData = BidData(bidResponse, watermark)
    adLoader.loadInterstitialAd(
      bidData,
      object : AdLoader.LoadInterstitialAdCallback {
        override fun onLoad(fiveAdInterstitial: FiveAdInterstitial) {
          interstitialAd = fiveAdInterstitial
          if (networkExtras != null) {
            interstitialAd.enableSound(networkExtras.getBoolean(KEY_ENABLE_AD_SOUND, true))
          }
          mediationInterstitialAdCallback =
            mediationAdLoadCallback.onSuccess(this@LineInterstitialAd)
          interstitialAd.setEventListener(this@LineInterstitialAd)
        }

        override fun onError(adErrorCode: FiveAdErrorCode) {
          val adError = AdError(adErrorCode.value, adErrorCode.name, SDK_ERROR_DOMAIN)
          mediationAdLoadCallback.onFailure(adError)
        }
      },
    )
  }

  override fun showAd(context: Context) {
    interstitialAd.showAd()
  }

  override fun onFiveAdLoad(ad: FiveAdInterface) {
    Log.d(TAG, "Finished loading Line Interstitial Ad for slotId: ${ad.slotId}")
    mediationInterstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
    interstitialAd.setEventListener(this)
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

  override fun onViewError(fiveAdInterstitial: FiveAdInterstitial, errorCode: FiveAdErrorCode) {
    val adError =
      AdError(
        errorCode.value,
        String.format(LineMediationAdapter.ERROR_MSG_AD_SHOWING, errorCode.name),
        SDK_ERROR_DOMAIN,
      )
    Log.w(TAG, adError.message)
    mediationInterstitialAdCallback?.onAdFailedToShow(adError)
  }

  override fun onClick(fiveAdInterstitial: FiveAdInterstitial) {
    Log.d(TAG, "Line interstitial ad did record a click.")
    mediationInterstitialAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  override fun onFullScreenClose(fiveAdInterstitial: FiveAdInterstitial) {
    Log.d(TAG, "Line interstitial ad closed")
    mediationInterstitialAdCallback?.onAdClosed()
  }

  override fun onPlay(fiveAdInterstitial: FiveAdInterstitial) {
    Log.d(TAG, "Line interstitial video ad played")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onPause(fiveAdInterstitial: FiveAdInterstitial) {
    Log.d(TAG, "Line interstitial video ad paused")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFullScreenOpen(fiveAdInterstitial: FiveAdInterstitial) {
    Log.d(TAG, "Line interstitial video ad opened")
    mediationInterstitialAdCallback?.onAdOpened()
  }

  override fun onViewThrough(fiveAdInterstitial: FiveAdInterstitial) {
    Log.d(TAG, "Line interstitial video ad viewed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onImpression(fiveAdInterstitial: FiveAdInterstitial) {
    Log.d(TAG, "Line interstitial ad recorded an impression.")
    mediationInterstitialAdCallback?.reportAdImpression()
  }

  companion object {
    private val TAG = LineInterstitialAd::class.simpleName

    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<LineInterstitialAd> {
      val serverParameters = mediationInterstitialAdConfiguration.serverParameters

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
      val bidResponse = mediationInterstitialAdConfiguration.bidResponse
      val watermark = mediationInterstitialAdConfiguration.watermark

      return Result.success(
        LineInterstitialAd(
          mediationInterstitialAdConfiguration.context,
          appId,
          slotId,
          bidResponse,
          watermark,
          mediationAdLoadCallback,
          mediationInterstitialAdConfiguration.mediationExtras,
        )
      )
    }
  }
}
