package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.openwrap.core.signal.POBSignalConfig
import com.pubmatic.sdk.openwrap.core.signal.POBSignalGenerator

/**
 * Implements [PubMaticSignalGenerator] interface.
 *
 * Invokes PubMatic SDK's function for signal generation.
 */
class PubMaticSignalGeneratorImpl : PubMaticSignalGenerator {

  override fun generateSignal(
    context: Context,
    biddingHost: POBBiddingHost,
    pobSignalConfig: POBSignalConfig,
  ): String = POBSignalGenerator.generateSignal(context, biddingHost, pobSignalConfig)
}
