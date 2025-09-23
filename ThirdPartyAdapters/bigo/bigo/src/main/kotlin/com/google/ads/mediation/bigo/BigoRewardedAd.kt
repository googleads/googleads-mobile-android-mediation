// Copyright 2025 Google LLC
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

package com.google.ads.mediation.bigo

import android.content.Context
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration

/**
 * Used to load Bigo rewarded ads and mediate callbacks between Google Mobile Ads SDK and Bigo SDK.
 */
class BigoRewardedAd
private constructor(
  private val context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  // TODO: Add other parameters or remove unnecessary ones.
) : MediationRewardedAd {

  fun loadAd() {
    // TODO: Implement this method.
  }

  override fun showAd(context: Context) {
    // TODO: Implement this method.
  }

  companion object {
    fun newInstance(
      mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
    ): Result<BigoRewardedAd> {
      val context = mediationRewardedAdConfiguration.context
      val serverParameters = mediationRewardedAdConfiguration.serverParameters

      // TODO: Implement necessary initialization steps.

      return Result.success(BigoRewardedAd(context, mediationAdLoadCallback))
    }
  }
}
