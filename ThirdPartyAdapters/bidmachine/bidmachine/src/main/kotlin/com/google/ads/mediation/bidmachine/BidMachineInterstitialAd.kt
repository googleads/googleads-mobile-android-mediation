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

package com.google.ads.mediation.bidmachine

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.createPlacement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import io.bidmachine.Ad
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.interstitialad.InterstitialAd
import io.bidmachine.format.interstitialad.InterstitialListener
import io.bidmachine.model.Placement

/**
 * Used to load BidMachine interstitial ads and mediate callbacks between Google Mobile Ads SDK and
 * BidMachine SDK.
 */
class BidMachineInterstitialAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  @get:VisibleForTesting internal val placement: Placement?,
) : MediationInterstitialAd, AdLoadCallback<InterstitialAd>, InterstitialListener {
  private var bidMachineInterstitialAd: InterstitialAd? = null
  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadWaterfallAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadInterstitial(placement, this)
  }

  fun loadRtbAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadInterstitial(bidResponse, this)
  }

  override fun showAd(context: Context) {
    val interstitialAd = bidMachineInterstitialAd
    if (interstitialAd == null || !canShow(interstitialAd)) {
      val adError =
        AdError(
          ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD,
          ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD,
          SDK_ERROR_DOMAIN,
        )
      interstitialAdCallback?.onAdFailedToShow(adError)
      return
    }

    interstitialAd.show()
  }

  override fun onAdLoaded(ad: InterstitialAd) {
    bidMachineInterstitialAd = ad
    setWatermark(ad)
    ad.setListener(this)
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(bmError: BMError) {
    val adError = AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onAdPresented(ad: InterstitialAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdImpression(ad: InterstitialAd) {
    interstitialAdCallback?.reportAdImpression()
    interstitialAdCallback?.onAdOpened()
  }

  override fun onAdShowFailed(ad: InterstitialAd, bmError: BMError) {
    val adError = AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN)
    interstitialAdCallback?.onAdFailedToShow(adError)
  }

  override fun onAdClicked(ad: InterstitialAd) {
    interstitialAdCallback?.reportAdClicked()
  }

  override fun onAdExpired(ad: InterstitialAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClosed(ad: InterstitialAd) {
    interstitialAdCallback?.onAdClosed()
  }

  private fun canShow(ad: Ad): Boolean = ad.canShow()

  private fun setWatermark(ad: Ad) {
    ad.setExtras(mapOf(WATERMARK_KEY to watermark))
  }

  companion object {
    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<BidMachineInterstitialAd> {
      val bidResponse = mediationInterstitialAdConfiguration.bidResponse
      val watermark = mediationInterstitialAdConfiguration.watermark
      val placement =
        createPlacement(
          mediationInterstitialAdConfiguration.serverParameters.getString(PLACEMENT_ID_KEY)
        )

      return Result.success(
        BidMachineInterstitialAd(mediationAdLoadCallback, bidResponse, watermark, placement)
      )
    }
  }
}
