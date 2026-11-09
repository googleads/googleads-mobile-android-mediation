package com.google.ads.mediation.yandex.nativeads.view

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.ads.mediation.yandex.nativeads.YandexNativeAdAsset
import com.google.ads.mediation.yandex.nativeads.view.ViewUtils.castView
import com.google.android.gms.ads.nativead.NativeAdView

internal class YandexAdAssetViewsProviderCreator(
        extras: Bundle,
        private val viewsFinder: YandexNativeAdViewsFinder = YandexNativeAdViewsFinder(extras),
        private val providerBuilder: YandexNativeAssetViewsProvider.Builder =
                YandexNativeAssetViewsProvider.Builder()
) {

    fun createProvider(nativeAdView: NativeAdView): YandexNativeAssetViewsProvider {
        val bodyView = castView(nativeAdView.bodyView, TextView::class.java)
        val callToActionView = castView(nativeAdView.callToActionView, TextView::class.java)
        val domainView = castView(nativeAdView.storeView, TextView::class.java)
        val iconView = castView(nativeAdView.iconView, ImageView::class.java)
        val imageView = castView(nativeAdView.imageView, ImageView::class.java)
        val priceView = castView(nativeAdView.priceView, TextView::class.java)
        val sponsoredView = castView(nativeAdView.advertiserView, TextView::class.java)
        val titleView = castView(nativeAdView.headlineView, TextView::class.java)
        var builder = createBuilderWithYandexSupportedViews(nativeAdView)

        builder = builder.withBodyView(bodyView)
                .withCallToActionView(callToActionView)
                .withDomainView(domainView)
                .withIconView(iconView)
                .withImageView(imageView)
                .withPriceView(priceView)
                .withSponsoredView(sponsoredView)
                .withTitleView(titleView)

        return builder.build()
    }

    private fun createBuilderWithYandexSupportedViews(
            nativeAdView: View
    ): YandexNativeAssetViewsProvider.Builder {
        val ageView = viewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.AGE)
        val faviconView = viewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.FAVICON)
        val feedbackView = viewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.FEEDBACK)
        val reviewCountView = viewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.REVIEW_COUNT)
        val warningView = viewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.WARNING)

        return providerBuilder
                .withAgeView(castView(ageView, TextView::class.java))
                .withFaviconView(castView(faviconView, ImageView::class.java))
                .withFeedbackView(castView(feedbackView, ImageView::class.java))
                .withRatingView(viewsFinder.findRatingView(nativeAdView))
                .withReviewCountView(castView(reviewCountView, TextView::class.java))
                .withWarningView(castView(warningView, TextView::class.java))
    }
}
