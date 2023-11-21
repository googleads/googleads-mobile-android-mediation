package com.google.ads.mediation.maio

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.maio.MaioUtils.getVersionInfo
import com.google.common.truth.Truth.assertThat
import jp.maio.sdk.android.MaioAds
import jp.maio.sdk.android.MaioAds.getSdkVersion
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/** Class containing unit tests for [MaioMediationAdapter] */
@RunWith(AndroidJUnit4::class)
class MaioMediationAdapterTest {

  private var adapter: MaioMediationAdapter = MaioMediationAdapter()

  @Test
  fun instanceOfMaioAdapter_returnsAnInstanceOfMaioMediationAdapter() {
    assertThat(adapter is MaioMediationAdapter).isTrue()
  }

  @Test
  fun getVersionInfo_validVersionFor4Digits_returnsTheSameVersion() {
    mockStatic(MaioUtils::class.java).use {
      whenever(getVersionInfo()) doReturn "7.3.2.1"

      adapter.assertGetVersionInfo(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getVersionInfo_validVersionFor5Digits_returnsTheValidVersion() {
    mockStatic(MaioUtils::class.java).use {
      whenever(getVersionInfo()) doReturn "7.3.2.1.5"

      adapter.assertGetVersionInfo(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZeros() {
    mockStatic(MaioUtils::class.java).use {
      whenever(getVersionInfo()) doReturn "3.2"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor3Digits_returnsTheSameVersion() {
    mockStatic(MaioAds::class.java).use {
      whenever(getSdkVersion()) doReturn "7.3.2"

      adapter.assertGetSdkVersion(expectedValue = "7.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor4Digits_returnsTheValidVersion() {
    mockStatic(MaioAds::class.java).use {
      whenever(getSdkVersion()) doReturn "7.3.2.1"

      adapter.assertGetSdkVersion(expectedValue = "7.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZeros() {
    mockStatic(MaioAds::class.java).use {
      whenever(getSdkVersion()) doReturn "3.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }
}
