/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.rewarded;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.yandex.base.AdapterLoadErrorHandler;
import com.google.ads.mediation.yandex.base.MediationDataParser;
import com.google.ads.mediation.yandex.base.YandexAdRequestCreator;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

public class RewardedAdapter implements MediationRewardedAd {

    private static final String TAG = "Yandex AdMob Adapter";

    @NonNull
    private final YandexAdRequestCreator mYandexAdRequestCreator;

    @NonNull
    private final MediationDataParser mMediationDataParser;

    @Nullable
    private RewardedAd mRewardedAd;

    public RewardedAdapter() {
        mYandexAdRequestCreator = new YandexAdRequestCreator();
        mMediationDataParser = new MediationDataParser();
    }

    public void loadRewardedAd(
            @NonNull final MediationRewardedAdConfiguration adConfiguration,
            @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> loadCallback) {
        final AdapterLoadErrorHandler loadErrorHandler = new AdapterLoadErrorHandler(loadCallback);

        try {
            final Context context = adConfiguration.getContext();
            final String blockId = mMediationDataParser.parseBlockId(adConfiguration.getServerParameters());

            if (TextUtils.isEmpty(blockId) == false && context instanceof Activity) {
                final com.yandex.mobile.ads.common.AdRequest adRequest =
                        mYandexAdRequestCreator.createAdRequest(adConfiguration);
                final RewardedAdEventListener eventListener =
                        new RewardedAdapterEventListener(this, loadCallback);

                mRewardedAd = new RewardedAd(context);
                mRewardedAd.setBlockId(blockId);
                mRewardedAd.setRewardedAdEventListener(eventListener);
                mRewardedAd.loadAd(adRequest);
            } else {
                loadErrorHandler.handleInvalidConfigurationError();
            }
        } catch (final Exception e) {
            loadErrorHandler.handleInternalAdapterError();
        }
    }

    @Override
    public void showAd(final Context context) {
        if (mRewardedAd != null && mRewardedAd.isLoaded()) {
            mRewardedAd.show();
        } else {
            Log.d(TAG, "Tried to show a Yandex Mobile Ads SDK rewarded ad before it finished loading. " +
                    "Please try again.");
        }
    }
}
