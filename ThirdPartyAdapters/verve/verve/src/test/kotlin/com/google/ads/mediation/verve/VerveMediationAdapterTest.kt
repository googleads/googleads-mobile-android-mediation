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
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.loadRtbBannerAdWithFailure
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.APP_TOKEN_KEY
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_UNSUPPORTED_AD_SIZE
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_CHILD_USER
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_ERROR_INITIALIZE_VERVE_SDK
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_MISSING_APP_TOKEN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_UNSUPPORTED_AD_SIZE
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import net.pubnative.lite.sdk.HyBid
import net.pubnative.lite.sdk.views.HyBidBannerAdView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class VerveMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: VerveMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCallback: InitializationCompleteCallback = mock()
  private val mockHyBidBannerAd: HyBidBannerAdView = mock()

  @Before
  fun setUp() {
    adapter = VerveMediationAdapter()
    VerveSdkFactory.delegate =
      org.mockito.kotlin.mock { on { createHyBidBannerAdView(context) } doReturn mockHyBidBannerAd }
  }

  @After
  fun tearDown() {
    // Reset child-directed and under-age tags.
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .build()
    )
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
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

  // endregion

  // region Initialize tests
  @Test
  fun initialize_whenTaggedAsChildDirected_invokesOnInitializationFailed() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    adapter.initialize(context, mockInitializationCallback, mediationConfigurations = listOf())

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_CHILD_USER))
  }

  @Test
  fun initialize_whenTaggedAsUnderAge_invokesOnInitializationFailed() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    adapter.initialize(context, mockInitializationCallback, mediationConfigurations = listOf())

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_CHILD_USER))
  }

  @Test
  fun initialize_withEmptyConfiguration_invokesOnInitializationFailed() {
    adapter.initialize(context, mockInitializationCallback, mediationConfigurations = listOf())

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_APP_TOKEN))
  }

  @Test
  fun initialize_withoutAnyAppToken_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_APP_TOKEN))
  }

  @Test
  fun initialize_withEmptyAppToken_invokesOnInitializationFailed() {
    val mediationConfiguration =
      MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf(APP_TOKEN_KEY to ""))

    adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

    verify(mockInitializationCallback).onInitializationFailed(eq(ERROR_MSG_MISSING_APP_TOKEN))
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

      adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

      mockedHyBid.verify { HyBid.initialize(eq(TEST_APP_TOKEN), any(), listenerCaptor.capture()) }
      mockedHyBid.verify { HyBid.setTestMode(eq(true)) }
      listenerCaptor.firstValue.onInitialisationFinished(/* success= */ true)
      verify(mockInitializationCallback).onInitializationSucceeded()
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

      adapter.initialize(context, mockInitializationCallback, listOf(mediationConfiguration))

      mockedHyBid.verify { HyBid.initialize(eq(TEST_APP_TOKEN), any(), listenerCaptor.capture()) }
      // Default testMode value = false
      mockedHyBid.verify { HyBid.setTestMode(eq(false)) }
      listenerCaptor.firstValue.onInitialisationFinished(/* success= */ false)
      verify(mockInitializationCallback)
        .onInitializationFailed(eq(ERROR_MSG_ERROR_INITIALIZE_VERVE_SDK))
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
      val mockSignalCallbacks = mock<SignalCallbacks>()

      adapter.collectSignals(signalData, mockSignalCallbacks)

      verify(mockSignalCallbacks).onSuccess(TEST_BID_RESPONSE)
    }
  }

  @Test
  fun collectSignals_withValidAdSize_invokesOnSuccess() {
    mockStatic(HyBid::class.java).use {
      whenever(HyBid.getEncodedCustomRequestSignalData(context, "Admob")) doReturn TEST_BID_RESPONSE
      val signalData =
        RtbSignalData(
          context,
          /* configurations = */ listOf<MediationConfiguration>(),
          /* networkExtras = */ bundleOf(),
          AdSize.BANNER,
        )
      val mockSignalCallbacks = mock<SignalCallbacks>()

      adapter.collectSignals(signalData, mockSignalCallbacks)

      verify(mockSignalCallbacks).onSuccess(TEST_BID_RESPONSE)
    }
  }

  @Test
  fun collectSignals_withInvalidAdSize_invokesOnFailure() {
    mockStatic(HyBid::class.java).use {
      val signalData =
        RtbSignalData(
          context,
          /* configurations = */ listOf<MediationConfiguration>(),
          /* networkExtras = */ bundleOf(),
          AdSize.FLUID,
        )
      val expectedAdError =
        AdError(ERROR_CODE_UNSUPPORTED_AD_SIZE, ERROR_MSG_UNSUPPORTED_AD_SIZE, ADAPTER_ERROR_DOMAIN)
      val mockSignalCallbacks = mock<SignalCallbacks>()

      adapter.collectSignals(signalData, mockSignalCallbacks)

      verify(mockSignalCallbacks).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  // endregion

  // region Banner tests
  @Test
  fun loadRtbBannerAd_withUnsupportedAdSize_invokesOnFailure() {
    val bannerAdConfiguration = createMediationBannerAdConfiguration(context, adSize = AdSize.FLUID)
    val mockBannerAdLoadCallback =
      mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
    val expectedAdError =
      AdError(ERROR_CODE_UNSUPPORTED_AD_SIZE, ERROR_MSG_UNSUPPORTED_AD_SIZE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbBannerAdWithFailure(
      bannerAdConfiguration,
      mockBannerAdLoadCallback,
      expectedAdError,
    )
  }

  // endregion

  private companion object {
    const val TEST_APP_TOKEN = "AppToken"
  }
}
