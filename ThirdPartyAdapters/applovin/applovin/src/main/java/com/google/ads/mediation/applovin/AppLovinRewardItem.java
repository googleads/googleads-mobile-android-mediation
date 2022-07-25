package com.google.ads.mediation.applovin;

import androidx.annotation.NonNull;
import com.google.android.gms.ads.rewarded.RewardItem;

public final class AppLovinRewardItem implements RewardItem {

  private final int mAmount;
  private final String mType;

  public AppLovinRewardItem(int amount, String type) {
    mAmount = amount;
    mType = type;
  }

  @Override
  @NonNull
  public String getType() {
    return mType;
  }

  @Override
  public int getAmount() {
    return mAmount;
  }
}
