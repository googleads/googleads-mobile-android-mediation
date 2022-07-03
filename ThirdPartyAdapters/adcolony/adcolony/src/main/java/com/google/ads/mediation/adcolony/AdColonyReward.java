package com.google.ads.mediation.adcolony;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * A {@link RewardItem} used to map AdColony rewards to Google's rewarded video ad rewards.
 */
class AdColonyReward implements RewardItem {

  private final String rewardType;
  private final int rewardAmount;

  public AdColonyReward(@NonNull String type, int amount) {
    rewardType = type;
    rewardAmount = amount;
  }

  @Override
  @NonNull
  public String getType() {
    return rewardType;
  }

  @Override
  public int getAmount() {
    return rewardAmount;
  }
}
