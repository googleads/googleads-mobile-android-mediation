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
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_AD_NOT_READY
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.POBConstants.KEY_POB_ADMOB_WATERMARK
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial.POBInterstitialListener

/**
 * Used to load PubMatic interstitial ads and mediate callbacks between Google Mobile Ads SDK and
 * PubMatic SDK.
 */
class PubMaticInterstitialAd
private constructor(
  context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  pubMaticAdFactory: PubMaticAdFactory,
) : MediationInterstitialAd, POBInterstitialListener() {

  /** PubMatic SDK's interstitial ad object. */
  private val pobInterstitial: POBInterstitial = pubMaticAdFactory.createPOBInterstitial(context)

  private var mediationInterstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd() {
    pobInterstitial.setListener(this)
    pobInterstitial.addExtraInfo(KEY_POB_ADMOB_WATERMARK, watermark)
    pobInterstitial.loadAd(bidResponse, POBBiddingHost.ADMOB)
  }

  override fun onAdReceived(pobInterstitial: POBInterstitial) {
    mediationInterstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdFailedToLoad(pobInterstitial: POBInterstitial, pobError: POBError) {
    mediationAdLoadCallback.onFailure(
      AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    )
  }

  override fun showAd(context: Context) {
    if (pobInterstitial.isReady) {
      pobInterstitial.show()
    } else {
      mediationInterstitialAdCallback?.onAdFailedToShow(
        AdError(ERROR_AD_NOT_READY, "Ad not ready", ADAPTER_ERROR_DOMAIN)
      )
    }
  }

  override fun onAdFailedToShow(pobInterstitial: POBInterstitial, pobError: POBError) {
    mediationInterstitialAdCallback?.onAdFailedToShow(
      AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    )
  }

  override fun onAdImpression(pobInterstitial: POBInterstitial) {
    mediationInterstitialAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(pobInterstitial: POBInterstitial) {
    mediationInterstitialAdCallback?.reportAdClicked()
  }

  override fun onAppLeaving(pobInterstitial: POBInterstitial) {
    mediationInterstitialAdCallback?.onAdLeftApplication()
  }

  override fun onAdOpened(pobInterstitial: POBInterstitial) {
    mediationInterstitialAdCallback?.onAdOpened()
  }

  override fun onAdClosed(pobInterstitial: POBInterstitial) {
    mediationInterstitialAdCallback?.onAdClosed()
  }

  companion object {
    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
      pubMaticAdFactory: PubMaticAdFactory,
    ): Result<PubMaticInterstitialAd> {
      return Result.success(
        PubMaticInterstitialAd(
          mediationInterstitialAdConfiguration.context,
          mediationAdLoadCallback,
          mediationInterstitialAdConfiguration.bidResponse,
          mediationInterstitialAdConfiguration.watermark,
          pubMaticAdFactory,
        )
      )
    }
  }
}
