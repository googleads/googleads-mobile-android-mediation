/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.YandexNativeAdMappersFactory;

public class NativeAdapterLoadListener implements NativeAdLoadListener {

    private static final String INTERNAL_ERROR_MESSAGE = "Internal error";

    @NonNull
    private final YandexNativeAdMappersFactory mAdMappersFactory;

    @NonNull
    private final MediationNativeAdConfiguration mAdConfiguration;

    @NonNull
    private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mLoadCallback;

    NativeAdapterLoadListener(
            @NonNull final MediationNativeAdConfiguration adConfiguration,
            @NonNull final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> loadCallback) {
        mAdConfiguration = adConfiguration;
        mLoadCallback = loadCallback;

        mAdMappersFactory = new YandexNativeAdMappersFactory();
    }

    @Override
    public void onAdFailedToLoad(@NonNull final AdRequestError error) {
        mLoadCallback.onFailure(error.getDescription());
    }

    @Override
    public void onAdLoaded(@NonNull final NativeAd nativeAd) {
        final Context context = mAdConfiguration.getContext();
        if (context != null) {
            final Bundle extras = mAdConfiguration.getMediationExtras();
            final UnifiedNativeAdMapper unifiedNativeAdMapper =
                    mAdMappersFactory.createAdMapper(context, nativeAd, extras);

            final MediationNativeAdCallback eventCallback = mLoadCallback.onSuccess(unifiedNativeAdMapper);
            if (eventCallback != null) {
                nativeAd.setNativeAdEventListener(new NativeAdapterEventListener(eventCallback));
            }
        } else {
            mLoadCallback.onFailure(INTERNAL_ERROR_MESSAGE);
        }
    }
}
