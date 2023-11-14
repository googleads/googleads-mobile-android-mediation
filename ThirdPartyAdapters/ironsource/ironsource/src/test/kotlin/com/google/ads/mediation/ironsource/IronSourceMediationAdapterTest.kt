package com.google.ads.mediation.ironsource

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.ironsource.IronSourceAdapterUtils.getAdapterVersion
import com.ironsource.mediationsdk.utils.IronSourceUtils
import com.ironsource.mediationsdk.utils.IronSourceUtils.getSDKVersion
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/** Tests for [IronSourceMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class IronSourceMediationAdapterTest {

  private lateinit var adapter: IronSourceMediationAdapter

  @Before
  fun setUp() {
    adapter = IronSourceMediationAdapter()
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor3Digits_returnsTheSameVersion() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "7.3.2"

      adapter.assertGetSdkVersion(expectedValue = "7.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor4Digits_returnsTheSameVersion() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "7.3.2.1"

      adapter.assertGetSdkVersion(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZeros() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "3.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_validVersionWith4Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2.1"

      adapter.assertGetVersionInfo(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getVersionInfo_validVersionWith5Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2.1.8"

      adapter.assertGetVersionInfo(expectedValue = "7.3.20108")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZeros() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }
}
