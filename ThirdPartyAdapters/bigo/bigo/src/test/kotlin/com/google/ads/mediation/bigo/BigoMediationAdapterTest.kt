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
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.assertThat
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
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions

@RunWith(AndroidJUnit4::class)
class BigoMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: BigoMediationAdapter
  private lateinit var mockBigoSdk: MockedStatic<BigoAdSdk>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val initializationCompleteCallback = FakeInitializationCompleteCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
  private val rewardedInterstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
  private val appOpenAdLoadCallback =
    FakeMediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>()
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    adapter = BigoMediationAdapter(mediationUtils)
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
    adapter.initialize(context, initializationCompleteCallback, mediationConfigurations = listOf())

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_MISSING_APP_ID)
  }

  @Test
  fun initialize_withoutAnySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_MISSING_APP_ID)
  }

  @Test
  fun initialize_withEmptySourceId_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf(APP_ID_KEY to ""))

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_MISSING_APP_ID)
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
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    )
    val callbackCaptor = argumentCaptor<BigoAdSdk.InitListener>()

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.initialize(eq(context), any(), callbackCaptor.capture()) }
    callbackCaptor.firstValue.onInitialized()
    assertThat(initializationCompleteCallback).hasSucceeded()
    mockBigoSdk.verify {
      BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(false))
    }
  }

  @Test
  fun initialize_tagForUnderAgeTrue_invokesOnInitializationSucceededAndBigoCoppaToFalse() {
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
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    val callbackCaptor = argumentCaptor<BigoAdSdk.InitListener>()

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.initialize(eq(context), any(), callbackCaptor.capture()) }
    callbackCaptor.firstValue.onInitialized()
    assertThat(initializationCompleteCallback).hasSucceeded()
    mockBigoSdk.verify {
      BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(false))
    }
  }

  @Test
  fun initialize_ageRestrictedTreatmentChild_invokesOnInitializationSucceededAndBigoCoppaToFalse() {
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
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    )
    val callbackCaptor = argumentCaptor<BigoAdSdk.InitListener>()

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.initialize(eq(context), any(), callbackCaptor.capture()) }
    callbackCaptor.firstValue.onInitialized()
    assertThat(initializationCompleteCallback).hasSucceeded()
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
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    )

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(true)) }
  }

  @Test
  fun initialize_tagForUnderAgeFalse_setBigoCoppaToTrue() {
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
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    )

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(true)) }
  }

  @Test
  fun initialize_tagForChildAndUnderAgeUnspecified_doesNotModifyBigoCoppa() {
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

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    mockBigoSdk.verify(
      { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), any()) },
      never(),
    )
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
    val signalCallbacks = FakeSignalCallbacks()

    adapter.collectSignals(signalData, signalCallbacks)

    mockBigoSdk.verify { BigoAdSdk.getBidderToken() }
    assertThat(signalCallbacks).hasSucceededWith(TEST_BID_RESPONSE)
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
    val signalCallbacks = FakeSignalCallbacks()

    adapter.collectSignals(signalData, signalCallbacks)

    mockBigoSdk.verify { BigoAdSdk.getBidderToken() }
    assertThat(signalCallbacks).hasSucceededWith("")
  }

  // endregion

  // region Banner tests
  @Test
  fun loadRtbBannerAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationBannerAdConfiguration(context, adSize = AdSize.BANNER)
    whenever(mediationUtils.findClosestSize(eq(context), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbBannerAd(adConfiguration, bannerAdLoadCallback)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  // endregion

  // region Interstitial tests
  @Test
  fun loadRtbInterstitialAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationInterstitialAdConfiguration(context)
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbInterstitialAd(adConfiguration, interstitialAdLoadCallback)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  // endregion

  // region Rewarded tests
  @Test
  fun loadRtbRewardedAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationRewardedAdConfiguration(context)
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbRewardedAd(adConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  // endregion

  // region Rewarded Interstitial tests
  @Test
  fun loadRtbRewardedInterstitialAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationRewardedAdConfiguration(context)
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbRewardedInterstitialAd(adConfiguration, rewardedInterstitialAdLoadCallback)

    assertThat(rewardedInterstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  // endregion

  // region AppOpen tests
  @Test
  fun loadRtbAppOpenAd_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationAppOpenAdConfiguration(context)
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbAppOpenAd(adConfiguration, appOpenAdLoadCallback)

    assertThat(appOpenAdLoadCallback).hasFailedWith(expectedAdError)
  }

  // endregion

  // region Native tests
  @Test
  fun loadRtbNativeAdMapper_withEmptySlotId_invokesOnFailure() {
    val adConfiguration = createMediationNativeAdConfiguration(context)
    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbNativeAdMapper(adConfiguration, nativeAdLoadCallback)

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  // endregion

}
