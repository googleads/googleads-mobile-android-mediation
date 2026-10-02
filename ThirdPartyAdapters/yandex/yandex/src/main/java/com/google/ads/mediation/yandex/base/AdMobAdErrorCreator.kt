package com.google.ads.mediation.yandex.base

import com.google.android.gms.ads.AdRequest
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import com.google.android.gms.ads.AdError as AdMobError

class AdMobAdErrorCreator {

    private val yandexErrorConverter: YandexErrorConverter = YandexErrorConverter()

    fun createLoadAdError(code: Int): AdMobError {
        return AdMobError(code, FAILED_TO_LOAD_AD_MESSAGE, YANDEX_MOBILE_ADS_DOMAIN)
    }

    fun createLoadAdError(adRequestError: AdRequestError?): AdMobError {
        return if (adRequestError != null) {
            val code = yandexErrorConverter.convertToAdMobErrorCode(adRequestError)
            AdMobError(code, adRequestError.description, YANDEX_MOBILE_ADS_DOMAIN)
        } else {
            AdMobError(
                    AdRequest.ERROR_CODE_INVALID_REQUEST,
                    FAILED_TO_LOAD_AD_MESSAGE,
                    YANDEX_MOBILE_ADS_DOMAIN,
                    null
            )
        }
    }

    fun createRequiresActivityError() = AdMobError(
            yandexErrorConverter.convertToAdMobErrorCode(null),
            REQUIRES_ACTIVITY_MESSAGE,
            YANDEX_MOBILE_ADS_DOMAIN
    )

    fun convertToAdMobError(adError: AdError): AdMobError {
        return AdMobError(
                yandexErrorConverter.convertToAdMobErrorCode(null),
                adError.description,
                YANDEX_MOBILE_ADS_DOMAIN
        )
    }

    companion object {

        private const val REQUIRES_ACTIVITY_MESSAGE =
                "Yandex Mobile Ads SDK requires an Activity context to show an ad."
        private const val FAILED_TO_LOAD_AD_MESSAGE = "Failed to load ad"
        private const val YANDEX_MOBILE_ADS_DOMAIN = "com.yandex.mobile.ads"
    }
}
