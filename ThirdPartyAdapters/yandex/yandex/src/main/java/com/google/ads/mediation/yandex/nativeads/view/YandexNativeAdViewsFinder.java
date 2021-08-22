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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.yandex.nativeads.asset.YandexNativeAdAsset;
import com.yandex.mobile.ads.nativeads.Rating;

class YandexNativeAdViewsFinder {

    @NonNull
    private final Bundle mExtras;

    YandexNativeAdViewsFinder(@NonNull final Bundle extras) {
        mExtras = extras;
    }

    @Nullable
    View findViewByExtraKey(@NonNull final View nativeAdView,
                            @NonNull final String extraKey) {
        if (mExtras.containsKey(extraKey)) {
            return nativeAdView.findViewById(mExtras.getInt(extraKey));
        }

        return null;
    }

    @Nullable
    <T extends View & Rating> T findRatingView(@NonNull final View nativeAdView) {
        try {
            final View view = findViewByExtraKey(nativeAdView, YandexNativeAdAsset.RATING);
            if (view instanceof Rating) {
                return (T) view;
            }
        } catch (final Exception ignored) {
        }

        return null;
    }
}
