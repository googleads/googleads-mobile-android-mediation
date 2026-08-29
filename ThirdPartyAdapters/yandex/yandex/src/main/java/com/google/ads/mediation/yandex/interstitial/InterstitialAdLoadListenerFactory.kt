package com.google.ads.mediation.yandex.interstitial

import com.google.ads.mediation.yandex.base.AdMobAdErrorCreator
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener

internal class InterstitialAdLoadListenerFactory {

    fun create(
            callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
            adMobAdErrorCreator: AdMobAdErrorCreator,
            mediationInterstitialAdFactory: MediationInterstitialAdFactory = MediationInterstitialAdFactory()
    ) = object : InterstitialAdLoadListener {

        var interstitialAdCallback: MediationInterstitialAdCallback? = null

        override fun onAdLoaded(loadedAd: InterstitialAd) {
            val mediationInterstitialAd = mediationInterstitialAdFactory.create(
                    loadedAd, ::onContextIsNotActivityError
            )
            interstitialAdCallback = callback.onSuccess(mediationInterstitialAd).also { interstitialAdCallback ->
                val mediationInterstitialAdEventListener = MediationInterstitialAdEventListener(
                        interstitialAdCallback,
                        adMobAdErrorCreator
                )
                loadedAd.setAdEventListener(mediationInterstitialAdEventListener)
            }
        }

        override fun onAdFailedToLoad(error: AdRequestError) {
            val adError = adMobAdErrorCreator.createLoadAdError(error)
            callback.onFailure(adError)
        }

        private fun onContextIsNotActivityError() {
            val error = adMobAdErrorCreator.createRequiresActivityError()
            interstitialAdCallback?.onAdFailedToShow(error)
        }
    }
}
