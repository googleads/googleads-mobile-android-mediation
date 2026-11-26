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
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_MISSING_SOURCE_ID
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_NO_SUPPORTED_SIGNAL_CONFIGURATIONS
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_SDK_NOT_INITIALIZED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_MISSING_SOURCE_ID
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_NO_SUPPORTED_SIGNAL_CONFIGURATIONS
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_SDK_NOT_INITIALIZED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.MEDIATION_NAME
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SOURCE_ID_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
import com.google.common.truth.Truth.assertThat
import io.bidmachine.BidMachine
import io.bidmachine.BidMachineInstance
import io.bidmachine.BidTokenCallback
import io.bidmachine.model.BannerAdSize
import io.bidmachine.model.BidMachineConfiguration
import io.bidmachine.model.ConsentData
import io.bidmachine.model.Placement
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class BidMachineMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: BidMachineMediationAdapter
  private lateinit var mockBidMachine: MockedStatic<BidMachine>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockBidMachineInstance = mock<BidMachineInstance>()
  private val initializationCompleteCallback = FakeInitializationCompleteCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>()

  @Before
  fun setUp() {
    mockBidMachine = mockStatic(BidMachine::class.java)
    mockBidMachine
      .`when`<BidMachineInstance> {
        BidMachine.initialize(any(), any(), anyOrNull<BidMachineConfiguration>())
      }
      .thenReturn(mockBidMachineInstance)
    adapter = BidMachineMediationAdapter()
    BidMachineMediationAdapter.bidMachineSdkVersionDelegate = null
    BidMachineMediationAdapter.adapterVersionDelegate = null
    BidMachineMediationAdapter.bidMachineInstance = null
    MobileAds.setRequestConfiguration(RequestConfiguration.Builder().build())
  }

  @After
  fun tearDown() {
    BidMachineMediationAdapter.bidMachineInstance = null
    mockBidMachine.close()
  }

  // region Version tests
  @Test
  fun getSDKVersionInfo_returnsValidVersionInfo() {
    BidMachineMediationAdapter.bidMachineSdkVersionDelegate = "1.2.3"

    adapter.assertGetSdkVersion(expectedValue = "1.2.3")
  }

  @Test
  fun getSDKVersionInfo_withInvalidVersion_returnsZeros() {
    BidMachineMediationAdapter.bidMachineSdkVersionDelegate = "invalid"

    adapter.assertGetSdkVersion(expectedValue = "0.0.0")
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    BidMachineMediationAdapter.adapterVersionDelegate = "1.2.3.4"

    adapter.assertGetVersionInfo(expectedValue = "1.2.304")
  }

  @Test
  fun getVersionInfo_with5Digits_returnsValidVersionInfo() {
    BidMachineMediationAdapter.adapterVersionDelegate = "1.2.3.4.5"

    adapter.assertGetVersionInfo(expectedValue = "1.2.304")
  }

  @Test
  fun getVersionInfo_withInvalidVersion_returnsZeros() {
    BidMachineMediationAdapter.adapterVersionDelegate = "invalid"

    adapter.assertGetVersionInfo(expectedValue = "0.0.0")
  }

  // endregion

  // region initialize tests
  @Test
  fun initialize_withEmptyConfiguration_invokesOnInitializationFailed() {
    adapter.initialize(context, initializationCompleteCallback, mediationConfigurations = listOf())

    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SOURCE_ID, ERROR_MSG_MISSING_SOURCE_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(initializationCompleteCallback).hasFailedWith(expectedAdError)
    mockBidMachine.verify(
      { BidMachine.initialize(any(), any(), anyOrNull<BidMachineConfiguration>()) },
      never(),
    )
  }

  @Test
  fun initialize_withoutAnySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SOURCE_ID, ERROR_MSG_MISSING_SOURCE_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(initializationCompleteCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun initialize_withEmptySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf(SOURCE_ID_KEY to ""))

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SOURCE_ID, ERROR_MSG_MISSING_SOURCE_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(initializationCompleteCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun initialize_initializesBidMachineWithMediationInfoAndInvokesOnInitializationSucceeded() {
    val configurationCaptor = argumentCaptor<BidMachineConfiguration>()

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createSourceIdConfiguration(TEST_SOURCE_ID)),
    )

    mockBidMachine.verify {
      BidMachine.initialize(eq(context), eq(TEST_SOURCE_ID), configurationCaptor.capture())
    }
    val mediationInfo = configurationCaptor.firstValue.mediationInfo
    assertThat(mediationInfo).isNotNull()
    assertThat(mediationInfo?.name).isEqualTo(MEDIATION_NAME)
    assertThat(mediationInfo?.adapterVersion).isEqualTo(BuildConfig.ADAPTER_VERSION)
    assertThat(BidMachineMediationAdapter.bidMachineInstance).isSameInstanceAs(mockBidMachineInstance)
    assertThat(initializationCompleteCallback).hasSucceeded()
  }

  @Test
  fun initialize_withMultipleSourceIds_initializesBidMachineWithTheFirstOne() {
    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(
        MediationConfiguration(AdFormat.BANNER, bundleOf()),
        createSourceIdConfiguration(TEST_SOURCE_ID),
        createSourceIdConfiguration("anotherSourceId"),
      ),
    )

    mockBidMachine.verify {
      BidMachine.initialize(eq(context), eq(TEST_SOURCE_ID), anyOrNull<BidMachineConfiguration>())
    }
    mockBidMachine.verify(
      {
        BidMachine.initialize(
          any(),
          eq("anotherSourceId"),
          anyOrNull<BidMachineConfiguration>(),
        )
      },
      never(),
    )
    assertThat(initializationCompleteCallback).hasSucceeded()
  }

  @Test
  fun initialize_tagForChildTrue_setsBidMachineCoppaToTrue() {
    setRequestConfiguration(
      tagForChildDirectedTreatment = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
    )

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createSourceIdConfiguration(TEST_SOURCE_ID)),
    )

    mockBidMachine.verify { BidMachine.setConsentData(eq(ConsentData(coppa = true))) }
  }

  @Test
  fun initialize_tagForUnderAgeTrue_setsBidMachineCoppaToTrue() {
    setRequestConfiguration(
      tagForUnderAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
    )

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createSourceIdConfiguration(TEST_SOURCE_ID)),
    )

    mockBidMachine.verify { BidMachine.setConsentData(eq(ConsentData(coppa = true))) }
  }

  @Test
  fun initialize_ageRestrictedTreatmentChild_setsBidMachineCoppaToTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    )

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createSourceIdConfiguration(TEST_SOURCE_ID)),
    )

    mockBidMachine.verify { BidMachine.setConsentData(eq(ConsentData(coppa = true))) }
  }

  @Test
  fun initialize_tagForChildFalse_setsBidMachineCoppaToFalse() {
    setRequestConfiguration(
      tagForChildDirectedTreatment = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
    )

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createSourceIdConfiguration(TEST_SOURCE_ID)),
    )

    mockBidMachine.verify { BidMachine.setConsentData(eq(ConsentData(coppa = false))) }
  }

  @Test
  fun initialize_tagForUnderAgeFalse_setsBidMachineCoppaToFalse() {
    setRequestConfiguration(
      tagForUnderAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
    )

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createSourceIdConfiguration(TEST_SOURCE_ID)),
    )

    mockBidMachine.verify { BidMachine.setConsentData(eq(ConsentData(coppa = false))) }
  }

  @Test
  fun initialize_tagForChildAndUnderAgeUnspecified_neverSetsBidMachineConsentData() {
    setRequestConfiguration()

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createSourceIdConfiguration(TEST_SOURCE_ID)),
    )

    mockBidMachine.verify({ BidMachine.setConsentData(anyOrNull()) }, never())
    assertThat(initializationCompleteCallback).hasSucceeded()
  }

  // endregion

  // region collectSignals tests
  @Test
  fun collectSignals_beforeInitialization_invokesOnFailure() {
    val signalCallbacks = FakeSignalCallbacks()

    adapter.collectSignals(
      createSignalData(createMediationConfiguration(AdFormat.BANNER, placementIdParams())),
      signalCallbacks,
    )

    assertThat(signalCallbacks).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun collectSignals_forBannerFormat_collectsTokenForPlacement() {
    assertCollectSignalsSucceeds(AdFormat.BANNER)
  }

  @Test
  fun collectSignals_forInterstitialFormat_collectsTokenForPlacement() {
    assertCollectSignalsSucceeds(AdFormat.INTERSTITIAL)
  }

  @Test
  fun collectSignals_forRewardedFormat_collectsTokenForPlacement() {
    assertCollectSignalsSucceeds(AdFormat.REWARDED)
  }

  @Test
  fun collectSignals_forNativeFormat_collectsTokenForPlacement() {
    assertCollectSignalsSucceeds(AdFormat.NATIVE)
  }

  @Test
  fun collectSignals_withoutPlacementId_collectsTokenWithoutPlacements() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val placementsCaptor = argumentCaptor<List<Placement>>()

    adapter.collectSignals(
      createSignalData(createMediationConfiguration(AdFormat.BANNER, bundleOf())),
      FakeSignalCallbacks(),
    )

    verify(mockBidMachineInstance).collectToken(placementsCaptor.capture(), any())
    assertThat(placementsCaptor.firstValue).isEmpty()
  }

  @Test
  fun collectSignals_withSupportedAndUnsupportedFormats_collectsTokenForSupportedOnly() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val placementsCaptor = argumentCaptor<List<Placement>>()

    adapter.collectSignals(
      createSignalData(
        createMediationConfiguration(AdFormat.APP_OPEN_AD, bundleOf(PLACEMENT_ID_KEY to "appOpen")),
        createMediationConfiguration(AdFormat.REWARDED, placementIdParams()),
      ),
      FakeSignalCallbacks(),
    )

    verify(mockBidMachineInstance).collectToken(placementsCaptor.capture(), any())
    assertThat(placementsCaptor.firstValue.map { it.id }).containsExactly(TEST_PLACEMENT_ID)
  }

  @Test
  fun collectSignals_onCollectionFailed_invokesOnFailure() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val signalCallbacks = FakeSignalCallbacks()
    val tokenCallbackCaptor = argumentCaptor<BidTokenCallback>()
    val bmError = createBMError()

    adapter.collectSignals(
      createSignalData(createMediationConfiguration(AdFormat.BANNER, placementIdParams())),
      signalCallbacks,
    )
    verify(mockBidMachineInstance).collectToken(any(), tokenCallbackCaptor.capture())
    tokenCallbackCaptor.firstValue.onCollectionFailed(bmError)

    assertThat(signalCallbacks)
      .hasFailedWith(AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN))
  }

  @Test
  fun collectSignals_withEmptyConfigurations_invokesOnFailure() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val signalCallbacks = FakeSignalCallbacks()

    adapter.collectSignals(createSignalData(), signalCallbacks)

    assertThat(signalCallbacks).hasFailedWith(noSupportedSignalConfigurationsError())
    verifyNoInteractions(mockBidMachineInstance)
  }

  @Test
  fun collectSignals_withUnsupportedAdFormat_invokesOnFailure() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val signalCallbacks = FakeSignalCallbacks()

    adapter.collectSignals(
      createSignalData(createMediationConfiguration(AdFormat.APP_OPEN_AD, bundleOf())),
      signalCallbacks,
    )

    assertThat(signalCallbacks).hasFailedWith(noSupportedSignalConfigurationsError())
    verifyNoInteractions(mockBidMachineInstance)
  }

  // endregion

  // region Banner tests
  @Test
  fun loadBannerAd_beforeInitialization_invokesOnFailure() {
    adapter.loadBannerAd(
      createMediationBannerAdConfiguration(context, serverParameters = placementIdParams()),
      bannerAdLoadCallback,
    )

    assertThat(bannerAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadBannerAd_loadsBannerWithPlacement() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val placementCaptor = argumentCaptor<Placement>()

    adapter.loadBannerAd(
      createMediationBannerAdConfiguration(
        context,
        adSize = AdSize.MEDIUM_RECTANGLE,
        serverParameters = placementIdParams(),
      ),
      bannerAdLoadCallback,
    )

    verify(mockBidMachineInstance)
      .loadBanner(
        placementCaptor.capture(),
        eq(BannerAdSize.MediumRectangle),
        eq(adapter.bannerAd),
      )
    assertThat(placementCaptor.firstValue.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun loadRtbBannerAd_beforeInitialization_invokesOnFailure() {
    adapter.loadRtbBannerAd(
      createMediationBannerAdConfiguration(context, bidResponse = TEST_BID_RESPONSE),
      bannerAdLoadCallback,
    )

    assertThat(bannerAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadRtbBannerAd_loadsBannerWithBidResponse() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance

    adapter.loadRtbBannerAd(
      createMediationBannerAdConfiguration(
        context,
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.LEADERBOARD,
      ),
      bannerAdLoadCallback,
    )

    verify(mockBidMachineInstance)
      .loadBanner(eq(TEST_BID_RESPONSE), eq(BannerAdSize.Leaderboard), eq(adapter.bannerAd))
  }

  // endregion

  // region Interstitial tests
  @Test
  fun loadInterstitialAd_beforeInitialization_invokesOnFailure() {
    adapter.loadInterstitialAd(
      createMediationInterstitialAdConfiguration(context, serverParameters = placementIdParams()),
      interstitialAdLoadCallback,
    )

    assertThat(interstitialAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadInterstitialAd_loadsInterstitialWithPlacement() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val placementCaptor = argumentCaptor<Placement>()

    adapter.loadInterstitialAd(
      createMediationInterstitialAdConfiguration(context, serverParameters = placementIdParams()),
      interstitialAdLoadCallback,
    )

    verify(mockBidMachineInstance).loadInterstitial(placementCaptor.capture(), any())
    assertThat(placementCaptor.firstValue.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun loadRtbInterstitialAd_beforeInitialization_invokesOnFailure() {
    adapter.loadRtbInterstitialAd(
      createMediationInterstitialAdConfiguration(context, bidResponse = TEST_BID_RESPONSE),
      interstitialAdLoadCallback,
    )

    assertThat(interstitialAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadRtbInterstitialAd_loadsInterstitialWithBidResponse() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance

    adapter.loadRtbInterstitialAd(
      createMediationInterstitialAdConfiguration(context, bidResponse = TEST_BID_RESPONSE),
      interstitialAdLoadCallback,
    )

    verify(mockBidMachineInstance).loadInterstitial(eq(TEST_BID_RESPONSE), any())
  }

  // endregion

  // region Rewarded tests
  @Test
  fun loadRewardedAd_beforeInitialization_invokesOnFailure() {
    adapter.loadRewardedAd(
      createMediationRewardedAdConfiguration(context, serverParameters = placementIdParams()),
      rewardedAdLoadCallback,
    )

    assertThat(rewardedAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadRewardedAd_loadsRewardedWithPlacement() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val placementCaptor = argumentCaptor<Placement>()

    adapter.loadRewardedAd(
      createMediationRewardedAdConfiguration(context, serverParameters = placementIdParams()),
      rewardedAdLoadCallback,
    )

    verify(mockBidMachineInstance).loadRewarded(placementCaptor.capture(), any())
    assertThat(placementCaptor.firstValue.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun loadRtbRewardedAd_beforeInitialization_invokesOnFailure() {
    adapter.loadRtbRewardedAd(
      createMediationRewardedAdConfiguration(context, bidResponse = TEST_BID_RESPONSE),
      rewardedAdLoadCallback,
    )

    assertThat(rewardedAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadRtbRewardedAd_loadsRewardedWithBidResponse() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance

    adapter.loadRtbRewardedAd(
      createMediationRewardedAdConfiguration(context, bidResponse = TEST_BID_RESPONSE),
      rewardedAdLoadCallback,
    )

    verify(mockBidMachineInstance).loadRewarded(eq(TEST_BID_RESPONSE), any())
  }

  // endregion

  // region Native tests
  @Test
  fun loadNativeAdMapper_beforeInitialization_invokesOnFailure() {
    adapter.loadNativeAdMapper(
      createMediationNativeAdConfiguration(context, serverParameters = placementIdParams()),
      nativeAdLoadCallback,
    )

    assertThat(nativeAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadNativeAdMapper_loadsNativeWithPlacement() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val placementCaptor = argumentCaptor<Placement>()

    adapter.loadNativeAdMapper(
      createMediationNativeAdConfiguration(context, serverParameters = placementIdParams()),
      nativeAdLoadCallback,
    )

    verify(mockBidMachineInstance).loadNative(placementCaptor.capture(), any())
    assertThat(placementCaptor.firstValue.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun loadRtbNativeAdMapper_beforeInitialization_invokesOnFailure() {
    adapter.loadRtbNativeAdMapper(
      createMediationNativeAdConfiguration(context, bidResponse = TEST_BID_RESPONSE),
      nativeAdLoadCallback,
    )

    assertThat(nativeAdLoadCallback).hasFailedWith(sdkNotInitializedError())
  }

  @Test
  fun loadRtbNativeAdMapper_loadsNativeWithBidResponse() {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance

    adapter.loadRtbNativeAdMapper(
      createMediationNativeAdConfiguration(context, bidResponse = TEST_BID_RESPONSE),
      nativeAdLoadCallback,
    )

    verify(mockBidMachineInstance).loadNative(eq(TEST_BID_RESPONSE), any())
  }

  // endregion

  // region createPlacement tests
  @Test
  fun createPlacement_withPlacementId_returnsPlacement() {
    val placement = BidMachineMediationAdapter.createPlacement(TEST_PLACEMENT_ID)

    assertThat(placement?.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun createPlacement_withNullOrEmptyPlacementId_returnsNull() {
    assertThat(BidMachineMediationAdapter.createPlacement(null)).isNull()
    assertThat(BidMachineMediationAdapter.createPlacement("")).isNull()
  }

  // endregion

  private fun assertCollectSignalsSucceeds(adFormat: AdFormat) {
    BidMachineMediationAdapter.bidMachineInstance = mockBidMachineInstance
    val signalCallbacks = FakeSignalCallbacks()
    val placementsCaptor = argumentCaptor<List<Placement>>()
    val tokenCallbackCaptor = argumentCaptor<BidTokenCallback>()

    adapter.collectSignals(
      createSignalData(createMediationConfiguration(adFormat, placementIdParams())),
      signalCallbacks,
    )

    verify(mockBidMachineInstance)
      .collectToken(placementsCaptor.capture(), tokenCallbackCaptor.capture())
    assertThat(placementsCaptor.firstValue.map { it.id }).containsExactly(TEST_PLACEMENT_ID)
    tokenCallbackCaptor.firstValue.onCollected(TEST_BID_RESPONSE)
    assertThat(signalCallbacks).hasSucceededWith(TEST_BID_RESPONSE)
  }

  private fun createSignalData(vararg configurations: MediationConfiguration) =
    RtbSignalData(
      context,
      /* configurations = */ configurations.toList(),
      /* networkExtras = */ bundleOf(),
      /* adSize = */ null,
    )

  private fun createSourceIdConfiguration(sourceId: String) =
    MediationConfiguration(AdFormat.BANNER, bundleOf(SOURCE_ID_KEY to sourceId))

  private fun placementIdParams() = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID)

  private fun setRequestConfiguration(
    tagForChildDirectedTreatment: Int =
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    tagForUnderAgeOfConsent: Int = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
  ) {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(tagForChildDirectedTreatment)
        .setTagForUnderAgeOfConsent(tagForUnderAgeOfConsent)
        .build()
    )
  }

  private fun sdkNotInitializedError() =
    AdError(ERROR_CODE_SDK_NOT_INITIALIZED, ERROR_MSG_SDK_NOT_INITIALIZED, ADAPTER_ERROR_DOMAIN)

  private fun noSupportedSignalConfigurationsError() =
    AdError(
      ERROR_CODE_NO_SUPPORTED_SIGNAL_CONFIGURATIONS,
      ERROR_MSG_NO_SUPPORTED_SIGNAL_CONFIGURATIONS,
      ADAPTER_ERROR_DOMAIN,
    )

  private companion object {
    const val TEST_SOURCE_ID = "testSourceId"
  }
}
