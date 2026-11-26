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
import android.view.View
import androidx.annotation.VisibleForTesting
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.createPlacement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.bannerad.BannerListener
import io.bidmachine.format.bannerad.BannerView
import io.bidmachine.model.BannerAdSize
import io.bidmachine.model.Placement

/**
 * Used to load BidMachine banner ads and mediate callbacks between Google Mobile Ads SDK and
 * BidMachine SDK.
 */
class BidMachineBannerAd
internal constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  @get:VisibleForTesting internal val placement: Placement?,
  @get:VisibleForTesting internal val bannerAdSize: BannerAdSize,
) : MediationBannerAd, AdLoadCallback<BannerView>, BannerListener {
  private var bannerAdCallback: MediationBannerAdCallback? = null
  private lateinit var adView: BannerView

  fun loadWaterfallAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadBanner(placement, bannerAdSize, this)
  }

  fun loadRtbAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadBanner(bidResponse, bannerAdSize, this)
  }

  override fun getView(): View = adView

  override fun onAdLoaded(ad: BannerView) {
    adView = ad
    adView.setExtras(mapOf(WATERMARK_KEY to watermark))
    adView.setListener(this)
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(bmError: BMError) {
    val adError = AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onAdPresented(ad: BannerView) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdImpression(ad: BannerView) {
    bannerAdCallback?.reportAdImpression()
  }

  override fun onAdShowFailed(ad: BannerView, bmError: BMError) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClicked(ad: BannerView) {
    bannerAdCallback?.reportAdClicked()
    bannerAdCallback?.onAdOpened()
    bannerAdCallback?.onAdLeftApplication()
  }

  override fun onAdExpired(ad: BannerView) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  companion object {
    fun mapAdSizeToBidMachineBannerAdSize(context: Context, adSize: AdSize): BannerAdSize {
      return when (adSize) {
        AdSize.BANNER -> BannerAdSize.Banner
        AdSize.MEDIUM_RECTANGLE -> BannerAdSize.MediumRectangle
        AdSize.LEADERBOARD -> BannerAdSize.Leaderboard
        else -> {
          val displayMetrics = context.resources.displayMetrics
          val width =
            if (adSize.width > 0) adSize.width
            else (displayMetrics.widthPixels / displayMetrics.density).toInt()
          BannerAdSize.adaptive(width, adSize.height.coerceAtLeast(0))
        }
      }
    }

    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
    ): Result<BidMachineBannerAd> {
      val adSize = mediationBannerAdConfiguration.adSize
      val bidResponse = mediationBannerAdConfiguration.bidResponse
      val watermark = mediationBannerAdConfiguration.watermark
      val placement =
        createPlacement(mediationBannerAdConfiguration.serverParameters.getString(PLACEMENT_ID_KEY))
      val bannerAdSize =
        mapAdSizeToBidMachineBannerAdSize(mediationBannerAdConfiguration.context, adSize)

      return Result.success(
        BidMachineBannerAd(mediationAdLoadCallback, bidResponse, watermark, placement, bannerAdSize)
      )
    }
  }
}
