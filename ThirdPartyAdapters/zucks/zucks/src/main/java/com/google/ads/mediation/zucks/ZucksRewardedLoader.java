// Copyright 2021 Google LLC
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

package com.google.ads.mediation.zucks;

import android.content.Context;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public class ZucksRewardedLoader implements MediationRewardedAd {

  /**
   * Configuration of the rewarded ad request.
   */
  MediationRewardedAdConfiguration adConfiguration;

  /**
   * The mediation callback for ad load events.
   */
  MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback;

  /**
   * The mediation callback for rewarded ad events.
   */
  MediationRewardedAdCallback rewardedAdCallback;

  ZucksRewardedLoader(MediationRewardedAdConfiguration mediationRewardedAdConfiguration) {
    adConfiguration = mediationRewardedAdConfiguration;
  }

  void loadAd(MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback) {
    adLoadCallback = mediationAdLoadCallback;

    // TODO: Load rewarded ad and forward the success callback:
    rewardedAdCallback = adLoadCallback.onSuccess(ZucksRewardedLoader.this);
  }

  @Override
  public void showAd(Context context) {
    // TODO: Show the rewarded ad.
  }

}
