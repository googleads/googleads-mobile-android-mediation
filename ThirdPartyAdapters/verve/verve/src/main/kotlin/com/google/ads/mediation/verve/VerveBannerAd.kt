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

package com.google.ads.mediation.verve

import android.content.Context
import android.view.View
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration

/**
 * Used to load Verve banner ads and mediate callbacks between Google Mobile Ads SDK and Verve SDK.
 */
class VerveBannerAd
private constructor(
  private val context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val adSize: AdSize,
  // TODO: Add other parameters or remove unnecessary ones.
) : MediationBannerAd {
  // TODO: Replace with 3p View. Ideally avoid lateinit and initialize in constructor.
  private lateinit var adView: View

  fun loadAd() {
    // TODO: Implement this method.
  }

  override fun getView(): View {
    // TODO: Implement this method.
    return adView
  }

  companion object {
    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
    ): Result<VerveBannerAd> {
      val context = mediationBannerAdConfiguration.context
      val serverParameters = mediationBannerAdConfiguration.serverParameters
      val adSize = mediationBannerAdConfiguration.adSize

      // TODO: Implement necessary initialization steps.

      return Result.success(VerveBannerAd(context, mediationAdLoadCallback, adSize))
    }
  }
}
