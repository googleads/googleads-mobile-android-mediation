package com.google.ads.mediation.yandex.interstitial

import android.content.Context
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader

internal class InterstitialLoaderFactory {
    fun create(context: Context) = InterstitialAdLoader(context)
}
