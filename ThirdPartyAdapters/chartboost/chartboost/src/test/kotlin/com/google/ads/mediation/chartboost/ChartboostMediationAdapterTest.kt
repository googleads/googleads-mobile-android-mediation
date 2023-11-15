package com.google.ads.mediation.chartboost

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Chartboost.getSDKVersion
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.getAdapterVersion
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/** Tests for [ChartboostMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class ChartboostMediationAdapterTest {

  private lateinit var adapter: ChartboostMediationAdapter

  @Before
  fun setUp() {
    adapter = ChartboostMediationAdapter()
  }

  @Test
  fun getVersionInfo_valid4Digits_returnsCorrectVersion() {
    mockStatic(ChartboostAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_valid5Digits_returnsCorrectVersion() {
    mockStatic(ChartboostAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4.5"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZero() {
    mockStatic(ChartboostAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSDKVersionInfo_valid3Digits_returnsCorrectVersion() {
    mockStatic(Chartboost::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2.3"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSDKVersionInfo_valid4Digits_returnsCorrectVersion() {
    mockStatic(Chartboost::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2.3.4"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZero() {
    mockStatic(Chartboost::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }
}
