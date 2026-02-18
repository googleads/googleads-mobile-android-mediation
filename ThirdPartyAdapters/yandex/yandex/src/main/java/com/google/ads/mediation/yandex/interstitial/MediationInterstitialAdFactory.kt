package com.google.ads.mediation.yandex.interstitial

import android.app.Activity
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAd

internal class MediationInterstitialAdFactory {

    fun create(
            loadedAd: InterstitialAd,
            onContextIsNotActivity: () -> Unit
    ) = MediationInterstitialAd { context ->
        if (context is Activity) {
            loadedAd.show(context)
        } else onContextIsNotActivity()
    }
}
