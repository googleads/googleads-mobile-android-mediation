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

package com.google.ads.mediation.mytarget

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.my.target.ads.InterstitialAd
import com.my.target.common.CustomParams
import com.my.target.common.models.IAdLoadingError

class MyTargetInterstitialAd(
  private val adLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
) : MediationInterstitialAd, InterstitialAd.InterstitialAdListener {

  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  // myTarget Interstitial ad object
  private lateinit var myTargetInterstitialAd: InterstitialAd

  fun loadAd(adConfiguration: MediationInterstitialAdConfiguration) {
    val context = adConfiguration.context
    val serverParameters = adConfiguration.serverParameters

    val slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters)
    Log.d(TAG, "Requesting myTarget interstitial mediation with Slot ID: $slotId")

    if (slotId < 0) {
      val slotIdError =
        AdError(
          MyTargetMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Slot ID.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      Log.e(TAG, slotIdError.message)
      adLoadCallback.onFailure(slotIdError)
      return
    }

    myTargetInterstitialAd = MyTargetSdkWrapper.createInterstitialAd(slotId, context)

    val params: CustomParams = myTargetInterstitialAd.customParams
    MyTargetTools.handleMediationExtras(TAG, adConfiguration.mediationExtras, params)
    params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE)

    myTargetInterstitialAd.listener = this
    myTargetInterstitialAd.load()
  }

  // region MediationInterstitialAd implementation

  override fun showAd(context: Context) {
    myTargetInterstitialAd.show()
  }

  // endregion

  // region myTarget InterstitialAdListener implementation
  override fun onLoad(interstitialAd: InterstitialAd) {
    Log.d(TAG, "Interstitial mediation Ad loaded.")
    interstitialAdCallback = adLoadCallback.onSuccess(this)
  }

  override fun onNoAd(reason: IAdLoadingError, interstitialAd: InterstitialAd) {
    val loadError =
      AdError(
        MyTargetMediationAdapter.ERROR_MY_TARGET_SDK,
        reason.message,
        MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN,
      )
    Log.e(TAG, loadError.message)
    adLoadCallback.onFailure(loadError)
  }

  override fun onDisplay(interstitialAd: InterstitialAd) {
    Log.d(TAG, "Interstitial mediation Ad displayed.")
    interstitialAdCallback?.onAdOpened()
    interstitialAdCallback?.reportAdImpression()
  }

  override fun onFailedToShow(interstitialAd: InterstitialAd) {
    val showError =
      AdError(
        MyTargetMediationAdapter.ERROR_AD_FAILED_TO_SHOW,
        MyTargetMediationAdapter.ERROR_MSG_AD_FAILED_TO_SHOW,
        MyTargetMediationAdapter.ERROR_DOMAIN,
      )
    Log.e(TAG, showError.message)
    interstitialAdCallback?.onAdFailedToShow(showError)
  }

  override fun onClick(interstitialAd: InterstitialAd) {
    Log.d(TAG, "Interstitial mediation Ad clicked.")
    interstitialAdCallback?.reportAdClicked()

    // click redirects user to Google Play, or web browser, so we can notify
    // about left application.
    interstitialAdCallback?.onAdLeftApplication()
  }

  override fun onDismiss(interstitialAd: InterstitialAd) {
    Log.d(TAG, "Interstitial mediation Ad dismissed.")
    interstitialAdCallback?.onAdClosed()
  }

  override fun onVideoCompleted(interstitialAd: InterstitialAd) {
    Log.d(TAG, "Interstitial mediation Ad video completed.")
    // No events to forward to the Google Mobile Ads SDK.
  }

  // endregion

  companion object {
    const val TAG = "MyTargetInterstitialAd"
  }
}
