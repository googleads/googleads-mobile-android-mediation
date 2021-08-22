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

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;

public class AdapterLoadErrorHandler {

    private static final String INVALID_CONFIGURATION_ERROR_MESSAGE = "Invalid ad configuration";
    private static final String INTERNAL_ERROR_MESSAGE = "Internal error";

    @NonNull
    private final MediationAdLoadCallback mAdLoadCallback;

    public AdapterLoadErrorHandler(@NonNull final MediationAdLoadCallback adLoadCallback) {
        mAdLoadCallback = adLoadCallback;
    }

    public void handleInternalAdapterError() {
        mAdLoadCallback.onFailure(INTERNAL_ERROR_MESSAGE);
    }

    public void handleInvalidConfigurationError() {
        mAdLoadCallback.onFailure(INVALID_CONFIGURATION_ERROR_MESSAGE);
    }
}
