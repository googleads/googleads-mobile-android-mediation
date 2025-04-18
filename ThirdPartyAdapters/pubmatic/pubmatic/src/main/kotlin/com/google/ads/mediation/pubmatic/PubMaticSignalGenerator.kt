package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.openwrap.core.signal.POBSignalConfig

/** Interface for PubMatic signal generation. */
interface PubMaticSignalGenerator {
  fun generateSignal(
    context: Context,
    biddingHost: POBBiddingHost,
    pobSignalConfig: POBSignalConfig,
  ): String
}
