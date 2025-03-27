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

import android.view.View
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.BannerAdShowListener
import com.moloco.sdk.publisher.CreateBannerCallback
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.MolocoAdError.AdCreateError

/**
 * Used to load Moloco banner ads and mediate callbacks between Google Mobile Ads SDK and Moloco
 * SDK.
 */
class MolocoBannerAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val adSize: AdSize,
  private val adUnitId: String,
  private val bidResponse: String,
  private val watermark: String,
) : MediationBannerAd, AdLoad.Listener, BannerAdShowListener {
  private lateinit var molocoAd: Banner
  private var bannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd() {
    val createBannerCallback =
      object : CreateBannerCallback {
        override fun invoke(banner: Banner?, molocoError: AdCreateError?) {
          if (molocoError != null) {
            val adError = AdError(molocoError.errorCode, molocoError.description, SDK_ERROR_DOMAIN)
            mediationAdLoadCallback.onFailure(adError)
            return
          }
          // Gracefully handle the scenario where ad object is null even if no error is reported.
          if (banner == null) {
            val adError =
              AdError(
                MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
                MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
                MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
              )
            mediationAdLoadCallback.onFailure(adError)
            return
          }
          molocoAd = banner
          molocoAd.adShowListener = this@MolocoBannerAd
          molocoAd.load(bidResponse, this@MolocoBannerAd)
        }
      }
    when (adSize) {
      AdSize.LEADERBOARD -> {
        Moloco.createBannerTablet(adUnitId, watermark, createBannerCallback)
      }
      AdSize.MEDIUM_RECTANGLE -> {
        Moloco.createMREC(adUnitId, watermark, createBannerCallback)
      }
      else -> {
        Moloco.createBanner(adUnitId, watermark, createBannerCallback)
      }
    }
  }

  override fun getView(): View = molocoAd

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
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdClicked(molocoAd: MolocoAd) {
    bannerAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  override fun onAdHidden(molocoAd: MolocoAd) {
    bannerAdCallback?.onAdClosed()
  }

  override fun onAdShowFailed(molocoAdError: MolocoAdError) {
    val adError =
      AdError(
        molocoAdError.errorType.errorCode,
        molocoAdError.errorType.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onAdShowSuccess(molocoAd: MolocoAd) {
    bannerAdCallback?.apply {
      onAdOpened()
      reportAdImpression()
    }
  }

  companion object {
    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
    ): Result<MolocoBannerAd> {
      val serverParameters = mediationBannerAdConfiguration.serverParameters
      val adSize = mediationBannerAdConfiguration.adSize

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

      val bidResponse = mediationBannerAdConfiguration.bidResponse
      val watermark = mediationBannerAdConfiguration.watermark

      return Result.success(
        MolocoBannerAd(mediationAdLoadCallback, adSize, adUnitId, bidResponse, watermark)
      )
    }
  }
}
