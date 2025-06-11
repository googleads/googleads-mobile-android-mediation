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

package com.google.ads.mediation.verve

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import net.pubnative.lite.sdk.HyBid
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class VerveMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: VerveMediationAdapter

  @Before
  fun setUp() {
    adapter = VerveMediationAdapter()
  }

  // region Version tests
  @Test
  fun getSDKVersionInfo_withInvalidVersion_returnsZeroes() {
    mockStatic(HyBid::class.java).use {
      whenever(HyBid.getHyBidVersion()) doReturn "3.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSDKVersionInfo_returnsValidVersionInfo() {
    mockStatic(HyBid::class.java).use {
      whenever(HyBid.getHyBidVersion()) doReturn "3.2.1"

      adapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getVersionInfo_withInvalidVersion_returnsZeroes() {
    VerveMediationAdapter.adapterVersionDelegate = "1.2.3"

    adapter.assertGetVersionInfo(expectedValue = "0.0.0")
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    VerveMediationAdapter.adapterVersionDelegate = "1.2.3.4"

    adapter.assertGetVersionInfo(expectedValue = "1.2.304")
  }

  // endregion

  // TODO: Add tests for the methods that are implemented in the adapter.
}
