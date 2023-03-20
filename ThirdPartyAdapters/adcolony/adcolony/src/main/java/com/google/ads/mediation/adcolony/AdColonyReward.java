// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
