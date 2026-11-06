package com.google.ads.mediation.yandex.banner

import com.google.ads.mediation.yandex.base.AdMobAdErrorCreator
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData

internal class MediationAdEventListener(
        private val mediationBannerAd: MediationBannerAd,
        private val mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
        private val adMobAdErrorCreator: AdMobAdErrorCreator
) : BannerAdEventListener {

    private var bannerAdCallback: MediationBannerAdCallback? = null

    override fun onImpression(impressionData: ImpressionData?) = Unit

    override fun onAdLoaded() {
        bannerAdCallback = mediationAdLoadCallback.onSuccess(mediationBannerAd)
    }

    override fun onAdFailedToLoad(adRequestError: AdRequestError) {
        val adError = adMobAdErrorCreator.createLoadAdError(adRequestError)
        mediationAdLoadCallback.onFailure(adError)
    }

    override fun onAdClicked() {
        bannerAdCallback?.reportAdClicked()
    }

    override fun onLeftApplication() {
        bannerAdCallback?.onAdLeftApplication()
    }

    override fun onReturnedToApplication() {
        bannerAdCallback?.onAdClosed()
    }
}
