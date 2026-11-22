package com.google.ads.mediation.yandex.base

import com.google.android.gms.ads.MobileAds
import com.google.ads.mediation.yandex.BuildConfig

internal class AdMobAdRequestParametersProvider {

    private val adapterVersion: String
        get() = BuildConfig.ADAPTER_VERSION

    fun getAdRequestParameters(): Map<String, String> {
        val adRequestParameters: MutableMap<String, String> = HashMap()
        adRequestParameters[ADAPTER_NETWORK_NAME_KEY] = ADAPTER_NETWORK_NAME
        appendVersionParameters(adRequestParameters)

        return adRequestParameters
    }

    private fun getAdMobSdkVersion(): String? {
        return try {
            MobileAds.getVersion().toString()
        } catch (ignored: Throwable) {
            null
        }
    }

    private fun appendVersionParameters(adRequestParameters: MutableMap<String, String>) {
        val adMobSdkVersion = getAdMobSdkVersion()
        if (adMobSdkVersion != null) {
            adRequestParameters[ADAPTER_NETWORK_SDK_VERSION_KEY] = adMobSdkVersion
        }
        adRequestParameters[ADAPTER_VERSION_KEY] = adapterVersion
    }

    companion object {
        private const val ADAPTER_VERSION_KEY = "adapter_version"
        private const val ADAPTER_NETWORK_NAME_KEY = "adapter_network_name"
        private const val ADAPTER_NETWORK_SDK_VERSION_KEY = "adapter_network_sdk_version"
        private const val ADAPTER_NETWORK_NAME = "admob"
    }
}
