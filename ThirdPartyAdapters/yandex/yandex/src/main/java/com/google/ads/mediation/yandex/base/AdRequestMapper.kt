package com.google.ads.mediation.yandex.base

import com.yandex.mobile.ads.common.AdRequestConfiguration

internal class AdRequestMapper(
        private val adRequestParametersProvider: AdMobAdRequestParametersProvider = AdMobAdRequestParametersProvider()
) {

    fun toAdRequestConfiguration(parser: AdMobServerExtrasParser): AdRequestConfiguration? {
        val adUnitId = parser.parseAdUnitId()
                ?.takeIf { it.isNotBlank() }
                ?: return null

        val params = adRequestParametersProvider.getAdRequestParameters()
        return AdRequestConfiguration.Builder(adUnitId)
                .setParameters(params)
                .build()
    }
}
