package com.google.ads.mediation.vungle.renderers

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.ads.mediation.vungle.VungleConstants
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener
import com.google.ads.mediation.vungle.VungleMediationAdapter
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_CANNOT_PLAY_AD
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.vungle.VungleMediationAdapter.TAG
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.InterstitialAd
import com.vungle.ads.InterstitialAdListener
import com.vungle.ads.VungleError

/** Abstract class with app-open adapter logic common for both waterfall and RTB integrations. */
abstract class VungleAppOpenAd(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  private val vungleFactory: VungleFactory,
) : MediationAppOpenAd, InterstitialAdListener {

  /**
   * Liftoff's app open ad object. Note: Liftoff uses [InterstitialAd] object for displaying app
   * open ads.
   */
  private lateinit var appOpenAd: InterstitialAd

  private var mediationAppOpenAdCallback: MediationAppOpenAdCallback? = null

  /** Loads an app open ad. */
  fun render(mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration) {
    val mediationExtras: Bundle = mediationAppOpenAdConfiguration.mediationExtras
    val serverParameters: Bundle = mediationAppOpenAdConfiguration.serverParameters

    val appId = serverParameters.getString(VungleConstants.KEY_APP_ID)

    if (appId.isNullOrEmpty()) {
      val error =
        AdError(
          ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load app open ad from Liftoff Monetize. Missing or invalid App ID " +
            "configured for this ad source instance in the AdMob or Ad Manager UI.",
          ERROR_DOMAIN,
        )
      Log.w(TAG, error.toString())
      mediationAdLoadCallback.onFailure(error)
      return
    }

    val placement = serverParameters.getString(VungleConstants.KEY_PLACEMENT_ID)
    if (placement.isNullOrEmpty()) {
      val error =
        AdError(
          ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load app open ad from Liftoff Monetize. Missing or Invalid Placement " +
            "ID configured for this ad source instance in the AdMob or Ad Manager UI.",
          ERROR_DOMAIN,
        )
      Log.w(TAG, error.toString())
      mediationAdLoadCallback.onFailure(error)
      return
    }

    val context: Context = mediationAppOpenAdConfiguration.context

    VungleInitializer.getInstance()
      .initialize(
        // Safe to access appId here since we do a null-check for appId earlier in the function and
        // return if it's null.
        appId!!,
        context,
        object : VungleInitializationListener {
          override fun onInitializeSuccess() {
            val adConfig = vungleFactory.createAdConfig()
            if (mediationExtras.containsKey(VungleConstants.KEY_ORIENTATION)) {
              adConfig.adOrientation =
                mediationExtras.getInt(VungleConstants.KEY_ORIENTATION, AdConfig.AUTO_ROTATE)
            }
            maybeAddWatermarkToVungleAdConfig(adConfig, mediationAppOpenAdConfiguration)
            // Note: Safe to access placement here since we do a null-check for placement earlier in
            // the function and return if it's null.
            appOpenAd = vungleFactory.createInterstitialAd(context, placement!!, adConfig)
            appOpenAd.adListener = this@VungleAppOpenAd
            appOpenAd.load(getAdMarkup(mediationAppOpenAdConfiguration))
          }

          override fun onInitializeError(error: AdError) {
            Log.w(TAG, error.toString())
            mediationAdLoadCallback.onFailure(error)
          }
        },
      )
  }

  /** Gets ad markup that needs to be passed in when loading Liftoff's app open ad. */
  abstract fun getAdMarkup(
    mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration
  ): String?

  /** If needed, adds watermark to Liftoff's (fka Vungle) ad config. */
  abstract fun maybeAddWatermarkToVungleAdConfig(
    adConfig: AdConfig,
    mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
  )

  override fun showAd(context: Context) {
    if (appOpenAd.canPlayAd()) {
      appOpenAd.play(context)
    } else {
      val error =
        AdError(
          ERROR_CANNOT_PLAY_AD,
          "Failed to show app open ad from Liftoff Monetize.",
          ERROR_DOMAIN,
        )
      Log.w(TAG, error.toString())
      mediationAppOpenAdCallback?.onAdFailedToShow(error)
    }
  }

  override fun onAdLoaded(baseAd: BaseAd) {
    mediationAppOpenAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onAdStart(baseAd: BaseAd) {
    if (mediationAppOpenAdCallback != null) {
      mediationAppOpenAdCallback?.onAdOpened()
    }
  }

  override fun onAdEnd(baseAd: BaseAd) {
    if (mediationAppOpenAdCallback != null) {
      mediationAppOpenAdCallback?.onAdClosed()
    }
  }

  override fun onAdClicked(baseAd: BaseAd) {
    if (mediationAppOpenAdCallback != null) {
      mediationAppOpenAdCallback?.reportAdClicked()
    }
  }

  override fun onAdLeftApplication(baseAd: BaseAd) {}

  override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
    val error = VungleMediationAdapter.getAdError(adError)
    Log.w(TAG, error.toString())
    if (mediationAppOpenAdCallback != null) {
      mediationAppOpenAdCallback?.onAdFailedToShow(error)
    }
  }

  override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
    val error = VungleMediationAdapter.getAdError(adError)
    Log.w(TAG, error.toString())
    mediationAdLoadCallback.onFailure(error)
  }

  override fun onAdImpression(baseAd: BaseAd) {
    if (mediationAppOpenAdCallback != null) {
      mediationAppOpenAdCallback?.reportAdImpression()
    }
  }
}
