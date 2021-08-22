/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads.view;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.nativeads.Rating;

public final class YandexNativeAssetViewsProvider {

    @Nullable
    private final TextView mAgeView;

    @Nullable
    private final TextView mBodyView;

    @Nullable
    private final TextView mCallToActionView;

    @Nullable
    private final TextView mDomainView;

    @Nullable
    private final ImageView mIconView;

    @Nullable
    private final TextView mPriceView;

    @Nullable
    private final View mRatingView;

    @Nullable
    private final TextView mReviewCountView;

    @Nullable
    private final TextView mSponsoredView;

    @Nullable
    private final TextView mTitleView;

    @Nullable
    private final TextView mWarningView;

    private YandexNativeAssetViewsProvider(@NonNull final Builder builder) {
        mAgeView = builder.mAgeView;
        mBodyView = builder.mBodyView;
        mCallToActionView = builder.mCallToActionView;
        mDomainView = builder.mDomainView;
        mIconView = builder.mIconView;
        mPriceView = builder.mPriceView;
        mRatingView = builder.mRatingView;
        mReviewCountView = builder.mReviewCountView;
        mSponsoredView = builder.mSponsoredView;
        mTitleView = builder.mTitleView;
        mWarningView = builder.mWarningView;
    }

    @Nullable
    public TextView getAgeView() {
        return mAgeView;
    }

    @Nullable
    public TextView getBodyView() {
        return mBodyView;
    }

    @Nullable
    public TextView getCallToActionView() {
        return mCallToActionView;
    }

    @Nullable
    public TextView getDomainView() {
        return mDomainView;
    }

    @Nullable
    public ImageView getIconView() {
        return mIconView;
    }

    @Nullable
    public TextView getPriceView() {
        return mPriceView;
    }

    @Nullable
    public <T extends View & Rating> T getRatingView() {
        return (T) mRatingView;
    }

    @Nullable
    public TextView getReviewCountView() {
        return mReviewCountView;
    }

    @Nullable
    public TextView getSponsoredView() {
        return mSponsoredView;
    }

    @Nullable
    public TextView getTitleView() {
        return mTitleView;
    }

    @Nullable
    public TextView getWarningView() {
        return mWarningView;
    }

    public static class Builder {

        @Nullable
        private TextView mAgeView;

        @Nullable
        private TextView mBodyView;

        @Nullable
        private TextView mCallToActionView;

        @Nullable
        private TextView mDomainView;

        @Nullable
        private ImageView mIconView;

        @Nullable
        private TextView mPriceView;

        @Nullable
        private View mRatingView;

        @Nullable
        private TextView mReviewCountView;

        @Nullable
        private TextView mSponsoredView;

        @Nullable
        private TextView mTitleView;

        @Nullable
        private TextView mWarningView;

        @NonNull
        public YandexNativeAssetViewsProvider build() {
            return new YandexNativeAssetViewsProvider(this);
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setAgeView(@Nullable final TextView ageView) {
            mAgeView = ageView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setBodyView(@Nullable final TextView bodyView) {
            mBodyView = bodyView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setCallToActionView(@Nullable final TextView callToActionView) {
            mCallToActionView = callToActionView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setDomainView(@Nullable final TextView domainView) {
            mDomainView = domainView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setIconView(@Nullable final ImageView iconView) {
            mIconView = iconView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setPriceView(@Nullable final TextView priceView) {
            mPriceView = priceView;
            return this;
        }

        @NonNull
        <T extends View & Rating> YandexNativeAssetViewsProvider.Builder setRatingView(@Nullable final T ratingView) {
            mRatingView = ratingView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setReviewCountView(@Nullable final TextView reviewCountView) {
            mReviewCountView = reviewCountView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setSponsoredView(@Nullable final TextView sponsoredView) {
            mSponsoredView = sponsoredView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setTitleView(@Nullable final TextView titleView) {
            mTitleView = titleView;
            return this;
        }

        @NonNull
        YandexNativeAssetViewsProvider.Builder setWarningView(@Nullable final TextView warningView) {
            mWarningView = warningView;
            return this;
        }
    }
}
