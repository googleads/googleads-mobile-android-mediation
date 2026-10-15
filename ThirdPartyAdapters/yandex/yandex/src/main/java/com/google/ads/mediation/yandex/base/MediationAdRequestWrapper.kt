package com.google.ads.mediation.yandex.base

import com.google.android.gms.ads.mediation.MediationAdConfiguration

class MediationAdRequestWrapper(
        private val mediationBannerAdConfiguration: MediationAdConfiguration
) {
    val keywords: Set<String?>?
        get() = mediationBannerAdConfiguration.mediationExtras.keySet()

    val taggedForChildDirectedTreatment: Int
        get() = mediationBannerAdConfiguration.taggedForChildDirectedTreatment()
}
