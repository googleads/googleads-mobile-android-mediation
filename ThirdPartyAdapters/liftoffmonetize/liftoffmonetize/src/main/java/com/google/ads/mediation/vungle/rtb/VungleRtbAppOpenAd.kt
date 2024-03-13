package com.google.ads.mediation.vungle.rtb

import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.renderers.VungleAppOpenAd
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import com.vungle.ads.AdConfig

/**
 * Adapter object for adapting Liftoff's (fka Vungle) app open ad APIs for GMA SDK's RTB
 * implementation.
 */
class VungleRtbAppOpenAd(
  mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
  mediationAdLoadCallback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  vungleFactory: VungleFactory,
) : VungleAppOpenAd(mediationAppOpenAdConfiguration, mediationAdLoadCallback, vungleFactory) {

  override fun getAdMarkup(
    mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration
  ): String {
    return mediationAppOpenAdConfiguration.bidResponse
  }

  override fun maybeAddWatermarkToVungleAdConfig(
    adConfig: AdConfig,
    mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
  ) {
    // Should add watermark for RTB.
    val watermark: String = mediationAppOpenAdConfiguration.watermark
    if (watermark.isNotEmpty()) {
      adConfig.setWatermark(watermark)
    }
  }
}
