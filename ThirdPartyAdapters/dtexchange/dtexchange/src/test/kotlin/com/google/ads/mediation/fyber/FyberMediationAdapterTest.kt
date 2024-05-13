package com.google.ads.mediation.fyber

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class FyberMediationAdapterTest {

  private val adapter = FyberMediationAdapter()

  // region Version Tests
  @Test
  fun getSdkVersion_returnsCorrectVersion() {
    mockStatic(FyberAdapterUtils::class.java).use {
      whenever(FyberAdapterUtils.getSdkVersion()) doReturn "1.2.3"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSdkVersion_withMissingValues_returnsZeroes() {
    mockStatic(FyberAdapterUtils::class.java).use {
      whenever(FyberAdapterUtils.getSdkVersion()) doReturn "1.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test(expected = NumberFormatException::class)
  fun getSdkVersion_withInvalidValues_returnsZeroes() {
    mockStatic(FyberAdapterUtils::class.java).use {
      whenever(FyberAdapterUtils.getSdkVersion()) doReturn "INVALID-1.2.3"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_returnsSameVersion() {
    mockStatic(FyberAdapterUtils::class.java).use {
      whenever(FyberAdapterUtils.getAdapterVersion()) doReturn "1.2.3.0"

      adapter.assertGetVersionInfo(expectedValue = "1.2.300")
    }
  }

  @Test
  fun getVersionInfo_withMissingValue_returnsZeroes() {
    mockStatic(FyberAdapterUtils::class.java).use {
      whenever(FyberAdapterUtils.getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }
  // endregion
}
