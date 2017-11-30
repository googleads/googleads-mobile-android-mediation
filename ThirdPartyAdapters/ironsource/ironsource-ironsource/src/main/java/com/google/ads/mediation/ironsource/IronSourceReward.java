package com.google.ads.mediation.ironsource;

import com.google.android.gms.ads.reward.RewardItem;
import com.ironsource.mediationsdk.model.Placement;


class IronSourceReward implements RewardItem {

    private final Placement mPlacement;

    IronSourceReward(Placement placement) {
        this.mPlacement = placement;
    }

    @Override
    public String getType() {
        return mPlacement.getRewardName();
    }

    @Override
    public int getAmount() {
        return mPlacement.getRewardAmount();
    }
}
