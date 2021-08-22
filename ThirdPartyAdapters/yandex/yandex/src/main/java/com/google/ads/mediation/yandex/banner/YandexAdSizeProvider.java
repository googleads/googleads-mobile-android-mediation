/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.banner;

import androidx.annotation.Nullable;

import com.yandex.mobile.ads.banner.AdSize;

public class YandexAdSizeProvider {

    @Nullable
    public AdSize getAdSizeFromAdMobAdSize(@Nullable final com.google.android.gms.ads.AdSize adMobAdSize) {
        if (adMobAdSize != null) {
            return new AdSize(adMobAdSize.getWidth(), adMobAdSize.getHeight());
        }

        return null;
    }
}
