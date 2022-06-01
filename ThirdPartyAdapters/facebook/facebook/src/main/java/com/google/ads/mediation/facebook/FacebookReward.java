package com.google.ads.mediation.facebook;

import androidx.annotation.NonNull;
import com.google.android.gms.ads.rewarded.RewardItem;

public class FacebookReward implements RewardItem {

  @Override
  @NonNull
  public String getType() {
    // Facebook SDK does not provide a reward type.
    return "";
  }

  @Override
  public int getAmount() {
    // Facebook SDK does not provide reward amount, default to 1.
    return 1;
  }
}
