package com.google.ads.mediation.vungle

import android.content.Context
import com.vungle.ads.AdConfig
import com.vungle.ads.BannerAd
import com.vungle.ads.BannerAdSize
import com.vungle.ads.InterstitialAd

/** Factory for creating Vungle ad objects. */
class VungleFactory {

  fun createBannerAd(context: Context, placementId: String, adSize: BannerAdSize): BannerAd =
    BannerAd(context, placementId, adSize)

  fun createInterstitialAd(context: Context, placementId: String, adConfig: AdConfig) =
    InterstitialAd(context, placementId, adConfig)

  fun createAdConfig() = AdConfig()
}
