package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial
import com.pubmatic.sdk.rewardedad.POBRewardedAd

/**
 * Implementation of PubMaticAdFactory.
 *
 * Creates PubMatic ad objects.
 */
class PubMaticAdFactoryImpl : PubMaticAdFactory {
  override fun createPOBInterstitial(context: Context) = POBInterstitial(context)

  override fun createPOBRewardedAd(context: Context) = POBRewardedAd.getRewardedAd(context)

  override fun createPOBBannerView(context: Context) = POBBannerView(context)
}
