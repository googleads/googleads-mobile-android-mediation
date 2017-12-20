package com.google.ads.mediation.nend;

import com.google.android.gms.ads.reward.RewardItem;

import net.nend.android.NendAdRewardItem;

class NendMediationRewardItem implements RewardItem {
    private final String mRewardType;
    private final int mRewardAmount;

    NendMediationRewardItem(NendAdRewardItem item) {
        mRewardType = item.getCurrencyName();
        mRewardAmount = item.getCurrencyAmount();
    }

    @Override
    public String getType() {
        return mRewardType;
    }

    @Override
    public int getAmount() {
        return mRewardAmount;
    }
}