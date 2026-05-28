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

package com.google.ads.mediation.vungle.renderers

import android.content.Context
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
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.InterstitialAd
import com.vungle.ads.InterstitialAdListener
import com.vungle.ads.VungleError

/**
 * Abstract class with interstitial adapter logic common for both waterfall and RTB integrations.
 */
abstract class VungleInterstitialAd(
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val vungleFactory: VungleFactory,
) : MediationInterstitialAd, InterstitialAdListener {

  /** Liftoff's interstitial ad object. */
  private lateinit var interstitialAd: InterstitialAd
  private var mediationInterstitialAdCallback: MediationInterstitialAdCallback? = null

  /** Gets ad markup that needs to be passed in when loading Liftoff's interstitial ad. */
  abstract fun getAdMarkup(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration
  ): String?

  /** If needed, adds watermark to Liftoff's (fka Vungle) ad config. */
  abstract fun maybeAddWatermarkToVungleAdConfig(
    adConfig: AdConfig,
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
  )

  /** Loads an interstitial ad. */
  fun render(mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration) {
    val mediationExtras = mediationInterstitialAdConfiguration.mediationExtras
    val serverParameters = mediationInterstitialAdConfiguration.serverParameters

    val appID = serverParameters.getString(VungleConstants.KEY_APP_ID)

    if (appID.isNullOrEmpty()) {
      val error =
        AdError(
          ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load interstitial ad from Liftoff Monetize. " + "Missing or invalid App ID.",
          VungleMediationAdapter.ERROR_DOMAIN,
        )
      Log.w(TAG, error.toString())
      mediationAdLoadCallback.onFailure(error)
      return
    }

    val placement = serverParameters.getString(VungleConstants.KEY_PLACEMENT_ID)
    if (placement.isNullOrEmpty()) {
      val error =
        AdError(
          VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load interstitial ad from Liftoff Monetize. " +
            "Missing or Invalid Placement ID.",
          VungleMediationAdapter.ERROR_DOMAIN,
        )
      Log.w(TAG, error.toString())
      mediationAdLoadCallback.onFailure(error)
      return
    }

    val context = mediationInterstitialAdConfiguration.context

    VungleInitializer.getInstance()
      .initialize(
        appID,
        context,
        object : VungleInitializationListener {
          override fun onInitializeSuccess() {
            val adConfig = vungleFactory.createAdConfig()
            if (mediationExtras.containsKey(VungleConstants.KEY_ORIENTATION)) {
              adConfig.adOrientation =
                mediationExtras.getInt(VungleConstants.KEY_ORIENTATION, AdConfig.AUTO_ROTATE)
            }

            maybeAddWatermarkToVungleAdConfig(adConfig, mediationInterstitialAdConfiguration)
            interstitialAd = vungleFactory.createInterstitialAd(context, placement, adConfig)
            interstitialAd.adListener = this@VungleInterstitialAd
            interstitialAd.adapterAdFormat = "VungleInterstitialAd"
            val adMarkup = getAdMarkup(mediationInterstitialAdConfiguration)
            if (adMarkup != null) {
              interstitialAd.load(adMarkup)
            } else {
              interstitialAd.load()
            }
          }

          override fun onInitializeError(error: AdError) {
            Log.w(TAG, error.toString())
            mediationAdLoadCallback.onFailure(error)
          }
        },
      )
  }

  override fun showAd(context: Context) {
    if (interstitialAd == null) {
      val error =
        AdError(
          ERROR_CANNOT_PLAY_AD,
          "Failed to show interstitial ad from Liftoff Monetize.",
          ERROR_DOMAIN,
        )
      Log.w(TAG, error.toString())
      if (mediationInterstitialAdCallback != null) {
        mediationInterstitialAdCallback?.onAdFailedToShow(error)
      }
      return
    }

    interstitialAd.play(context)
  }

  /** Vungle SDK's InterstitialAdListener implementation */
  override fun onAdLoaded(baseAd: BaseAd) {
    mediationInterstitialAdCallback = mediationAdLoadCallback.onSuccess(this@VungleInterstitialAd)
  }

  override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
    val error = VungleMediationAdapter.getAdError(adError)
    Log.w(TAG, error.toString())
    mediationAdLoadCallback.onFailure(error)
  }

  override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
    val error = VungleMediationAdapter.getAdError(adError)
    Log.w(TAG, error.toString())
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback?.onAdFailedToShow(error)
    }
  }

  override fun onAdImpression(baseAd: BaseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback?.reportAdImpression()
    }
  }

  override fun onAdClicked(baseAd: BaseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback?.reportAdClicked()
    }
  }

  override fun onAdStart(baseAd: BaseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback?.onAdOpened()
    }
  }

  override fun onAdLeftApplication(baseAd: BaseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback?.onAdLeftApplication()
    }
  }

  override fun onAdEnd(baseAd: BaseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback?.onAdClosed()
    }
  }
}
