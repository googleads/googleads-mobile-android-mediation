package com.google.ads.mediation.vungle

import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.VungleAds.Companion.getSdkVersion
import com.vungle.mediation.VungleAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [VungleAdapter]. */
@RunWith(JUnit4::class)
class VungleAdapterTest {
  private lateinit var adapter: VungleMediationAdapter

  private val mockSdkWrapper = mock<SdkWrapper>()

  @Before
  fun setUp() {
    VungleSdkWrapper.delegate = mockSdkWrapper
    adapter = VungleAdapter()
  }

  @Test
  fun instanceOfVungleAdapter_returnsAnInstanceOfVungleMediationAdapter() {
    assertThat(adapter is VungleMediationAdapter).isTrue()
  }

  @Test
  fun getSdkVersion_returnsCorrectSdkVersionInfo() {
    whenever(mockSdkWrapper.getSdkVersion()) doReturn "4.3.2"

    adapter.assertGetSdkVersion(expectedValue = "4.3.2")
  }

  @Test
  fun getSdkVersion_versionTooShort_returnsZerosVersionInfo() {
    whenever(mockSdkWrapper.getSdkVersion()) doReturn "4.3"

    adapter.assertGetSdkVersion(expectedValue = "0.0.0")
  }

  @Test
  fun getSdkVersion_versionTooLong_returnsVersionInfoTruncatedToThreeTuple() {
    whenever(mockSdkWrapper.getSdkVersion()) doReturn "4.3.2.1"

    adapter.assertGetSdkVersion(expectedValue = "4.3.2")
  }
}
