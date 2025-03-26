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

package com.google.ads.mediation.moloco

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.moloco.MolocoAdapterUtils.setMolocoIsAgeRestricted
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.MEDIATION_PLATFORM_NAME
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.BuildConfig
import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.CreateBannerCallback
import com.moloco.sdk.publisher.CreateInterstitialAdCallback
import com.moloco.sdk.publisher.CreateNativeAdCallback
import com.moloco.sdk.publisher.CreateRewardedInterstitialAdCallback
import com.moloco.sdk.publisher.Initialization
import com.moloco.sdk.publisher.InterstitialAd
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.Moloco.createBanner
import com.moloco.sdk.publisher.Moloco.createBannerTablet
import com.moloco.sdk.publisher.Moloco.createInterstitial
import com.moloco.sdk.publisher.Moloco.createNativeAd
import com.moloco.sdk.publisher.Moloco.createRewardedInterstitial
import com.moloco.sdk.publisher.Moloco.getBidToken
import com.moloco.sdk.publisher.Moloco.initialize
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.MolocoBidTokenListener
import com.moloco.sdk.publisher.MolocoInitStatus
import com.moloco.sdk.publisher.MolocoInitializationListener
import com.moloco.sdk.publisher.NativeAd
import com.moloco.sdk.publisher.RewardedInterstitialAd
import com.moloco.sdk.publisher.init.MolocoInitParams
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MolocoMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: MolocoMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()
  private val mockSignalCallbacks = mock<SignalCallbacks>()
  private val mockMediationInterstitialAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
  private val mockMediationRewardedAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
  private val mockMediationBannerAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
  private val mockMediationNativeAdLoadCallback =
    mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>>()

  private val mockInterstitialAd = mock<InterstitialAd>()
  private val mockRewardedAd = mock<RewardedInterstitialAd>()
  private val mockBannerAd = mock<Banner>()
  private val mockNativeAd = mock<NativeAd>()

  private val molocoInitParamsCaptor = argumentCaptor<MolocoInitParams>()
  private val rtbSignalData = mock<RtbSignalData> { on { context } doReturn context }

  @Before
  fun setUp() {
    adapter = MolocoMediationAdapter()
  }

  // region Version Tests
  @Test
  fun getSDKVersionInfo_isSameAsMolocoSDKVersionName() {
    adapter.assertGetSdkVersion(expectedValue = BuildConfig.SDK_VERSION_NAME)
  }

  @Test
  fun getVersionInfo_validVersionWith4Digits_returnsValidVersionInfo() {
    mockStatic(MolocoAdapterUtils::class.java).use {
      whenever(MolocoAdapterUtils.adapterVersion) doReturn "4.3.2.1"

      adapter.assertGetVersionInfo(expectedValue = "4.3.201")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZeros() {
    mockStatic(MolocoAdapterUtils::class.java).use {
      whenever(MolocoAdapterUtils.adapterVersion) doReturn "4.3.2"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region initialize Tests
  @Test
  fun initialize_withEmptyMediationConfigurations_invokesOnInitializationFailed() {
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER)

    adapter.initialize(context, mockInitializationCompleteCallback, listOf(mediationConfiguration))

    verify(mockInitializationCompleteCallback)
      .onInitializationFailed(eq(MolocoMediationAdapter.ERROR_MSG_MISSING_APP_KEY))
  }

  @Test
  fun initialize_withInvalidApplicationId_invokesOnInitializationFailed() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_APP_KEY to "")
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    adapter.initialize(context, mockInitializationCompleteCallback, listOf(mediationConfiguration))

    verify(mockInitializationCompleteCallback)
      .onInitializationFailed(eq(MolocoMediationAdapter.ERROR_MSG_MISSING_APP_KEY))
  }

  @Test
  fun initialize_withMultipleAppIds_invokesInitializeOnlyOnce() {
    mockStatic(Moloco::class.java).use { mockMoloco ->
      val serverParameters1 = bundleOf(MolocoMediationAdapter.KEY_APP_KEY to TEST_APP_KEY_1)
      val serverParameters2 = bundleOf(MolocoMediationAdapter.KEY_APP_KEY to TEST_APP_KEY_2)
      val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters1)
      val mediationConfiguration2 = createMediationConfiguration(AdFormat.BANNER, serverParameters2)
      val initParamsCaptor = argumentCaptor<MolocoInitParams>()

      adapter.initialize(
        context,
        mockInitializationCompleteCallback,
        listOf(mediationConfiguration, mediationConfiguration2),
      )

      mockMoloco.verify({ initialize(initParamsCaptor.capture(), any()) }, times(1))
      assertEquals(initParamsCaptor.firstValue.appKey, TEST_APP_KEY_1)
    }
  }

  @Test
  fun initialize_withCorrectApplicationKey_invokesMolocoInitialize() {
    mockStatic(Moloco::class.java).use { mockMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_APP_KEY to TEST_APP_KEY_1)
      val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

      adapter.initialize(
        context,
        mockInitializationCompleteCallback,
        listOf(mediationConfiguration),
      )

      mockMoloco.verify { initialize(molocoInitParamsCaptor.capture(), any()) }
    }
    val molocoInitParams = molocoInitParamsCaptor.firstValue
    assertThat(molocoInitParams.mediationInfo.name).isEqualTo(MEDIATION_PLATFORM_NAME)
  }

  @Test
  fun initialize_initializationFailure_invokesOnInitializationFailed() {
    mockStatic(Moloco::class.java).use { mockMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_APP_KEY to TEST_APP_KEY_1)
      val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
      val molocoCallbackCaptor = argumentCaptor<MolocoInitializationListener>()

      adapter.initialize(
        context,
        mockInitializationCompleteCallback,
        listOf(mediationConfiguration),
      )

      mockMoloco.verify({ initialize(any(), molocoCallbackCaptor.capture()) }, times(1))
      val molocoCallback = molocoCallbackCaptor.firstValue
      molocoCallback.onMolocoInitializationStatus(
        MolocoInitStatus(Initialization.FAILURE, "TestError")
      )
      verify(mockInitializationCompleteCallback)
        .onInitializationFailed("Moloco SDK failed to initialize: TestError.")
    }
  }

  @Test
  fun initialize_initializationSuccess_invokesOnInitializationSucceededAndConfiguresTFUABit() {
    mockStatic(MolocoAdapterUtils::class.java).use { mockedMolocoUtils ->
      mockStatic(Moloco::class.java).use { mockedMoloco ->
        val serverParameters = bundleOf(MolocoMediationAdapter.KEY_APP_KEY to TEST_APP_KEY_1)
        val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
        val molocoCallbackCaptor = argumentCaptor<MolocoInitializationListener>()
        val requestConfig =
          RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(
              RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
            )
            .build()
        MobileAds.setRequestConfiguration(requestConfig)

        adapter.initialize(
          context,
          mockInitializationCompleteCallback,
          listOf(mediationConfiguration),
        )

        mockedMoloco.verify({ initialize(any(), molocoCallbackCaptor.capture()) }, times(1))
        val molocoCallback = molocoCallbackCaptor.firstValue
        molocoCallback.onMolocoInitializationStatus(
          MolocoInitStatus(Initialization.SUCCESS, "Test")
        )
        mockedMolocoUtils.verify { setMolocoIsAgeRestricted(true) }
        verify(mockInitializationCompleteCallback).onInitializationSucceeded()
      }
    }
  }

  private fun createMediationConfiguration(
    adFormat: AdFormat,
    serverParameters: Bundle = bundleOf(),
  ) = MediationConfiguration(adFormat, serverParameters)

  // endregion

  // region collectSignals test
  @Test
  fun collectSignals_withNullErrorType_invokesOnSuccess() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val tokenCallback = argumentCaptor<MolocoBidTokenListener>()

      adapter.collectSignals(rtbSignalData, mockSignalCallbacks)

      mockedMoloco.verify { getBidToken(eq(context), tokenCallback.capture()) }
      tokenCallback.firstValue.onBidTokenResult(TEST_BID_RESPONSE, null)
      verify(mockSignalCallbacks).onSuccess(TEST_BID_RESPONSE)
    }
  }

  @Test
  fun collectSignals_withErrorType_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val tokenCallback = argumentCaptor<MolocoBidTokenListener>()

      adapter.collectSignals(rtbSignalData, mockSignalCallbacks)

      mockedMoloco.verify { getBidToken(eq(context), tokenCallback.capture()) }
      tokenCallback.firstValue.onBidTokenResult(
        TEST_BID_RESPONSE,
        MolocoAdError.ErrorType.AD_BID_PARSE_ERROR,
      )
      val expectedAdError =
        AdError(
          MolocoAdError.ErrorType.AD_BID_PARSE_ERROR.errorCode,
          MolocoAdError.ErrorType.AD_BID_PARSE_ERROR.description,
          MolocoMediationAdapter.SDK_ERROR_DOMAIN,
        )
      verify(mockSignalCallbacks).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  // endregion

  // region Interstitial tests
  @Test
  fun loadRtbInterstitialAd_withNullAdUnit_invokesOnFailure() {
    val mediationInterstitialAdConfiguration = createMediationInterstitialAdConfiguration()

    adapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mockMediationInterstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationInterstitialAdLoadCallback)
      .onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbInterstitialAd_withEmptyAdUnit_invokesOnFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(serverParameters = serverParameters)

    adapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mockMediationInterstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationInterstitialAdLoadCallback)
      .onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbInterstitialAd_whenAdIsCreated_loadsMolocoInterstitial() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createInterstitialCaptor = argumentCaptor<CreateInterstitialAdCallback>()

      adapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        mockMediationInterstitialAdLoadCallback,
      )

      mockedMoloco.verify {
        createInterstitial(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createInterstitialCaptor.capture())
      }
      val capturedCallback = createInterstitialCaptor.firstValue
      capturedCallback.invoke(mockInterstitialAd, /* error= */ null)
      verify(mockInterstitialAd).load(eq(TEST_BID_RESPONSE), any())
    }
  }

  @Test
  fun loadRtbInterstitialAd_whenMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createInterstitialCaptor = argumentCaptor<CreateInterstitialAdCallback>()

      adapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        mockMediationInterstitialAdLoadCallback,
      )
      mockedMoloco.verify {
        createInterstitial(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createInterstitialCaptor.capture())
      }
      val capturedCallback = createInterstitialCaptor.firstValue
      // An example Moloco ad creation error.
      val molocoError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
      capturedCallback.invoke(/* interstitialAd= */ null, molocoError)

      val expectedAdError =
        AdError(
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
          MolocoMediationAdapter.SDK_ERROR_DOMAIN,
        )
      verify(mockMediationInterstitialAdLoadCallback)
        .onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun loadRtbInterstitialAd_whenAdIsNullAndNoMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createInterstitialCaptor = argumentCaptor<CreateInterstitialAdCallback>()

      adapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        mockMediationInterstitialAdLoadCallback,
      )

      mockedMoloco.verify {
        createInterstitial(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createInterstitialCaptor.capture())
      }
      val capturedCallback = createInterstitialCaptor.firstValue
      capturedCallback.invoke(/* interstitialAd= */ null, /* error= */ null)
      val expectedAdError =
        AdError(
          MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
          MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
          MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
        )
      verify(mockMediationInterstitialAdLoadCallback)
        .onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  private fun createMediationInterstitialAdConfiguration(
    bidResponse: String = "",
    serverParameters: Bundle = bundleOf(),
  ) =
    MediationInterstitialAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  // endregion

  // region Rewarded tests
  @Test
  fun loadRtbRewardedAd_withNullAdUnit_invokesOnFailure() {
    val mediationRewardedAdConfiguration = createMediationRewardedAdConfiguration()

    adapter.loadRtbRewardedAd(mediationRewardedAdConfiguration, mockMediationRewardedAdLoadCallback)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbRewardedAd_withEmptyAdUnit_invokesOnFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRtbRewardedAd(mediationRewardedAdConfiguration, mockMediationRewardedAdLoadCallback)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbRewardedAd_whenAdIsCreated_loadsMolocoRewarded() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationRewardedAdConfiguration =
        createMediationRewardedAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()

      adapter.loadRtbRewardedAd(
        mediationRewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )

      mockedMoloco.verify {
        createRewardedInterstitial(
          eq(TEST_AD_UNIT),
          eq(TEST_WATERMARK),
          createRewardedCaptor.capture(),
        )
      }
      val capturedCallback = createRewardedCaptor.firstValue
      capturedCallback.invoke(mockRewardedAd, /* error= */ null)
      verify(mockRewardedAd).load(eq(TEST_BID_RESPONSE), any())
    }
  }

  @Test
  fun loadRtbRewardedAd_whenMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationRewardedAdConfiguration =
        createMediationRewardedAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()

      adapter.loadRtbRewardedAd(
        mediationRewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )
      mockedMoloco.verify {
        createRewardedInterstitial(
          eq(TEST_AD_UNIT),
          eq(TEST_WATERMARK),
          createRewardedCaptor.capture(),
        )
      }
      val capturedCallback = createRewardedCaptor.firstValue
      // An example Moloco ad creation error.
      val molocoError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
      capturedCallback.invoke(/* rewardedAd= */ null, molocoError)

      val expectedAdError =
        AdError(
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
          MolocoMediationAdapter.SDK_ERROR_DOMAIN,
        )
      verify(mockMediationRewardedAdLoadCallback)
        .onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun loadRtbRewardedAd_whenAdIsNullAndNoMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationRewardedAdConfiguration =
        createMediationRewardedAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createRewardedCaptor = argumentCaptor<CreateRewardedInterstitialAdCallback>()

      adapter.loadRtbRewardedAd(
        mediationRewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )

      mockedMoloco.verify {
        createRewardedInterstitial(
          eq(TEST_AD_UNIT),
          eq(TEST_WATERMARK),
          createRewardedCaptor.capture(),
        )
      }
      val capturedCallback = createRewardedCaptor.firstValue
      capturedCallback.invoke(/* rewardedAd= */ null, /* error= */ null)
      val expectedAdError =
        AdError(
          MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
          MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
          MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
        )
      verify(mockMediationRewardedAdLoadCallback)
        .onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  private fun createMediationRewardedAdConfiguration(
    bidResponse: String = "",
    serverParameters: Bundle = bundleOf(),
  ) =
    MediationRewardedAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  // endregion

  // region Banner tests
  @Test
  fun loadRtbBannerAd_withNullAdUnit_invokesOnFailure() {
    val mediationBannerAdConfiguration = createMediationBannerAdConfiguration()

    adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_withEmptyAdUnit_invokesOnFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(serverParameters = serverParameters)

    adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_whenAdIsCreated_loadsMolocoBanner() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

      adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)

      mockedMoloco.verify {
        createBanner(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createBannerCaptor.capture())
      }
      val capturedCallback = createBannerCaptor.firstValue
      capturedCallback.invoke(mockBannerAd, /* error= */ null)
      verify(mockBannerAd).load(eq(TEST_BID_RESPONSE), any())
    }
  }

  @Test
  fun loadRtbBannerAd_whenMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

      adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)
      mockedMoloco.verify {
        createBanner(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createBannerCaptor.capture())
      }
      val capturedCallback = createBannerCaptor.firstValue
      // An example Moloco ad creation error.
      val molocoError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
      capturedCallback.invoke(/* banner= */ null, molocoError)

      val expectedAdError =
        AdError(
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
          MolocoMediationAdapter.SDK_ERROR_DOMAIN,
        )
      verify(mockMediationBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun loadRtbBannerAd_whenAdIsNullAndNoMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

      adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)

      mockedMoloco.verify {
        createBanner(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createBannerCaptor.capture())
      }
      val capturedCallback = createBannerCaptor.firstValue
      capturedCallback.invoke(/* banner= */ null, /* error= */ null)
      val expectedAdError =
        AdError(
          MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
          MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
          MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
        )
      verify(mockMediationBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun loadRtbBannerAd_whenSizeIsLeaderboard_loadsMolocoBannerTablet() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(
          TEST_BID_RESPONSE,
          serverParameters,
          AdSize.LEADERBOARD,
        )
      val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

      adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)

      mockedMoloco.verify {
        createBannerTablet(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createBannerCaptor.capture())
      }
      val capturedCallback = createBannerCaptor.firstValue
      capturedCallback.invoke(mockBannerAd, /* error= */ null)
      verify(mockBannerAd).load(eq(TEST_BID_RESPONSE), any())
    }
  }

  @Test
  fun loadRtbBannerTabletAd_whenMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(
          TEST_BID_RESPONSE,
          serverParameters,
          AdSize.LEADERBOARD,
        )
      val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

      adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)
      mockedMoloco.verify {
        createBannerTablet(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createBannerCaptor.capture())
      }
      val capturedCallback = createBannerCaptor.firstValue
      // An example Moloco ad creation error.
      val molocoError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
      capturedCallback.invoke(/* banner= */ null, molocoError)

      val expectedAdError =
        AdError(
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
          MolocoMediationAdapter.SDK_ERROR_DOMAIN,
        )
      verify(mockMediationBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun loadRtbBannerTabletAd_whenAdIsNullAndNoMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(
          TEST_BID_RESPONSE,
          serverParameters,
          AdSize.LEADERBOARD,
        )
      val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

      adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mockMediationBannerAdLoadCallback)
      mockedMoloco.verify {
        createBannerTablet(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createBannerCaptor.capture())
      }
      val capturedCallback = createBannerCaptor.firstValue
      capturedCallback.invoke(/* banner= */ null, /* error= */ null)

      val expectedAdError =
        AdError(
          MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
          MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
          MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
        )
      verify(mockMediationBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  private fun createMediationBannerAdConfiguration(
    bidResponse: String = "",
    serverParameters: Bundle = bundleOf(),
    adSize: AdSize = AdSize.BANNER,
  ) =
    MediationBannerAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      adSize,
      TEST_WATERMARK,
    )

  // endregion

  // region NativeAd tests

  @Test
  fun loadRtbNativeAdMapper_withNullAdUnit_invokesOnFailure() {
    val mediationNativeAdConfiguration = createMediationNativeAdConfiguration()

    adapter.loadRtbNativeAdMapper(mediationNativeAdConfiguration, mockMediationNativeAdLoadCallback)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbNativeAdMapper_withEmptyAdUnit_invokesOnFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(serverParameters = serverParameters)

    adapter.loadRtbNativeAdMapper(mediationNativeAdConfiguration, mockMediationNativeAdLoadCallback)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbNativeAdMapper_whenAdIsCreated_loadsMolocoNativeAd() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationNativeAdConfiguration =
        createMediationNativeAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()

      adapter.loadRtbNativeAdMapper(
        mediationNativeAdConfiguration,
        mockMediationNativeAdLoadCallback,
      )

      mockedMoloco.verify {
        createNativeAd(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createNativeAdCaptor.capture())
      }
      val capturedCallback = createNativeAdCaptor.firstValue
      capturedCallback.invoke(mockNativeAd, /* error= */ null)
      verify(mockNativeAd).load(eq(TEST_BID_RESPONSE), any())
    }
  }

  @Test
  fun loadRtbNativeAdMapper_whenMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationNativeAdConfiguration =
        createMediationNativeAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()

      adapter.loadRtbNativeAdMapper(
        mediationNativeAdConfiguration,
        mockMediationNativeAdLoadCallback,
      )
      mockedMoloco.verify {
        createNativeAd(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createNativeAdCaptor.capture())
      }
      val capturedCallback = createNativeAdCaptor.firstValue
      // An example Moloco ad creation error.
      val molocoError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
      capturedCallback.invoke(/* nativeAd= */ null, molocoError)

      val expectedAdError =
        AdError(
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
          MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
          MolocoMediationAdapter.SDK_ERROR_DOMAIN,
        )
      verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun loadRtbNativeAdMapper_whenAdIsNullAndNoMolocoErrorIsReported_invokesOnFailure() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
      val mediationNativeAdConfiguration =
        createMediationNativeAdConfiguration(TEST_BID_RESPONSE, serverParameters)
      val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()

      adapter.loadRtbNativeAdMapper(
        mediationNativeAdConfiguration,
        mockMediationNativeAdLoadCallback,
      )

      mockedMoloco.verify {
        createNativeAd(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createNativeAdCaptor.capture())
      }
      val capturedCallback = createNativeAdCaptor.firstValue
      capturedCallback.invoke(/* nativeAd= */ null, /* error= */ null)
      val expectedAdError =
        AdError(
          MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
          MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
          MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
        )
      verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  private fun createMediationNativeAdConfiguration(
    bidResponse: String = "",
    serverParameters: Bundle = bundleOf(),
  ) =
    MediationNativeAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
      null,
    )

  // endregion

  private companion object {
    const val TEST_APP_KEY_1 = "testAppKey1"
    const val TEST_APP_KEY_2 = "testAppKey2"
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
