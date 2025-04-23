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

import android.content.Context
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
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

/**
 * Used to load PubMatic banner ads and mediate callbacks between Google Mobile Ads SDK and PubMatic
 * SDK.
 */
class PubMaticBannerAd
private constructor(
  context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  pubMaticAdFactory: PubMaticAdFactory,
) : MediationBannerAd, POBBannerViewListener() {

  /** PubMatic SDK's banner view object. */
  private val pobBannerView: POBBannerView = pubMaticAdFactory.createPOBBannerView(context)

  private var mediationBannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd() {
    pobBannerView.setListener(this)
    // Pause auto-refresh since the GMA SDK will handle the banner refresh logic.
    pobBannerView.pauseAutoRefresh()
    pobBannerView.addExtraInfo(KEY_POB_ADMOB_WATERMARK, watermark)
    pobBannerView.loadAd(bidResponse, POBBiddingHost.ADMOB)
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
    ) =
      Result.success(
        PubMaticBannerAd(
          mediationBannerAdConfiguration.context,
          mediationAdLoadCallback,
          mediationBannerAdConfiguration.bidResponse,
          mediationBannerAdConfiguration.watermark,
          pubMaticAdFactory,
        )
      )
  }
}
