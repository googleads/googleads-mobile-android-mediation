/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads.view;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.ads.mediation.yandex.nativeads.asset.YandexNativeAdAsset;
import com.google.android.gms.ads.nativead.NativeAdView;

public class YandexAdAssetViewsProviderCreator {

    @NonNull
    private final YandexNativeAdViewsFinder mViewsFinder;

    public YandexAdAssetViewsProviderCreator(@NonNull final Bundle extras) {
        mViewsFinder = new YandexNativeAdViewsFinder(extras);
    }

    @NonNull
    public YandexNativeAssetViewsProvider createProvider(@NonNull final NativeAdView nativeAdView) {
        final TextView bodyView = ViewUtils.castView(nativeAdView.getBodyView(), TextView.class);
        final TextView callToActionView = ViewUtils.castView(nativeAdView.getCallToActionView(), TextView.class);
        final TextView domainView = ViewUtils.castView(nativeAdView.getStoreView(), TextView.class);
        final ImageView iconView = ViewUtils.castView(nativeAdView.getIconView(), ImageView.class);
        final TextView priceView = ViewUtils.castView(nativeAdView.getPriceView(), TextView.class);
        final TextView sponsoredView = ViewUtils.castView(nativeAdView.getAdvertiserView(), TextView.class);
        final TextView titleView = ViewUtils.castView(nativeAdView.getHeadlineView(), TextView.class);

        YandexNativeAssetViewsProvider.Builder builder = createBuilderWithYandexSupportedViews(nativeAdView);
        builder = builder.setBodyView(bodyView)
                .setCallToActionView(callToActionView)
                .setDomainView(domainView)
                .setIconView(iconView)
                .setPriceView(priceView)
                .setSponsoredView(sponsoredView)
                .setTitleView(titleView);

        return builder.build();
    }

    @NonNull
    private YandexNativeAssetViewsProvider.Builder createBuilderWithYandexSupportedViews(
            @NonNull final View nativeAdView) {
        final View ageView = mViewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.AGE);
        final View reviewCountView = mViewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.REVIEW_COUNT);
        final View warningView = mViewsFinder.findViewByExtraKey(nativeAdView, YandexNativeAdAsset.WARNING);

        return new YandexNativeAssetViewsProvider.Builder()
                .setAgeView(ViewUtils.castView(ageView, TextView.class))
                .setRatingView(mViewsFinder.findRatingView(nativeAdView))
                .setReviewCountView(ViewUtils.castView(reviewCountView, TextView.class))
                .setWarningView(ViewUtils.castView(warningView, TextView.class));
    }
}
