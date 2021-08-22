/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.interstitial;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.yandex.base.AdapterLoadErrorHandler;
import com.google.ads.mediation.yandex.base.MediationDataParser;
import com.google.ads.mediation.yandex.base.YandexAdRequestCreator;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;

public class InterstitialAdapter implements MediationInterstitialAd {

    private static final String TAG = "Yandex AdMob Adapter";

    @NonNull
    private final YandexAdRequestCreator mYandexAdRequestCreator;

    @NonNull
    private final MediationDataParser mMediationDataParser;

    @Nullable
    private InterstitialAd mInterstitialAd;

    public InterstitialAdapter() {
        mYandexAdRequestCreator = new YandexAdRequestCreator();
        mMediationDataParser = new MediationDataParser();
    }

    public void loadInterstitialAd(
            @NonNull final MediationInterstitialAdConfiguration adConfiguration,
            @NonNull final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        final AdapterLoadErrorHandler loadErrorHandler = new AdapterLoadErrorHandler(callback);

        try {
            final Context context = adConfiguration.getContext();
            final String blockId = mMediationDataParser.parseBlockId(adConfiguration.getServerParameters());

            if (context != null && TextUtils.isEmpty(blockId) == false) {
                final com.yandex.mobile.ads.common.AdRequest adRequest =
                        mYandexAdRequestCreator.createAdRequest(adConfiguration);

                mInterstitialAd = new InterstitialAd(context);
                mInterstitialAd.setBlockId(blockId);

                final InterstitialAdEventListener eventListener =
                        new InterstitialAdapterEventListener(this, callback);
                mInterstitialAd.setInterstitialAdEventListener(eventListener);
                mInterstitialAd.loadAd(adRequest);
            } else {
                loadErrorHandler.handleInvalidConfigurationError();
            }
        } catch (final Exception e) {
            loadErrorHandler.handleInternalAdapterError();
        }
    }

    @Override
    public void showAd(final Context context) {
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Log.d(TAG, "Tried to show a Yandex Mobile Ads interstitial ad before it finished loading. " +
                    "Please try again.");
        }
    }
}
