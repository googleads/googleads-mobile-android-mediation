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

package com.google.ads.mediation.pubmatic

import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_INVALID_BANNER_AD_SIZE
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_INVALID_BANNER_AD_SIZE_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_AD_UNIT
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.getPubMaticBannerAdSize
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import com.pubmatic.sdk.openwrap.banner.POBBannerView.POBBannerViewListener
import com.pubmatic.sdk.openwrap.core.POBConstants.KEY_POB_ADMOB_WATERMARK
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import java.lang.RuntimeException

/**
 * Used to load PubMatic banner ads and mediate callbacks between Google Mobile Ads SDK and PubMatic
 * SDK.
 */
class PubMaticBannerAd
private constructor(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  private val pobBannerView: POBBannerView,
  private val isRTB: Boolean,
) : MediationBannerAd, POBBannerViewListener() {

  private var mediationBannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd() {
    pobBannerView.setListener(this)
    // Pause auto-refresh since the GMA SDK will handle the banner refresh logic.
    pobBannerView.pauseAutoRefresh()
    if (isRTB) {
      pobBannerView.addExtraInfo(KEY_POB_ADMOB_WATERMARK, watermark)
      pobBannerView.loadAd(bidResponse, POBBiddingHost.ADMOB)
      return
    }
    pobBannerView.loadAd()
  }

  override fun getView() = pobBannerView

  override fun onAdReceived(pobBannerView: POBBannerView) {
    mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdFailed(pobBannerView: POBBannerView, pobError: POBError) {
    mediationAdLoadCallback.onFailure(
      AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    )
  }

  override fun onAdImpression(pobBannerView: POBBannerView) {
    mediationBannerAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(pobBannerView: POBBannerView) {
    mediationBannerAdCallback?.reportAdClicked()
  }

  override fun onAdOpened(pobBannerView: POBBannerView) {
    mediationBannerAdCallback?.onAdOpened()
  }

  override fun onAppLeaving(pobBannerView: POBBannerView) {
    mediationBannerAdCallback?.onAdLeftApplication()
  }

  override fun onAdClosed(pobBannerView: POBBannerView) {
    mediationBannerAdCallback?.onAdClosed()
  }

  companion object {
    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
      pubMaticAdFactory: PubMaticAdFactory,
      isRTB: Boolean,
    ): Result<PubMaticBannerAd> {
      val context = mediationBannerAdConfiguration.context
      val pobBannerAd =
        if (isRTB) {
          pubMaticAdFactory.createPOBBannerView(context)
        } else {
          val adSize = mediationBannerAdConfiguration.adSize
          val pobAdSize = getPubMaticBannerAdSize(context, adSize)
          if (pobAdSize == null) {
            val adError =
              AdError(
                ERROR_INVALID_BANNER_AD_SIZE,
                ERROR_INVALID_BANNER_AD_SIZE_MSG,
                ADAPTER_ERROR_DOMAIN,
              )
            mediationAdLoadCallback.onFailure(adError)
            return Result.failure(RuntimeException(adError.toString()))
          }
          val serverParameters = mediationBannerAdConfiguration.serverParameters
          val pubId = serverParameters.getString(KEY_PUBLISHER_ID)
          val profileId = serverParameters.getString(KEY_PROFILE_ID)?.toIntOrNull()
          val adUnit = serverParameters.getString(KEY_AD_UNIT)
          if (pubId.isNullOrEmpty()) {
            val adError =
              AdError(
                ERROR_MISSING_PUBLISHER_ID,
                ERROR_MISSING_PUBLISHER_ID_MSG,
                ADAPTER_ERROR_DOMAIN,
              )
            mediationAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          if (profileId == null) {
            val adError =
              AdError(
                ERROR_MISSING_OR_INVALID_PROFILE_ID,
                ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG,
                ADAPTER_ERROR_DOMAIN,
              )
            mediationAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          if (adUnit.isNullOrEmpty()) {
            val adError =
              AdError(ERROR_MISSING_AD_UNIT_ID, ERROR_MISSING_AD_UNIT_ID_MSG, ADAPTER_ERROR_DOMAIN)
            mediationAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          pubMaticAdFactory.createPOBBannerView(context, pubId, profileId, adUnit, pobAdSize)
        }

      return Result.success(
        PubMaticBannerAd(
          mediationAdLoadCallback,
          mediationBannerAdConfiguration.bidResponse,
          mediationBannerAdConfiguration.watermark,
          pobBannerAd,
          isRTB,
        )
      )
    }
  }
}
