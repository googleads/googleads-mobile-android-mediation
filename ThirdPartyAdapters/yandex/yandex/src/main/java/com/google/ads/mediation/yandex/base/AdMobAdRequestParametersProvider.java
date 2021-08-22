/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.yandex.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class AdMobAdRequestParametersProvider {

    private static final String ADAPTER_VERSION_KEY = "adapter_version";
    private static final String ADAPTER_NETWORK_NAME_KEY = "adapter_network_name";
    private static final String ADAPTER_NETWORK_SDK_VERSION_KEY = "adapter_network_sdk_version";

    private static final String ADAPTER_NETWORK_NAME = "admob";

    @NonNull
    public Map<String, String> getAdRequestParameters() {
        final Map<String, String> adRequestParameters = new HashMap<>();
        adRequestParameters.put(ADAPTER_NETWORK_NAME_KEY, ADAPTER_NETWORK_NAME);
        appendVersionParameters(adRequestParameters);

        return adRequestParameters;
    }

    private void appendVersionParameters(@NonNull final Map<String, String> adRequestParameters) {
        final String adMobSdkVersion = getAdMobSdkVersion();
        if (adMobSdkVersion != null) {
            adRequestParameters.put(ADAPTER_NETWORK_SDK_VERSION_KEY, adMobSdkVersion);
        }
        adRequestParameters.put(ADAPTER_VERSION_KEY, getAdapterVersion());
    }

    @NonNull
    private String getAdapterVersion() {
        return BuildConfig.ADAPTER_VERSION;
    }

    @Nullable
    private String getAdMobSdkVersion() {
        try {
            return com.google.android.gms.ads.MobileAds.getVersionString();
        } catch (final Throwable ignored) {}

        return null;
    }
}
