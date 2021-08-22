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

import com.google.ads.mediation.yandex.BuildConfig;

public class AdapterVersionProvider {

    @NonNull
    String getAdapterVersion() {
        return BuildConfig.ADAPTER_VERSION;
    }
}
