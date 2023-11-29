package com.google.ads.mediation.yahoo

import android.content.Context
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.yahoo.ads.inlineplacement.AdSize
import com.yahoo.ads.inlineplacement.InlineAdView
import com.yahoo.ads.inlineplacement.InlinePlacementConfig
import com.yahoo.ads.interstitialplacement.InterstitialAd
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig
import com.yahoo.ads.nativeplacement.NativeAd
import com.yahoo.ads.nativeplacement.NativePlacementConfig
import java.util.Collections

/** A factory for creating Yahoo Ads SDK objects. */
class YahooFactory {

  fun createInterstitialAd(
    context: Context,
    placementId: String,
    renderer: InterstitialAd.InterstitialAdListener
  ): InterstitialAd {
    return InterstitialAd(context, placementId, renderer)
  }

  fun createNativeAd(
    context: Context,
    placementId: String,
    renderer: NativeAd.NativeAdListener
  ): NativeAd {
    return NativeAd(context, placementId, renderer)
  }

  fun createInlineAd(
    context: Context,
    placementId: String,
    renderer: InlineAdView.InlineAdListener
  ): InlineAdView {
    return InlineAdView(context, placementId, renderer)
  }

  fun createInterstitialPlacementConfig(
    placementId: String,
    mediationAdRequest: MediationAdRequest
  ): InterstitialPlacementConfig {
    return InterstitialPlacementConfig(
      placementId,
      YahooAdapterUtils.getRequestMetadata(mediationAdRequest)
    )
  }

  fun createRewardedPlacementConfig(
    placementId: String,
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration
  ): InterstitialPlacementConfig {
    return InterstitialPlacementConfig(
      placementId,
      YahooAdapterUtils.getRequestMetaData(mediationRewardedAdConfiguration)
    )
  }

  fun createNativePlacementConfig(
    placementId: String,
    mediationAdRequest: MediationAdRequest,
    adTypes: Array<String>
  ): NativePlacementConfig {
    return NativePlacementConfig(
      placementId,
      YahooAdapterUtils.getRequestMetadata(mediationAdRequest),
      adTypes
    )
  }

  fun createInlinePlacementConfig(
    placementId: String,
    mediationAdRequest: MediationAdRequest,
    yahooAdSize: AdSize
  ): InlinePlacementConfig {
    return InlinePlacementConfig(
      placementId,
      YahooAdapterUtils.getRequestMetadata(mediationAdRequest),
      Collections.singletonList(yahooAdSize)
    )
  }

  fun createYahooAdSize(normalizedSize: com.google.android.gms.ads.AdSize): AdSize {
    return AdSize(/* width= */ normalizedSize.getWidth(), /* height= */ normalizedSize.getHeight())
  }
}
