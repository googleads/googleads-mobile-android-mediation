package com.google.ads.mediation.vungle

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.vungle.VungleInitializer.getInstance
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INITIALIZATION_FAILURE
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.vungle.VungleMediationAdapter.getAdapterVersion
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [VungleAdapter]. */
@RunWith(AndroidJUnit4::class)
class VungleAdapterTest {
  private lateinit var adapter: VungleMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()
  private val mockRtbSignalData = mock<RtbSignalData>() { on { context } doReturn context }
  private val mockSdkWrapper = mock<SdkWrapper>()
  private val mockSignalCallbacks = mock<SignalCallbacks>()
  private val mockVungleInitializer = mock<VungleInitializer>()

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
  fun initialize_alreadyInitialized_callsOnSuccess() {
    whenever(mockSdkWrapper.isInitialized()) doReturn true

    adapter.mediationAdapterInitializeVerifySuccess(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1)
    )
  }

  @Test
  fun initialize_zeroMediationConfigurations_callsOnFailure() {
    val error = AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.", ERROR_DOMAIN)
    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
      /* expectedError= */ error.toString()
    )
  }

  @Test
  fun initialize_oneMediationConfiguration_callsOnSuccess() {
    val serverParameters = bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1)
    val configs = listOf(createMediationConfiguration(serverParameters = serverParameters))
    val listener = argumentCaptor<VungleInitializer.VungleInitializationListener>()

    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.initialize(context, mockInitializationCompleteCallback, configs)

      verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), any(), listener.capture())
      listener.firstValue.onInitializeSuccess()
      verify(mockInitializationCompleteCallback).onInitializationSucceeded()
    }
  }

  @Test
  fun initialize_twoMediationConfiguration_callsOnSuccess() {
    val serverParameters = bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1)
    val configs =
      listOf(
        createMediationConfiguration(serverParameters = serverParameters),
        createMediationConfiguration(serverParameters = serverParameters)
      )
    val listener = argumentCaptor<VungleInitializer.VungleInitializationListener>()

    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer
      adapter.initialize(context, mockInitializationCompleteCallback, configs)

      verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), any(), listener.capture())
      listener.firstValue.onInitializeSuccess()
      verify(mockInitializationCompleteCallback).onInitializationSucceeded()
    }
  }

  @Test
  fun initialize_vungleSdkInitFails_callsOnFailure() {
    val error = AdError(ERROR_INITIALIZATION_FAILURE, "Oops.", ERROR_DOMAIN)
    val serverParameters = bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1)
    val configs = listOf(createMediationConfiguration(serverParameters = serverParameters))
    val listener = argumentCaptor<VungleInitializer.VungleInitializationListener>()

    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer
      adapter.initialize(context, mockInitializationCompleteCallback, configs)

      verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), any(), listener.capture())
      listener.firstValue.onInitializeError(error)
      verify(mockInitializationCompleteCallback).onInitializationFailed(error.toString())
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

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
  }
}
