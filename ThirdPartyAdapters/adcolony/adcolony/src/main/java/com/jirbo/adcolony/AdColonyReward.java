package com.jirbo.adcolony;

import com.google.android.gms.ads.reward.RewardItem;

/**
 * A {@link RewardItem} used to map AdColony rewards to Google's rewarded video ad rewards.
 *
 */
public class AdColonyReward implements RewardItem {
    private String _name;
    private int _amount;

    public AdColonyReward(String name, int amount) {
        _name = name;
        _amount = amount;
    }

    @Override
    public String getType() {
        return _name;
    }

    @Override
    public int getAmount() {
        return _amount;
    }
}
