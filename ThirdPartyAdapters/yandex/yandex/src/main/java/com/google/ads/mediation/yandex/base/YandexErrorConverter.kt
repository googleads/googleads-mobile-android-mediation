package com.google.ads.mediation.yandex.base

import com.google.android.gms.ads.AdRequest
import com.yandex.mobile.ads.common.AdRequestError

class YandexErrorConverter {

    fun convertToAdMobErrorCode(adRequestError: AdRequestError?): Int {

        return when (adRequestError?.code) {

            AdRequestError.Code.INTERNAL_ERROR,
            AdRequestError.Code.SYSTEM_ERROR -> AdRequest.ERROR_CODE_INTERNAL_ERROR

            AdRequestError.Code.INVALID_REQUEST -> AdRequest.ERROR_CODE_INVALID_REQUEST

            AdRequestError.Code.NETWORK_ERROR -> AdRequest.ERROR_CODE_NETWORK_ERROR

            AdRequestError.Code.NO_FILL -> AdRequest.ERROR_CODE_NO_FILL

            else -> AdRequest.ERROR_CODE_INTERNAL_ERROR
        }
    }

    fun convertToInvalidRequestError(message: String?): AdRequestError {
        return AdRequestError(AdRequestError.Code.INVALID_REQUEST, message)
    }
}
