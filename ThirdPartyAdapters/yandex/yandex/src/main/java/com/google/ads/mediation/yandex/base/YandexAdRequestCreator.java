/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.base;

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.mediation.MediationAdConfiguration;

import java.util.Map;

public class YandexAdRequestCreator {

    @NonNull
    private final AdMobAdRequestParametersProvider mAdRequestParametersProvider;

    public YandexAdRequestCreator() {
        mAdRequestParametersProvider = new AdMobAdRequestParametersProvider();
    }

    @NonNull
    public com.yandex.mobile.ads.common.AdRequest createAdRequest(@NonNull final MediationAdConfiguration config) {
        final Map<String, String> adRequestParameters = mAdRequestParametersProvider.getAdRequestParameters();

        final com.yandex.mobile.ads.common.AdRequest.Builder adRequestBuilder =
                new com.yandex.mobile.ads.common.AdRequest.Builder();
        adRequestBuilder.setParameters(adRequestParameters);

        final Location location = config.getLocation();
        if (location != null) {
            adRequestBuilder.setLocation(location);
        }

        return adRequestBuilder.build();
    }
}
