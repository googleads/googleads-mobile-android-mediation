/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads.asset;

import android.net.Uri;

import androidx.annotation.NonNull;

class UriGenerator {

    private static final String IMAGE_URI_PREFIX = "admob.image.url.";

    @NonNull
    Uri createImageUri(@NonNull final Object object) {
        return Uri.parse(IMAGE_URI_PREFIX + object.hashCode());
    }
}
