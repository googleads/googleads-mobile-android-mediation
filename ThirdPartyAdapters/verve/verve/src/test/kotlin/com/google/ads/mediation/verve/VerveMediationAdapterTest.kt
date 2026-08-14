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

package com.google.ads.mediation.verve

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.APP_TOKEN_KEY
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_CHILD_USER
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_CHILD_USER
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_ERROR_INITIALIZE_VERVE_SDK
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_MISSING_APP_TOKEN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
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
import net.pubnative.lite.sdk.HyBid
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd
import net.pubnative.lite.sdk.request.HyBidNativeAdRequest
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd
import net.pubnative.lite.sdk.views.HyBidBannerAdView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class VerveMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: VerveMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val initializationCompleteCallback = FakeInitializationCompleteCallback()
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
  private val rewardedInterstitialAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedInterstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedInterstitialAdCallback
    )
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val signalCallbacks = FakeSignalCallbacks()
  private val mockHyBidBannerAd: HyBidBannerAdView = mock()

  @Before
  fun setUp() {
    adapter = VerveMediationAdapter()
    VerveMediationAdapter.adapterVersionDelegate = null
    VerveSdkFactory.delegate =
      org.mockito.kotlin.mock { on { createHyBidBannerAdView(context) } doReturn mockHyBidBannerAd }
    // Reset child-directed, under-age, and age-restricted tags.
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    )
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

  @Test
  fun getVersionInfo_with5Digits_returnsValidVersionInfo() {
    VerveMediationAdapter.adapterVersionDelegate = "1.2.3.4.5"

    adapter.assertGetVersionInfo(expectedValue = "1.2.304")
  }

  // endregion

  // region Initialize tests
  @Test
  fun initialize_whenTaggedAsChildDirected_invokesOnInitializationFailed() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    adapter.initialize(context, initializationCompleteCallback, mediationConfigurations = listOf())

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_CHILD_USER)
  }

  @Test
  fun initialize_whenTaggedAsUnderAge_invokesOnInitializationFailed() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    adapter.initialize(context, initializationCompleteCallback, mediationConfigurations = listOf())

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_CHILD_USER)
  }

  @Test
  fun initialize_whenAgeRestrictedTreatmentChild_invokesOnInitializationFailed() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD).build()
    )
    adapter.initialize(context, initializationCompleteCallback, mediationConfigurations = listOf())

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_CHILD_USER)
  }

  @Test
  fun initialize_withEmptyConfiguration_invokesOnInitializationFailed() {
    adapter.initialize(context, initializationCompleteCallback, mediationConfigurations = listOf())

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_MISSING_APP_TOKEN)
  }

  @Test
  fun initialize_withoutAnyAppToken_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_MISSING_APP_TOKEN)
  }

  @Test
  fun initialize_withEmptyAppToken_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf(APP_TOKEN_KEY to ""))

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_MISSING_APP_TOKEN)
  }

  @Test
  fun initialize_onInitialisationSuccessTrue_invokesOnInitializationSucceeded() {
    VerveExtras.isTestMode = true
    mockStatic(HyBid::class.java).use { mockedHyBid ->
      val mediationConfiguration =
        MediationConfiguration(
          AdFormat.BANNER,
          /* serverParameters= */ bundleOf(APP_TOKEN_KEY to TEST_APP_TOKEN),
        )
      val listenerCaptor = argumentCaptor<HyBid.InitialisationListener>()

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockedHyBid.verify { HyBid.initialize(eq(TEST_APP_TOKEN), any(), listenerCaptor.capture()) }
      mockedHyBid.verify { HyBid.setTestMode(eq(true)) }
      listenerCaptor.firstValue.onInitialisationFinished(/* success= */ true)
      assertThat(initializationCompleteCallback).hasSucceeded()
    }
    // Resetting HyBid TestMode
    VerveExtras.isTestMode = false
  }

  @Test
  fun initialize_onInitialisationSuccessFalse_invokesOnInitializationFailed() {
    mockStatic(HyBid::class.java).use { mockedHyBid ->
      val mediationConfiguration =
        MediationConfiguration(
          AdFormat.BANNER,
          /* serverParameters= */ bundleOf(APP_TOKEN_KEY to TEST_APP_TOKEN),
        )
      val listenerCaptor = argumentCaptor<HyBid.InitialisationListener>()

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockedHyBid.verify { HyBid.initialize(eq(TEST_APP_TOKEN), any(), listenerCaptor.capture()) }
      // Default testMode value = false
      mockedHyBid.verify { HyBid.setTestMode(eq(false)) }
      listenerCaptor.firstValue.onInitialisationFinished(/* success= */ false)
      assertThat(initializationCompleteCallback).hasFailedWith(ERROR_MSG_ERROR_INITIALIZE_VERVE_SDK)
    }
  }

  // endregion

  // region Signal collection tests
  @Test
  fun collectSignals_invokesOnSuccess() {
    mockStatic(HyBid::class.java).use {
      whenever(HyBid.getEncodedCustomRequestSignalData(context, "Admob")) doReturn TEST_BID_RESPONSE
      val signalData =
        RtbSignalData(
          context,
          /* configurations = */ listOf<MediationConfiguration>(),
          /* networkExtras = */ bundleOf(),
          /* adSize = */ null,
        )

      adapter.collectSignals(signalData, signalCallbacks)

      assertThat(signalCallbacks).hasSucceededWith(TEST_BID_RESPONSE)
    }
  }

  @Test
  fun collectSignals_withValidBannerAdSize_invokesOnSuccess() {
    mockStatic(HyBid::class.java).use {
      whenever(HyBid.getEncodedCustomRequestSignalData(context, "Admob")) doReturn TEST_BID_RESPONSE
      val signalData =
        RtbSignalData(
          context,
          listOf(MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())),
          /* networkExtras = */ bundleOf(),
          AdSize.BANNER,
        )

      adapter.collectSignals(signalData, signalCallbacks)

      assertThat(signalCallbacks).hasSucceededWith(TEST_BID_RESPONSE)
    }
  }

  private fun runTestWithChildConfigurations(testBlock: () -> Unit) {
    val childConfigurations =
      listOf(
        RequestConfiguration.Builder()
          .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
          .build(),
        RequestConfiguration.Builder()
          .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
          .build(),
        RequestConfiguration.Builder()
          .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
          .build(),
      )

    for (config in childConfigurations) {
      MobileAds.setRequestConfiguration(config)
      testBlock()
    }
  }

  @Test
  fun collectSignals_whenChildUser_invokesOnFailure() {
    runTestWithChildConfigurations {
      val signalData =
        RtbSignalData(
          context,
          /* configurations = */ listOf<MediationConfiguration>(),
          /* networkExtras = */ bundleOf(),
          /* adSize = */ null,
        )

      adapter.collectSignals(signalData, signalCallbacks)

      val expectedError = AdError(ERROR_CODE_CHILD_USER, ERROR_MSG_CHILD_USER, ADAPTER_ERROR_DOMAIN)
      assertThat(signalCallbacks).hasFailedWith(expectedError)
    }
  }

  // endregion

  // region Load ad tests
  @Test
  fun loadRtbBannerAd_whenChildUser_invokesOnFailure() {
    runTestWithChildConfigurations {
      val bannerAdConfiguration =
        createMediationBannerAdConfiguration(context = context, adSize = AdSize.BANNER)

      adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

      val expectedError = AdError(ERROR_CODE_CHILD_USER, ERROR_MSG_CHILD_USER, ADAPTER_ERROR_DOMAIN)
      assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
    }
  }

  @Test
  fun loadRtbInterstitialAd_whenChildUser_invokesOnFailure() {
    runTestWithChildConfigurations {
      val interstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(context = context)

      adapter.loadRtbInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

      val expectedError = AdError(ERROR_CODE_CHILD_USER, ERROR_MSG_CHILD_USER, ADAPTER_ERROR_DOMAIN)
      assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
    }
  }

  @Test
  fun loadRtbRewardedAd_whenChildUser_invokesOnFailure() {
    runTestWithChildConfigurations {
      val rewardedAdConfiguration = createMediationRewardedAdConfiguration(context = context)

      adapter.loadRtbRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

      val expectedError = AdError(ERROR_CODE_CHILD_USER, ERROR_MSG_CHILD_USER, ADAPTER_ERROR_DOMAIN)
      assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
    }
  }

  @Test
  fun loadRtbRewardedInterstitialAd_whenChildUser_invokesOnFailure() {
    runTestWithChildConfigurations {
      val rewardedAdConfiguration = createMediationRewardedAdConfiguration(context = context)

      adapter.loadRtbRewardedInterstitialAd(
        rewardedAdConfiguration,
        rewardedInterstitialAdLoadCallback,
      )

      val expectedError = AdError(ERROR_CODE_CHILD_USER, ERROR_MSG_CHILD_USER, ADAPTER_ERROR_DOMAIN)
      assertThat(rewardedInterstitialAdLoadCallback).hasFailedWith(expectedError)
    }
  }

  @Test
  fun loadRtbNativeAdMapper_whenChildUser_invokesOnFailure() {
    runTestWithChildConfigurations {
      val nativeAdConfiguration = createMediationNativeAdConfiguration(context = context)

      adapter.loadRtbNativeAdMapper(nativeAdConfiguration, nativeAdLoadCallback)

      val expectedError = AdError(ERROR_CODE_CHILD_USER, ERROR_MSG_CHILD_USER, ADAPTER_ERROR_DOMAIN)
      assertThat(nativeAdLoadCallback).hasFailedWith(expectedError)
    }
  }

  @Test
  fun loadRtbBannerAd_whenNotChildUser_delegatesToVerveBannerAd() {
    val bannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.BANNER,
      )

    adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    verify(mockHyBidBannerAd).renderAd(eq(TEST_BID_RESPONSE), any())
  }

  @Test
  fun loadRtbInterstitialAd_whenNotChildUser_delegatesToVerveInterstitialAd() {
    val mockHyBidInterstitialAd = mock<HyBidInterstitialAd>()
    VerveSdkFactory.delegate = mock {
      on { createHyBidInterstitialAd(eq(context), any()) } doReturn mockHyBidInterstitialAd
    }
    val interstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)

    adapter.loadRtbInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    verify(mockHyBidInterstitialAd).prepareAd(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun loadRtbRewardedAd_whenNotChildUser_delegatesToVerveRewardedAd() {
    val mockHyBidRewardedAd = mock<HyBidRewardedAd>()
    VerveSdkFactory.delegate = mock {
      on { createHyBidRewardedAd(eq(context), any()) } doReturn mockHyBidRewardedAd
    }
    val rewardedAdConfiguration =
      createMediationRewardedAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)

    adapter.loadRtbRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    verify(mockHyBidRewardedAd).prepareAd(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun loadRtbRewardedInterstitialAd_whenNotChildUser_delegatesToVerveRewardedAd() {
    val mockHyBidRewardedAd = mock<HyBidRewardedAd>()
    VerveSdkFactory.delegate = mock {
      on { createHyBidRewardedAd(eq(context), any()) } doReturn mockHyBidRewardedAd
    }
    val rewardedAdConfiguration =
      createMediationRewardedAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)

    adapter.loadRtbRewardedInterstitialAd(
      rewardedAdConfiguration,
      rewardedInterstitialAdLoadCallback,
    )

    verify(mockHyBidRewardedAd).prepareAd(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun loadRtbNativeAdMapper_whenNotChildUser_delegatesToVerveNativeAd() {
    mockConstruction(HyBidNativeAdRequest::class.java).use { mockNativeAdRequest ->
      val nativeAdConfiguration =
        createMediationNativeAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)

      adapter.loadRtbNativeAdMapper(nativeAdConfiguration, nativeAdLoadCallback)

      val constructedRequest = mockNativeAdRequest.constructed().first()
      verify(constructedRequest).prepareAd(eq(TEST_BID_RESPONSE), any())
    }
  }

  // endregion

  private companion object {
    const val TEST_APP_TOKEN = "AppToken"
  }
}
