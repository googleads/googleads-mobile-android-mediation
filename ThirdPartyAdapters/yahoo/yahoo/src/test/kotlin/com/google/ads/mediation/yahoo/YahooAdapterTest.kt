package com.google.ads.mediation.yahoo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.yahoo.YahooAdapterUtils.getAdapterVersion
import com.google.ads.mediation.yahoo.YahooAdapterUtils.getSDKVersionInfo
import com.google.android.gms.ads.mediation.Adapter
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/** Tests for [YahooMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class YahooAdapterTest {

  private lateinit var adapter: YahooMediationAdapter

  @Before
  fun setUp() {
    adapter = YahooMediationAdapter()
  }

  @Test
  fun instanceOfYahooMediationAdapter_returnsAnInstanceOfAdapter() {
    assertThat(adapter is Adapter).isTrue()
  }

  @Test
  fun getVersionInfo_invalid3Digits_returnsZeros() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_invalidString_returnsZeros() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "foobar"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_valid4Digits_returnsValid() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_valid5Digits_returnsValid() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4.5"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getSDKVersionInfo_5Digits_returnsValid() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getSDKVersionInfo()) doReturn "1.2.3.4.5"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSDKVersionInfo_3Digits_returnsValid() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getSDKVersionInfo()) doReturn "3.2.1"

      adapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSDKVersionInfo_invalid2Digits_returnsZeros() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getSDKVersionInfo()) doReturn "1.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidString_returnsZeros() {
    mockStatic(YahooAdapterUtils::class.java).use {
      whenever(getSDKVersionInfo()) doReturn "foobar"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }
}
