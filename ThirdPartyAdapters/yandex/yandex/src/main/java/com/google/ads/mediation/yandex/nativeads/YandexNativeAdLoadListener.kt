package com.google.ads.mediation.yandex.nativeads

import android.content.Context
import android.os.Bundle
import com.google.ads.mediation.yandex.base.AdMobAdErrorCreator
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.YandexNativeAdMappersFactory
import java.lang.ref.WeakReference

internal class YandexNativeAdLoadListener @JvmOverloads constructor(
        context: Context,
        private val callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
        private val extras: Bundle?,
        private val adMappersFactory: YandexNativeAdMappersFactory = YandexNativeAdMappersFactory(),
        private val adMobAdErrorCreator: AdMobAdErrorCreator = AdMobAdErrorCreator()
) : NativeAdLoadListener {

    private val contextReference: WeakReference<Context> = WeakReference(context)

    private var admobNativeListener: MediationNativeAdCallback? = null

    private var nativeAd: NativeAd? = null

    override fun onAdFailedToLoad(error: AdRequestError) {
        val adError = adMobAdErrorCreator.createLoadAdError(error)
        callback.onFailure(adError)
    }

    override fun onAdLoaded(nativeAd: NativeAd) {
        finishWithAdMapper(nativeAd)
    }

    private fun finishWithAdMapper(nativeAd: NativeAd) {
        val context = contextReference.get()
        if (context != null) {
            val nativeAdMapper = adMappersFactory.createAdMapper(context, nativeAd, extras)
            admobNativeListener = callback.onSuccess(nativeAdMapper)
            configureNativeAd(nativeAd)
        } else {
            val adError = adMobAdErrorCreator.createLoadAdError(AdRequest.ERROR_CODE_INTERNAL_ERROR)
            callback.onFailure(adError)
        }
    }

    private fun configureNativeAd(nativeAd: NativeAd) {
        admobNativeListener?.let {
            nativeAd.setNativeAdEventListener(YandexNativeAdEventListener(it))
        }
        this.nativeAd = nativeAd
    }
}
