package com.google.ads.mediation.vungle

import android.content.Context
import com.vungle.ads.BannerAd
import com.vungle.ads.BannerAdSize

/** Factory for creating Vungle ad objects. */
class VungleFactory {

  fun createBannerAd(context: Context, placementId: String, adSize: BannerAdSize): BannerAd =
    BannerAd(context, placementId, adSize)
}
