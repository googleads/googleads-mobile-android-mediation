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

package com.google.ads.mediation.fyber

import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpot.RequestListener
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListener
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MediationUtils
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import kotlin.math.roundToInt

/**
 * Used to load DTExchange banner ads and mediate callbacks between Google Mobile Ads SDK and
 * DTExchange SDK.
 */
class DTExchangeWaterfallBannerAd(
  private val adLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
) : MediationBannerAd, RequestListener, InneractiveAdViewEventsListener {

  private var bannerAdCallback: MediationBannerAdCallback? = null

  /** Requested banner ad size. */
  private lateinit var requestedAdSize: AdSize

  /** DT Exchange's Spot object for the banner. */
  private lateinit var bannerSpot: InneractiveAdSpot

  /** A wrapper view for the DT Exchange banner view. */
  private lateinit var bannerWrapperView: ViewGroup

  // region MediationBannerAd implementation
  fun loadAd(adConfiguration: MediationBannerAdConfiguration) {
    val serverParameters = adConfiguration.serverParameters

    val keyAppId: String? = serverParameters.getString(FyberMediationAdapter.KEY_APP_ID)
    if (TextUtils.isEmpty(keyAppId)) {
      val serverParameterError =
        AdError(
          DTExchangeErrorCodes.ERROR_INVALID_SERVER_PARAMETERS,
          "App ID is null or empty.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      Log.w(TAG, serverParameterError.toString())
      adLoadCallback.onFailure(serverParameterError)
      return
    }

    InneractiveAdManager.setMediationName(FyberMediationAdapter.MEDIATOR_NAME)
    InneractiveAdManager.setMediationVersion(MobileAds.getVersion().toString())
    InneractiveAdManager.initialize(
      adConfiguration.context,
      keyAppId,
      object : OnFyberMarketplaceInitializedListener {
        override fun onFyberMarketplaceInitialized(fyberInitStatus: FyberInitStatus) {
          if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
            val loadError = DTExchangeErrorCodes.getAdError(fyberInitStatus)
            Log.w(TAG, loadError.toString())
            adLoadCallback.onFailure(loadError)
            return
          }

          // Check that we got a valid Spot ID from the server.
          val spotId: String? = serverParameters.getString(FyberMediationAdapter.KEY_SPOT_ID)
          if (TextUtils.isEmpty(spotId)) {
            val serverParameterError =
              AdError(
                DTExchangeErrorCodes.ERROR_INVALID_SERVER_PARAMETERS,
                "Cannot render banner ad. Please define a valid spot id on the AdMob UI.",
                DTExchangeErrorCodes.ERROR_DOMAIN,
              )
            Log.w(TAG, serverParameterError.toString())
            adLoadCallback.onFailure(serverParameterError)
            return
          }

          bannerSpot = InneractiveAdSpotManager.get().createSpot()

          val controller = InneractiveAdViewUnitController()
          bannerSpot.addUnitController(controller)

          // Prepare wrapper view before making request.
          bannerWrapperView = RelativeLayout(adConfiguration.context)

          val requestListener: RequestListener = this@DTExchangeWaterfallBannerAd
          bannerSpot.setRequestListener(requestListener)

          requestedAdSize = adConfiguration.adSize

          FyberAdapterUtils.updateFyberExtraParams(adConfiguration.mediationExtras)
          val request = InneractiveAdRequest(spotId)
          bannerSpot.requestAd(request)
        }
      },
    )
  }

  override fun getView(): View = bannerWrapperView

  // endregion

  // region RequestListener implementation
  override fun onInneractiveSuccessfulAdRequest(adSpot: InneractiveAdSpot?) {
    // Just a double check that we have the right type of selected controller.
    if (bannerSpot.selectedUnitController !is InneractiveAdViewUnitController) {
      val message =
        String.format(
          "Unexpected controller type. Expected: %s. Actual: %s",
          InneractiveUnitController::class.java.getName(),
          bannerSpot.selectedUnitController.javaClass.getName(),
        )
      val controllerMismatchError =
        AdError(
          DTExchangeErrorCodes.ERROR_WRONG_CONTROLLER_TYPE,
          message,
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      Log.w(TAG, controllerMismatchError.toString())
      adLoadCallback.onFailure(controllerMismatchError)
      bannerSpot.destroy()
    }

    val controller = bannerSpot.selectedUnitController as InneractiveAdViewUnitController
    val listener: InneractiveAdViewEventsListener = this@DTExchangeWaterfallBannerAd
    controller.eventsListener = listener
    controller.bindView(bannerWrapperView)

    // Validate the ad size returned by Fyber Marketplace with the requested ad size.
    val context = bannerWrapperView.context
    val density = context.resources.displayMetrics.density
    val fyberAdWidth = (controller.getAdContentWidth() / density).roundToInt()
    val fyberAdHeight = (controller.getAdContentHeight() / density).roundToInt()

    val potentials = ArrayList<AdSize?>()
    potentials.add(AdSize(fyberAdWidth, fyberAdHeight))
    val supportedAdSize = MediationUtils.findClosestSize(context, requestedAdSize, potentials)
    if (supportedAdSize == null) {
      val requestedAdWidth = (requestedAdSize.getWidthInPixels(context) / density).roundToInt()
      val requestedAdHeight = (requestedAdSize.getHeightInPixels(context) / density).roundToInt()
      val message =
        String.format(
          "The loaded ad size did not match the requested " +
            "ad size. Requested ad size: %dx%d. Loaded ad size: %dx%d.",
          requestedAdWidth,
          requestedAdHeight,
          fyberAdWidth,
          fyberAdHeight,
        )
      val adSizeError =
        AdError(
          DTExchangeErrorCodes.ERROR_BANNER_SIZE_MISMATCH,
          message,
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      Log.w(TAG, adSizeError.toString())
      adLoadCallback.onFailure(adSizeError)
      return
    }

    bannerAdCallback = adLoadCallback.onSuccess(this@DTExchangeWaterfallBannerAd)
  }

  override fun onInneractiveFailedAdRequest(
    adSpot: InneractiveAdSpot?,
    inneractiveErrorCode: InneractiveErrorCode,
  ) {
    val error = DTExchangeErrorCodes.getAdError(inneractiveErrorCode)
    Log.w(TAG, error.toString())
    adLoadCallback.onFailure(error)
    adSpot?.destroy()
  }

  // endregion

  // region InneractiveAdViewEventsListener implementation
  override fun onAdImpression(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.reportAdClicked()
    bannerAdCallback?.onAdOpened()
  }

  override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.onAdClosed()
  }

  override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.onAdLeftApplication()
  }

  override fun onAdEnteredErrorState(
    adSpot: InneractiveAdSpot?,
    adDisplayError: InneractiveUnitController.AdDisplayError?,
  ) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  override fun onAdExpanded(adSpot: InneractiveAdSpot?) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  override fun onAdResized(adSpot: InneractiveAdSpot?) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  override fun onAdCollapsed(adSpot: InneractiveAdSpot?) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  // endregion

  companion object {
    private val TAG = DTExchangeWaterfallBannerAd::class.simpleName
  }
}
