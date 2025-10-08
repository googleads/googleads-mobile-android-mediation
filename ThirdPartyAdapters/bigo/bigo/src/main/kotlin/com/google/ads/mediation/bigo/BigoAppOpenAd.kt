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
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import java.lang.IllegalArgumentException
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.SplashAd
import sg.bigo.ads.api.SplashAdInteractionListener

/**
 * Used to load Bigo app open ads and mediate callbacks between Google Mobile Ads SDK and Bigo SDK.
 */
class BigoAppOpenAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  private val bidResponse: String,
  private val slotId: String,
) : MediationAppOpenAd, AdLoadListener<SplashAd>, SplashAdInteractionListener {
  private var appOpenAdCallback: MediationAppOpenAdCallback? = null
  private var splashAd: SplashAd? = null

  fun loadAd(versionString: String) {
    val adRequest = BigoFactory.delegate.createSplashAdRequest(bidResponse, slotId)
    val splashAdLoader = BigoFactory.delegate.createSplashAdLoader()
    splashAdLoader.initializeAdLoader(loadListener = this, versionString)
    splashAdLoader.loadAd(adRequest)
  }

  override fun showAd(context: Context) {
    splashAd?.show()
  }

  override fun onError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(gmaAdError)
  }

  override fun onAdLoaded(splashAd: SplashAd) {
    splashAd.setAdInteractionListener(this)
    this.splashAd = splashAd
    appOpenAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    appOpenAdCallback?.onAdFailedToShow(gmaAdError)
  }

  override fun onAdImpression() {
    appOpenAdCallback?.reportAdImpression()
  }

  override fun onAdClicked() {
    appOpenAdCallback?.reportAdClicked()
  }

  override fun onAdOpened() {
    appOpenAdCallback?.onAdOpened()
  }

  override fun onAdClosed() {
    appOpenAdCallback?.onAdClosed()
  }

  override fun onAdSkipped() {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdFinished() {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  companion object {
    fun newInstance(
      mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
    ): Result<BigoAppOpenAd> {
      val serverParameters = mediationAppOpenAdConfiguration.serverParameters
      val bidResponse = mediationAppOpenAdConfiguration.bidResponse
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

      return Result.success(BigoAppOpenAd(mediationAdLoadCallback, bidResponse, slotId))
    }
  }
}
