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
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.models.AuctionResult
import io.bidmachine.utils.BMError

/**
 * Used to load BidMachine interstitial ads and mediate callbacks between Google Mobile Ads SDK and
 * BidMachine SDK.
 */
class BidMachineInterstitialAd
private constructor(
  private val context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val bidResponse: String,
) : MediationInterstitialAd, InterstitialRequest.AdRequestListener, InterstitialListener {
  @VisibleForTesting internal var interstitialRequestBuilder = InterstitialRequest.Builder()
  private lateinit var bidMachineInterstitialAd: InterstitialAd
  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd(interstitialAd: InterstitialAd) {
    bidMachineInterstitialAd = interstitialAd
    val interstitialRequest =
      interstitialRequestBuilder.setBidPayload(bidResponse).setListener(this).build()
    interstitialRequest.request(context)
  }

  override fun showAd(context: Context) {
    if (!bidMachineInterstitialAd.canShow()) {
      val adError =
        AdError(
          ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD,
          ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD,
          SDK_ERROR_DOMAIN,
        )
      interstitialAdCallback?.onAdFailedToShow(adError)
      return
    }

    bidMachineInterstitialAd.show()
  }

  override fun onRequestSuccess(
    interstitialRequest: InterstitialRequest,
    auctionResult: AuctionResult,
  ) {
    if (interstitialRequest.isExpired) {
      val adError =
        AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
      mediationAdLoadCallback.onFailure(adError)
      interstitialRequest.destroy()
      return
    }
    bidMachineInterstitialAd.setListener(this)
    bidMachineInterstitialAd.load(interstitialRequest)
  }

  override fun onRequestFailed(interstitialRequest: InterstitialRequest, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    interstitialRequest.destroy()
  }

  override fun onRequestExpired(interstitialRequest: InterstitialRequest) {
    val adError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    interstitialRequest.destroy()
  }

  override fun onAdLoaded(interstitialAd: InterstitialAd) {
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(interstitialAd: InterstitialAd, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    interstitialAd.destroy()
  }

  override fun onAdImpression(interstitialAd: InterstitialAd) {
    interstitialAdCallback?.reportAdImpression()
    interstitialAdCallback?.onAdOpened()
  }

  override fun onAdShowFailed(interstitialAd: InterstitialAd, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    interstitialAdCallback?.onAdFailedToShow(adError)
  }

  override fun onAdClicked(interstitialAd: InterstitialAd) {
    interstitialAdCallback?.reportAdClicked()
  }

  override fun onAdExpired(interstitialAd: InterstitialAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClosed(interstitialAd: InterstitialAd, finished: Boolean) {
    interstitialAdCallback?.onAdClosed()
  }

  companion object {
    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<BidMachineInterstitialAd> {
      val context = mediationInterstitialAdConfiguration.context
      val bidResponse = mediationInterstitialAdConfiguration.bidResponse

      return Result.success(BidMachineInterstitialAd(context, mediationAdLoadCallback, bidResponse))
    }
  }
}
