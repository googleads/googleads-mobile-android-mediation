// Copyright 2023 Google LLC
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

package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.loadAppOpenAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadInterstitialAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbAppOpenAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbInterstitialAdWithFailure
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_BANNER_SIZE_UNSUPPORTED
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.MintegralUtils.getAdapterVersion
import com.google.ads.mediation.mintegral.MintegralUtils.getSdkVersion
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
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import com.mbridge.msdk.out.MBBannerView
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.mbridge.msdk.out.SDKInitStatusListener
import com.mbridge.msdk.system.MBridgeSDKImpl
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MintegralMediationAdapterTest {
  // Subject of tests
  private var mintegralMediationAdapter = MintegralMediationAdapter()

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockInitializationCompleteCallback: InitializationCompleteCallback = mock()
  private val mockInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mockAppOpenAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock()
  private val mediationBannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val mbBannerView: MBBannerView = mock()

  @Before
  fun setUp() {
    mintegralMediationAdapter = MintegralMediationAdapter(SynchronousExecutorService())
  }

  // region version tests
  @Test
  fun getSdkVersion_returnsCorrectVersion() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getSdkVersion()) doReturn "TEST_3.2.1"

      mintegralMediationAdapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSdkVersion_withoutUnderscoreDivider_returnsZeroes() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getSdkVersion()) doReturn "3.2.1"

      mintegralMediationAdapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSdkVersion_withInvalidValues_returnsZeroes() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getSdkVersion()) doReturn "TEST_3.2"

      mintegralMediationAdapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_returnsTheSameVersion() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "3.2.1.0"

      mintegralMediationAdapter.assertGetVersionInfo(expectedValue = "3.2.100")
    }
  }

  @Test
  fun getVersionInfo_withInvalidValue_returnsZeroes() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "3.2.1"

      mintegralMediationAdapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region initialize tests
  @Test
  fun initialize_withValidValues_callsPrivacyConfigurationAndOnInitializationSucceeded() {
    mockStatic(MBridgeSDKFactory::class.java).use {
      val mockMBridgeSdk = mock<MBridgeSDKImpl>()
      whenever(MBridgeSDKFactory.getMBridgeSDK()) doReturn mockMBridgeSdk
      val serverParameters =
        bundleOf(
          MintegralConstants.APP_KEY to TEST_APP_KEY,
          MintegralConstants.APP_ID to TEST_APP_ID,
        )
      val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
      val requestConfig =
        RequestConfiguration.Builder()
          .setTagForChildDirectedTreatment(
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
          )
          .build()
      MobileAds.setRequestConfiguration(requestConfig)

      mintegralMediationAdapter.initialize(
        context,
        mockInitializationCompleteCallback,
        listOf(mediationConfiguration),
      )

      val initStatusCaptor = argumentCaptor<SDKInitStatusListener>()
      verify(mockMBridgeSdk).init(any(), eq(context), initStatusCaptor.capture())
      initStatusCaptor.firstValue.onInitSuccess()
      verify(mockMBridgeSdk).setCoppaStatus(eq(context), eq(true))
      verify(mockInitializationCompleteCallback).onInitializationSucceeded()
    }
  }

  // endregion

  // region Banner Ad Tests

  @Test
  fun loadWaterfallBannerAd_forUnsupportedAdSize_fails() {
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(context = context, adSize = AdSize(800, 50))

    mintegralMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_BANNER_SIZE_UNSUPPORTED)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  /**
   * Tests that RTB Banner ad load succeeds even if the requested size is a non-standard one for
   * Mintegral.
   */
  @Test
  fun loadRtbBannerAd_forNonStandardMintegralSize_succeeds() {
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        adSize = AdSize(800, 50),
      )

    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMBBannerView(any())) doReturn mbBannerView

      mintegralMediationAdapter.loadRtbBannerAd(
        mediationBannerAdConfiguration,
        mediationBannerAdLoadCallback,
      )

      verify(mbBannerView).loadFromBid(TEST_BID_RESPONSE)
    }
  }

  // endregion

  // region Interstitial Ad Tests
  @Test
  fun loadInterstitialAd_withoutAdUnitId_invokesOnFailure() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(context = context)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadInterstitialAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadInterstitialAd_withoutPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadInterstitialAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadInterstitialAd_loadsNewInterstitialAd() {
    mockStatic(MintegralFactory::class.java).use {
      val mockInterstitialAdWrapper = mock<MintegralNewInterstitialAdWrapper>()
      whenever(MintegralFactory.createInterstitialHandler()) doReturn mockInterstitialAdWrapper
      val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )

      mintegralMediationAdapter.loadInterstitialAd(
        mediationInterstitialAdConfiguration,
        mockInterstitialAdLoadCallback,
      )

      verify(mockInterstitialAdWrapper).createAd(context, TEST_PLACEMENT_ID, TEST_AD_UNIT)
      verify(mockInterstitialAdWrapper).setInterstitialVideoListener(any())
      verify(mockInterstitialAdWrapper).load()
    }
  }

  @Test
  fun loadRtbInterstitialAd_withoutAdUnitId_invokesOnFailure() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(context = context)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbInterstitialAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbInterstitialAd_withoutPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbInterstitialAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbInterstitialAd_withEmptyBidResponse_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_BID_RESPONSE,
        ("Missing or invalid Mintegral bidding signal in this ad request."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbInterstitialAd_invokesLoadFromBid() {
    mockStatic(MintegralFactory::class.java).use {
      val mockBinInterstitialAdWrapper = mock<MintegralBidNewInterstitialAdWrapper>()
      whenever(MintegralFactory.createBidInterstitialHandler()) doReturn
        mockBinInterstitialAdWrapper
      val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
        )

      mintegralMediationAdapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        mockInterstitialAdLoadCallback,
      )

      verify(mockBinInterstitialAdWrapper).setExtraInfo(any())
      verify(mockBinInterstitialAdWrapper).createAd(context, TEST_PLACEMENT_ID, TEST_AD_UNIT)
      verify(mockBinInterstitialAdWrapper).setInterstitialVideoListener(any())
      verify(mockBinInterstitialAdWrapper).loadFromBid(TEST_BID_RESPONSE)
    }
  }

  // endregion

  // region AppOpen Ad Tests
  @Test
  fun loadAppOpenAd_withoutAdUnitId_invokesOnFailure() {
    val mediationAppOpenAdConfiguration = createMediationAppOpenAdConfiguration(context = context)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadAppOpenAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadAppOpenAd_withoutPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadAppOpenAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadAppOpenAd_preLoadsSplashAd() {
    mockStatic(MintegralFactory::class.java).use {
      val mockSplashAd = mock<MintegralSplashAdWrapper>()
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn mockSplashAd
      val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
      val mediationAppOpenAdConfiguration =
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )

      mintegralMediationAdapter.loadAppOpenAd(
        mediationAppOpenAdConfiguration,
        mockAppOpenAdLoadCallback,
      )

      verify(mockSplashAd).createAd(TEST_PLACEMENT_ID, TEST_AD_UNIT)
      verify(mockSplashAd).setSplashLoadListener(any())
      verify(mockSplashAd).setSplashShowListener(any())
      verify(mockSplashAd).preLoad()
    }
  }

  @Test
  fun loadRtbAppOpenAd_withoutAdUnitId_invokesOnFailure() {
    val mediationAppOpenAdConfiguration = createMediationAppOpenAdConfiguration(context = context)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbAppOpenAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbAppOpenAd_withoutPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbAppOpenAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbAppOpenAd_withEmptyBidResponse_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_BID_RESPONSE,
        ("Missing or invalid Mintegral bidding signal in this ad request."),
        MintegralConstants.ERROR_DOMAIN,
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbAppOpenAd_invokesPreLoadByToken() {
    mockStatic(MintegralFactory::class.java).use {
      val mockSplashAd = mock<MintegralSplashAdWrapper>()
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn mockSplashAd
      val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
      val mediationAppOpenAdConfiguration =
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters = serverParameters,
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
        )

      mintegralMediationAdapter.loadRtbAppOpenAd(
        mediationAppOpenAdConfiguration,
        mockAppOpenAdLoadCallback,
      )

      verify(mockSplashAd).setExtraInfo(any())
      verify(mockSplashAd).createAd(TEST_PLACEMENT_ID, TEST_AD_UNIT)
      verify(mockSplashAd).setSplashLoadListener(any())
      verify(mockSplashAd).setSplashShowListener(any())
      verify(mockSplashAd).preLoadByToken(TEST_BID_RESPONSE)
    }
  }

  // endregion

  private companion object {
    val TEST_APP_KEY = "testAppKey"
    val TEST_APP_ID = "testAppId"
  }

  /** A synchronous executor service used for testing purpose. */
  class SynchronousExecutorService : ExecutorService {
    override fun shutdown() {}

    override fun shutdownNow() = emptyList<Runnable>()

    override fun isShutdown() = true

    override fun isTerminated() = true

    override fun awaitTermination(timeout: Long, unit: TimeUnit) = true

    override fun execute(command: Runnable) {
      command.run()
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
      val result = task.call()
      return object : Future<T> {
        override fun cancel(mayInterruptIfRunning: Boolean) = false

        override fun isCancelled() = false

        override fun isDone() = true

        override fun get() = result

        override fun get(timeout: Long, unit: TimeUnit) = result
      }
    }

    override fun <T> submit(task: Runnable, result: T): Future<T> {
      task.run()
      return object : Future<T> {
        override fun cancel(mayInterruptIfRunning: Boolean) = false

        override fun isCancelled() = false

        override fun isDone() = true

        override fun get() = result

        override fun get(timeout: Long, unit: TimeUnit) = result
      }
    }

    override fun submit(task: Runnable): Future<*> {
      task.run()
      return object : Future<Void> {
        override fun cancel(mayInterruptIfRunning: Boolean) = false

        override fun isCancelled() = false

        override fun isDone() = true

        override fun get() = null

        override fun get(timeout: Long, unit: TimeUnit) = null
      }
    }

    override fun <T : Any?> invokeAll(
      tasks: MutableCollection<out Callable<T>>?
    ): MutableList<Future<T>> = throw UnsupportedOperationException()

    override fun <T : Any?> invokeAll(
      tasks: MutableCollection<out Callable<T>>?,
      timeout: Long,
      unit: TimeUnit?,
    ): MutableList<Future<T>> = throw UnsupportedOperationException()

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?): T =
      throw UnsupportedOperationException()

    override fun <T : Any?> invokeAny(
      tasks: MutableCollection<out Callable<T>>?,
      timeout: Long,
      unit: TimeUnit?,
    ): T = throw UnsupportedOperationException()
  }
}
