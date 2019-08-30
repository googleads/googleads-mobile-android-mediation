package com.google.ads.mediation.adcolony;

import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * A {@link RewardItem} used to map AdColony rewards to Google's rewarded video ad rewards.
 */
class AdColonyReward implements RewardItem {
    private String rewardType;
    private int rewardAmount;

    public AdColonyReward(String type, int amount) {
        rewardType = type;
        rewardAmount = amount;
    }

    @Override
    public String getType() {
        return rewardType;
    }

    @Override
    public int getAmount() {
        return rewardAmount;
    }
}
