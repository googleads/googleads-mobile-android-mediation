/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.banner;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

public class BannerAdapterEventListener implements BannerAdEventListener {

    @NonNull
    private final BannerAdapter mBannerAdapter;

    @NonNull
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mLoadCallback;

    @Nullable
    private MediationBannerAdCallback mEventCallback;

    BannerAdapterEventListener(
            @NonNull final BannerAdapter bannerAdapter,
            @NonNull final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> loadCallback) {
        mBannerAdapter = bannerAdapter;
        mLoadCallback = loadCallback;
    }

    @Override
    public void onAdLoaded() {
        mEventCallback = mLoadCallback.onSuccess(mBannerAdapter);
    }

    @Override
    public void onAdFailedToLoad(@NonNull final AdRequestError error) {
        mLoadCallback.onFailure(error.getDescription());
    }

    @Override
    public void onLeftApplication() {
        if (mEventCallback != null) {
            mEventCallback.reportAdClicked();
            mEventCallback.onAdOpened();
            mEventCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onReturnedToApplication() {
        if (mEventCallback != null) {
            mEventCallback.onAdClosed();
        }
    }

    @Keep
    public void onImpression(@Nullable final ImpressionData impressionData) {
        if (mEventCallback != null) {
            mEventCallback.reportAdImpression();
        }
    }
}
