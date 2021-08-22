/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.yandex.base.AdapterLoadErrorHandler;
import com.google.ads.mediation.yandex.base.MediationDataParser;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.yandex.mobile.ads.nativeads.NativeAdLoader;
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration;

public class NativeAdapter {

    @NonNull
    private final YandexNativeAdRequestCreator mYandexNativeAdRequestCreator;

    @NonNull
    private final MediationDataParser mMediationDataParser;

    @Nullable
    private NativeAdLoader mNativeAdLoader;

    public NativeAdapter() {
        mYandexNativeAdRequestCreator = new YandexNativeAdRequestCreator();
        mMediationDataParser = new MediationDataParser();
    }

    public void loadNativeAd(
            @NonNull final MediationNativeAdConfiguration adConfiguration,
            @NonNull final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> loadCallback) {
        final AdapterLoadErrorHandler loadErrorHandler = new AdapterLoadErrorHandler(loadCallback);

        try {
            final Context context = adConfiguration.getContext();
            final String blockId = mMediationDataParser.parseBlockId(adConfiguration.getServerParameters());

            if (context instanceof Activity && TextUtils.isEmpty(blockId) == false) {
                mNativeAdLoader = new NativeAdLoader(context);
                final NativeAdapterLoadListener loadListener =
                        new NativeAdapterLoadListener(adConfiguration, loadCallback);
                mNativeAdLoader.setNativeAdLoadListener(loadListener);
                final NativeAdRequestConfiguration nativeAdRequestConfiguration =
                        mYandexNativeAdRequestCreator.createNativeAdRequest(blockId, adConfiguration);
                mNativeAdLoader.loadAd(nativeAdRequestConfiguration);
            } else {
                loadErrorHandler.handleInvalidConfigurationError();
            }
        } catch (final Exception e) {
            loadErrorHandler.handleInternalAdapterError();
        }
    }
}
