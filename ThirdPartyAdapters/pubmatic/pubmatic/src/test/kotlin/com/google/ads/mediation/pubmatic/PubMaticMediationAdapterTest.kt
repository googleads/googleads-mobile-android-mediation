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

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.OpenWrapSDK
import com.pubmatic.sdk.common.OpenWrapSDK.initialize
import com.pubmatic.sdk.common.OpenWrapSDK.setCoppa
import com.pubmatic.sdk.common.OpenWrapSDKConfig
import com.pubmatic.sdk.common.OpenWrapSDKInitializer
import com.pubmatic.sdk.common.POBError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times

@RunWith(AndroidJUnit4::class)
class PubMaticMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: PubMaticMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val openWrapSdkConfigCaptor = argumentCaptor<OpenWrapSDKConfig>()

  private val initializationCompleteCallback = mock<InitializationCompleteCallback>()

  private val adErrorStringCaptor = argumentCaptor<String>()

  private val openWrapSdkInitListenerCaptor = argumentCaptor<OpenWrapSDKInitializer.Listener>()

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

  // region initialize() tests

  @Test
  fun initialize_whenTFCDIsTrue_setsOpenWrapCoppaTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )

    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      adapter.initialize(
        context = context,
        initializationCompleteCallback = mock(),
        mediationConfigurations = emptyList(),
      )

      openWrapSdk.verify { setCoppa(eq(true)) }
    }
  }

  @Test
  fun initialize_whenTFCDIsFalse_setsOpenWrapCoppaFalse() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .build()
    )

    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      adapter.initialize(
        context = context,
        initializationCompleteCallback = mock(),
        mediationConfigurations = emptyList(),
      )

      openWrapSdk.verify { setCoppa(eq(false)) }
    }
  }

  @Test
  fun initialize_whenTFCDIsUnset_doesNotSetOpenWrapCoppa() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .build()
    )

    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      adapter.initialize(
        context = context,
        initializationCompleteCallback = mock(),
        mediationConfigurations = emptyList(),
      )

      openWrapSdk.verify({ setCoppa(any()) }, times(0))
    }
  }

  @Test
  fun initialize_initializesOpenWrapSdk() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

      adapter.initialize(context, mock(), listOf(mediationConfiguration))

      openWrapSdk.verify { initialize(eq(context), openWrapSdkConfigCaptor.capture(), any()) }
      val openWrapSdkConfig = openWrapSdkConfigCaptor.firstValue
      assertThat(openWrapSdkConfig.publisherId).isEqualTo(TEST_PUBLISHER_ID)
      assertThat(openWrapSdkConfig.profileIds.size).isEqualTo(1)
      assertThat(openWrapSdkConfig.profileIds[0]).isEqualTo(TEST_PROFILE_ID_1.toInt())
    }
  }

  @Test
  fun initialize_ifPublisherIdIsMissing_fails() {
    val serverParameters = bundleOf(PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1)
    val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    val expectedError =
      AdError(ERROR_MISSING_PUBLISHER_ID, "Publisher ID is missing.", ADAPTER_ERROR_DOMAIN)
    verify(initializationCompleteCallback).onInitializationFailed(adErrorStringCaptor.capture())
    assertThat(adErrorStringCaptor.firstValue).isEqualTo(expectedError.toString())
  }

  @Test
  fun initialize_withMultipleProfileIds_initializesOpenWrapSdkWithMultipleProfileIds() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters1 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration1 = MediationConfiguration(AdFormat.BANNER, serverParameters1)
      val serverParameters2 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_2,
        )
      val mediationConfiguration2 = MediationConfiguration(AdFormat.BANNER, serverParameters2)

      adapter.initialize(context, mock(), listOf(mediationConfiguration1, mediationConfiguration2))

      openWrapSdk.verify { initialize(eq(context), openWrapSdkConfigCaptor.capture(), any()) }
      val openWrapSdkConfig = openWrapSdkConfigCaptor.firstValue
      assertThat(openWrapSdkConfig.publisherId).isEqualTo(TEST_PUBLISHER_ID)
      assertThat(openWrapSdkConfig.profileIds.size).isEqualTo(2)
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_1.toInt())
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_2.toInt())
    }
  }

  @Test
  fun initialize_ifAProfileIdIsNotInt_stillInitializesSuccessfullyWithOtherProfileIds() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters1 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration1 = MediationConfiguration(AdFormat.BANNER, serverParameters1)
      val serverParameters2 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_2,
        )
      val mediationConfiguration2 = MediationConfiguration(AdFormat.BANNER, serverParameters2)
      val serverParameters3 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to INVALID_PROFILE_ID,
        )
      val mediationConfiguration3 = MediationConfiguration(AdFormat.BANNER, serverParameters3)

      adapter.initialize(
        context,
        mock(),
        listOf(mediationConfiguration1, mediationConfiguration2, mediationConfiguration3),
      )

      openWrapSdk.verify { initialize(eq(context), openWrapSdkConfigCaptor.capture(), any()) }
      val openWrapSdkConfig = openWrapSdkConfigCaptor.firstValue
      assertThat(openWrapSdkConfig.publisherId).isEqualTo(TEST_PUBLISHER_ID)
      assertThat(openWrapSdkConfig.profileIds.size).isEqualTo(2)
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_1.toInt())
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_2.toInt())
    }
  }

  @Test
  fun initialize_ifOpenWrapInitializationSucceeds_invokesInitializationSucceededCallback() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
      openWrapSdk.verify {
        initialize(
          eq(context),
          openWrapSdkConfigCaptor.capture(),
          openWrapSdkInitListenerCaptor.capture(),
        )
      }
      val openWrapSdkInitListener = openWrapSdkInitListenerCaptor.firstValue
      // Let OpenWrap SDK init succeed.
      openWrapSdkInitListener.onSuccess()

      verify(initializationCompleteCallback).onInitializationSucceeded()
    }
  }

  @Test
  fun initialize_ifOpenWrapInitializationFails_invokesInitializationFailedCallback() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
      openWrapSdk.verify {
        initialize(
          eq(context),
          openWrapSdkConfigCaptor.capture(),
          openWrapSdkInitListenerCaptor.capture(),
        )
      }
      val openWrapSdkInitListener = openWrapSdkInitListenerCaptor.firstValue
      // Let OpenWrap SDK init failure.
      val openWrapSdkInitError = POBError(OPENWRAP_INIT_ERROR_CODE, OPENWRAP_INIT_ERROR_MSG)
      openWrapSdkInitListener.onFailure(openWrapSdkInitError)

      val expectedError =
        AdError(
          OPENWRAP_INIT_ERROR_CODE,
          "INVALID_REQUEST: $OPENWRAP_INIT_ERROR_MSG",
          SDK_ERROR_DOMAIN,
        )
      verify(initializationCompleteCallback).onInitializationFailed(adErrorStringCaptor.capture())
      assertThat(adErrorStringCaptor.firstValue).isEqualTo(expectedError.toString())
    }
  }

  // endregion

  private companion object {
    const val TEST_PUBLISHER_ID = "a_pubmatic_publisher_id"
    const val TEST_PROFILE_ID_1 = "1234"
    const val TEST_PROFILE_ID_2 = "5678"
    // Profile ID should be parsable as an integer.
    const val INVALID_PROFILE_ID = "a123"
    const val OPENWRAP_INIT_ERROR_CODE = 1001
    const val OPENWRAP_INIT_ERROR_MSG = "Init failed"
  }
}
