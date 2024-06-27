package com.google.ads.mediation.vungle

import android.content.Context
import com.vungle.ads.AdConfig
import com.vungle.ads.InterstitialAd
import com.vungle.ads.NativeAd
import com.vungle.ads.RewardedAd
import com.vungle.ads.VungleAdSize
import com.vungle.ads.VungleBannerView

/** Factory for creating Vungle ad objects. */
class VungleFactory {

  fun createBannerAd(context: Context, placementId: String, adSize: VungleAdSize): VungleBannerView =
    VungleBannerView(context, placementId, adSize)

  fun createInterstitialAd(context: Context, placementId: String, adConfig: AdConfig) =
    InterstitialAd(context, placementId, adConfig)

  fun createRewardedAd(context: Context, placementId: String, adConfig: AdConfig) =
    RewardedAd(context, placementId, adConfig)

  fun createNativeAd(context: Context, placementId: String) = NativeAd(context, placementId)

  fun createAdConfig() = AdConfig()
}
