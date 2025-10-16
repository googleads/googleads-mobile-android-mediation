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

package com.google.ads.mediation.bigo

import android.content.Context
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_CODE_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_MSG_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import java.lang.IllegalArgumentException
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.InterstitialAd

/**
 * Used to load Bigo interstitial ads and mediate callbacks between Google Mobile Ads SDK and Bigo
 * SDK.
 */
class BigoInterstitialAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val bidResponse: String,
  private val slotId: String,
) : MediationInterstitialAd, AdLoadListener<InterstitialAd>, AdInteractionListener {

  private var interstitialAdCallback: MediationInterstitialAdCallback? = null
  private var interstitialAd: InterstitialAd? = null

  fun loadAd(versionString: String) {
    val adRequest = BigoFactory.delegate.createInterstitialAdRequest(bidResponse, slotId)
    val interstitialAdLoader = BigoFactory.delegate.createInterstitialAdLoader()
    interstitialAdLoader.initializeAdLoader(loadListener = this, versionString)
    interstitialAdLoader.loadAd(adRequest)
  }

  override fun showAd(context: Context) {
    interstitialAd?.show()
  }

  override fun onError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(gmaAdError)
  }

  override fun onAdLoaded(interstitialAd: InterstitialAd) {
    interstitialAd.setAdInteractionListener(this)
    this.interstitialAd = interstitialAd
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    interstitialAdCallback?.onAdFailedToShow(gmaAdError)
  }

  override fun onAdImpression() {
    interstitialAdCallback?.reportAdImpression()
  }

  override fun onAdClicked() {
    interstitialAdCallback?.reportAdClicked()
  }

  override fun onAdOpened() {
    interstitialAdCallback?.onAdOpened()
  }

  override fun onAdClosed() {
    interstitialAdCallback?.onAdClosed()
  }

  companion object {
    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<BigoInterstitialAd> {
      val serverParameters = mediationInterstitialAdConfiguration.serverParameters
      val bidResponse = mediationInterstitialAdConfiguration.bidResponse
      val slotId = serverParameters.getString(SLOT_ID_KEY)

      if (slotId.isNullOrEmpty()) {
        val gmaAdError =
          BigoUtils.getGmaAdError(
            ERROR_CODE_MISSING_SLOT_ID,
            ERROR_MSG_MISSING_SLOT_ID,
            ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(gmaAdError)
        return Result.failure(IllegalArgumentException(gmaAdError.toString()))
      }

      return Result.success(BigoInterstitialAd(mediationAdLoadCallback, bidResponse, slotId))
    }
  }
}
