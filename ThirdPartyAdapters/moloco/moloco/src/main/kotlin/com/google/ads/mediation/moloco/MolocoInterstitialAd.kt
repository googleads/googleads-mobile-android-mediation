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
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.InterstitialAd
import com.moloco.sdk.publisher.InterstitialAdShowListener
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError

/**
 * Used to load Moloco interstitial ads and mediate callbacks between Google Mobile Ads SDK and
 * Moloco SDK.
 */
class MolocoInterstitialAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val adUnitId: String,
  private val bidResponse: String,
  private val watermark: String,
) : MediationInterstitialAd, AdLoad.Listener, InterstitialAdShowListener {

  private lateinit var molocoAd: InterstitialAd
  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd() {
    Moloco.createInterstitial(adUnitId, watermark) { returnedAd, molocoError ->
      if (molocoError != null) {
        val adError = AdError(molocoError.errorCode, molocoError.description, SDK_ERROR_DOMAIN)
        mediationAdLoadCallback.onFailure(adError)
        return@createInterstitial
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
        return@createInterstitial
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
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdClicked(molocoAd: MolocoAd) {
    interstitialAdCallback?.reportAdClicked()
  }

  override fun onAdHidden(molocoAd: MolocoAd) {
    interstitialAdCallback?.onAdClosed()
  }

  override fun onAdShowFailed(molocoAdError: MolocoAdError) {
    val adError =
      AdError(
        molocoAdError.errorType.errorCode,
        molocoAdError.errorType.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    interstitialAdCallback?.onAdFailedToShow(adError)
  }

  override fun onAdShowSuccess(molocoAd: MolocoAd) {
    interstitialAdCallback?.apply {
      onAdOpened()
      reportAdImpression()
    }
  }

  companion object {
    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<MolocoInterstitialAd> {
      val serverParameters = mediationInterstitialAdConfiguration.serverParameters

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

      val bidResponse = mediationInterstitialAdConfiguration.bidResponse
      val watermark = mediationInterstitialAdConfiguration.watermark

      return Result.success(
        MolocoInterstitialAd(mediationAdLoadCallback, adUnitId, bidResponse, watermark)
      )
    }
  }
}
