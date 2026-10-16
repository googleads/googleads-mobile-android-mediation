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

package com.google.ads.mediation.adsurge

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adsurge.adn.ads.AdSurgeAdError
import com.adsurge.adn.managers.AdSurgeAdSdk
import com.adsurge.adn.managers.OnStartListener
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AdSurgeMediationAdapterTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mockSdkWrapper: SdkWrapper = mock()
  private val mockInitializationCallback: InitializationCompleteCallback = mock()
  private val mockSignalCallbacks: SignalCallbacks = mock()
  private val mockSignalData: RtbSignalData = mock()

  private lateinit var adapter: AdSurgeMediationAdapter

  @Before
  fun setUp() {
    AdSurgeFactory.delegate = mockSdkWrapper
    adapter = AdSurgeMediationAdapter()
  }

  @After
  fun tearDown() {
    AdSurgeMediationAdapter.adapterVersionDelegate = null
    AdSurgeMediationAdapter.sdkVersionDelegate = null
    AdSurgeFactory.reset()
  }

  // region Version tests
  @Test
  fun getSDKVersionInfo_returnsValidVersionInfo() {
    whenever(mockSdkWrapper.getSdkVersion()).thenReturn("1.9.9")

    adapter.assertGetSdkVersion(expectedValue = "1.9.9")
  }

  @Test
  fun getSDKVersionInfo_withInvalidVersion_returnsZeros() {
    whenever(mockSdkWrapper.getSdkVersion()).thenReturn("invalid")

    adapter.assertGetSdkVersion(expectedValue = "0.0.0")
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    AdSurgeMediationAdapter.adapterVersionDelegate = "1.9.9.0"

    adapter.assertGetVersionInfo(expectedValue = "1.9.900")
  }

  @Test
  fun getVersionInfo_withInvalidVersion_returnsZeros() {
    AdSurgeMediationAdapter.adapterVersionDelegate = "invalid"

    adapter.assertGetVersionInfo(expectedValue = "0.0.0")
  }

  // endregion

  // region Initialize tests
  @Test
  fun initialize_withValidConfiguration_invokesOnInitializationSucceeded() {
    val bundle = Bundle().apply { putString(AdSurgeUtils.KEY_APPLICATION_ID, "test_app_id") }
    val mediationConfig = MediationConfiguration(AdFormat.BANNER, bundle)

    doAnswer { invocation ->
        val listener = invocation.getArgument<OnStartListener>(2)
        listener.onStartComplete()
        null
      }
      .whenever(mockSdkWrapper)
      .init(eq(context), eq("test_app_id"), any())

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfig))

    verify(mockInitializationCallback).onInitializationSucceeded()
  }

  @Test
  fun initialize_withEmptyConfiguration_invokesOnInitializationFailed() {
    adapter.initialize(context, mockInitializationCallback, emptyList())

    val captor = argumentCaptor<AdError>()
    verify(mockInitializationCallback).onInitializationFailed(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(captor.firstValue.message).contains("Missing or invalid application_id")
    assertThat(captor.firstValue.domain).isEqualTo(AdSurgeMediationAdapter.ADAPTER_ERROR_DOMAIN)
    verify(mockSdkWrapper, never()).init(any(), any(), any())
  }

  @Test
  fun initialize_withMissingAppId_invokesOnInitializationFailed() {
    val bundle = Bundle()
    val mediationConfig = MediationConfiguration(AdFormat.BANNER, bundle)

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfig))

    val captor = argumentCaptor<AdError>()
    verify(mockInitializationCallback).onInitializationFailed(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(captor.firstValue.message).contains("Missing or invalid application_id")
    assertThat(captor.firstValue.domain).isEqualTo(AdSurgeMediationAdapter.ADAPTER_ERROR_DOMAIN)
    verify(mockSdkWrapper, never()).init(any(), any(), any())
  }

  @Test
  fun initialize_whenSdkInitFails_invokesOnInitializationFailed() {
    val bundle = Bundle().apply { putString(AdSurgeUtils.KEY_APPLICATION_ID, "test_app_id") }
    val mediationConfig = MediationConfiguration(AdFormat.BANNER, bundle)

    doAnswer { invocation ->
        val listener = invocation.getArgument<OnStartListener>(2)
        listener.onStartFailed(AdSurgeAdError(500, "SDK init failed"))
        null
      }
      .whenever(mockSdkWrapper)
      .init(eq(context), eq("test_app_id"), any())

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfig))

    val captor = argumentCaptor<AdError>()
    verify(mockInitializationCallback).onInitializationFailed(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(500)
    assertThat(captor.firstValue.message).isEqualTo("SDK init failed")
    assertThat(captor.firstValue.domain).isEqualTo(AdSurgeMediationAdapter.SDK_ERROR_DOMAIN)
  }

  // endregion

  // region Signal Collection tests
  @Test
  fun collectSignals_whenSuccessful_invokesOnSuccessWithToken() {
    doAnswer { invocation ->
        val callback = invocation.getArgument<AdSurgeAdSdk.BidderTokenCallback>(0)
        callback.onSuccess("test_bid_token")
        null
      }
      .whenever(mockSdkWrapper)
      .getBidderToken(any())

    adapter.collectSignals(mockSignalData, mockSignalCallbacks)

    verify(mockSignalCallbacks).onSuccess("test_bid_token")
  }

  @Test
  fun collectSignals_whenSdkFails_invokesOnFailureWithAdError() {
    doAnswer { invocation ->
        val callback = invocation.getArgument<AdSurgeAdSdk.BidderTokenCallback>(0)
        callback.onFailure(AdSurgeAdError(400, "Token failure"))
        null
      }
      .whenever(mockSdkWrapper)
      .getBidderToken(any())

    adapter.collectSignals(mockSignalData, mockSignalCallbacks)

    val captor = argumentCaptor<AdError>()
    verify(mockSignalCallbacks).onFailure(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(400)
    assertThat(captor.firstValue.message).isEqualTo("Token failure")
    assertThat(captor.firstValue.domain).isEqualTo(AdSurgeMediationAdapter.SDK_ERROR_DOMAIN)
  }
  // endregion
}
