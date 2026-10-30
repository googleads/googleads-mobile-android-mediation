package com.google.ads.mediation.yandex.base

import com.google.android.gms.ads.VersionInfo
import com.google.ads.mediation.yandex.BuildConfig;
import com.yandex.mobile.ads.common.MobileAds

class YandexVersionInfoProvider {

    val adapterVersion: String
        get() = BuildConfig.ADAPTER_VERSION

    val adapterVersionInfo: VersionInfo
        get() = createVersionInfo(adapterVersion)

    fun getSdkVersionInfo(): VersionInfo {
        val sdkVersionString = MobileAds.getLibraryVersion()
        val normalizedSdkVersionString = sdkVersionString ?: ""
        return createVersionInfo(normalizedSdkVersionString)
    }

    private fun createVersionInfo(version: String): VersionInfo {
        val splitVersion = version.split(VERSION_SPLIT_REGEX).toTypedArray()

        val numbers = splitVersion.map { parseInt(it) }

        val versionInfo: VersionInfo = if (numbers.size >= SEMANTIC_VERSION_LENGTH) {
            VersionInfo(numbers[0], numbers[1], numbers[2])
        } else {
            VersionInfo(0, 0, 0)
        }

        return versionInfo
    }

    private fun parseInt(value: String): Int {
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            DEFAULT_PARSE_VALUE
        }
    }

    companion object {
        private const val SEMANTIC_VERSION_LENGTH = 3
        private const val DEFAULT_PARSE_VALUE = 0
        private const val VERSION_SPLIT_REGEX = "."
    }
}
