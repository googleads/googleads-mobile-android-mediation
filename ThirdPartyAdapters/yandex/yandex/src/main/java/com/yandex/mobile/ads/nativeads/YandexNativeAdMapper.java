/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.yandex.mobile.ads.nativeads;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.yandex.nativeads.view.YandexAdAssetViewsProviderCreator;
import com.google.ads.mediation.yandex.nativeads.view.YandexNativeAssetViewsProvider;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.Map;

public class YandexNativeAdMapper extends UnifiedNativeAdMapper {

    private static final String TAG = "Yandex AdMob Adapter";

    @NonNull
    private final YandexAdAssetViewsProviderCreator mAssetViewsProviderCreator;

    @Nullable
    private final TextView mFeedbackView;

    @NonNull
    private final NativeAd mNativeAd;

    @Nullable
    private MediaView mMediaView;

    YandexNativeAdMapper(@NonNull final NativeAd nativeAd,
                         @NonNull final Bundle extras,
                         @Nullable final TextView feedbackView) {
        mNativeAd = nativeAd;
        mAssetViewsProviderCreator = new YandexAdAssetViewsProviderCreator(extras);
        mFeedbackView = feedbackView;
    }

    @Override
    public void trackViews(@Nullable final View view,
                           @Nullable final Map<String, View> clickableAssetViews,
                           @Nullable final Map<String, View> nonclickableAssetViews) {
        super.trackViews(view, clickableAssetViews, nonclickableAssetViews);
        if (view instanceof NativeAdView) {
            final NativeAdView nativeAdView = (NativeAdView) view;

            try {
                final YandexNativeAssetViewsProvider provider =
                        mAssetViewsProviderCreator.createProvider(nativeAdView);

                final NativeAdViewBinder binder = new NativeAdViewBinder.Builder(view)
                        .setAgeView(provider.getAgeView())
                        .setBodyView(provider.getBodyView())
                        .setCallToActionView(provider.getCallToActionView())
                        .setDomainView(provider.getDomainView())
                        .setFeedbackView(mFeedbackView)
                        .setIconView(provider.getIconView())
                        .setMediaView(mMediaView)
                        .setPriceView(provider.getPriceView())
                        .setRatingView(provider.getRatingView())
                        .setReviewCountView(provider.getReviewCountView())
                        .setSponsoredView(provider.getSponsoredView())
                        .setTitleView(provider.getTitleView())
                        .setWarningView(provider.getWarningView())
                        .build();

                mNativeAd.bindNativeAd(binder);
            } catch (final Exception e) {
                Log.w(TAG, "Error while binding native ad. " + e);
            }
        } else {
            Log.w(TAG, "Invalid view type");
        }
    }

    @Override
    public void setMediaView(@Nullable final View view) {
        super.setMediaView(view);
        if (view instanceof MediaView) {
            mMediaView = (MediaView) view;
        }
    }
}