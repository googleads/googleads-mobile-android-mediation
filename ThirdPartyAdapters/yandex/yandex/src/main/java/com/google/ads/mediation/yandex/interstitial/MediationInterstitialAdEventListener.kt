package com.google.ads.mediation.yandex.interstitial

import com.google.ads.mediation.yandex.base.AdMobAdErrorCreator
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener

internal class MediationInterstitialAdEventListener(
        private val interstitialAdCallback: MediationInterstitialAdCallback,
        private val errorConverter: AdMobAdErrorCreator
) : InterstitialAdEventListener {

    override fun onAdClicked() {
        interstitialAdCallback.reportAdClicked()
    }

    override fun onAdImpression(impressionData: ImpressionData?) {
        interstitialAdCallback.reportAdImpression()
    }

    override fun onAdDismissed() {
        interstitialAdCallback.onAdClosed()
    }

    override fun onAdShown() {
        interstitialAdCallback.onAdOpened()
    }

    override fun onAdFailedToShow(adError: AdError) {
        interstitialAdCallback.onAdFailedToShow(errorConverter.convertToAdMobError(adError))
    }
}
