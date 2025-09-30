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

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.APP_ID_KEY
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_MSG_MISSING_APP_ID
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import sg.bigo.ads.BigoAdSdk

@RunWith(AndroidJUnit4::class)
class BigoMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: BigoMediationAdapter
  private lateinit var mockBigoSdk: MockedStatic<BigoAdSdk>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCallback: InitializationCompleteCallback = mock()

  @Before
  fun setUp() {
    adapter = BigoMediationAdapter()
    mockBigoSdk = mockStatic(BigoAdSdk::class.java)
  }

  @After
  fun tearDown() {
    mockBigoSdk.close()
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

  // region Initialize tests
  @Test
  fun initialize_withEmptyConfiguration_invokesOnInitializationFailed() {
    adapter.initialize(context, mockInitializationCallback, mediationConfigurations = listOf())

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_APP_ID))
  }

  @Test
  fun initialize_withoutAnySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_APP_ID))
  }

  @Test
  fun initialize_withEmptySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf(APP_ID_KEY to ""))

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_APP_ID))
  }

  @Test
  fun initialize_invokesOnInitializationSucceeded() {
    val mediationConfiguration =
      MediationConfiguration(
        AdFormat.BANNER,
        /* serverParameters= */ bundleOf(APP_ID_KEY to TEST_APP_ID),
      )
    val callbackCaptor = argumentCaptor<BigoAdSdk.InitListener>()

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.initialize(eq(context), any(), callbackCaptor.capture()) }
    callbackCaptor.firstValue.onInitialized()
    verify(mockInitializationCallback).onInitializationSucceeded()
  }

  // endregion

}
