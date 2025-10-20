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
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.APP_ID_KEY
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_CODE_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_MSG_MISSING_APP_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_MSG_MISSING_SLOT_ID
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions

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
  fun initialize_tagForChildTrue_invokesOnInitializationSucceededAndBigoCoppaToFalse() {
    val mediationConfiguration =
      MediationConfiguration(
        AdFormat.BANNER,
        /* serverParameters= */ bundleOf(APP_ID_KEY to TEST_APP_ID),
      )
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    val callbackCaptor = argumentCaptor<BigoAdSdk.InitListener>()

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.initialize(eq(context), any(), callbackCaptor.capture()) }
    callbackCaptor.firstValue.onInitialized()
    verify(mockInitializationCallback).onInitializationSucceeded()
    mockBigoSdk.verify {
      BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(false))
    }
  }

  @Test
  fun initialize_tagForChildFalse_setBigoCoppaToTrue() {
    val mediationConfiguration =
      MediationConfiguration(
        AdFormat.BANNER,
        /* serverParameters= */ bundleOf(APP_ID_KEY to TEST_APP_ID),
      )
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .build()
    )

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(true)) }
  }

  @Test
  fun initialize_tagForChildUnspecified_setBigoCoppaToTrue() {
    val mediationConfiguration =
      MediationConfiguration(
        AdFormat.BANNER,
        /* serverParameters= */ bundleOf(APP_ID_KEY to TEST_APP_ID),
      )
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .build()
    )

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(true)) }
  }

  // endregion

  // region Collect Signals tests
  @Test
  fun collectSignals_invokesOnSuccess() {
    whenever(BigoAdSdk.getBidderToken()) doReturn TEST_BID_RESPONSE
    val configuration =
      createMediationConfiguration(AdFormat.INTERSTITIAL, /* serverParameters= */ bundleOf())
    val signalData =
      RtbSignalData(
        context,
        /* configurations = */ listOf(configuration),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val mockSignalCallbacks: SignalCallbacks = mock()

    adapter.collectSignals(signalData, mockSignalCallbacks)

    mockBigoSdk.verify { BigoAdSdk.getBidderToken() }
    mockSignalCallbacks.onSuccess(TEST_BID_RESPONSE)
  }

  @Test
  fun collectSignals_withNullSginals_invokesOnSuccessWithEmptySignals() {
    whenever(BigoAdSdk.getBidderToken()) doReturn null
    val configuration =
      createMediationConfiguration(AdFormat.INTERSTITIAL, /* serverParameters= */ bundleOf())
    val signalData =
      RtbSignalData(
        context,
        /* configurations = */ listOf(configuration),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val mockSignalCallbacks: SignalCallbacks = mock()

    adapter.collectSignals(signalData, mockSignalCallbacks)

    mockBigoSdk.verify { BigoAdSdk.getBidderToken() }
    mockSignalCallbacks.onSuccess("")
  }

  // endregion

  // region Banner tests
  @Test
  fun loadRtbBannerAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationBannerAdConfiguration(context, adSize = AdSize.BANNER)
    val mockBannerAdLoadCallback =
      mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbBannerAd(adConfiguration, mockBannerAdLoadCallback)

    verify(mockBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Interstitial tests
  @Test
  fun loadRtbInterstitialAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationInterstitialAdConfiguration(context)
    val mockInterstitialAdLoadCallback =
      mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbInterstitialAd(adConfiguration, mockInterstitialAdLoadCallback)

    verify(mockInterstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Rewarded tests
  @Test
  fun loadRtbRewardedAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationRewardedAdConfiguration(context)
    val mockRewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbRewardedAd(adConfiguration, mockRewardedAdLoadCallback)

    verify(mockRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Rewarded Interstitial tests
  @Test
  fun loadRtbRewardedInterstitialAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationRewardedAdConfiguration(context)
    val mockRewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbRewardedInterstitialAd(adConfiguration, mockRewardedAdLoadCallback)

    verify(mockRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region AppOpen tests
  @Test
  fun loadRtbAppOpenAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationAppOpenAdConfiguration(context)
    val mockAppOpenAdLoadCallback =
      mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>>()
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbAppOpenAd(adConfiguration, mockAppOpenAdLoadCallback)

    verify(mockAppOpenAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Native tests
  @Test
  fun loadRtbNativeAdMapper_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationNativeAdConfiguration(context)
    val mockNativeAdLoadCallback =
      mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>>()
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbNativeAdMapper(adConfiguration, mockNativeAdLoadCallback)

    verify(mockNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

}
