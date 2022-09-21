package com.google.ads.mediation.nend;

import androidx.annotation.NonNull;
import com.google.android.gms.ads.rewarded.RewardItem;
import net.nend.android.NendAdRewardItem;

/**
 * The {@link NendMediationRewardItem} class is used to create rewards to users.
 */
class NendMediationRewardItem implements RewardItem {

  private final String rewardType;
  private final int rewardAmount;

  NendMediationRewardItem(@NonNull NendAdRewardItem item) {
    rewardType = item.getCurrencyName();
    rewardAmount = item.getCurrencyAmount();
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
