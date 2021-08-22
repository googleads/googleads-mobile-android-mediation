/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.interstitial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;

public class InterstitialAdapterEventListener implements InterstitialAdEventListener {

    @NonNull
    private final InterstitialAdapter mInterstitialAdapter;

    @NonNull
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mLoadCallback;

    @Nullable
    private MediationInterstitialAdCallback mEventCallback;

    InterstitialAdapterEventListener(
            @NonNull final InterstitialAdapter interstitialAdapter,
            @NonNull final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        mInterstitialAdapter = interstitialAdapter;
        mLoadCallback = callback;
    }

    @Override
    public void onAdLoaded() {
        mEventCallback = mLoadCallback.onSuccess(mInterstitialAdapter);
    }

    @Override
    public void onAdFailedToLoad(@NonNull final AdRequestError error) {
        mLoadCallback.onFailure(error.getDescription());
    }

    @Override
    public void onAdDismissed() {
        if (mEventCallback != null) {
            mEventCallback.onAdClosed();
        }
    }

    @Override
    public void onAdShown() {
        if (mEventCallback != null) {
            mEventCallback.onAdOpened();
        }
    }

    @Override
    public void onLeftApplication() {
        if (mEventCallback != null) {
            mEventCallback.reportAdClicked();
            mEventCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onReturnedToApplication() {
        // do nothing.
    }

    @Keep
    public void onImpression(@Nullable final ImpressionData impressionData) {
        if (mEventCallback != null) {
            mEventCallback.reportAdImpression();
        }
    }
}
