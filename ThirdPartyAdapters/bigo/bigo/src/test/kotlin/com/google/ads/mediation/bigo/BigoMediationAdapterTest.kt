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

package com.google.ads.mediation.bigo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BigoMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: BigoMediationAdapter

  @Before
  fun setUp() {
    adapter = BigoMediationAdapter()
  }

  // region Version tests
  @Test
  fun getSDKVersionInfo_returnsValidVersionInfo() {
    BigoMediationAdapter.bigoSdkVersionDelegate = "1.2.3"

    adapter.assertGetSdkVersion(expectedValue = "1.2.3")
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    BigoMediationAdapter.adapterVersionDelegate = "1.2.3.1"

    adapter.assertGetVersionInfo(expectedValue = "1.2.301")
  }

  // endregion

  // TODO: Add tests for the methods that are implemented in the adapter.
}
