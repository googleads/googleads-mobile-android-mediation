package com.google.ads.mediation.facebook

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.BidderTokenProvider
import com.facebook.ads.BidderTokenProvider.getBidderToken
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.loadRtbInterstitialAdWithFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyNoFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.facebook.FacebookAdapterUtils.adapterVersion
import com.google.ads.mediation.facebook.FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER
import com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience
import com.google.ads.mediation.facebook.FacebookSdkWrapper.sdkVersion
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class FacebookMediationAdapterTest {
  private var facebookMediationAdapter: FacebookMediationAdapter = FacebookMediationAdapter()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rtbSignalData = mock<RtbSignalData>() { on { context } doReturn context }
  private val signalCallbacks = mock<SignalCallbacks>()
  private val mockInitializationCompleteCallback: InitializationCompleteCallback = mock()
  private val mockInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  val mediationAdConfiguration: MediationAdConfiguration = mock()

  @Before
  fun setUp() {
    facebookMediationAdapter = FacebookMediationAdapter()
  }

  // region Version Tests
  @Test
  fun getVersionInfo_returnCorrectVersionInfo() {
    mockStatic(FacebookAdapterUtils::class.java).use {
      whenever(adapterVersion) doReturn "4.3.2.1"

      facebookMediationAdapter.assertGetVersionInfo(expectedValue = "4.3.201")
    }
  }

  @Test
  fun getVersionInfo_whenUnexpectedVersionFormat_returnZeroesVersionInfo() {
    mockStatic(FacebookAdapterUtils::class.java).use {
      whenever(adapterVersion) doReturn "3.2.1"

      facebookMediationAdapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSdkVersion_returnsCorrectSdkVersionInfo() {
    mockStatic(FacebookSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "3.2.1"

      facebookMediationAdapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSdkVersion_whenUnexpectedVersionFormat_returnsZerosVersionInfo() {
    mockStatic(FacebookSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "1.0"

      facebookMediationAdapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region signal collection tests
  @Test
  fun collectSignals_invokesOnSuccessCallbackWithBiddingToken() {
    mockStatic(BidderTokenProvider::class.java).use {
      whenever(getBidderToken(any())) doReturn (AdapterTestKitConstants.TEST_BID_RESPONSE)

      facebookMediationAdapter.collectSignals(rtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(AdapterTestKitConstants.TEST_BID_RESPONSE)
    }
  }

  // endregion

  @Test
  fun setMixedAudience_whenTfcdTrue_setsMixedAudienceTrue() {
    whenever(mediationAdConfiguration.taggedForChildDirectedTreatment()) doReturn
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE

    setMixedAudience(mediationAdConfiguration)

    assertThat(AdSettings.isMixedAudience()).isTrue()
  }

  @Test
  fun setMixedAudience_whenTfcdFalse_setsMixedAudienceFalse() {
    whenever(mediationAdConfiguration.taggedForChildDirectedTreatment()) doReturn
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE

    setMixedAudience(mediationAdConfiguration)

    assertThat(AdSettings.isMixedAudience()).isFalse()
  }

  // region Initialize Tests
  @Test
  fun initialize_initializesAudienceNetworkAds() {
    mockStatic(AudienceNetworkAds::class.java).use {
      val mockInitSettingsBuilder =
        mock<AudienceNetworkAds.InitSettingsBuilder> {
          on { withMediationService(any()) } doReturn it
          on { withPlacementIds(any()) } doReturn it
          on { withInitListener(any()) } doReturn it
        }
      whenever(AudienceNetworkAds.buildInitSettings(context)) doReturn mockInitSettingsBuilder

      facebookMediationAdapter.mediationAdapterInitializeVerifyNoFailure(
        context,
        mockInitializationCompleteCallback,
        bundleOf(RTB_PLACEMENT_PARAMETER to TEST_APP_ID)
      )

      verify(mockInitSettingsBuilder).initialize()
    }
  }

  @Test
  fun initialize_whenInitializerListenerSucceeds_invokesOnInitializationSucceeded() {
    mockStatic(FacebookInitializer::class.java).use {
      val mockInitializer: FacebookInitializer = mock()
      whenever(FacebookInitializer.getInstance()) doReturn mockInitializer
      whenever(mockInitializer.initialize(any(), any<ArrayList<String>>(), any())) doAnswer
        { invocation ->
          val arguments = invocation.arguments
          (arguments[2] as FacebookInitializer.Listener).onInitializeSuccess()
        }

      facebookMediationAdapter.mediationAdapterInitializeVerifySuccess(
        context,
        mockInitializationCompleteCallback,
        bundleOf(RTB_PLACEMENT_PARAMETER to TEST_APP_ID)
      )
    }
  }

  @Test
  fun initialize_withEmptyMediationConfigurations_invokesOnInitializationFailed() {
    facebookMediationAdapter.mediationAdapterInitializeVerifyFailure(
      context,
      mockInitializationCompleteCallback,
      bundleOf(),
      "Initialization failed. No placement IDs found."
    )
  }

  @Test
  fun initialize_withEmptyPlacementId_invokesOnInitializationFailed() {
    facebookMediationAdapter.mediationAdapterInitializeVerifyFailure(
      context,
      mockInitializationCompleteCallback,
      bundleOf(RTB_PLACEMENT_PARAMETER to ""),
      "Initialization failed. No placement IDs found."
    )
  }

  // endregion

  // region Interstitial Ad Tests
  @Test
  fun loadRtbInterstitialAd_withoutPlacementId_invokesOnFailure() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(context = context)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty. ",
        FacebookMediationAdapter.ERROR_DOMAIN
      )

    facebookMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadRtbInterstitialAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(RTB_PLACEMENT_PARAMETER to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters
      )
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty. ",
        FacebookMediationAdapter.ERROR_DOMAIN
      )

    facebookMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError
    )
  }

  // endregion
}
