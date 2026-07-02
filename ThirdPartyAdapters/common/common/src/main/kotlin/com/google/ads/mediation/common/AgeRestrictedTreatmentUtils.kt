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

package com.google.ads.mediation.common

import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VersionInfo

/** Contains util functions for comparing two instances of [VersionInfo]. */
object AgeRestrictedTreatmentUtils {
  /**
   * Checks whether the runtime GMA SDK supports checking
   * [com.google.android.gms.ads.RequestConfiguration.getAgeRestrictedTreatment] ==
   * [com.google.android.gms.ads.AgeRestrictedTreatment.CHILD].
   *
   * Classic GMA SDK (play-services-ads) supports it regardless of version. NextGen GMA SDK
   * (ads-mobile-sdk) supports it for versions >= 1.2.0.
   */
  @JvmStatic
  @JvmOverloads
  fun runtimeGmaSdkSupportsChildAgeRestrictedTreatment(
    gmaVersion: VersionInfo = MobileAds.getVersion()
  ): Boolean {
    return VersionInfoCompareUtils.isVersionGreaterThanOrEqualTo(gmaVersion, VersionInfo(1, 2, 0))
  }
}
