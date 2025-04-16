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

package com.google.ads.mediation.bidmachine

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_MISSING_SOURCE_ID
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SOURCE_ID_KEY
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import io.bidmachine.BidMachine
import io.bidmachine.InitializationCallback
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class BidMachineMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: BidMachineMediationAdapter
  private lateinit var mockBidMachine: MockedStatic<BidMachine>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCallback: InitializationCompleteCallback = mock()

  @Before
  fun setUp() {
    adapter = BidMachineMediationAdapter()
    mockBidMachine = mockStatic(BidMachine::class.java)
  }

  @After
  fun tearDown() {
    mockBidMachine.close()
  }

  // region Version tests
  @Test
  fun getSDKVersionInfo_returnsValidVersionInfo() {
    BidMachineMediationAdapter.bidMachineSdkVersionDelegate = "1.2.3"

    adapter.assertGetSdkVersion(expectedValue = "1.2.3")
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    BidMachineMediationAdapter.adapterVersionDelegate = "1.2.3.4"

    adapter.assertGetVersionInfo(expectedValue = "1.2.304")
  }

  // endregion

  // region initialize tests
  @Test
  fun initialize_withEmptyConfiguration_invokesOnInitializationFailed() {
    adapter.initialize(context, mockInitializationCallback, mediationConfigurations = listOf())

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_SOURCE_ID))
  }

  @Test
  fun initialize_withoutAnySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_SOURCE_ID))
  }

  @Test
  fun initialize_withEmptySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf(SOURCE_ID_KEY to ""))

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_SOURCE_ID))
  }

  @Test
  fun initialize_invokesOnInitializationSucceeded() {
    val mediationConfiguration =
      MediationConfiguration(
        AdFormat.BANNER,
        /* serverParameters= */ bundleOf(SOURCE_ID_KEY to TEST_SOURCE_ID),
      )
    val callbackCaptor = argumentCaptor<InitializationCallback>()

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBidMachine.verify {
      BidMachine.initialize(eq(context), eq(TEST_SOURCE_ID), callbackCaptor.capture())
    }
    callbackCaptor.firstValue.onInitialized()
    verify(mockInitializationCallback).onInitializationSucceeded()
  }

  // endregion

  private companion object {
    const val TEST_SOURCE_ID = "testSourceId"
  }
}
