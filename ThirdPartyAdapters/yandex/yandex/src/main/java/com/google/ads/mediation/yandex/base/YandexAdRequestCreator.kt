package com.google.ads.mediation.yandex.base

import com.google.android.gms.ads.RequestConfiguration
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration

class YandexAdRequestCreator {

    private val adRequestParametersProvider: AdMobAdRequestParametersProvider = AdMobAdRequestParametersProvider()

    fun createAdRequest(mediationAdRequest: MediationAdRequestWrapper): AdRequest {
        val adRequestParameters = adRequestParametersProvider.getAdRequestParameters()
        val adRequestBuilder = AdRequest.Builder()
        adRequestBuilder.setParameters(adRequestParameters)

        setAgeRestrictedUser(mediationAdRequest)

        val mediationKeywords = mediationAdRequest.keywords
        if (mediationKeywords != null) {
            val keywords = ArrayList(mediationKeywords)
            adRequestBuilder.setContextTags(keywords)
        }

        return adRequestBuilder.build()
    }

    fun createAdRequest(): AdRequest {
        val adRequestParameters = adRequestParametersProvider.getAdRequestParameters()
        val adRequestBuilder = AdRequest.Builder()
        adRequestBuilder.setParameters(adRequestParameters)

        return adRequestBuilder.build()
    }

    fun createNativeAdRequestConfiguration(
            adRequestWrapper: MediationAdRequestWrapper,
            adUnitId: String
    ): NativeAdRequestConfiguration {
        val adRequestParameters = adRequestParametersProvider.getAdRequestParameters()
        val adRequestConfigurationBuilder = NativeAdRequestConfiguration.Builder(adUnitId)
        adRequestConfigurationBuilder.setParameters(adRequestParameters)

        val mediationKeywords = adRequestWrapper.keywords
        if (mediationKeywords != null) {
            val keywords = ArrayList(mediationKeywords)
            adRequestConfigurationBuilder.setContextTags(keywords)
        }

        return adRequestConfigurationBuilder.build()
    }

    private fun setAgeRestrictedUser(adRequestWrapper: MediationAdRequestWrapper) {
        val tagForChildDirectedTreatment = adRequestWrapper.taggedForChildDirectedTreatment

        if (tagForChildDirectedTreatment == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
            MobileAds.setAgeRestrictedUser(true)
        } else if (tagForChildDirectedTreatment == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
            MobileAds.setAgeRestrictedUser(false)
        }
    }
}
