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
import com.adsurge.adn.ads.interstitial.IInterstitialAd
import com.adsurge.adn.ads.interstitial.InterstitialAdListener
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration

/**
 * Used to load AdSurge interstitial ads and mediate callbacks between Google Mobile Ads SDK and
 * AdSurge SDK.
 */
class AdSurgeInterstitialAd
@VisibleForTesting
internal constructor(
  private val mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val adUnitId: String,
  private val bidResponse: String,
  private val interstitialAd: IInterstitialAd,
) : MediationInterstitialAd, InterstitialAdListener {

  @VisibleForTesting internal var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd() {
    AdSurgeUtils.applyPrivacyConfiguration(mediationInterstitialAdConfiguration)

    val adConfig = AdConfig.Builder().payload(bidResponse).build()

    interstitialAd.loadAd(adConfig, this)
  }

  override fun showAd(context: Context) {
    if (context !is Activity) {
      val error =
        AdSurgeUtils.createAdapterError(
          AdSurgeUtils.ERROR_CONTEXT_NOT_ACTIVITY,
          "AdSurge interstitial ad presentation requires an Activity context.",
        )
      interstitialAdCallback?.onAdFailedToShow(error)
      return
    }

    if (!interstitialAd.isValid) {
      val error =
        AdSurgeUtils.createAdapterError(
          AdSurgeUtils.ERROR_AD_NOT_READY,
          "AdSurge interstitial ad is not ready to be presented.",
        )
      interstitialAdCallback?.onAdFailedToShow(error)
      return
    }

    interstitialAd.showAd(context)
  }

  override fun onAdLoaded(adSurgeAd: AdSurgeAd) {
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdFailed(adSurgeAdError: AdSurgeAdError) {
    mediationAdLoadCallback.onFailure(AdSurgeUtils.createSdkError(adSurgeAdError))
  }

  override fun onAdImpression(adSurgeAd: AdSurgeAd) {
    interstitialAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(adSurgeAd: AdSurgeAd) {
    interstitialAdCallback?.reportAdClicked()
  }

  override fun onAdDismissed(adSurgeAd: AdSurgeAd) {
    interstitialAdCallback?.onAdClosed()
  }

  override fun onAdShowFailed(adSurgeAd: AdSurgeAd, adSurgeAdError: AdSurgeAdError) {
    interstitialAdCallback?.onAdFailedToShow(AdSurgeUtils.createSdkError(adSurgeAdError))
  }

  companion object {
    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<AdSurgeInterstitialAd> {
      val context = mediationInterstitialAdConfiguration.context
      val serverParameters = mediationInterstitialAdConfiguration.serverParameters
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

      val bidResponse = mediationInterstitialAdConfiguration.bidResponse
      if (bidResponse.isNullOrEmpty()) {
        val error =
          AdSurgeUtils.createAdapterError(
            AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS,
            "Missing or invalid bid response.",
          )
        mediationAdLoadCallback.onFailure(error)
        return Result.failure(IllegalArgumentException(error.message))
      }

      val interstitialAd = AdSurgeFactory.createInterstitialAd(context, adUnitId)
      return Result.success(
        AdSurgeInterstitialAd(
          mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration,
          mediationAdLoadCallback = mediationAdLoadCallback,
          adUnitId = adUnitId,
          bidResponse = bidResponse,
          interstitialAd = interstitialAd,
        )
      )
    }
  }
}
