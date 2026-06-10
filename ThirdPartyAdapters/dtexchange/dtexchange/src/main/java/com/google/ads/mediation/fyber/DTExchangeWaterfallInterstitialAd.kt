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

package com.google.ads.mediation.fyber

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_AD_FAILED_TO_DISPLAY
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_AD_NOT_READY
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_DOMAIN
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.ERROR_WRONG_CONTROLLER_TYPE
import com.google.ads.mediation.fyber.DTExchangeErrorCodes.getAdError
import com.google.ads.mediation.fyber.FyberMediationAdapter.KEY_APP_ID
import com.google.ads.mediation.fyber.FyberMediationAdapter.MEDIATOR_NAME
import com.google.ads.mediation.fyber.FyberMediationAdapter.TAG
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration

/**
 * Used to load DTExchange Waterfall interstitial ads and mediate callbacks between Google Mobile
 * Ads SDK and DTExchange SDK.
 */
class DTExchangeWaterfallInterstitialAd :
  MediationInterstitialAd,
  InneractiveAdSpot.RequestListener,
  InneractiveFullscreenAdEventsListener {

  private lateinit var adLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>

  private lateinit var interstitialSpot: InneractiveAdSpot

  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd(
    interstitialAdConfig: MediationInterstitialAdConfiguration,
    adLoadCallback:
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    this.adLoadCallback = adLoadCallback
    val serverParameters = interstitialAdConfig.serverParameters

    val keyAppId: String? = serverParameters.getString(KEY_APP_ID)
    val error =
      AdError(
        DTExchangeErrorCodes.ERROR_INVALID_SERVER_PARAMETERS,
        "App ID is null or empty.",
        DTExchangeErrorCodes.ERROR_DOMAIN,
      )
    if (TextUtils.isEmpty(keyAppId)) {
      Log.w(TAG, error.message)
      adLoadCallback.onFailure(error)
      return
    }

    // Check that we got a valid spot id from the server.
    val spotId: String? = serverParameters.getString(FyberMediationAdapter.KEY_SPOT_ID)
    if (TextUtils.isEmpty(spotId)) {
      val error =
        AdError(
          DTExchangeErrorCodes.ERROR_INVALID_SERVER_PARAMETERS,
          "Cannot render interstitial ad. Please define a valid spot id on the AdMob UI.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      Log.w(TAG, error.message)
      adLoadCallback.onFailure(error)
      return
    }

    InneractiveAdManager.setMediationName(MEDIATOR_NAME)
    InneractiveAdManager.setMediationVersion(MobileAds.getVersion().toString())

    // Since this is Waterfall, initialize the DT SDK before loading the ad.
    InneractiveAdManager.initialize(
      interstitialAdConfig.context,
      keyAppId,
      object : OnFyberMarketplaceInitializedListener {
        override fun onFyberMarketplaceInitialized(fyberInitStatus: FyberInitStatus) {
          if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
            val error = getAdError(fyberInitStatus)
            Log.w(TAG, error.message)
            adLoadCallback.onFailure(error)
            return
          }

          interstitialSpot = InneractiveAdSpotManager.get().createSpot()

          val controller = InneractiveFullscreenUnitController()
          interstitialSpot.addUnitController(controller)

          interstitialSpot.setRequestListener(this@DTExchangeWaterfallInterstitialAd)

          FyberAdapterUtils.updateFyberExtraParams(interstitialAdConfig.mediationExtras)
          val request = InneractiveAdRequest(spotId)
          interstitialSpot.requestAd(request)
        }
      },
    )
  }

  // region MediationInterstitialAd implementation
  override fun showAd(context: Context) {
    if (interstitialSpot.selectedUnitController !is InneractiveFullscreenUnitController) {
      val errorMessage =
        "showInterstitial called, but wrong spot has been used (should not happen)."
      Log.w(TAG, errorMessage)
      val error = AdError(ERROR_WRONG_CONTROLLER_TYPE, errorMessage, ERROR_DOMAIN)
      interstitialAdCallback?.onAdFailedToShow(error)
      interstitialSpot.destroy()
      return
    }
    val controller = interstitialSpot.selectedUnitController as InneractiveFullscreenUnitController

    if (!interstitialSpot.isReady) {
      // Shouldn't really happen since GMA SDK hands the ad object to the publisher only after the
      // ad is ready (i.e. is loaded).
      val errorMessage = "showInterstitial called, but the ad is not ready."
      Log.w(TAG, errorMessage)
      val error = AdError(ERROR_AD_NOT_READY, errorMessage, ERROR_DOMAIN)
      interstitialAdCallback?.onAdFailedToShow(error)
      interstitialSpot.destroy()
      return
    }

    // The context passed here is guaranteed to be an Activity.
    controller.show(context as Activity)
  }

  // endregion

  // region InneractiveAdSpot.RequestListener implementation
  override fun onInneractiveSuccessfulAdRequest(adSpot: InneractiveAdSpot?) {
    if (interstitialSpot.selectedUnitController !is InneractiveFullscreenUnitController) {
      val message =
        java.lang.String.format(
          "Unexpected controller type. Expected: %s. Actual: %s",
          InneractiveUnitController::class.java.getName(),
          interstitialSpot.selectedUnitController.javaClass.getName(),
        )
      val error = AdError(ERROR_WRONG_CONTROLLER_TYPE, message, ERROR_DOMAIN)
      Log.w(TAG, error.message)
      adLoadCallback.onFailure(error)
      interstitialSpot.destroy()
      return
    }

    val controller = interstitialSpot.selectedUnitController as InneractiveFullscreenUnitController
    controller.eventsListener = this

    interstitialAdCallback = adLoadCallback.onSuccess(this)
  }

  override fun onInneractiveFailedAdRequest(
    adSpot: InneractiveAdSpot?,
    inneractiveErrorCode: InneractiveErrorCode?,
  ) {
    // Convert DT Exchange error code into custom error code
    val error: AdError = getAdError(inneractiveErrorCode)
    Log.w(TAG, error.message)
    adLoadCallback.onFailure(error)
    interstitialSpot.destroy()
  }

  // endregion

  // region InneractiveFullscreenAdEventsListener implementation
  override fun onAdImpression(p0: InneractiveAdSpot?) {
    interstitialAdCallback?.onAdOpened()
    interstitialAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(p0: InneractiveAdSpot?) {
    interstitialAdCallback?.reportAdClicked()
  }

  override fun onAdWillOpenExternalApp(p0: InneractiveAdSpot?) {
    interstitialAdCallback?.onAdLeftApplication()
  }

  override fun onAdEnteredErrorState(
    adSpot: InneractiveAdSpot?,
    adDisplayError: InneractiveUnitController.AdDisplayError?,
  ) {
    val error = AdError(ERROR_AD_FAILED_TO_DISPLAY, adDisplayError?.message ?: "", ERROR_DOMAIN)
    interstitialAdCallback?.onAdFailedToShow(error)
    interstitialSpot.destroy()
  }

  override fun onAdWillCloseInternalBrowser(p0: InneractiveAdSpot?) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  override fun onAdDismissed(p0: InneractiveAdSpot?) {
    interstitialAdCallback?.onAdClosed()
    interstitialSpot.destroy()
  }

  // endregion
}
