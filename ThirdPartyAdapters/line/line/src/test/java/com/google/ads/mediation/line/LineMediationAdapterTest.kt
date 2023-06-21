package com.google.ads.mediation.line

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.VersionInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class LineMediationAdapterTest {

  // Subject of tests
  private lateinit var lineMediationAdapter: LineMediationAdapter

  private val sdkWrapper = mock<SdkWrapper>()

  @Before
  fun setUp() {
    lineSdkDelegate = sdkWrapper
    lineMediationAdapter = LineMediationAdapter()
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
    whenever(sdkWrapper.getSdkVersion()) doReturn CORRECT_TEST_VERSION

    val sdkVersionInfo = lineMediationAdapter.sdkVersionInfo

    assertThat(sdkVersionInfo.toString()).isEqualTo(VersionInfo(4, 3, 2).toString())
  }

  @Test
  fun getSdkVersion_whenUnexpectedVersionFormat_returnsZerosVersionInfo() {
    whenever(sdkWrapper.getSdkVersion()) doReturn INVALID_SDK_VERSION

    val sdkVersionInfo = lineMediationAdapter.sdkVersionInfo

    assertThat(sdkVersionInfo.toString()).isEqualTo(VersionInfo(0, 0, 0).toString())
  }

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
  }
}
