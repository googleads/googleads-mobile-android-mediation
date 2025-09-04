package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.common.POBAdSize
import com.pubmatic.sdk.nativead.POBNativeAdLoader
import com.pubmatic.sdk.nativead.datatype.POBNativeTemplateType
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

  override fun createPOBInterstitial(
    context: Context,
    pubId: String,
    profileId: Int,
    adUnit: String,
  ) = POBInterstitial(context, pubId, profileId, adUnit)

  override fun createPOBRewardedAd(context: Context) = POBRewardedAd.getRewardedAd(context)

  override fun createPOBRewardedAd(
    context: Context,
    pubId: String,
    profileId: Int,
    adUnit: String,
  ) = POBRewardedAd.getRewardedAd(context, pubId, profileId, adUnit)

  override fun createPOBBannerView(context: Context) = POBBannerView(context)

  override fun createPOBBannerView(
    context: Context,
    pubId: String,
    profileId: Int,
    adUnit: String,
    pobAdSize: POBAdSize,
  ) = POBBannerView(context, pubId, profileId, adUnit, pobAdSize)

  override fun createPOBNativeAdLoader(context: Context) = POBNativeAdLoader(context)

  override fun createPOBNativeAdLoader(
    context: Context,
    pubId: String,
    profileId: Int,
    adUnit: String,
  ) = POBNativeAdLoader(context, pubId, profileId, adUnit, POBNativeTemplateType.CUSTOM)
}
