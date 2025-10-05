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
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_UNSUPPORTED_AD_SIZE
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_UNSUPPORTED_AD_SIZE
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import net.pubnative.lite.sdk.views.HyBidAdView
import net.pubnative.lite.sdk.views.HyBidBannerAdView
import net.pubnative.lite.sdk.views.HyBidLeaderboardAdView
import net.pubnative.lite.sdk.views.HyBidMRectAdView
import net.pubnative.lite.sdk.views.PNAdView

/**
 * Used to load Verve banner ads and mediate callbacks between Google Mobile Ads SDK and Verve SDK.
 */
class VerveBannerAd
@VisibleForTesting
internal constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val bidResponse: String,
  private val adView: HyBidAdView,
  private val adSize: AdSize,
) : MediationBannerAd, PNAdView.Listener {
  private var bannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd(context: Context) {
    val adViewLayoutParams =
      ViewGroup.LayoutParams(adSize.getWidthInPixels(context), adSize.getHeightInPixels(context))
    adView.layoutParams = adViewLayoutParams

    adView.renderAd(bidResponse, this)
  }

  override fun onAdLoaded() {
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(error: Throwable?) {
    val errorMessage = if (error == null) "null" else error.message.toString()
    val adError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "Could not load banner ad. Error: $errorMessage",
        SDK_ERROR_DOMAIN,
      )
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onAdImpression() {
    bannerAdCallback?.reportAdImpression()
  }

  override fun onAdClick() {
    bannerAdCallback?.apply {
      reportAdClicked()
      onAdOpened()
      onAdLeftApplication()
    }
  }

  override fun getView(): View = adView

  companion object {
    internal fun mapAdSize(adSize: AdSize, context: Context): HyBidAdView? {
      return when (adSize) {
        AdSize.BANNER -> HyBidBannerAdView(context)
        AdSize.MEDIUM_RECTANGLE -> HyBidMRectAdView(context)
        AdSize.LEADERBOARD -> HyBidLeaderboardAdView(context)
        else -> null
      }
    }

    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
    ): Result<VerveBannerAd> {
      val context = mediationBannerAdConfiguration.context
      val adSize = mediationBannerAdConfiguration.adSize

      val bidResponse = mediationBannerAdConfiguration.bidResponse
      val verveBannerView = mapAdSize(adSize, context)
      if (verveBannerView == null) {
        val adError =
          AdError(
            ERROR_CODE_UNSUPPORTED_AD_SIZE,
            ERROR_MSG_UNSUPPORTED_AD_SIZE,
            ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      return Result.success(
        VerveBannerAd(mediationAdLoadCallback, bidResponse, verveBannerView, adSize)
      )
    }
  }
}
