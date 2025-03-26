// Copyright 2023 Google LLC
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

package com.google.ads.mediation.line

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.NeedChildDirectedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Singleton used to initialize [FiveAd] SDK through the [LineSdkWrapper] which facilitates unit
 * testing of the class.
 */
object LineInitializer {
  private var fiveAdConfig: FiveAdConfig? = null

  fun initialize(context: Context, appId: String) {

    if (LineSdkWrapper.delegate.isInitialized()) {
      return
    }

    val config = getFiveAdConfig(appId)
    LineSdkWrapper.delegate.initialize(context, config)
  }

  fun getFiveAdConfig(appId: String): FiveAdConfig {
    if (fiveAdConfig == null) {
      fiveAdConfig = LineSdkFactory.delegate.createFiveAdConfig(appId)
      fiveAdConfig?.needChildDirectedTreatment =
        when (MobileAds.getRequestConfiguration().tagForChildDirectedTreatment) {
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE ->
            NeedChildDirectedTreatment.TRUE
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE ->
            NeedChildDirectedTreatment.FALSE
          else -> NeedChildDirectedTreatment.UNSPECIFIED
        }

      fiveAdConfig?.isTest = LineMediationAdapter.isTestMode
    }
    return fiveAdConfig as FiveAdConfig
  }

  @VisibleForTesting
  internal fun resetFiveAdConfig() {
    fiveAdConfig = null
  }
}
