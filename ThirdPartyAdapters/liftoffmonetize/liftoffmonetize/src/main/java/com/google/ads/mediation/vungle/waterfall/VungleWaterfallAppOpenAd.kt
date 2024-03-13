package com.google.ads.mediation.vungle.waterfall

import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.renderers.VungleAppOpenAd
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import com.vungle.ads.AdConfig

/**
 * Adapter object for adapting Liftoff's (fka Vungle) app open ad APIs for GMA SDK's waterfall
 * implementation.
 */
class VungleWaterfallAppOpenAd(
  mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
  mediationAdLoadCallback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  vungleFactory: VungleFactory,
) : VungleAppOpenAd(mediationAppOpenAdConfiguration, mediationAdLoadCallback, vungleFactory) {

  override fun getAdMarkup(
    mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration
  ): String? {
    // No adMarkup for waterfall ads.
    return null
  }

  override fun maybeAddWatermarkToVungleAdConfig(
    adConfig: AdConfig,
    mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
  ) {
    // No need to add watermark for waterfall ads.
  }
}
