/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads;

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.ads.mediation.yandex.base.AdMobAdRequestParametersProvider;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration;

import java.util.Map;

public class YandexNativeAdRequestCreator {

    @NonNull
    private final AdMobAdRequestParametersProvider mAdRequestParametersProvider;

    public YandexNativeAdRequestCreator() {
        mAdRequestParametersProvider = new AdMobAdRequestParametersProvider();
    }

    @NonNull
    public NativeAdRequestConfiguration createNativeAdRequest(@NonNull final String blockId,
                                                              @NonNull final MediationAdConfiguration adConfiguration) {
        final Map<String, String> adRequestParameters = mAdRequestParametersProvider.getAdRequestParameters();

        final NativeAdRequestConfiguration.Builder builder =
                new NativeAdRequestConfiguration.Builder(blockId);
        builder.setParameters(adRequestParameters);

        final Location location = adConfiguration.getLocation();
        if (location != null) {
            builder.setLocation(location);
        }

        return builder.build();
    }
}
