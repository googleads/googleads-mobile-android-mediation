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

package com.google.ads.mediation.common

import com.google.ads.mediation.common.VersionInfoCompareUtils.isVersionGreaterThanOrEqualTo
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VersionInfo

/** Contains helper functions for mediation adapters' native ad implementation. */
object NativeAdHelper {

  /**
   * Checks whether the runtime GMA SDK is a version of GMA SDK that listens to adapter-reported
   * native ad impressions.
   *
   * GMA SDK versions >= 24.4.0 listen to adapter-reported native ad impressions.
   */
  @JvmStatic
  fun runtimeGmaSdkListensToAdapterReportedImpressions() =
    isVersionGreaterThanOrEqualTo(MobileAds.getVersion(), VersionInfo(24, 4, 0))
}
