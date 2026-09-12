package com.yandex.mobile.ads.nativeads

import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.ads.mediation.yandex.nativeads.view.YandexAdAssetViewsProviderCreator
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdView

internal class YandexNativeAdMapper(
        private val nativeAd: NativeAd,
        extras: Bundle,
        private val nativeAdViewBinderBuilderFactory: NativeAdViewBinderBuilderFactory = NativeAdViewBinderBuilderFactory(),
        private val assetViewsProviderCreator: YandexAdAssetViewsProviderCreator = YandexAdAssetViewsProviderCreator(extras)
) : UnifiedNativeAdMapper() {

    private var mediaView: MediaView? = null

    override fun trackViews(
            view: View,
            clickableAssetViews: Map<String, View>,
            nonclickableAssetViews: Map<String, View>
    ) {
        super.trackViews(view, clickableAssetViews, nonclickableAssetViews)

        if (view is NativeAdView) {
            try {
                val provider = assetViewsProviderCreator.createProvider(view)

                val binder = nativeAdViewBinderBuilderFactory.create(view)
                        .setAgeView(provider.ageView)
                        .setBodyView(provider.bodyView)
                        .setCallToActionView(provider.callToActionView)
                        .setDomainView(provider.domainView)
                        .setFaviconView(provider.faviconView)
                        .setFeedbackView(provider.feedbackView)
                        .setIconView(provider.iconView)
                        .setMediaView(mediaView)
                        .setPriceView(provider.priceView)
                        .setRatingView(provider.getRatingView())
                        .setReviewCountView(provider.reviewCountView)
                        .setSponsoredView(provider.sponsoredView)
                        .setTitleView(provider.titleView)
                        .setWarningView(provider.warningView)
                        .build()

                nativeAd.bindNativeAd(binder)
            } catch (e: Exception) {
                Log.w(TAG, "Error while binding native ad. $e")
            }
        } else {
            Log.w(TAG, "Invalid view type")
        }
    }

    override fun setMediaView(view: View) {
        super.setMediaView(view)
        if (view is MediaView) {
            mediaView = view
        }
    }

    companion object {
        private const val TAG = "Yandex AdMob Adapter"
    }
}
