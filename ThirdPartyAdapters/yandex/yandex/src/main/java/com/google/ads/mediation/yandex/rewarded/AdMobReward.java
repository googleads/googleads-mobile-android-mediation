/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.rewarded;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.rewarded.RewardItem;
import com.yandex.mobile.ads.rewarded.Reward;

public class AdMobReward implements RewardItem {

    @NonNull
    private final Reward mReward;

    public AdMobReward(@NonNull final Reward reward) {
        mReward = reward;
    }

    @Override
    public String getType() {
        return mReward.getType();
    }

    @Override
    public int getAmount() {
        return mReward.getAmount();
    }
}
