package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial

/**
 * Implementation of PubMaticAdFactory.
 *
 * Creates PubMatic ad objects.
 */
class PubMaticAdFactoryImpl : PubMaticAdFactory {
  override fun createPOBInterstitial(context: Context) = POBInterstitial(context)
}
