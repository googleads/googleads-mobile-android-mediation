// Copyright 2025 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.facebook.rtb

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.ExtraHints
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdExtendedListener
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FAILED_TO_PRESENT_AD
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG
import com.google.ads.mediation.facebook.FacebookMediationAdapter.getAdError
import com.google.ads.mediation.facebook.FacebookMediationAdapter.getPlacementID
import com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience
import com.google.ads.mediation.facebook.MetaFactory
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import java.util.concurrent.atomic.AtomicBoolean

class MetaRtbAppOpenAd(
  private val loadCallback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  private val metaFactory: MetaFactory,
) : MediationAppOpenAd, InterstitialAdExtendedListener {

  // Meta SDK uses InterstitialAd for displaying app open ads.
  private var appOpenAd: InterstitialAd? = null
  private var appOpenAdCallback: MediationAppOpenAdCallback? = null
  private val showAdCalled = AtomicBoolean()
  private val didAppOpenAdClose = AtomicBoolean()

  fun loadAd(adConfiguration: MediationAppOpenAdConfiguration) {
    val serverParameters: Bundle = adConfiguration.serverParameters
    val placementID = getPlacementID(serverParameters)
    if (TextUtils.isEmpty(placementID)) {
      val error =
        com.google.android.gms.ads.AdError(
          ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty. ",
          ERROR_DOMAIN,
        )
      Log.e(TAG, error.message)
      loadCallback.onFailure(error)
      return
    }

    setMixedAudience(adConfiguration)
    appOpenAd = metaFactory.createAppOpenAd(adConfiguration.context, placementID)
    if (!TextUtils.isEmpty(adConfiguration.watermark)) {
      appOpenAd?.setExtraHints(
        ExtraHints.Builder().mediationData(adConfiguration.watermark).build()
      )
    }

    appOpenAd?.loadAd(
      appOpenAd
        ?.buildLoadAdConfig()
        ?.withBid(adConfiguration.bidResponse)
        ?.withAdListener(this)
        ?.build()
    )
  }

  override fun showAd(context: Context) {
    showAdCalled.set(true)
    if (appOpenAd?.show() == false) {
      val showError =
        com.google.android.gms.ads.AdError(
          ERROR_FAILED_TO_PRESENT_AD,
          "Failed to present app open ad.",
          ERROR_DOMAIN,
        )
      Log.w(TAG, showError.toString())

      appOpenAdCallback?.onAdFailedToShow(showError)
    }
  }

  override fun onInterstitialDisplayed(ad: Ad) {
    appOpenAdCallback?.onAdOpened()
  }

  override fun onInterstitialDismissed(ad: Ad) {
    if (!didAppOpenAdClose.getAndSet(true)) {
      appOpenAdCallback?.onAdClosed()
    }
  }

  override fun onError(ad: Ad, adError: AdError) {
    val error = getAdError(adError)
    Log.w(TAG, error.message)
    if (showAdCalled.get()) {
      appOpenAdCallback?.onAdFailedToShow(error)
      return
    }
    loadCallback.onFailure(error)
  }

  override fun onAdLoaded(ad: Ad) {
    appOpenAdCallback = loadCallback.onSuccess(this)
  }

  override fun onAdClicked(ad: Ad) {
    appOpenAdCallback?.reportAdClicked()
  }

  override fun onLoggingImpression(ad: Ad) {
    appOpenAdCallback?.reportAdImpression()
  }

  override fun onInterstitialActivityDestroyed() {
    if (!didAppOpenAdClose.getAndSet(true)) {
      appOpenAdCallback?.onAdClosed()
    }
  }

  override fun onRewardedAdCompleted() {
    // no-op
  }

  override fun onRewardedAdServerSucceeded() {
    // no-op
  }

  override fun onRewardedAdServerFailed() {
    // no-op
  }
}
