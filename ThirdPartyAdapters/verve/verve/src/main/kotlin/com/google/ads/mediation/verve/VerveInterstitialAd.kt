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
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd

/**
 * Used to load Verve interstitial ads and mediate callbacks between Google Mobile Ads SDK and Verve
 * SDK.
 */
class VerveInterstitialAd
private constructor(
  private val context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val bidResponse: String,
) : MediationInterstitialAd, HyBidInterstitialAd.Listener {
  private var hyBidInterstitialAd: HyBidInterstitialAd? = null
  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd() {
    hyBidInterstitialAd = VerveSdkFactory.delegate.createHyBidInterstitialAd(context, this)
    hyBidInterstitialAd?.prepareAd(bidResponse)
  }

  override fun showAd(context: Context) {
    if (hyBidInterstitialAd == null) {
      val adError =
        AdError(
          ERROR_CODE_FULLSCREEN_AD_IS_NULL,
          ERROR_MSG_FULLSCREEN_AD_IS_NULL,
          ADAPTER_ERROR_DOMAIN,
        )
      interstitialAdCallback?.onAdFailedToShow(adError)
      return
    }
    hyBidInterstitialAd?.show()
  }

  override fun onInterstitialLoaded() {
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onInterstitialLoadFailed(error: Throwable?) {
    val errorMessage = if (error == null) "" else error.message.toString()
    val adError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "Could not load interstitial ad Error: $errorMessage",
        SDK_ERROR_DOMAIN,
      )
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onInterstitialImpression() {
    interstitialAdCallback?.apply {
      onAdOpened()
      reportAdImpression()
    }
  }

  override fun onInterstitialDismissed() {
    interstitialAdCallback?.onAdClosed()
  }

  override fun onInterstitialClick() {
    interstitialAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  companion object {
    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<VerveInterstitialAd> {
      val context = mediationInterstitialAdConfiguration.context

      val bidResponse = mediationInterstitialAdConfiguration.bidResponse

      return Result.success(VerveInterstitialAd(context, mediationAdLoadCallback, bidResponse))
    }
  }
}
