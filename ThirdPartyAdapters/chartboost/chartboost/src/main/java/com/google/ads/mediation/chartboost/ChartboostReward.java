// Copyright 2016 Google Inc.
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

package com.google.ads.mediation.chartboost;

import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * The {@link ChartboostReward} class is used to create rewards for users.
 */
public class ChartboostReward implements RewardItem {

    /**
     * Reward item amount to be rewarded.
     */
    private final int mAmount;

    public ChartboostReward(int amount) {
        this.mAmount = amount;
    }

    @Override
    public String getType() {
        // Charboost doesn't provide reward type.
        return "";
    }

    @Override
    public int getAmount() {
        return mAmount;
    }
}
