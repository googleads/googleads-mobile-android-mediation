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

package com.google.ads.mediation.pubmatic

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PubMaticMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: PubMaticMediationAdapter

  @Before
  fun setUp() {
    adapter = PubMaticMediationAdapter()
  }

  // region Version tests
  @Test
  fun getSDKVersionInfo_returnsValidVersionInfo() {
    val sdkVersion = adapter.sdkVersionInfo

    assertThat(sdkVersion.majorVersion).isGreaterThan(0)
    assertThat(sdkVersion.majorVersion).isLessThan(100)
    assertThat(sdkVersion.minorVersion).isAtLeast(0)
    assertThat(sdkVersion.minorVersion).isLessThan(100)
    assertThat(sdkVersion.microVersion).isAtLeast(0)
    assertThat(sdkVersion.microVersion).isLessThan(100)
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    val adapterVersion = adapter.versionInfo

    assertThat(adapterVersion.majorVersion).isGreaterThan(0)
    assertThat(adapterVersion.majorVersion).isLessThan(100)
    assertThat(adapterVersion.minorVersion).isAtLeast(0)
    assertThat(adapterVersion.minorVersion).isLessThan(100)
    assertThat(adapterVersion.microVersion).isAtLeast(0)
    assertThat(adapterVersion.microVersion).isLessThan(100)
  }

  // endregion

  // TODO: Add tests for the methods that are implemented in the adapter.
}
