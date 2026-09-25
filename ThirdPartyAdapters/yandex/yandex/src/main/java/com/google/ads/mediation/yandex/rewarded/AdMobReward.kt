package com.google.ads.mediation.yandex.rewarded

import com.google.android.gms.ads.rewarded.RewardItem
import com.yandex.mobile.ads.rewarded.Reward

internal class AdMobReward(private val reward: Reward) : RewardItem {

    override fun getType(): String {
        return reward.type
    }

    override fun getAmount(): Int {
        return reward.amount
    }
}
