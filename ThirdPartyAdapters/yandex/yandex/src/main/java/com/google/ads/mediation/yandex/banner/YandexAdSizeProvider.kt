package com.google.ads.mediation.yandex.banner

import android.content.Context
import com.google.ads.mediation.yandex.base.AdMobServerExtrasParser
import com.yandex.mobile.ads.banner.BannerAdSize
import com.google.android.gms.ads.AdSize as GoogleAdSize

internal class YandexAdSizeProvider {

    fun getAdSize(
            context: Context,
            serverExtrasParser: AdMobServerExtrasParser,
            adMobAdSize: GoogleAdSize?
    ): BannerAdSize? {
        var adSize = serverExtrasParser.parseAdSize(context)

        if (adSize == null) {
            adSize = getAdSizeFromAdMobAdSize(context, adMobAdSize)
        }

        return adSize
    }

    private fun getAdSizeFromAdMobAdSize(context: Context, adMobAdSize: GoogleAdSize?): BannerAdSize? {
        return adMobAdSize?.let {
            BannerAdSize.fixedSize(context, adMobAdSize.width, adMobAdSize.height)
        }
    }
}
