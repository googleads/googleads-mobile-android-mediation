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

package com.google.ads.mediation.bidmachine

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_EMPTY_SIGNAL_CONFIGURATIONS
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_INVALID_AD_FORMAT
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_INVALID_AD_SIZE
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_EMPTY_SIGNAL_CONFIGURATIONS
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_INVALID_AD_FORMAT
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_INVALID_AD_SIZE
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_MISSING_SOURCE_ID
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SOURCE_ID_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import io.bidmachine.AdPlacementConfig
import io.bidmachine.AdsFormat
import io.bidmachine.BidMachine
import io.bidmachine.BidTokenCallback
import io.bidmachine.InitializationCallback
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class BidMachineMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: BidMachineMediationAdapter
  private lateinit var mockBidMachine: MockedStatic<BidMachine>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCallback: InitializationCompleteCallback = mock()
  private val mockBannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val mockInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mockRewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()

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
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    val callbackCaptor = argumentCaptor<InitializationCallback>()

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBidMachine.verify { BidMachine.setCoppa(eq(true)) }
    mockBidMachine.verify {
      BidMachine.initialize(eq(context), eq(TEST_SOURCE_ID), callbackCaptor.capture())
    }
    callbackCaptor.firstValue.onInitialized()
    verify(mockInitializationCallback).onInitializationSucceeded()
  }

  @Test
  fun initialize_tagForChildFalse_setBidMachineCoppaToFalse() {
    val mediationConfiguration =
      MediationConfiguration(
        AdFormat.BANNER,
        /* serverParameters= */ bundleOf(SOURCE_ID_KEY to TEST_SOURCE_ID),
      )
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .build()
    )

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBidMachine.verify { BidMachine.setCoppa(eq(false)) }
  }

  @Test
  fun initialize_tagForChildUnspecified_setBidMachineCoppaToFalse() {
    val mediationConfiguration =
      MediationConfiguration(
        AdFormat.BANNER,
        /* serverParameters= */ bundleOf(SOURCE_ID_KEY to TEST_SOURCE_ID),
      )
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .build()
    )

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    mockBidMachine.verify { BidMachine.setCoppa(eq(false)) }
  }

  // endregion

  // region collectSignals tests
  @Test
  fun collectSignals_withEmptyMediationConfiguration_invokesOnFailure() {
    val signalData =
      RtbSignalData(
        context,
        /* configurations = */ listOf<MediationConfiguration>(),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val mockSignalCallbacks: SignalCallbacks = mock()
    val expectedAdError =
      AdError(
        ERROR_CODE_EMPTY_SIGNAL_CONFIGURATIONS,
        ERROR_MSG_EMPTY_SIGNAL_CONFIGURATIONS,
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.collectSignals(signalData, mockSignalCallbacks)

    mockSignalCallbacks.onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun collectSignals_withInvalidAdFormat_invokesOnFailure() {
    val configuration =
      createMediationConfiguration(AdFormat.APP_OPEN_AD, /* serverParameters= */ bundleOf())
    val signalData =
      RtbSignalData(
        context,
        /* configurations = */ listOf<MediationConfiguration>(configuration),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val mockSignalCallbacks: SignalCallbacks = mock()
    val expectedAdError =
      AdError(ERROR_CODE_INVALID_AD_FORMAT, ERROR_MSG_INVALID_AD_FORMAT, ADAPTER_ERROR_DOMAIN)

    adapter.collectSignals(signalData, mockSignalCallbacks)

    mockSignalCallbacks.onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun collectSignals_invokesOnSuccess() {
    val configuration =
      createMediationConfiguration(AdFormat.INTERSTITIAL, /* serverParameters= */ bundleOf())
    val signalData =
      RtbSignalData(
        context,
        /* configurations = */ listOf<MediationConfiguration>(configuration),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val mockSignalCallbacks: SignalCallbacks = mock()
    val tokenCallbackCaptor = argumentCaptor<BidTokenCallback>()

    adapter.collectSignals(signalData, mockSignalCallbacks)

    mockBidMachine.verify {
      BidMachine.getBidToken(eq(context), any<AdPlacementConfig>(), tokenCallbackCaptor.capture())
    }
    tokenCallbackCaptor.firstValue.onCollected(TEST_BID_RESPONSE)
    mockSignalCallbacks.onSuccess(TEST_BID_RESPONSE)
  }

  @Test
  fun collectSignals_mapsCorrectFormatToBitMachineAdsFormat() {
    val configurationBanner =
      createMediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())
    val configurationRewarded =
      createMediationConfiguration(AdFormat.REWARDED, /* serverParameters= */ bundleOf())
    val configurationNative =
      createMediationConfiguration(AdFormat.NATIVE, /* serverParameters= */ bundleOf())
    val signalDataBanner =
      RtbSignalData(
        context,
        /* configurations = */ listOf<MediationConfiguration>(configurationBanner),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val signalDataRewarded =
      RtbSignalData(
        context,
        /* configurations = */ listOf<MediationConfiguration>(configurationRewarded),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val signalDataNative =
      RtbSignalData(
        context,
        /* configurations = */ listOf<MediationConfiguration>(configurationNative),
        /* networkExtras = */ bundleOf(),
        /* adSize = */ null,
      )
    val mockSignalCallbacks: SignalCallbacks = mock()
    val configCaptor = argumentCaptor<AdPlacementConfig>()

    adapter.collectSignals(signalDataBanner, mockSignalCallbacks)

    adapter.collectSignals(signalDataRewarded, mockSignalCallbacks)

    adapter.collectSignals(signalDataNative, mockSignalCallbacks)
    mockBidMachine.verify(
      { BidMachine.getBidToken(eq(context), configCaptor.capture(), any()) },
      times(3),
    )
    assertThat(configCaptor.firstValue.adsFormat).isEqualTo(AdsFormat.Banner)
    assertThat(configCaptor.secondValue.adsFormat).isEqualTo(AdsFormat.Rewarded)
    assertThat(configCaptor.thirdValue.adsFormat).isEqualTo(AdsFormat.Native)
  }

  // endregion

  // region banner tests
  @Test
  fun loadRtbBannerAd_withNonSupportedAdSize_invokesOnFailure() {
    val bannerAdConfiguration =
      createMediationBannerAdConfiguration(context, adSize = AdSize.FULL_BANNER)
    val expectedAdError =
      AdError(ERROR_CODE_INVALID_AD_SIZE, ERROR_MSG_INVALID_AD_SIZE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbBannerAd(bannerAdConfiguration, mockBannerAdLoadCallback)

    verify(mockBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  private companion object {
    const val TEST_SOURCE_ID = "testSourceId"
  }
}
