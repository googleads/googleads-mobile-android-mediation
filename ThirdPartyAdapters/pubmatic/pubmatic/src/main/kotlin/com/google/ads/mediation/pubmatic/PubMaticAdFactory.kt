package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial

/** Interface for factory to create PubMatic ad objects. */
interface PubMaticAdFactory {

  fun createPOBInterstitial(context: Context): POBInterstitial
}
