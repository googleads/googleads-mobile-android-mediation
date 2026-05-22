package com.google.ads.mediation.fyber

import android.app.Activity
import android.content.Context
import android.util.Log
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration

/**
 * Used to load DTExchange interstitial ads and mediate callbacks between Google Mobile Ads SDK and
 * DTExchange SDK.
 */
class DTExchangeInterstitialAd(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
) :
  MediationInterstitialAd,
  InneractiveAdSpot.RequestListener,
  InneractiveFullscreenAdEventsListener {
  private lateinit var adSpot: InneractiveAdSpot
  private var interstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd(mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration) {
    InneractiveAdManager.setMediationName(FyberMediationAdapter.MEDIATOR_NAME)
    InneractiveAdManager.setMediationVersion(MobileAds.getVersion().toString())

    val bidResponse = mediationInterstitialAdConfiguration.bidResponse
    adSpot = InneractiveAdSpotManager.get().createSpot()
    val controller = InneractiveFullscreenUnitController()
    adSpot.addUnitController(controller)
    adSpot.setRequestListener(this)
    controller.eventsListener = this
    FyberAdapterUtils.updateFyberExtraParams(mediationInterstitialAdConfiguration.mediationExtras)
    val watermark = mediationInterstitialAdConfiguration.watermark
    adSpot.loadAd(bidResponse, watermark)
  }

  override fun showAd(context: Context) {
    val controller = adSpot.selectedUnitController as? InneractiveFullscreenUnitController
    if (controller == null) {
      Log.w(TAG, "showInterstitial called, but wrong spot has been used (should not happen).")
      interstitialAdCallback?.onAdOpened()
      interstitialAdCallback?.onAdClosed()
      adSpot.destroy()
      return
    }
    controller.show(context as Activity)
  }

  override fun onInneractiveSuccessfulAdRequest(iAdSpot: InneractiveAdSpot?) {
    if (!adSpot.isReady) {
      val adError =
        AdError(
          DTExchangeErrorCodes.ERROR_AD_NOT_READY,
          "DT Exchange's interstitial ad spot is not ready.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      Log.w(TAG, adError.message)
      mediationAdLoadCallback.onFailure(adError)
      adSpot.destroy()
      return
    }
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onInneractiveFailedAdRequest(
    iAdSpot: InneractiveAdSpot?,
    errorCode: InneractiveErrorCode,
  ) {
    val adError = DTExchangeErrorCodes.getAdError(errorCode)
    mediationAdLoadCallback.onFailure(adError)
    iAdSpot?.destroy()
  }

  override fun onAdImpression(iAdSpot: InneractiveAdSpot?) {
    interstitialAdCallback?.onAdOpened()
    interstitialAdCallback?.reportAdImpression()
  }

  override fun onAdClicked(iAdSpot: InneractiveAdSpot?) {
    interstitialAdCallback?.reportAdClicked()
  }

  override fun onAdWillCloseInternalBrowser(iAdSpot: InneractiveAdSpot?) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  override fun onAdWillOpenExternalApp(iAdSpot: InneractiveAdSpot?) {
    interstitialAdCallback?.onAdLeftApplication()
  }

  override fun onAdEnteredErrorState(
    iAdSpot: InneractiveAdSpot?,
    displayError: InneractiveUnitController.AdDisplayError?,
  ) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  override fun onAdDismissed(iAdSpot: InneractiveAdSpot?) {
    interstitialAdCallback?.onAdClosed()
  }

  companion object {
    private val TAG = DTExchangeInterstitialAd::class.simpleName
  }
}
