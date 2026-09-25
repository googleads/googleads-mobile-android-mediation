package com.google.ads.mediation.yandex.nativeads.view

import android.os.Bundle
import android.view.View
import com.google.ads.mediation.yandex.nativeads.YandexNativeAdAsset
import com.yandex.mobile.ads.nativeads.Rating

internal class YandexNativeAdViewsFinder(private val extras: Bundle) {

    fun findViewByExtraKey(
            nativeAdView: View,
            extraKey: String
    ): View? {
        return if (extras.containsKey(extraKey)) {
            nativeAdView.findViewById(extras.getInt(extraKey))
        } else {
            null
        }
    }

    fun <T> findRatingView(nativeAdView: View): T? where T : View?, T : Rating? {
        try {
            val view = findViewByExtraKey(nativeAdView, YandexNativeAdAsset.RATING)
            return view as? Rating as? T
        } catch (ignored: Exception) {
        }

        return null
    }
}
