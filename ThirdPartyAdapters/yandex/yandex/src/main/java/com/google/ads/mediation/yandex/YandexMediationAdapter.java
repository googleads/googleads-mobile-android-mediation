/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.ads.mediation.yandex.banner.BannerAdapter;
import com.google.ads.mediation.yandex.base.VersionInfoProvider;
import com.google.ads.mediation.yandex.interstitial.InterstitialAdapter;
import com.google.ads.mediation.yandex.nativeads.NativeAdapter;
import com.google.ads.mediation.yandex.rewarded.RewardedAdapter;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;

import java.util.List;

public class YandexMediationAdapter extends Adapter {

    private static final String NULL_CALLBACK_ERROR_MESSAGE = "Callback must not be null";
    private static final String NULL_AD_CONFIGURATION_ERROR_MESSAGE = "Ad configuration must not be null";

    private static final String TAG = "Yandex AdMob Adapter";

    @NonNull
    private final VersionInfoProvider mVersionInfoProvider;

    @Nullable
    private BannerAdapter mBannerAdapter;

    @Nullable
    private InterstitialAdapter mInterstitialAdapter;

    @Nullable
    private RewardedAdapter mRewardedAdapter;

    @Nullable
    private NativeAdapter mNativeAdapter;

    public YandexMediationAdapter() {
        mVersionInfoProvider = new VersionInfoProvider();
    }

    @Override
    public void initialize(@Nullable final Context context,
                           @Nullable final InitializationCompleteCallback initializationCompleteCallback,
                           @Nullable final List<MediationConfiguration> configurations) {
        if (initializationCompleteCallback != null) {
            initializationCompleteCallback.onInitializationSucceeded();
        }
    }

    @Override
    public VersionInfo getVersionInfo() {
        return mVersionInfoProvider.getAdapterVersionInfo();
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        return mVersionInfoProvider.getSdkVersionInfo();
    }

    @Override
    public void loadBannerAd(
            @Nullable final MediationBannerAdConfiguration adConfiguration,
            @Nullable final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback) {
        if (adLoadCallback == null) {
            Log.w(TAG, NULL_CALLBACK_ERROR_MESSAGE);
        } else if (adConfiguration == null) {
            adLoadCallback.onFailure(NULL_AD_CONFIGURATION_ERROR_MESSAGE);
        } else {
            mBannerAdapter = new BannerAdapter();
            mBannerAdapter.loadBannerAd(adConfiguration, adLoadCallback);
        }
    }

    @Override
    public void loadInterstitialAd(
            @Nullable final MediationInterstitialAdConfiguration adConfiguration,
            @Nullable final MediationAdLoadCallback<MediationInterstitialAd,
                    MediationInterstitialAdCallback> callback) {
        if (callback == null) {
            Log.w(TAG, NULL_CALLBACK_ERROR_MESSAGE);
        } else if (adConfiguration == null) {
            callback.onFailure(NULL_AD_CONFIGURATION_ERROR_MESSAGE);
        } else {
            mInterstitialAdapter = new InterstitialAdapter();
            mInterstitialAdapter.loadInterstitialAd(adConfiguration, callback);
        }
    }

    @Override
    public void loadRewardedAd(
            @Nullable final MediationRewardedAdConfiguration adConfiguration,
            @Nullable final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
        if (adLoadCallback == null) {
            Log.w(TAG, NULL_CALLBACK_ERROR_MESSAGE);
        } else if (adConfiguration == null) {
            adLoadCallback.onFailure(NULL_AD_CONFIGURATION_ERROR_MESSAGE);
        } else {
            mRewardedAdapter = new RewardedAdapter();
            mRewardedAdapter.loadRewardedAd(adConfiguration, adLoadCallback);
        }
    }

    @Override
    public void loadNativeAd(
            @Nullable final MediationNativeAdConfiguration adConfiguration,
            @Nullable final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> adLoadCallback) {
        if (adLoadCallback == null) {
            Log.w(TAG, NULL_CALLBACK_ERROR_MESSAGE);
        } else if (adConfiguration == null) {
            adLoadCallback.onFailure(NULL_AD_CONFIGURATION_ERROR_MESSAGE);
        } else {
            mNativeAdapter = new NativeAdapter();
            mNativeAdapter.loadNativeAd(adConfiguration, adLoadCallback);
        }
    }
}
