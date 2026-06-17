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

/** Tests for [AgeRestrictedTreatmentUtils]. */
@RunWith(AndroidJUnit4::class)
class AgeRestrictedTreatmentUtilsTest {

  @Test
  fun runtimeGmaSdkSupportsChildAgeRestrictedTreatment_classicGmaSdk_returnsTrue() {
    assertThat(
        AgeRestrictedTreatmentUtils.runtimeGmaSdkSupportsChildAgeRestrictedTreatment(
          VersionInfo(25, 3, 0)
        )
      )
      .isTrue()
  }

  @Test
  fun runtimeGmaSdkSupportsChildAgeRestrictedTreatment_nextGenGmaSdkAtLeast120_returnsTrue() {
    assertThat(
        AgeRestrictedTreatmentUtils.runtimeGmaSdkSupportsChildAgeRestrictedTreatment(
          VersionInfo(1, 2, 0)
        )
      )
      .isTrue()
  }

  @Test
  fun runtimeGmaSdkSupportsChildAgeRestrictedTreatment_nextGenGmaSdkBelow120_returnsFalse() {
    assertThat(
        AgeRestrictedTreatmentUtils.runtimeGmaSdkSupportsChildAgeRestrictedTreatment(
          VersionInfo(1, 1, 9)
        )
      )
      .isFalse()
  }
}
