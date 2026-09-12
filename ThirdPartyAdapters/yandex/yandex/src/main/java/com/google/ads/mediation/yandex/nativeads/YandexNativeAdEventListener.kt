package com.google.ads.mediation.yandex.nativeads

import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.nativeads.NativeAdEventListener

internal class YandexNativeAdEventListener(
        private val adMobListener: MediationNativeAdCallback
) : NativeAdEventListener {

    override fun onReturnedToApplication() {
        adMobListener.onAdClosed()
    }

    override fun onAdClicked() {
        adMobListener.reportAdClicked()
    }

    override fun onLeftApplication() {
        adMobListener.onAdOpened()
        adMobListener.onAdLeftApplication()
    }

    override fun onImpression(impressionData: ImpressionData?) {
        adMobListener.reportAdImpression()
    }
}
