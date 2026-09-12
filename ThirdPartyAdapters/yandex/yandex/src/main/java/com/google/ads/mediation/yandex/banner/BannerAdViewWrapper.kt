package com.google.ads.mediation.yandex.banner

import android.view.View
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.yandex.mobile.ads.banner.BannerAdView

class BannerAdViewWrapper(private val bannerAdView: BannerAdView) : MediationBannerAd {
    override fun getView(): View {
        return bannerAdView.rootView
    }
}
