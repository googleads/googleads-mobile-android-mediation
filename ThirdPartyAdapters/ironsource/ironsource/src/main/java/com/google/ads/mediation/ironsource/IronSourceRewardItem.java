// Copyright 2020 Google LLC
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

package com.google.ads.mediation.ironsource;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.rewarded.RewardItem;

public class IronSourceRewardItem implements RewardItem {
  @Override
  public int getAmount() {
    return 1;
  }

  @NonNull
  @Override
  public String getType() {
    return "";
  }
}
