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

import android.view.View
import androidx.annotation.VisibleForTesting
import com.adsurge.adn.ads.AdConfig
import com.adsurge.adn.ads.AdSurgeAd
import com.adsurge.adn.ads.AdSurgeAdError
import com.adsurge.adn.ads.AdSurgeAdSize
import com.adsurge.adn.ads.banner.BannerAdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration

/**
 * Used to load AdSurge banner ads and mediate callbacks between Google Mobile Ads SDK and AdSurge
 * SDK.
 */
class AdSurgeBannerAd
@VisibleForTesting
internal constructor(
  private val mediationBannerAdConfiguration: MediationBannerAdConfiguration,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val adUnitId: String,
  private val bidResponse: String,
  private val adSurgeAdSize: AdSurgeAdSize,
  private val adViewWrapper: AdSurgeBannerAdWrapper,
) : MediationBannerAd, BannerAdListener {

  @VisibleForTesting internal var bannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd() {
    AdSurgeUtils.applyPrivacyConfiguration(mediationBannerAdConfiguration)

    val adConfig = AdConfig.Builder().payload(bidResponse).adSize(adSurgeAdSize).build()

    adViewWrapper.setListener(this)
    adViewWrapper.loadAd(adConfig)
  }

  override fun getView(): View = adViewWrapper.getView()

  override fun onAdLoaded(adSurgeAd: AdSurgeAd) {
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdFailed(adSurgeAdError: AdSurgeAdError) {
    mediationAdLoadCallback.onFailure(AdSurgeUtils.createSdkError(adSurgeAdError))
  }

  override fun onAdImpression(adSurgeAd: AdSurgeAd) {
    bannerAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(adSurgeAd: AdSurgeAd) {
    bannerAdCallback?.reportAdClicked()
  }

  override fun onAdDismissed(adSurgeAd: AdSurgeAd) {
    bannerAdCallback?.onAdClosed()
  }

  override fun onAdShowFailed(adSurgeAd: AdSurgeAd, adSurgeAdError: AdSurgeAdError) {
    // Banner ads do not have a separate show failure in GMA SDK.
  }

  companion object {
    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
      mediationUtils: MediationUtilsWrapper = MediationUtilsWrapper(),
    ): Result<AdSurgeBannerAd> {
      val context = mediationBannerAdConfiguration.context
      val serverParameters = mediationBannerAdConfiguration.serverParameters
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

      val bidResponse = mediationBannerAdConfiguration.bidResponse
      if (bidResponse.isNullOrEmpty()) {
        val error =
          AdSurgeUtils.createAdapterError(
            AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS,
            "Missing or invalid bid response.",
          )
        mediationAdLoadCallback.onFailure(error)
        return Result.failure(IllegalArgumentException(error.message))
      }

      val requestedAdSize = mediationBannerAdConfiguration.adSize
      val potentials = listOf(AdSize.BANNER, AdSize.LEADERBOARD, AdSize.MEDIUM_RECTANGLE)
      val closestSize = mediationUtils.findClosestSize(context, requestedAdSize, potentials)

      val adSurgeAdSize =
        when (closestSize) {
          AdSize.BANNER -> AdSurgeAdSize.BANNER
          AdSize.LEADERBOARD -> AdSurgeAdSize.BANNER_TABLET
          AdSize.MEDIUM_RECTANGLE -> AdSurgeAdSize.MREC
          else -> {
            val error =
              AdSurgeUtils.createAdapterError(
                AdSurgeUtils.ERROR_BANNER_SIZE_MISMATCH,
                "AdSurge does not support the requested banner size: $requestedAdSize.",
              )
            mediationAdLoadCallback.onFailure(error)
            return Result.failure(IllegalArgumentException(error.message))
          }
        }

      val adViewWrapper = AdSurgeFactory.createAdView(context, adUnitId)
      return Result.success(
        AdSurgeBannerAd(
          mediationBannerAdConfiguration = mediationBannerAdConfiguration,
          mediationAdLoadCallback = mediationAdLoadCallback,
          adUnitId = adUnitId,
          bidResponse = bidResponse,
          adSurgeAdSize = adSurgeAdSize,
          adViewWrapper = adViewWrapper,
        )
      )
    }
  }
}
