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

package com.google.ads.mediation.imobile

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import jp.co.imobile.sdkads.android.FailNotificationReason
import jp.co.imobile.sdkads.android.ImobileSdkAdListener

/** Loads and shows i-Mobile interstitial ad. */
class IMobileInterstitialAd(
  val adLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  val iMobileSdkWrapper: IMobileSdkWrapper,
) : MediationInterstitialAd, ImobileSdkAdListener() {

  private var interstitialSpotId: String? = null

  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd(adConfig: MediationInterstitialAdConfiguration) {
    // Validate Context.
    if (adConfig.context !is Activity) {
      val error =
        AdError(
          IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Context is not an Activity.",
          IMobileMediationAdapter.ERROR_DOMAIN,
        )
      Log.w(IMobileAdapter.TAG, error.message)
      adLoadCallback.onFailure(error)
      return
    }
    val interstitialActivity = adConfig.context as Activity

    // Get parameters for i-mobile SDK.
    val serverParameters = adConfig.serverParameters
    val publisherId: String? = serverParameters.getString(Constants.KEY_PUBLISHER_ID)
    val mediaId: String? = serverParameters.getString(Constants.KEY_MEDIA_ID)
    interstitialSpotId = serverParameters.getString(Constants.KEY_SPOT_ID)

    // Call i-mobile SDK.
    iMobileSdkWrapper.registerSpotFullScreen(
      interstitialActivity,
      publisherId,
      mediaId,
      interstitialSpotId,
    )
    iMobileSdkWrapper.setImobileSdkAdListener(
      interstitialSpotId,
      object : ImobileSdkAdListener() {},
    )

    // Start getting ads.
    if (iMobileSdkWrapper.isShowAd(interstitialSpotId)) {
      interstitialAdCallback = adLoadCallback.onSuccess(this)
    } else {
      iMobileSdkWrapper.start(interstitialSpotId)
    }
  }

  // region MediationInterstitialAd implementation
  override fun showAd(context: Context) {
    // context passed here is guaranteed by GMA SDK to be an activity.
    val activity = context as Activity
    if (activity.hasWindowFocus() && interstitialSpotId != null) {
      iMobileSdkWrapper.showAdforce(activity, interstitialSpotId)
    }
  }

  // endregion

  // region ImobileSdkAdListener implementation
  override fun onAdReadyCompleted() {
    interstitialAdCallback = adLoadCallback.onSuccess(this)
  }

  override fun onAdShowCompleted() {
    if (interstitialAdCallback != null) {
      interstitialAdCallback?.onAdOpened()
    }
  }

  override fun onAdCliclkCompleted() {
    if (interstitialAdCallback != null) {
      interstitialAdCallback?.reportAdClicked()
      interstitialAdCallback?.onAdLeftApplication()
    }
  }

  override fun onAdCloseCompleted() {
    if (interstitialAdCallback != null) {
      interstitialAdCallback?.onAdClosed()
    }
  }

  override fun onFailed(reason: FailNotificationReason) {
    val error = AdapterHelper.getAdError(reason)
    Log.w(IMobileAdapter.TAG, error.getMessage())
    adLoadCallback.onFailure(error)
  }
  // endregion
}
