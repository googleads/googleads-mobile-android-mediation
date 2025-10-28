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

package com.google.ads.mediation.applovin

import android.content.Context
import android.os.Bundle
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MISSING_AD_UNIT_ID
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MISSING_SDK_KEY
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MSG_MISSING_SDK
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_PRESENTATION_AD_NOT_READY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration

class AppLovinWaterfallAppOpenAd(
  private val loadCallback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  private val appLovinInitializer: AppLovinInitializer,
  private val appLovinAdFactory: AppLovinAdFactory,
) : MediationAppOpenAd, MaxAdListener {

  private var appLovinAppOpenAd: MaxAppOpenAd? = null

  private var appOpenAdCallback: MediationAppOpenAdCallback? = null

  fun loadAd(appOpenAdConfiguration: MediationAppOpenAdConfiguration) {
    val serverParameters: Bundle = appOpenAdConfiguration.serverParameters

    val sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY)
    if (sdkKey.isNullOrEmpty()) {
      val error = AdError(ERROR_MISSING_SDK_KEY, ERROR_MSG_MISSING_SDK, ERROR_DOMAIN)
      loadCallback.onFailure(error)
      return
    }

    val adUnitId = serverParameters.getString(ServerParameterKeys.AD_UNIT_ID)
    if (adUnitId.isNullOrEmpty()) {
      val error = AdError(ERROR_MISSING_AD_UNIT_ID, "Ad Unit ID is missing.", ERROR_DOMAIN)
      loadCallback.onFailure(error)
      return
    }

    appLovinInitializer.initialize(appOpenAdConfiguration.context, sdkKey) {
      appLovinAppOpenAd = appLovinAdFactory.createMaxAppOpenAd(adUnitId)
      appLovinAppOpenAd?.setListener(this@AppLovinWaterfallAppOpenAd)
      appLovinAppOpenAd?.loadAd()
    }
  }

  // region MediationAppOpenAd implementation

  override fun showAd(context: Context) {
    if (appLovinAppOpenAd != null) {
      if (appLovinAppOpenAd?.isReady == true) {
        appLovinAppOpenAd?.showAd()
      } else {
        appOpenAdCallback?.onAdFailedToShow(
          AdError(ERROR_PRESENTATION_AD_NOT_READY, "Ad is not ready to be displayed", ERROR_DOMAIN)
        )
      }
    }
  }

  // endregion

  // region MaxAdListener implementation

  override fun onAdLoaded(ad: MaxAd) {
    appOpenAdCallback = loadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(p0: String, appLovinError: MaxError) {
    loadCallback.onFailure(
      AdError(
        appLovinError.code,
        appLovinError.message,
        AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN,
      )
    )
  }

  override fun onAdDisplayed(ad: MaxAd) {
    appOpenAdCallback?.onAdOpened()
    appOpenAdCallback?.reportAdImpression()
  }

  override fun onAdHidden(ad: MaxAd) {
    appOpenAdCallback?.onAdClosed()
  }

  override fun onAdClicked(ad: MaxAd) {
    appOpenAdCallback?.reportAdClicked()
  }

  override fun onAdDisplayFailed(ad: MaxAd, appLovinError: MaxError) {
    appOpenAdCallback?.onAdFailedToShow(
      AdError(
        appLovinError.code,
        appLovinError.message,
        AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN,
      )
    )
  }

  // endregion
}
