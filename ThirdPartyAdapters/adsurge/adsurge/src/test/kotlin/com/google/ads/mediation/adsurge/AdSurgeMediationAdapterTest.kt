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
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdSurgeMediationAdapterTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var fakeSdkWrapper: FakeSdkWrapper
  private lateinit var initializationCallback: FakeInitializationCompleteCallback
  private lateinit var signalCallbacks: FakeSignalCallbacks
  private lateinit var signalData: RtbSignalData
  private lateinit var adapter: AdSurgeMediationAdapter

  @Before
  fun setUp() {
    fakeSdkWrapper = FakeSdkWrapper()
    initializationCallback = FakeInitializationCompleteCallback()
    signalCallbacks = FakeSignalCallbacks()
    signalData = RtbSignalData(context, emptyList(), Bundle(), null)
    AdSurgeFactory.delegate = fakeSdkWrapper
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
    fakeSdkWrapper.sdkVersion = "2.0.1"

    adapter.assertGetSdkVersion(expectedValue = "2.0.1")
  }

  @Test
  fun getSDKVersionInfo_withInvalidVersion_returnsZeros() {
    fakeSdkWrapper.sdkVersion = "invalid"

    adapter.assertGetSdkVersion(expectedValue = "0.0.0")
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    AdSurgeMediationAdapter.adapterVersionDelegate = "2.0.1.0"

    adapter.assertGetVersionInfo(expectedValue = "2.0.100")
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

    adapter.initialize(context, initializationCallback, listOf(mediationConfig))

    assertThat(fakeSdkWrapper.initializedContext).isEqualTo(context)
    assertThat(fakeSdkWrapper.initializedAppId).isEqualTo("test_app_id")
    assertThat(initializationCallback.isInitializationSucceeded).isTrue()
  }

  @Test
  fun initialize_withMultipleAppIds_initializesWithFirstAppId() {
    val bundle1 = Bundle().apply { putString(AdSurgeUtils.KEY_APPLICATION_ID, "test_app_id_1") }
    val bundle2 = Bundle().apply { putString(AdSurgeUtils.KEY_APPLICATION_ID, "test_app_id_2") }
    val config1 = MediationConfiguration(AdFormat.BANNER, bundle1)
    val config2 = MediationConfiguration(AdFormat.INTERSTITIAL, bundle2)

    adapter.initialize(context, initializationCallback, listOf(config1, config2))

    assertThat(fakeSdkWrapper.initCallCount).isEqualTo(1)
    assertThat(fakeSdkWrapper.initializedAppId).isEqualTo("test_app_id_1")
    assertThat(initializationCallback.isInitializationSucceeded).isTrue()
  }

  @Test
  fun initialize_withEmptyConfiguration_invokesOnInitializationFailed() {
    adapter.initialize(context, initializationCallback, emptyList())

    assertThat(initializationCallback.isInitializationFailed).isTrue()
    assertThat(initializationCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(initializationCallback.adError?.message)
      .contains("Missing or invalid application_id")
    assertThat(initializationCallback.adError?.domain)
      .isEqualTo(AdSurgeMediationAdapter.ADAPTER_ERROR_DOMAIN)
    assertThat(fakeSdkWrapper.initCallCount).isEqualTo(0)
  }

  @Test
  fun initialize_withMissingAppId_invokesOnInitializationFailed() {
    val bundle = Bundle()
    val mediationConfig = MediationConfiguration(AdFormat.BANNER, bundle)

    adapter.initialize(context, initializationCallback, listOf(mediationConfig))

    assertThat(initializationCallback.isInitializationFailed).isTrue()
    assertThat(initializationCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(initializationCallback.adError?.message)
      .contains("Missing or invalid application_id")
    assertThat(initializationCallback.adError?.domain)
      .isEqualTo(AdSurgeMediationAdapter.ADAPTER_ERROR_DOMAIN)
    assertThat(fakeSdkWrapper.initCallCount).isEqualTo(0)
  }

  @Test
  fun initialize_withEmptyAppId_invokesOnInitializationFailed() {
    val bundle = Bundle().apply { putString(AdSurgeUtils.KEY_APPLICATION_ID, "") }
    val mediationConfig = MediationConfiguration(AdFormat.BANNER, bundle)

    adapter.initialize(context, initializationCallback, listOf(mediationConfig))

    assertThat(initializationCallback.isInitializationFailed).isTrue()
    assertThat(initializationCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(initializationCallback.adError?.message)
      .contains("Missing or invalid application_id")
    assertThat(initializationCallback.adError?.domain)
      .isEqualTo(AdSurgeMediationAdapter.ADAPTER_ERROR_DOMAIN)
    assertThat(fakeSdkWrapper.initCallCount).isEqualTo(0)
  }

  @Test
  fun initialize_whenSdkInitFails_invokesOnInitializationFailed() {
    val bundle = Bundle().apply { putString(AdSurgeUtils.KEY_APPLICATION_ID, "test_app_id") }
    val mediationConfig = MediationConfiguration(AdFormat.BANNER, bundle)
    fakeSdkWrapper.initError = AdSurgeAdError(500, "SDK init failed")

    adapter.initialize(context, initializationCallback, listOf(mediationConfig))

    assertThat(initializationCallback.isInitializationFailed).isTrue()
    assertThat(initializationCallback.adError?.code).isEqualTo(500)
    assertThat(initializationCallback.adError?.message).isEqualTo("SDK init failed")
    assertThat(initializationCallback.adError?.domain)
      .isEqualTo(AdSurgeMediationAdapter.SDK_ERROR_DOMAIN)
  }

  // endregion

  // region Signal Collection tests
  @Test
  fun collectSignals_whenSuccessful_invokesOnSuccessWithToken() {
    fakeSdkWrapper.bidderToken = "test_bid_token"

    adapter.collectSignals(signalData, signalCallbacks)

    assertThat(signalCallbacks.isSuccess).isTrue()
    assertThat(signalCallbacks.signals).isEqualTo("test_bid_token")
  }

  @Test
  fun collectSignals_whenSdkFails_invokesOnFailureWithAdError() {
    fakeSdkWrapper.bidderTokenError = AdSurgeAdError(400, "Token failure")

    adapter.collectSignals(signalData, signalCallbacks)

    assertThat(signalCallbacks.isFailure).isTrue()
    assertThat(signalCallbacks.adError?.code).isEqualTo(400)
    assertThat(signalCallbacks.adError?.message).isEqualTo("Token failure")
    assertThat(signalCallbacks.adError?.domain).isEqualTo(AdSurgeMediationAdapter.SDK_ERROR_DOMAIN)
  }
  // endregion
}
