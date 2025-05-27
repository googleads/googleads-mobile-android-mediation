package com.google.ads.mediation.fyber

import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListener
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration

/**
 * Used to load DTExchange banner ads and mediate callbacks between Google Mobile Ads SDK and
 * DTExchange SDK.
 */
class DTExchangeBannerAd(
  private val mediationBannerAdConfiguration: MediationBannerAdConfiguration,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
) : MediationBannerAd, InneractiveAdSpot.RequestListener, InneractiveAdViewEventsListener {
  private lateinit var adSpot: InneractiveAdSpot
  private lateinit var wrapperView: RelativeLayout
  private var bannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd() {
    InneractiveAdManager.setMediationName(FyberMediationAdapter.MEDIATOR_NAME)
    InneractiveAdManager.setMediationVersion(MobileAds.getVersion().toString())

    val bidResponse = mediationBannerAdConfiguration.bidResponse
    adSpot = InneractiveAdSpotManager.get().createSpot()
    val controller = InneractiveAdViewUnitController()
    adSpot.addUnitController(controller)
    wrapperView = RelativeLayout(mediationBannerAdConfiguration.context)
    adSpot.setRequestListener(this)
    controller.eventsListener = this
    FyberAdapterUtils.updateFyberExtraParams(mediationBannerAdConfiguration.mediationExtras)
    adSpot.loadAd(bidResponse)
  }

  override fun onInneractiveSuccessfulAdRequest(iAdSpot: InneractiveAdSpot?) {
    if (!adSpot.isReady) {
      val adError =
        AdError(
          DTExchangeErrorCodes.ERROR_AD_NOT_READY,
          "DT Exchange's banner ad spot is not ready.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      Log.w(TAG, adError.message)
      mediationAdLoadCallback.onFailure(adError)
      adSpot.destroy()
      return
    }

    val controller = adSpot.selectedUnitController as? InneractiveAdViewUnitController
    if (controller == null) {
      val message = "Unexpected controller type."
      val adError =
        AdError(
          DTExchangeErrorCodes.ERROR_WRONG_CONTROLLER_TYPE,
          message,
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      Log.w(TAG, adError.message)
      mediationAdLoadCallback.onFailure(adError)
      adSpot.destroy()
      return
    }

    controller.bindView(wrapperView)
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onInneractiveFailedAdRequest(
    adSpot: InneractiveAdSpot?,
    errorCode: InneractiveErrorCode,
  ) {
    val adError = DTExchangeErrorCodes.getAdError(errorCode)
    mediationAdLoadCallback.onFailure(adError)
    adSpot?.destroy()
  }

  override fun getView(): View = wrapperView

  override fun onAdImpression(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.reportAdClicked()
  }

  override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.onAdClosed()
  }

  override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot?) {
    bannerAdCallback?.apply {
      onAdOpened()
      onAdLeftApplication()
    }
  }

  override fun onAdEnteredErrorState(
    adSpot: InneractiveAdSpot?,
    displayError: InneractiveUnitController.AdDisplayError?,
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

  companion object {
    private val TAG = DTExchangeBannerAd::class.simpleName
  }
}
