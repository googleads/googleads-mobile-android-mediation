/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.rewarded;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

public class RewardedAdapterEventListener implements RewardedAdEventListener {

    @NonNull
    private final RewardedAdapter mRewardedAdapter;

    @NonNull
    private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mLoadCallback;

    @Nullable
    private MediationRewardedAdCallback mEventCallback;

    RewardedAdapterEventListener(
            @NonNull final RewardedAdapter rewardedAdapter,
            @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> loadCallback) {
        mRewardedAdapter = rewardedAdapter;
        mLoadCallback = loadCallback;
    }

    @Override
    public void onAdLoaded() {
        mEventCallback = mLoadCallback.onSuccess(mRewardedAdapter);
    }

    @Override
    public void onAdFailedToLoad(@NonNull final AdRequestError error) {
        mLoadCallback.onFailure(error.getDescription());
    }

    @Override
    public void onAdShown() {
        if (mEventCallback != null) {
            mEventCallback.onAdOpened();
            mEventCallback.onVideoStart();
        }
    }

    @Override
    public void onAdDismissed() {
        if (mEventCallback != null) {
            mEventCallback.onVideoComplete();
            mEventCallback.onAdClosed();
        }
    }

    @Keep
    public void onImpression(@Nullable final ImpressionData impressionData) {
        if (mEventCallback != null) {
            mEventCallback.reportAdImpression();
        }
    }

    @Override
    public void onLeftApplication() {
        if (mEventCallback != null) {
            mEventCallback.reportAdClicked();
        }
    }

    @Override
    public void onReturnedToApplication() {
        // do nothing.
    }

    @Override
    public void onRewarded(@NonNull final Reward reward) {
        if (mEventCallback != null) {
            final RewardItem rewardItem = new AdMobReward(reward);
            mEventCallback.onUserEarnedReward(rewardItem);
        }
    }
}
