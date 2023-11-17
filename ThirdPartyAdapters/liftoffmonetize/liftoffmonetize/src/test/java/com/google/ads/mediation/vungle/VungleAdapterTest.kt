package com.google.ads.mediation.vungle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.vungle.VungleMediationAdapter.getAdapterVersion
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import com.vungle.mediation.VungleAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [VungleAdapter]. */
@RunWith(AndroidJUnit4::class)
class VungleAdapterTest {
  private lateinit var adapter: VungleMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockRtbSignalData = mock<RtbSignalData>() { on { context } doReturn context }
  private val mockSdkWrapper = mock<SdkWrapper>()
  private val mockSignalCallbacks = mock<SignalCallbacks>()

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

  @Test
  fun getVersionInfo_returnsCorrectVersionInfo() {
    mockStatic(VungleMediationAdapter::class.java).use {
      whenever(getAdapterVersion()) doReturn "4.3.2.1"

      adapter.assertGetVersionInfo(expectedValue = "4.3.201")
    }
  }

  @Test
  fun getVersionInfo_versionTooShort_returnsZerosVersionInfo() {
    mockStatic(VungleMediationAdapter::class.java).use {
      whenever(getAdapterVersion()) doReturn "4.3.2"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_versionTooLong_returnsVersionInfoTruncatedToThreeTuple() {
    mockStatic(VungleMediationAdapter::class.java).use {
      whenever(getAdapterVersion()) doReturn "4.3.2.1.0"

      adapter.assertGetVersionInfo(expectedValue = "4.3.201")
    }
  }

  @Test
  fun collectSignals_onSuccessCalled() {
    val biddingToken = "token"
    whenever(mockSdkWrapper.getBiddingToken(any())) doReturn biddingToken

    adapter.collectSignals(mockRtbSignalData, mockSignalCallbacks)

    verify(mockSignalCallbacks).onSuccess(biddingToken)
  }

  @Test
  fun collectSignals_emptyBidToken_onFailureCalled() {
    val error =
      AdError(
        VungleMediationAdapter.ERROR_CANNOT_GET_BID_TOKEN,
        "Liftoff Monetize returned an empty bid token.",
        VungleMediationAdapter.ERROR_DOMAIN
      )
    whenever(mockSdkWrapper.getBiddingToken(any())) doReturn ""

    adapter.collectSignals(mockRtbSignalData, mockSignalCallbacks)

    verify(mockSignalCallbacks).onFailure(argThat(AdErrorMatcher(error)))
  }
}
