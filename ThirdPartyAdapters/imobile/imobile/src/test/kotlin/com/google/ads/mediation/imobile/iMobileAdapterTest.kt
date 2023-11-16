package com.google.ads.mediation.imobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.imobile.AdapterHelper.getAdapterVersion
import com.google.android.gms.ads.mediation.Adapter
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/** Tests for [IMobileMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class IMobileMediationAdapterTest {

  private lateinit var adapter: IMobileMediationAdapter

  @Before
  fun setUp() {
    adapter = IMobileMediationAdapter()
  }

  @Test
  fun instanceOfIMobileMediationAdapter_returnsAnInstanceOfAdapter() {
    assertThat(adapter is Adapter).isTrue()
  }

  @Test
  fun getVersionInfo_invalid3Digits_returnsZeros() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_invalidString_returnsZeros() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "foobar"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_valid4Digits_returnsValid() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_valid5Digits_returnsValid() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4.5"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getSDKVersionInfo_returnsDefault() {
    adapter.assertGetSdkVersion(expectedValue = "0.0.0")
  }
}
