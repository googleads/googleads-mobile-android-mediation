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

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_CODE_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_MSG_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import sg.bigo.ads.ad.banner.BigoAdView
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.AdSize

/**
 * Used to load Bigo banner ads and mediate callbacks between Google Mobile Ads SDK and Bigo SDK.
 */
class BigoBannerAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val adSize: AdSize,
  private val bidResponse: String,
  private val slotId: String,
  private val adView: BigoAdView,
) : MediationBannerAd, AdLoadListener<BigoAdView>, AdInteractionListener {

  private var bannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd(versionString: String) {
    val adRequest = BigoFactory.delegate.createBannerAdRequest(bidResponse, slotId, adSize)
    adView.setAdLoadListener(this)
    adView.setAdInteractionListener(this)
    adView.loadAd(adRequest, versionString)
  }

  override fun getView(): View {
    return adView
  }

  override fun onError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(gmaAdError)
  }

  override fun onAdLoaded(bigoAdView: BigoAdView) {
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdError(adError: AdError) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdImpression() {
    bannerAdCallback?.reportAdImpression()
  }

  override fun onAdClicked() {
    bannerAdCallback?.reportAdClicked()
  }

  override fun onAdOpened() {
    bannerAdCallback?.onAdOpened()
  }

  override fun onAdClosed() {
    bannerAdCallback?.onAdClosed()
  }

  companion object {
    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
    ): Result<BigoBannerAd> {
      val context = mediationBannerAdConfiguration.context
      val serverParameters = mediationBannerAdConfiguration.serverParameters
      val adSize =
        BigoUtils.mapAdSizeToBigoBannerSize(context, mediationBannerAdConfiguration.adSize)
      val bidResponse = mediationBannerAdConfiguration.bidResponse
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

      val bigoAdView = BigoFactory.delegate.createBigoAdView(context)
      val width = ViewGroup.LayoutParams.MATCH_PARENT
      val height = ViewGroup.LayoutParams.WRAP_CONTENT
      val layoutParams = FrameLayout.LayoutParams(width, height)
      layoutParams.gravity = Gravity.CENTER
      bigoAdView.layoutParams = layoutParams

      return Result.success(
        BigoBannerAd(mediationAdLoadCallback, adSize, bidResponse, slotId, bigoAdView)
      )
    }
  }
}
