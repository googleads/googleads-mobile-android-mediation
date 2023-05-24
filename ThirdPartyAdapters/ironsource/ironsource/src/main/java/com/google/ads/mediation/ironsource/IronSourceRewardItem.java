package com.google.ads.mediation.ironsource;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.rewarded.RewardItem;

public class IronSourceRewardItem implements RewardItem {
    @Override
    public int getAmount() {
        return 1;
    }

    @NonNull
    @Override
    public String getType() {
        return "";
    }
}
