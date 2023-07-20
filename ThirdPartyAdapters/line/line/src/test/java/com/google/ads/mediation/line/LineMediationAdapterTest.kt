package com.google.ads.mediation.line

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.FiveAdConfig
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class LineMediationAdapterTest {

  // Subject of tests
  private var lineMediationAdapter = LineMediationAdapter()

  private val mockSdkWrapper = mock<SdkWrapper>()
  private val mockFiveAdConfig = mock<FiveAdConfig>()
  private val mockSdkFactory =
    mock<SdkFactory> { on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig }
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    LineSdkWrapper.delegate = mockSdkWrapper
    LineSdkFactory.delegate = mockSdkFactory
  }

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

  @Test
  fun initialize_withEmptyMediationConfigurations_invokesOnInitializationFailed() {
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    verify(mockInitializationCompleteCallback)
      .onInitializationFailed(eq(LineMediationAdapter.ERROR_MSG_MISSING_APP_ID))
  }

  @Test
  fun initialize_withInvalidApplicationId_invokesOnInitializationFailed() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to "")
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    verify(mockInitializationCompleteCallback)
      .onInitializationFailed(eq(LineMediationAdapter.ERROR_MSG_MISSING_APP_ID))
  }

  @Test
  fun initialize_withMultipleAppIds_invokesInitializeOnlyOnce() {
    val serverParameters1 = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID_1)
    val serverParameters2 = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID_2)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters1)
    val mediationConfiguration2 = createMediationConfiguration(AdFormat.BANNER, serverParameters2)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration, mediationConfiguration2)
    )

    verify(mockSdkFactory, times(1)).createFiveAdConfig(eq(TEST_APP_ID_1))
    verify(mockSdkFactory, never()).createFiveAdConfig(eq(TEST_APP_ID_2))
    verify(mockSdkWrapper, times(1)).initialize(eq(context), eq(mockFiveAdConfig))
  }

  @Test
  fun initialize_withInitializedSdk_doesNotInvokeInitializeAgain() {
    whenever(mockSdkWrapper.isInitialized()) doReturn true
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    verify(mockSdkWrapper, never()).initialize(any(), any())
  }

  @Test
  fun initialize_withCorrectApplicationId_invokesLineSdkInitialize() {
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    verify(mockSdkFactory).createFiveAdConfig(eq(TEST_APP_ID_1))
    verify(mockSdkWrapper).initialize(eq(context), eq(mockFiveAdConfig))
  }

  @Test
  fun initialize_whenExceptionThrownOnSdk_invokesOnInitializationFailed() {
    whenever(mockSdkWrapper.initialize(any(), any())) doThrow
      IllegalArgumentException(TEST_INITIALIZE_ERROR_MSG)
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    verify(mockInitializationCompleteCallback).onInitializationFailed(eq(TEST_INITIALIZE_ERROR_MSG))
  }

  @Test
  fun initialize_withTestDevicesIds_configuresTestToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder().setTestDeviceIds(listOf("TEST_DEVICE")).build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    assertThat(mockFiveAdConfig.isTest).isTrue()
  }

  @Test
  fun initialize_withoutTestDevicesIds_configuresTestToFalse() {
    val requestConfiguration = RequestConfiguration.Builder().setTestDeviceIds(null).build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val serverParameters = bundleOf(LineMediationAdapter.KEY_APP_ID to TEST_APP_ID_1)
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER, serverParameters)

    lineMediationAdapter.initialize(
      context,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    assertThat(mockFiveAdConfig.isTest).isFalse()
  }

  private fun createMediationConfiguration(
    adFormat: AdFormat,
    serverParameters: Bundle = bundleOf(),
  ) = MediationConfiguration(adFormat, serverParameters)

  companion object {
    private const val MAJOR_VERSION = 4
    private const val MINOR_VERSION = 3
    private const val MICRO_VERSION = 2
    private const val PATCH_VERSION = 1
    private const val CORRECT_TEST_VERSION =
      "${MAJOR_VERSION}.${MINOR_VERSION}.${MICRO_VERSION}.${PATCH_VERSION}"
    // Invalid Adapter Version has less than 4 digits
    private const val INVALID_ADAPTER_VERSION = "${PATCH_VERSION}.${PATCH_VERSION}.${PATCH_VERSION}"
    // Invalid Sdk Version has less than 3 digits
    private const val INVALID_SDK_VERSION = "${PATCH_VERSION}.${PATCH_VERSION}"
    private const val TEST_APP_ID_1 = "testAppId1"
    private const val TEST_APP_ID_2 = "testAppId2"
    private const val TEST_INITIALIZE_ERROR_MSG = "testInitializeErrorMessage"
  }
}
