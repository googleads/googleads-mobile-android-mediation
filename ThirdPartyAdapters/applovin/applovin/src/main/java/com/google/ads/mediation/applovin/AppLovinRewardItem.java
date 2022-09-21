package com.google.ads.mediation.applovin;

import androidx.annotation.NonNull;
import com.google.android.gms.ads.rewarded.RewardItem;

public final class AppLovinRewardItem implements RewardItem {

  private final int amount;
  private final String type;

  public AppLovinRewardItem(int amount, String type) {
    this.amount = amount;
    this.type = type;
  }

  @Override
  @NonNull
  public String getType() {
    return type;
  }

  @Override
  public int getAmount() {
    return amount;
  }
}
