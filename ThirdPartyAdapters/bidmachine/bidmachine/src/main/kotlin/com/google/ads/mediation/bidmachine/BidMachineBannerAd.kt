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
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_INVALID_AD_SIZE
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_INVALID_AD_SIZE
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerSize
import io.bidmachine.banner.BannerView
import io.bidmachine.models.AuctionResult
import io.bidmachine.utils.BMError

/**
 * Used to load BidMachine banner ads and mediate callbacks between Google Mobile Ads SDK and
 * BidMachine SDK.
 */
class BidMachineBannerAd
internal constructor(
  private val context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val bannerSize: BannerSize,
  private val bidResponse: String,
  private val placementId: String?,
) : MediationBannerAd, BannerRequest.AdRequestListener, BannerListener {
  private var bannerAdCallback: MediationBannerAdCallback? = null
  @VisibleForTesting internal var bannerRequestBuilder = BannerRequest.Builder()
  private lateinit var adView: BannerView

  fun loadWaterfallAd(bannerView: BannerView) {
    val bannerRequest =
      bannerRequestBuilder.setSize(bannerSize).setPlacementId(placementId).setListener(this).build()
    loadAdOnBannerView(bannerView, bannerRequest)
  }

  fun loadRtbAd(bannerView: BannerView) {
    val bannerRequest =
      bannerRequestBuilder.setSize(bannerSize).setBidPayload(bidResponse).setListener(this).build()
    loadAdOnBannerView(bannerView, bannerRequest)
  }

  private fun loadAdOnBannerView(bannerView: BannerView, bannerRequest: BannerRequest) {
    adView = bannerView
    adView.setListener(this)
    bannerRequest.request(context)
  }

  override fun getView(): View = adView

  override fun onRequestSuccess(bannerRequest: BannerRequest, auctionResult: AuctionResult) {
    if (bannerRequest.isExpired) {
      val adError =
        AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
      mediationAdLoadCallback.onFailure(adError)
      bannerRequest.destroy()
      return
    }
    adView.load(bannerRequest)
  }

  override fun onRequestFailed(bannerRequest: BannerRequest, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    bannerRequest.destroy()
  }

  override fun onRequestExpired(bannerRequest: BannerRequest) {
    val adError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    bannerRequest.destroy()
  }

  override fun onAdLoaded(bannerView: BannerView) {
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(bannerView: BannerView, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationAdLoadCallback.onFailure(adError)
    adView.destroy()
  }

  override fun onAdImpression(bannerView: BannerView) {
    bannerAdCallback?.reportAdImpression()
  }

  override fun onAdShowFailed(bannerView: BannerView, bMError: BMError) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClicked(bannerView: BannerView) {
    bannerAdCallback?.reportAdClicked()
    bannerAdCallback?.onAdOpened()
    bannerAdCallback?.onAdLeftApplication()
  }

  override fun onAdExpired(bannerView: BannerView) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  companion object {
    private fun mapAdSizeToBidMachineBannerSize(adSize: AdSize): BannerSize? =
      when (adSize) {
        AdSize.BANNER -> BannerSize.Size_320x50
        AdSize.MEDIUM_RECTANGLE -> BannerSize.Size_300x250
        AdSize.LEADERBOARD -> BannerSize.Size_728x90
        else -> null
      }

    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
    ): Result<BidMachineBannerAd> {
      val context = mediationBannerAdConfiguration.context
      val adSize = mediationBannerAdConfiguration.adSize
      val bidResponse = mediationBannerAdConfiguration.bidResponse
      val placementId = mediationBannerAdConfiguration.serverParameters.getString(PLACEMENT_ID_KEY)

      val bannerSize = mapAdSizeToBidMachineBannerSize(adSize)
      if (bannerSize == null) {
        val adError =
          AdError(ERROR_CODE_INVALID_AD_SIZE, ERROR_MSG_INVALID_AD_SIZE, ADAPTER_ERROR_DOMAIN)
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      return Result.success(
        BidMachineBannerAd(context, mediationAdLoadCallback, bannerSize, bidResponse, placementId)
      )
    }
  }
}
