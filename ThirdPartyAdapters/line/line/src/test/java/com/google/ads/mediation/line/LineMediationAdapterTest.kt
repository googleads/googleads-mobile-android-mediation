package com.google.ads.mediation.line

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.AdLoader
import com.five_corp.ad.AdLoader.CollectSignalCallback
import com.five_corp.ad.AdSlotConfig
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterstitial
import com.five_corp.ad.FiveAdNative
import com.five_corp.ad.FiveAdVideoReward
import com.five_corp.ad.NeedChildDirectedTreatment
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.line.LineExtras.Companion.KEY_ENABLE_AD_SOUND
import com.google.ads.mediation.line.LineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.line.LineMediationAdapter.Companion.ERROR_CODE_MISSING_SLOT_ID
import com.google.ads.mediation.line.LineMediationAdapter.Companion.ERROR_CODE_NULL_AD_LOADER
import com.google.ads.mediation.line.LineMediationAdapter.Companion.ERROR_MSG_MISSING_SLOT_ID
import com.google.ads.mediation.line.LineMediationAdapter.Companion.ERROR_MSG_NULL_AD_LOADER
import com.google.ads.mediation.line.LineMediationAdapter.Companion.KEY_APP_ID
import com.google.ads.mediation.line.LineMediationAdapter.Companion.KEY_SLOT_ID
import com.google.ads.mediation.line.LineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.VideoOptions
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
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class LineMediationAdapterTest {

  // Subject of tests
  private var lineMediationAdapter = LineMediationAdapter()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mockSdkWrapper = mock<SdkWrapper>()
  private val fiveAdConfig = FiveAdConfig(TEST_APP_ID_1)
  private val mockFiveAdCustomLayout =
    mock<FiveAdCustomLayout> {
      on { logicalWidth } doReturn AdSize.BANNER.width
      on { logicalHeight } doReturn AdSize.BANNER.height
    }
  private val mockFiveAdInterstitial = mock<FiveAdInterstitial>()
  private val mockFiveAdVideoReward = mock<FiveAdVideoReward>()
  private val mockFiveAdNative = mock<FiveAdNative>()
  private val mockSdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(TEST_APP_ID_1) } doReturn fiveAdConfig
      on { createFiveAdConfig(TEST_APP_ID_2) } doReturn FiveAdConfig(TEST_APP_ID_2)
      on { createFiveAdCustomLayout(context, TEST_SLOT_ID, AdSize.BANNER.width) } doReturn
        mockFiveAdCustomLayout
      on { createFiveAdInterstitial(context, TEST_SLOT_ID) } doReturn mockFiveAdInterstitial
      on { createFiveVideoRewarded(context, TEST_SLOT_ID) } doReturn mockFiveAdVideoReward
      on { createFiveAdInterstitial(activity, TEST_SLOT_ID) } doReturn mockFiveAdInterstitial
      on { createFiveVideoRewarded(activity, TEST_SLOT_ID) } doReturn mockFiveAdVideoReward
      on { createFiveAdNative(context, TEST_SLOT_ID) } doReturn mockFiveAdNative
    }
  private val initializationCompleteCallback = FakeInitializationCompleteCallback()
  private val signalCallbacks = FakeSignalCallbacks()
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
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)

  @Before
  fun setUp() {
    LineSdkWrapper.delegate = mockSdkWrapper
    LineSdkFactory.delegate = mockSdkFactory
    LineInitializer.resetFiveAdConfig()
  }

  // region Version Tests
  @Test
  fun getVersionInfo_returnsCorrectVersionInfo() {
    LineMediationAdapter.adapterVersionDelegate = CORRECT_TEST_VERSION

    val versionInfo = lineMediationAdapter.versionInfo

    assertThat(versionInfo.toString()).isEqualTo(VersionInfo(4, 3, 201).toString())
  }

  @Test
  fun getVersionInfo_whenUnexpectedVersionFormat_returnsZerosVersionInfo() {
    LineMediationAdapter.adapterVersionDelegate = INVALID_ADAPTER_VERSION

    val versionInfo = lineMediationAdapter.versionInfo

    assertThat(versionInfo.toString()).isEqualTo(VersionInfo(0, 0, 0).toString())
  }

  @Test
  fun getSdkVersion_returnsCorrectSdkVersionInfo() {
    whenever(mockSdkWrapper.getSdkVersion()) doReturn CORRECT_TEST_VERSION

    val sdkVersionInfo = lineMediationAdapter.sdkVersionInfo

    assertThat(sdkVersionInfo.toString()).isEqualTo(VersionInfo(4, 3, 2).toString())
  }

  @Test
  fun getSdkVersion_whenUnexpectedVersionFormat_returnsZerosVersionInfo() {
    whenever(mockSdkWrapper.getSdkVersion()) doReturn INVALID_SDK_VERSION

    val sdkVersionInfo = lineMediationAdapter.sdkVersionInfo

    assertThat(sdkVersionInfo.toString()).isEqualTo(VersionInfo(0, 0, 0).toString())
  }

  // endregion

  // region Initialize Tests
  @Test
  fun initialize_withEmptyMediationConfigurations_invokesOnInitializationFailed() {
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(initializationCompleteCallback)
      .hasFailedWith(LineMediationAdapter.ERROR_MSG_MISSING_APP_ID)
  }

  @Test
  fun initialize_withInvalidApplicationId_invokesOnInitializationFailed() {
    val serverParameters = bundleOf(KEY_APP_ID to "")
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(initializationCompleteCallback)
      .hasFailedWith(LineMediationAdapter.ERROR_MSG_MISSING_APP_ID)
  }

  @Test
  fun initialize_withMultipleAppIds_invokesInitializeOnlyOnce() {
    val serverParameters1 = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val serverParameters2 = bundleOf(KEY_APP_ID to TEST_APP_ID_2)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters1)
    val mediationConfiguration2 = createMediationConfiguration(AdFormat.BANNER, serverParameters2)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration, mediationConfiguration2),
    )

    verify(mockSdkFactory, times(1)).createFiveAdConfig(eq(TEST_APP_ID_1))
    verify(mockSdkFactory, never()).createFiveAdConfig(eq(TEST_APP_ID_2))
    verify(mockSdkWrapper, times(1)).initialize(eq(context), eq(fiveAdConfig))
  }

  @Test
  fun initialize_withInitializedSdk_doesNotInvokeInitializeAgain() {
    whenever(mockSdkWrapper.isInitialized()) doReturn true
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    verify(mockSdkWrapper, never()).initialize(any(), any())
  }

  @Test
  fun initialize_withCorrectApplicationId_invokesLineSdkInitialize() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    verify(mockSdkFactory).createFiveAdConfig(eq(TEST_APP_ID_1))
    verify(mockSdkWrapper).initialize(eq(context), eq(fiveAdConfig))
  }

  @Test
  fun initialize_whenExceptionThrownOnSdk_invokesOnInitializationFailed() {
    whenever(mockSdkWrapper.initialize(any(), any())) doThrow
      IllegalArgumentException(TEST_INITIALIZE_ERROR_MSG)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(initializationCompleteCallback).hasFailedWith(TEST_INITIALIZE_ERROR_MSG)
  }

  @Test
  fun initialize_withTagForChildTreatmentTrue_configuresFiveAdSDKWithTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.TRUE)
  }

  @Test
  fun initialize_withTagForUnderageConsentTrue_configuresFiveAdSDKWithTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.TRUE)
  }

  @Test
  fun initialize_withTagForChildTreatmentFalse_configuresFiveAdSDKWithFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.FALSE)
  }

  @Test
  fun initialize_withTagUnderageConsentFalse_configuresFiveAdSDKWithFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.FALSE)
  }

  @Test
  fun initialize_withTagForChildTreatmentUnspecified_configuresFiveAdSDKWithUnspecified() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.needChildDirectedTreatment)
      .isEqualTo(NeedChildDirectedTreatment.UNSPECIFIED)
  }

  @Test
  fun initialize_withAgeRestrictedTreatmentChild_configuresFiveAdSDKWithTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD).build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.needChildDirectedTreatment).isEqualTo(NeedChildDirectedTreatment.TRUE)
  }

  @Test
  fun initialize_withAgeRestrictedTreatmentTeen_configuresFiveAdSDKWithUnspecified() {
    val requestConfiguration =
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN).build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.needChildDirectedTreatment)
      .isEqualTo(NeedChildDirectedTreatment.UNSPECIFIED)
  }

  @Test
  fun initialize_withTestModeTrue_configuresTestModeToTrueOnLineSdk() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    LineMediationAdapter.setTestMode(true)
    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.isTest).isTrue()
  }

  @Test
  fun initialize_withTestModeFalse_configuresTestModeToFalseOnLineSdk() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    LineMediationAdapter.setTestMode(false)
    lineMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfiguration),
    )

    assertThat(fiveAdConfig.isTest).isFalse()
  }

  private fun createMediationConfiguration(
    adFormat: AdFormat,
    serverParameters: Bundle = bundleOf(),
  ) = MediationConfiguration(adFormat, serverParameters)

  // endregion

  // region Collect Signals Tests
  @Test
  fun collectSignals_withNoSlotId_invokeOnFailure() {
    val serverParameters = bundleOf()
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
    val signalData =
      RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), /* adSize= */ null)

    lineMediationAdapter.collectSignals(signalData, signalCallbacks)

    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(signalCallbacks).hasFailedWith(expectedAdError)
  }

  @Test
  fun collectSignals_withEmptySlotId_invokeOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to "")
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
    val signalData =
      RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), /* adSize= */ null)

    lineMediationAdapter.collectSignals(signalData, signalCallbacks)

    val expectedAdError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(signalCallbacks).hasFailedWith(expectedAdError)
  }

  @Test
  fun collectSignals_withProperValues_invokeOnSuccess() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
      val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
      val signalData =
        RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), /* adSize= */ null)
      val collectSignalCallbackCaptor = argumentCaptor<CollectSignalCallback>()
      lineMediationAdapter.initialize(
        context,
        initializationCompleteCallback,
        listOf(mediationConfiguration),
      )

      lineMediationAdapter.collectSignals(signalData, signalCallbacks)

      verify(mockAdLoader).collectSignal(any<AdSlotConfig>(), collectSignalCallbackCaptor.capture())
      collectSignalCallbackCaptor.firstValue.onCollect(TEST_BID_RESPONSE)
      assertThat(signalCallbacks).hasSucceededWith(TEST_BID_RESPONSE)
    }
  }

  @Test
  fun collectSignals_withProperValuesAndCallbackError_invokeOnFailure() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
      val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
      val signalData =
        RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), /* adSize= */ null)
      val collectSignalCallbackCaptor = argumentCaptor<CollectSignalCallback>()
      lineMediationAdapter.initialize(
        context,
        initializationCompleteCallback,
        listOf(mediationConfiguration),
      )

      lineMediationAdapter.collectSignals(signalData, signalCallbacks)

      verify(mockAdLoader).collectSignal(any<AdSlotConfig>(), collectSignalCallbackCaptor.capture())
      collectSignalCallbackCaptor.firstValue.onError(FiveAdErrorCode.INTERNAL_ERROR)
      val expectedAdError =
        AdError(
          FiveAdErrorCode.INTERNAL_ERROR.value,
          FiveAdErrorCode.INTERNAL_ERROR.name,
          SDK_ERROR_DOMAIN,
        )
      assertThat(signalCallbacks).hasFailedWith(expectedAdError)
    }
  }

  @Test
  fun collectSignals_whenNullAdLoader_invokesOnFailure() {
    mockStatic(AdLoader::class.java).use {
      whenever(AdLoader.forConfig(eq(context), any())) doReturn null
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
      val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)
      val signalData =
        RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), /* adSize= */ null)

      lineMediationAdapter.collectSignals(signalData, signalCallbacks)

      val expectedAdError =
        AdError(ERROR_CODE_NULL_AD_LOADER, ERROR_MSG_NULL_AD_LOADER, SDK_ERROR_DOMAIN)
      assertThat(signalCallbacks).hasFailedWith(expectedAdError)
    }
  }

  // endregion

  // region Banner Ad Tests
  @Test
  fun loadBannerAd_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to "")
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_withNullSlotId_invokesOnFailure() {
    // Default mediationBannerAdConfiguration does not contain serverParameters with a value for the
    // key [LineMediationAdapter#.KEY_SLOT_ID]
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationBannerAdConfiguration = createMediationBannerAdConfiguration(serverParameters)

    lineMediationAdapter.loadBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_withEmptySlotId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to "", KEY_APP_ID to TEST_APP_ID_1)
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_verifiesInitializationAndThenCreatesAndLoadsCustomLayout() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to TEST_APP_ID_1)
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    inOrder(mockSdkWrapper, mockFiveAdCustomLayout) {
      verify(mockSdkWrapper).initialize(context, fiveAdConfig)
      verify(mockFiveAdCustomLayout).setLoadListener(isA<LineBannerAd>())
      verify(mockFiveAdCustomLayout).enableSound(false)
      verify(mockFiveAdCustomLayout).loadAdAsync()
    }
  }

  @Test
  fun loadBannerAd_withExtras_modifiesEnablesSound() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to TEST_APP_ID_1)
    val mediationExtras = bundleOf(KEY_ENABLE_AD_SOUND to true)
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        serverParameters = serverParameters,
        mediationExtras = mediationExtras,
      )

    lineMediationAdapter.loadBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    verify(mockFiveAdCustomLayout).enableSound(true)
  }

  @Test
  fun loadBannerAd_withNullAppId_invokesOnFailure() {
    // Default mediationBannerAdConfiguration does not contain serverParameters with a value for the
    // key [LineMediationAdapter#.KEY_APP_ID]
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationBannerAdConfiguration = createMediationBannerAdConfiguration(serverParameters)

    lineMediationAdapter.loadBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  private fun createMediationBannerAdConfiguration(
    serverParameters: Bundle = bundleOf(),
    adSize: AdSize = AdSize.BANNER,
    mediationExtras: Bundle = bundleOf(),
    bidResponse: String = "",
  ) =
    MediationBannerAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      mediationExtras,
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      adSize,
      TEST_WATERMARK,
    )

  // endregion

  // region RTB Banner Ad Tests
  @Test
  fun loadRtbBannerAd_withNullAppId_invokesOnFailure() {
    val serverParameters = bundleOf()
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(serverParameters, bidResponse = TEST_BID_RESPONSE)

    lineMediationAdapter.loadRtbBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbBannerAd_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to "")
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        serverParameters = serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    lineMediationAdapter.loadRtbBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbBannerAd_createsAndLoadsCustomLayout() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(
          serverParameters = serverParameters,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadBannerAdCallback>()
      verify(mockAdLoader).loadBannerAd(any<BidData>(), any<Int>(), loadCallbackCaptor.capture())
      val capturedCallback = loadCallbackCaptor.firstValue
      capturedCallback.onLoad(mockFiveAdCustomLayout)
      verify(mockFiveAdCustomLayout).setEventListener(isA<LineBannerAd>())
      verify(mockFiveAdCustomLayout).enableSound(false)
      assertThat(bannerAdLoadCallback).hasSucceeded()
    }
  }

  @Test
  fun loadRtbBannerAd_withExtras_modifiesEnablesSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationExtras = bundleOf(KEY_ENABLE_AD_SOUND to true)
      val mediationBannerAdConfiguration =
        createMediationBannerAdConfiguration(
          serverParameters = serverParameters,
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbBannerAd(mediationBannerAdConfiguration, bannerAdLoadCallback)

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadBannerAdCallback>()
      verify(mockAdLoader).loadBannerAd(any<BidData>(), any<Int>(), loadCallbackCaptor.capture())
      val capturedCallback = loadCallbackCaptor.firstValue
      capturedCallback.onLoad(mockFiveAdCustomLayout)
      verify(mockFiveAdCustomLayout).enableSound(true)
      assertThat(bannerAdLoadCallback).hasSucceeded()
    }
  }

  // endregion

  // region Interstitial Ad Tests

  @Test
  fun loadInterstitialAd_withNullAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to "", KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_withNullSlotId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_withEmptySlotId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_verifiesInitializationAndThenCreatesAndLoadsFiveAdInterstitial() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to TEST_APP_ID_1)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    inOrder(mockSdkWrapper, mockFiveAdInterstitial) {
      verify(mockSdkWrapper).initialize(activity, fiveAdConfig)
      verify(mockFiveAdInterstitial).setLoadListener(isA<LineInterstitialAd>())
      verify(mockFiveAdInterstitial).enableSound(true)
      verify(mockFiveAdInterstitial).loadAdAsync()
    }
  }

  @Test
  fun loadInterstitialAd_evenWithNonActivityContext_loadsFiveAdInterstitial() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    inOrder(mockSdkWrapper, mockFiveAdInterstitial) {
      verify(mockSdkWrapper).initialize(context, fiveAdConfig)
      verify(mockFiveAdInterstitial).setLoadListener(isA<LineInterstitialAd>())
      verify(mockFiveAdInterstitial).enableSound(true)
      verify(mockFiveAdInterstitial).loadAdAsync()
    }
  }

  @Test
  fun loadInterstitialAd_withExtras_modifiesEnableSound() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to TEST_APP_ID_1)
    val mediationExtras = bundleOf(KEY_ENABLE_AD_SOUND to false)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        activity,
        serverParameters = serverParameters,
        mediationExtras = mediationExtras,
      )

    lineMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    verify(mockFiveAdInterstitial).enableSound(false)
  }

  private fun createMediationInterstitialAdConfiguration(
    context: Context = this.context,
    serverParameters: Bundle = bundleOf(),
    mediationExtras: Bundle = bundleOf(),
    bidResponse: String = "",
  ) =
    MediationInterstitialAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      mediationExtras,
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  // endregion

  // region RTB Interstitial Ad Tests

  @Test
  fun loadRtbInterstitialAd_withNullAppId_invokesOnFailure() {
    val serverParameters = bundleOf()
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        activity,
        serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    lineMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbInterstitialAd_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        activity,
        serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    lineMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      interstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbInterstitialAd_verifiesInitializationCreatesAdLoaderAndSuccessfullyLoads() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(
          activity,
          serverParameters,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        interstitialAdLoadCallback,
      )

      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onLoad(mockFiveAdInterstitial)
      verify(mockFiveAdInterstitial).setEventListener(isA<LineInterstitialAd>())
      assertThat(interstitialAdLoadCallback).hasSucceeded()
    }
  }

  @Test
  fun loadRtbInterstitialAd_evenWithNonActivityContext_successfullyLoads() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(
          context,
          serverParameters,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        interstitialAdLoadCallback,
      )

      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onLoad(mockFiveAdInterstitial)
      verify(mockFiveAdInterstitial).setEventListener(isA<LineInterstitialAd>())
      assertThat(interstitialAdLoadCallback).hasSucceeded()
    }
  }

  @Test
  fun loadRtbInterstitialAd_withExtras_modifiesEnableSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationExtras = bundleOf(KEY_ENABLE_AD_SOUND to false)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(
          activity,
          serverParameters = serverParameters,
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        interstitialAdLoadCallback,
      )

      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onLoad(mockFiveAdInterstitial)
      verify(mockFiveAdInterstitial).enableSound(false)
      assertThat(interstitialAdLoadCallback).hasSucceeded()
    }
  }

  @Test
  fun loadRtbInterstitialAd_verifiesInitializationAndCreatesAdLoaderButFailsLoads() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadInterstitialAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationExtras = bundleOf(KEY_ENABLE_AD_SOUND to false)
      val mediationInterstitialAdConfiguration =
        createMediationInterstitialAdConfiguration(
          activity,
          serverParameters = serverParameters,
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration,
        interstitialAdLoadCallback,
      )

      verify(mockAdLoader).loadInterstitialAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onError(FiveAdErrorCode.INTERNAL_ERROR)
      val expectedAdError =
        AdError(
          FiveAdErrorCode.INTERNAL_ERROR.value,
          FiveAdErrorCode.INTERNAL_ERROR.name,
          SDK_ERROR_DOMAIN,
        )
      assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
    }
  }

  // endregion

  // region Rewarded Ad Tests

  @Test
  fun loadRewardedAd_withNullAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to "", KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_withNullSlotId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_withEmptySlotId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to "")
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_verifyInitializationAndThenCreatesAndLoadsFiveAdInterstitial() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to TEST_APP_ID_1)
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(activity, serverParameters)

    lineMediationAdapter.loadRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    inOrder(mockSdkWrapper, mockFiveAdVideoReward) {
      verify(mockSdkWrapper).initialize(activity, fiveAdConfig)
      verify(mockFiveAdVideoReward).setLoadListener(isA<LineRewardedAd>())
      verify(mockFiveAdVideoReward).enableSound(true)
      verify(mockFiveAdVideoReward).loadAdAsync()
    }
  }

  @Test
  fun loadRewardedAd_evenWithNonActivityContext_loadsFiveAdInterstitial() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    inOrder(mockSdkWrapper, mockFiveAdVideoReward) {
      verify(mockSdkWrapper).initialize(context, fiveAdConfig)
      verify(mockFiveAdVideoReward).setLoadListener(isA<LineRewardedAd>())
      verify(mockFiveAdVideoReward).enableSound(true)
      verify(mockFiveAdVideoReward).loadAdAsync()
    }
  }

  @Test
  fun loadRewardedAd_withExtras_modifiesEnableSound() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to TEST_APP_ID_1)
    val mediationExtras: Bundle = bundleOf(KEY_ENABLE_AD_SOUND to false)
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters = serverParameters,
        mediationExtras = mediationExtras,
      )

    lineMediationAdapter.loadRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    inOrder(mockSdkWrapper, mockFiveAdVideoReward) {
      verify(mockSdkWrapper).initialize(activity, fiveAdConfig)
      verify(mockFiveAdVideoReward).setLoadListener(isA<LineRewardedAd>())
      verify(mockFiveAdVideoReward).enableSound(false)
      verify(mockFiveAdVideoReward).loadAdAsync()
    }
  }

  private fun createMediationRewardedAdConfiguration(
    context: Context = this.context,
    serverParameters: Bundle = Bundle(),
    mediationExtras: Bundle = bundleOf(),
    bidResponse: String = "",
  ) =
    MediationRewardedAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      mediationExtras,
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  // endregion

  // region RTB Rewarded Ad Tests

  @Test
  fun loadRtbRewardedAd_withNullAppId_invokesOnFailure() {
    val serverParameters = bundleOf()
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    lineMediationAdapter.loadRtbRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbRewardedAd_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to "")
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    lineMediationAdapter.loadRtbRewardedAd(mediationRewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbRewardedAd_verifyCreatesAdLoaderAndSuccessfullyLoads() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationRewardedAdConfiguration =
        createMediationRewardedAdConfiguration(
          activity,
          serverParameters,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbRewardedAd(
        mediationRewardedAdConfiguration,
        rewardedAdLoadCallback,
      )

      verify(mockAdLoader).loadRewardAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onLoad(mockFiveAdVideoReward)
      verify(mockFiveAdVideoReward).setEventListener(isA<LineRewardedAd>())
      verify(mockFiveAdVideoReward).enableSound(true)
      assertThat(rewardedAdLoadCallback).hasSucceeded()
    }
  }

  @Test
  fun loadRtbRewardedAd_evenWithNonActivityContext_successfullyLoads() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationRewardedAdConfiguration =
        createMediationRewardedAdConfiguration(
          context,
          serverParameters,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbRewardedAd(
        mediationRewardedAdConfiguration,
        rewardedAdLoadCallback,
      )

      verify(mockAdLoader).loadRewardAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onLoad(mockFiveAdVideoReward)
      verify(mockFiveAdVideoReward).setEventListener(isA<LineRewardedAd>())
      verify(mockFiveAdVideoReward).enableSound(true)
      assertThat(rewardedAdLoadCallback).hasSucceeded()
    }
  }

  @Test
  fun loadRtbRewardedAd_withExtras_modifiesEnableSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationExtras: Bundle = bundleOf(KEY_ENABLE_AD_SOUND to false)
      val mediationRewardedAdConfiguration =
        createMediationRewardedAdConfiguration(
          activity,
          serverParameters = serverParameters,
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbRewardedAd(
        mediationRewardedAdConfiguration,
        rewardedAdLoadCallback,
      )

      verify(mockAdLoader).loadRewardAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onLoad(mockFiveAdVideoReward)
      verify(mockFiveAdVideoReward).enableSound(false)
      assertThat(rewardedAdLoadCallback).hasSucceeded()
    }
  }

  @Test
  fun loadRtbRewardedAd_verifiesInitializationAndCreatesAdLoaderButFailsLoads() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(activity), any())) doReturn mockAdLoader
      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadRewardAdCallback>()
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
      val mediationExtras = bundleOf(KEY_ENABLE_AD_SOUND to false)
      val mediationRewardedAdConfiguration =
        createMediationRewardedAdConfiguration(
          activity,
          serverParameters = serverParameters,
          mediationExtras = mediationExtras,
          bidResponse = TEST_BID_RESPONSE,
        )

      lineMediationAdapter.loadRtbRewardedAd(
        mediationRewardedAdConfiguration,
        rewardedAdLoadCallback,
      )

      verify(mockAdLoader).loadRewardAd(any<BidData>(), loadCallbackCaptor.capture())
      val loadCallback = loadCallbackCaptor.firstValue
      loadCallback.onError(FiveAdErrorCode.INTERNAL_ERROR)
      val expectedAdError =
        AdError(
          FiveAdErrorCode.INTERNAL_ERROR.value,
          FiveAdErrorCode.INTERNAL_ERROR.name,
          SDK_ERROR_DOMAIN,
        )
      assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
    }
  }

  // endregion

  // region Native Ad Tests
  @Test
  fun loadNativeAdMapper_withNullAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadNativeAdMapper_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to "", KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadNativeAdMapper_withNullSlotId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadNativeAdMapper_withEmptySlotId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to "")
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    val expectedError =
      AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadNativeAdMapper_withMuteNativeAdOptions_setsEnableSound() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationNativeAdConfiguration =
      spy(createMediationNativeAdConfiguration(serverParameters = serverParameters))
    val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
    val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn nativeAdOptions

    lineMediationAdapter.loadNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    verify(mockFiveAdNative).enableSound(false)
  }

  @Test
  fun loadNativeAdMapper_verifyInitializationAndThenCreatesAndLoadsFiveAdNative() {
    whenever(mockSdkWrapper.isInitialized()) doReturn false
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID, KEY_APP_ID to TEST_APP_ID_1)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(serverParameters = serverParameters)

    lineMediationAdapter.loadNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    inOrder(mockSdkWrapper, mockFiveAdNative) {
      verify(mockSdkWrapper).initialize(context, fiveAdConfig)
      verify(mockFiveAdNative).setLoadListener(isA<LineNativeAd>())
      verify(mockFiveAdNative).loadAdAsync()
    }
  }

  private fun createMediationNativeAdConfiguration(
    context: Context = this.context,
    serverParameters: Bundle = Bundle(),
    bidResponse: String = "",
  ) =
    MediationNativeAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
      /*nativeAdOptions=*/ null,
    )

  // endregion

  // region RTB Native Ad Tests
  @Test
  fun loadRtbNativeAdMapper_withNullAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        serverParameters = serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    lineMediationAdapter.loadRtbNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRtbNativeAdMapper_withEmptyAppId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to "", KEY_SLOT_ID to TEST_SLOT_ID)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        serverParameters = serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    lineMediationAdapter.loadRtbNativeAdMapper(mediationNativeAdConfiguration, nativeAdLoadCallback)

    val expectedError =
      AdError(
        LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
        LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
        ADAPTER_ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRtbNativeAdMapper_withMuteNativeAdOptions_setsEnableSound() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
      val mediationNativeAdConfiguration =
        spy(
          createMediationNativeAdConfiguration(
            serverParameters = serverParameters,
            bidResponse = TEST_BID_RESPONSE,
          )
        )
      val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
      val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
      whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn nativeAdOptions

      lineMediationAdapter.loadRtbNativeAdMapper(
        mediationNativeAdConfiguration,
        nativeAdLoadCallback,
      )

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadNativeAdCallback>()
      verify(mockAdLoader).loadNativeAd(any(), loadCallbackCaptor.capture())
      val capturedCallback = loadCallbackCaptor.firstValue
      capturedCallback.onLoad(mockFiveAdNative)
      verify(mockFiveAdNative).enableSound(false)
    }
  }

  @Test
  fun loadRtbNativeAdMapper_onErrorWhenLoading_invokesOnFailure() {
    mockStatic(AdLoader::class.java).use {
      val mockAdLoader = mock<AdLoader>()
      whenever(AdLoader.forConfig(eq(context), any())) doReturn mockAdLoader
      val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_SLOT_ID to TEST_SLOT_ID)
      val mediationNativeAdConfiguration =
        spy(
          createMediationNativeAdConfiguration(
            serverParameters = serverParameters,
            bidResponse = TEST_BID_RESPONSE,
          )
        )
      val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
      val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
      whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn nativeAdOptions

      lineMediationAdapter.loadRtbNativeAdMapper(
        mediationNativeAdConfiguration,
        nativeAdLoadCallback,
      )

      val loadCallbackCaptor = argumentCaptor<AdLoader.LoadNativeAdCallback>()
      verify(mockAdLoader).loadNativeAd(any(), loadCallbackCaptor.capture())
      val capturedCallback = loadCallbackCaptor.firstValue
      capturedCallback.onError(FiveAdErrorCode.NO_AD)
      val expectedAdError =
        AdError(FiveAdErrorCode.NO_AD.value, FiveAdErrorCode.NO_AD.name, SDK_ERROR_DOMAIN)
      assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
    }
  }

  // endregion

  private companion object {
    const val MAJOR_VERSION = 4
    const val MINOR_VERSION = 3
    const val MICRO_VERSION = 2
    const val PATCH_VERSION = 1
    const val CORRECT_TEST_VERSION =
      "${MAJOR_VERSION}.${MINOR_VERSION}.${MICRO_VERSION}.${PATCH_VERSION}"
    // Invalid Adapter Version has less than 4 digits
    const val INVALID_ADAPTER_VERSION = "${PATCH_VERSION}.${PATCH_VERSION}.${PATCH_VERSION}"
    // Invalid Sdk Version has less than 3 digits
    const val INVALID_SDK_VERSION = "${PATCH_VERSION}.${PATCH_VERSION}"
    const val TEST_APP_ID_1 = "testAppId1"
    const val TEST_APP_ID_2 = "testAppId2"
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_INITIALIZE_ERROR_MSG = "testInitializeErrorMessage"
    const val TEST_WATERMARK = "testWatermark"
  }
}
