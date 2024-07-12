// Copyright 2024 Google LLC
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

package com.google.ads.mediation.moloco

import android.content.Context
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper

/**
 * Used to load Moloco native ads and mediate callbacks between Google Mobile Ads SDK and Moloco
 * SDK.
 */
class MolocoNativeAd
private constructor(
  private val context: Context,
  private val mediationNativeAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  // TODO: Add other parameters or remove unnecessary ones.
) : UnifiedNativeAdMapper() {

  fun loadAd() {
    // TODO: Implement this method.
  }

  companion object {
    fun newInstance(
      mediationNativeAdConfiguration: MediationNativeAdConfiguration,
      mediationNativeAdLoadCallback:
        MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
    ): Result<MolocoNativeAd> {
      val context = mediationNativeAdConfiguration.context
      val serverParameters = mediationNativeAdConfiguration.serverParameters
      val nativeAdOptions = mediationNativeAdConfiguration.nativeAdOptions

      // TODO: Implement necessary initialization steps.

      return Result.success(MolocoNativeAd(context, mediationNativeAdLoadCallback))
    }
  }
}
