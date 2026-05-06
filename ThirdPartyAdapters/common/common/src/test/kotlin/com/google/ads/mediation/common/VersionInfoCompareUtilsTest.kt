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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.VersionInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [VersionInfoCompareUtils]. */
@RunWith(AndroidJUnit4::class)
class VersionInfoCompareUtilsTest {

  @Test
  fun isVersionGreaterThanOrEqualTo_ifVersionIsGreater_returnsTrue() {
    val versionInfo1 = VersionInfo(21, 3, 1)
    val versionInfo2 = VersionInfo(21, 2, 3)
    assertThat(VersionInfoCompareUtils.isVersionGreaterThanOrEqualTo(versionInfo1, versionInfo2))
      .isTrue()
  }

  @Test
  fun isVersionGreaterThanOrEqualTo_ifVersionIsLesser_returnsFalse() {
    val versionInfo1 = VersionInfo(21, 2, 3)
    val versionInfo2 = VersionInfo(21, 3, 1)
    assertThat(VersionInfoCompareUtils.isVersionGreaterThanOrEqualTo(versionInfo1, versionInfo2))
      .isFalse()
  }
}
