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

import com.google.android.gms.ads.VersionInfo

/** Contains util functions for comparing two instances of [VersionInfo]. */
object VersionInfoCompareUtils {

  /** Returns true iff version1 is greater than or equal to version2. */
  fun isVersionGreaterThanOrEqualTo(version1: VersionInfo, version2: VersionInfo): Boolean {
    if (version1.majorVersion > version2.majorVersion) {
      return true
    } else if (version1.majorVersion == version2.majorVersion) {
      if (version1.minorVersion > version2.minorVersion) {
        return true
      } else if (version1.minorVersion == version2.minorVersion) {
        return version1.microVersion >= version2.microVersion
      }
    }
    return false
  }
}
