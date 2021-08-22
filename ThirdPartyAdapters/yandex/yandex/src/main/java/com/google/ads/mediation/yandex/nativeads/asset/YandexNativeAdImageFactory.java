/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads.asset;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.nativeads.NativeAdImage;

public class YandexNativeAdImageFactory {

    @NonNull
    private final UriGenerator mUriGenerator;

    public YandexNativeAdImageFactory() {
        mUriGenerator = new UriGenerator();
    }

    @Nullable
    public YandexNativeAdImage createYandexNativeAdImage(@NonNull final Context context,
                                                         @Nullable final NativeAdImage imageData) {
        YandexNativeAdImage nativeAdImage = null;
        if (imageData != null) {
            final Resources resources = context.getResources();
            final Bitmap bitmap = imageData.getBitmap();

            final Drawable imageDrawable = new BitmapDrawable(resources, bitmap);
            final Uri imageUri = mUriGenerator.createImageUri(imageData);
            nativeAdImage = new YandexNativeAdImage(imageDrawable, imageUri);
        }

        return nativeAdImage;
    }
}
