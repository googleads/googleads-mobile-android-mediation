/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads.asset;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.formats.NativeAd;

public class YandexNativeAdImage extends NativeAd.Image {

    @NonNull
    private final Drawable mDrawable;

    @NonNull
    private final Uri mUri;

    YandexNativeAdImage(@NonNull final Drawable drawable,
                        @NonNull final Uri uri) {
        mDrawable = drawable;
        mUri = uri;
    }

    @NonNull
    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    public double getScale() {
        return 1;
    }
}