// Copyright 2019 Google LLC
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
