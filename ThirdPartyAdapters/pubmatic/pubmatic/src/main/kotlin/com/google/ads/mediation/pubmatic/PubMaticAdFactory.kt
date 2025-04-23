package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial
import com.pubmatic.sdk.rewardedad.POBRewardedAd

/** Interface for factory to create PubMatic ad objects. */
interface PubMaticAdFactory {

  fun createPOBInterstitial(context: Context): POBInterstitial

  fun createPOBRewardedAd(context: Context): POBRewardedAd

  fun createPOBBannerView(context: Context): POBBannerView
}
