package com.google.ads.mediation.nend;

import com.google.android.gms.ads.rewarded.RewardItem;

import net.nend.android.NendAdRewardItem;

/*
 * The {@link NendMediationRewardItem} class is used to create rewards to users.
 */
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