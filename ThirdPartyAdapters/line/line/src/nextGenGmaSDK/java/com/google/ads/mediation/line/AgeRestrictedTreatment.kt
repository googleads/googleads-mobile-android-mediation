// Copyright 2026 Google LLC
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

import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment as SdkAgeRestrictedTreatment

/**
 * Wrapper enum for GMA's AgeRestrictedTreatment to support both Play Services and NextGen GMA SDKs.
 */
internal enum class AgeRestrictedTreatment {
  CHILD,
  TEEN,
  UNSPECIFIED;

  companion object {
    private var ageRestrictedTreatment: AgeRestrictedTreatment? = null

    /** Sets the [AgeRestrictedTreatment] value. Useful for testing. */
    @JvmStatic
    fun setAgeRestrictedTreatment(value: AgeRestrictedTreatment?) {
      ageRestrictedTreatment = value
    }

    @JvmStatic
    fun getAgeRestrictedTreatment(): AgeRestrictedTreatment {
      ageRestrictedTreatment?.let {
        return it
      }

      return when (MobileAds.getRequestConfiguration().ageRestrictedTreatment) {
        SdkAgeRestrictedTreatment.CHILD -> CHILD
        SdkAgeRestrictedTreatment.TEEN -> TEEN
        SdkAgeRestrictedTreatment.UNSPECIFIED -> UNSPECIFIED
      }
    }
  }
}
