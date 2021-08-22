/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.nativeads.NativeAdEventListener;

class NativeAdapterEventListener implements NativeAdEventListener {

    @NonNull
    private final MediationNativeAdCallback mEventCallback;

    NativeAdapterEventListener(@NonNull final MediationNativeAdCallback eventCallback) {
        mEventCallback = eventCallback;
    }

    @Override
    public void onLeftApplication() {
        mEventCallback.reportAdClicked();
        mEventCallback.onAdOpened();
        mEventCallback.onAdLeftApplication();
    }

    @Override
    public void onReturnedToApplication() {
        mEventCallback.onAdClosed();
    }

    @Keep
    public void onImpression(@Nullable final ImpressionData impressionData) {
        mEventCallback.reportAdImpression();
    }
}
