/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.banner;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.google.ads.mediation.yandex.base.AdapterLoadErrorHandler;
import com.google.ads.mediation.yandex.base.MediationDataParser;
import com.google.ads.mediation.yandex.base.YandexAdRequestCreator;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;

public class BannerAdapter implements MediationBannerAd {

    @NonNull
    private final YandexAdSizeProvider mYandexAdSizeProvider;

    @NonNull
    private final YandexAdRequestCreator mYandexAdRequestCreator;

    @NonNull
    private final MediationDataParser mMediationDataParser;

    @Nullable
    private BannerAdView mBannerAdView;

    public BannerAdapter() {
        mYandexAdSizeProvider = new YandexAdSizeProvider();
        mYandexAdRequestCreator = new YandexAdRequestCreator();
        mMediationDataParser = new MediationDataParser();
    }

    public void loadBannerAd(
            @NonNull final MediationBannerAdConfiguration adConfiguration,
            @NonNull final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> loadCallback) {
        final AdapterLoadErrorHandler loadErrorHandler = new AdapterLoadErrorHandler(loadCallback);

        try {
            final Context context = adConfiguration.getContext();
            final String blockId = mMediationDataParser.parseBlockId(adConfiguration.getServerParameters());
            final AdSize adSize = mYandexAdSizeProvider.getAdSizeFromAdMobAdSize(adConfiguration.getAdSize());

            if (context != null && TextUtils.isEmpty(blockId) == false && adSize != null) {
                final com.yandex.mobile.ads.common.AdRequest adRequest =
                        mYandexAdRequestCreator.createAdRequest(adConfiguration);
                mBannerAdView = new BannerAdView(context);

                mBannerAdView.setAdSize(adSize);
                mBannerAdView.setBlockId(blockId);

                final BannerAdEventListener adEventListener = new BannerAdapterEventListener(this, loadCallback);
                mBannerAdView.setBannerAdEventListener(adEventListener);
                mBannerAdView.loadAd(adRequest);
            } else {
                loadErrorHandler.handleInvalidConfigurationError();
            }
        } catch (final Exception e) {
            loadErrorHandler.handleInternalAdapterError();
        }
    }

    @NonNull
    @Override
    public View getView() {
        return mBannerAdView;
    }
}
