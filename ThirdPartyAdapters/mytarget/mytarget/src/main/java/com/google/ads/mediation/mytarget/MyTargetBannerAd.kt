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

import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.my.target.ads.MyTargetView
import com.my.target.ads.MyTargetView.MyTargetViewListener
import com.my.target.common.CustomParams
import com.my.target.common.models.IAdLoadingError

class MyTargetBannerAd(
  private val adLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
) : MediationBannerAd, MyTargetViewListener {

  private var bannerAdCallback: MediationBannerAdCallback? = null

  // myTarget Banner ad object
  private lateinit var myTargetBannerAdView: MyTargetView

  // region MediationBannerAd implementation
  fun loadAd(adConfiguration: MediationBannerAdConfiguration) {
    val context = adConfiguration.context
    val serverParameters = adConfiguration.serverParameters

    val slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters)
    Log.d(TAG, "Requesting myTarget banner mediation with Slot ID: $slotId")

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

    val requestedAdSize = adConfiguration.adSize
    val myTargetSize = MyTargetTools.getSupportedAdSize(requestedAdSize, context)
    if (myTargetSize == null) {
      val errorMessage = String.format("Unsupported ad size: $requestedAdSize")
      val sizeError =
        AdError(
          MyTargetMediationAdapter.ERROR_BANNER_SIZE_MISMATCH,
          errorMessage,
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      Log.e(TAG, sizeError.message)
      adLoadCallback.onFailure(sizeError)
      return
    }

    myTargetBannerAdView = MyTargetSdkWrapper.createBannerAd(context)
    myTargetBannerAdView.setSlotId(slotId)
    myTargetBannerAdView.setAdSize(myTargetSize)
    myTargetBannerAdView.setRefreshAd(false)

    val params: CustomParams = myTargetBannerAdView.customParams
    MyTargetTools.handleMediationExtras(TAG, adConfiguration.mediationExtras, params)
    params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE)

    myTargetBannerAdView.listener = this@MyTargetBannerAd
    Log.d(TAG, "Loading myTarget banner with size: ${myTargetSize.width} x ${myTargetSize.height}")
    myTargetBannerAdView.load()
  }

  override fun getView(): View {
    return myTargetBannerAdView
  }

  // endregion

  // region myTarget MyTargetViewListener implementation
  override fun onLoad(p0: MyTargetView) {
    Log.d(TAG, "Banner mediation Ad loaded.")
    bannerAdCallback = adLoadCallback.onSuccess(this@MyTargetBannerAd)
  }

  override fun onNoAd(reason: IAdLoadingError, view: MyTargetView) {
    val loadError =
      AdError(
        MyTargetMediationAdapter.ERROR_MY_TARGET_SDK,
        reason.message,
        MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN,
      )
    Log.e(TAG, loadError.message)
    adLoadCallback.onFailure(loadError)
  }

  override fun onShow(p0: MyTargetView) {
    Log.d(TAG, "Banner mediation Ad show.")
    bannerAdCallback?.reportAdImpression()
  }

  override fun onClick(p0: MyTargetView) {
    Log.d(TAG, "Banner mediation Ad clicked.")
    bannerAdCallback?.reportAdClicked()
    bannerAdCallback?.onAdOpened()

    // click redirects user to Google Play, or web browser, so we can notify
    // about left application.
    bannerAdCallback?.onAdLeftApplication()
  }

  // endregion

  companion object {
    const val TAG = "MyTargetBannerAd"
  }
}
