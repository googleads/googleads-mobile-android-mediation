package com.google.ads.mediation.vungle

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_USER_ID
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener
import com.google.ads.mediation.vungle.VungleInitializer.getInstance
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INITIALIZATION_FAILURE
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.ads.mediation.vungle.VungleMediationAdapter.getAdapterVersion
import com.google.ads.mediation.vungle.rtb.VungleRtbBannerAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize.BANNER
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_LEFT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_RIGHT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.AdConfig
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.InterstitialAd
import com.vungle.ads.NativeAd
import com.vungle.ads.NativeAd.Companion.BOTTOM_LEFT
import com.vungle.ads.NativeAd.Companion.BOTTOM_RIGHT
import com.vungle.ads.NativeAd.Companion.TOP_LEFT
import com.vungle.ads.NativeAd.Companion.TOP_RIGHT
import com.vungle.ads.RewardedAd
import com.vungle.ads.VungleAdSize
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

/** Tests for [VungleMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class VungleMediationAdapterTest {
  private lateinit var adapter: VungleMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()
  private val mockRtbSignalData = mock<RtbSignalData>() { on { context } doReturn context }
  private val mockSdkWrapper = mock<SdkWrapper>()
  private val mockSignalCallbacks = mock<SignalCallbacks>()
  private val mockVungleInitializer = mock<VungleInitializer>()
  private val vungleAdConfig = mock<AdConfig>()
  private val vungleNativeAd = mock<NativeAd> { on { adConfig } doReturn vungleAdConfig }
  private val vungleFactory =
    mock<VungleFactory> {
      on { createNativeAd(any(), any()) } doReturn vungleNativeAd
      on { createAdConfig() } doReturn vungleAdConfig
    }
  private val mediationNativeAdConfiguration =
    mock<MediationNativeAdConfiguration> {
      on { context } doReturn context
      on { serverParameters } doReturn
        bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID)
    }

  @Before
  fun setUp() {
    VungleSdkWrapper.delegate = mockSdkWrapper
    adapter = VungleMediationAdapter(vungleFactory)
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
      /* serverParameters= */ bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1),
    )
  }

  @Test
  fun initialize_zeroMediationConfigurations_callsOnFailure() {
    val error = AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.", ERROR_DOMAIN)
    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
      /* expectedError= */ error.toString(),
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
        createMediationConfiguration(serverParameters = serverParameters),
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
  fun initialize_withMultipleAppIds_initializesLiftoffSdkUsingOneOfTheAppIds() {
    val serverParameters1 = bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1)
    val serverParameters2 = bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_2)
    val configs =
      listOf(
        createMediationConfiguration(serverParameters = serverParameters1),
        createMediationConfiguration(serverParameters = serverParameters2),
      )
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.initialize(context, mockInitializationCompleteCallback, configs)
    }

    val appIdCaptor = argumentCaptor<String>()
    verify(mockVungleInitializer, times(1)).initialize(appIdCaptor.capture(), any(), any())
    assertThat(appIdCaptor.firstValue).isAnyOf(TEST_APP_ID_1, TEST_APP_ID_2)
  }

  @Test
  fun loadRewardedAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRewardedAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRewardedAd_loadsLiftoffRewardedAd() {
    stubVungleInitializerToSucceed()
    val vungleRewardedAd = mock<RewardedAd>()
    whenever(vungleFactory.createRewardedAd(any(), any(), any())) doReturn vungleRewardedAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRewardedAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleFactory).createRewardedAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleRewardedAd).adListener = adapter
    verify(vungleRewardedAd).setUserId(TEST_USER_ID)
    verify(vungleRewardedAd).load(eq(null))
  }

  @Test
  fun loadRewardedAd_withoutAppId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRewardedAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load waterfall rewarded ad from Liftoff Monetize. " +
          "Missing or invalid App ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_withoutPlacementId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRewardedAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load waterfall rewarded ad from Liftoff Monetize. " +
          "Missing or Invalid Placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRewardedAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        rewardedAdLoadCallback,
      )
    }

    verify(rewardedAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadNativeAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(createMediationNativeAdConfiguration(context = context), mock())
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadNativeAd_loadsLiftoffNativeAd() {
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(
        createMediationNativeAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleFactory).createNativeAd(context, TEST_PLACEMENT_ID)
    verify(vungleNativeAd).adOptionsPosition = TOP_RIGHT
    verify(vungleNativeAd).adListener = any()
    verify(vungleNativeAd).load("")
  }

  @Test
  fun loadNativeAd_forTopLeftAdChoicesPlacement_setsTopLeftPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_TOP_LEFT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = TOP_LEFT
  }

  @Test
  fun loadNativeAd_forBottomLeftAdChoicesPlacement_setsBottomLeftPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_BOTTOM_LEFT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = BOTTOM_LEFT
  }

  @Test
  fun loadNativeAd_forBottomRightAdChoicesPlacement_setsBottomRightPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_BOTTOM_RIGHT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = BOTTOM_RIGHT
  }

  @Test
  fun loadNativeAd_forTopRightAdChoicesPlacement_setsTopRightPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_TOP_RIGHT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = TOP_RIGHT
  }

  @Test
  fun loadNativeAd_withoutAppId_callsLoadFailure() {
    val nativeAdLoadCallback =
      mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()

    adapter.loadNativeAd(
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      ),
      nativeAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding native ad from Liftoff Monetize. " +
          "Missing or invalid app ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadNativeAd_withoutPlacementId_callsLoadFailure() {
    val nativeAdLoadCallback =
      mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()

    adapter.loadNativeAd(
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
      ),
      nativeAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding native ad from Liftoff Monetize. " +
          "Missing or Invalid placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadNativeAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val nativeAdLoadCallback =
      mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(
        createMediationNativeAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        ),
        nativeAdLoadCallback,
      )
    }

    verify(nativeAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadRewardedInterstitialAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRewardedInterstitialAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRewardedInterstitialAd_loadsLiftoffRewardedAd() {
    stubVungleInitializerToSucceed()
    val vungleRewardedAd = mock<RewardedAd>()
    whenever(vungleFactory.createRewardedAd(any(), any(), any())) doReturn vungleRewardedAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRewardedInterstitialAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleFactory).createRewardedAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleRewardedAd).adListener = adapter
    verify(vungleRewardedAd).setUserId(TEST_USER_ID)
    verify(vungleRewardedAd).load(eq(null))
  }

  @Test
  fun loadRewardedInterstitialAd_withoutAppId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRewardedInterstitialAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load waterfall rewarded ad from Liftoff Monetize. " +
          "Missing or invalid App ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedInterstitialAd_withoutPlacementId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRewardedInterstitialAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load waterfall rewarded ad from Liftoff Monetize. " +
          "Missing or Invalid Placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRewardedInterstitialAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        rewardedAdLoadCallback,
      )
    }

    verify(rewardedAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadAppOpenAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadAppOpenAd(createMediationAppOpenAdConfiguration(context = context), mock())
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadAppOpenAd_loadsLiftoffInterstitialAd() {
    stubVungleInitializerToSucceed()
    val vungleAppOpenAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleAppOpenAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleFactory).createInterstitialAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleAppOpenAd).adListener = any()
    verify(vungleAppOpenAd).load(eq(null))
  }

  @Test
  fun loadAppOpenAd_withoutAppId_callsLoadFailure() {
    val appOpenAdLoadCallback =
      mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>>()

    adapter.loadAppOpenAd(
      createMediationAppOpenAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      appOpenAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load app open ad from Liftoff Monetize. Missing or invalid App ID " +
          "configured for this ad source instance in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(appOpenAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAppOpenAd_withoutPlacementId_callsLoadFailure() {
    val appOpenAdLoadCallback =
      mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>>()

    adapter.loadAppOpenAd(
      createMediationAppOpenAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      appOpenAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load app open ad from Liftoff Monetize. Missing or Invalid Placement " +
          "ID configured for this ad source instance in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(appOpenAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAppOpenAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val appOpenAdLoadCallback =
      mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(appOpenAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadRtbRewardedAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedAd(createMediationRewardedAdConfiguration(context = context), mock())
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRtbRewardedAd_loadsLiftoffRewardedAdWithBidResponse() {
    stubVungleInitializerToSucceed()
    val vungleRewardedAd = mock<RewardedAd>()
    whenever(vungleFactory.createRewardedAd(any(), any(), any())) doReturn vungleRewardedAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
    verify(vungleFactory).createRewardedAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleRewardedAd).adListener = any()
    verify(vungleRewardedAd).setUserId(TEST_USER_ID)
    verify(vungleRewardedAd).load(TEST_BID_RESPONSE)
  }

  @Test
  fun loadRtbRewardedAd_withoutAppId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRtbRewardedAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding rewarded ad from Liftoff Monetize. " +
          "Missing or invalid App ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbRewardedAd_withoutPlacementId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRtbRewardedAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding rewarded ad from Liftoff Monetize. " +
          "Missing or invalid Placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbRewardedAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        rewardedAdLoadCallback,
      )
    }

    verify(rewardedAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadRtbBannerAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbBannerAd(createMediationBannerAdConfiguration(context = context), mock())
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRtbBannerAd_loadsLiftoffBannerAdWithBidResponse() {
    stubVungleInitializerToSucceed()
    val vungleBannerAd = mock<VungleBannerView> { on { adConfig } doReturn vungleAdConfig }
    whenever(vungleFactory.createBannerAd(any(), any(), any())) doReturn vungleBannerAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbBannerAd(
        createMediationBannerAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleFactory).createBannerAd(context, TEST_PLACEMENT_ID, VungleAdSize.BANNER)
    verify(vungleBannerAd).load(TEST_BID_RESPONSE)
    val bannerAdCaptor = argumentCaptor<VungleRtbBannerAd>()
    verify(vungleBannerAd).adListener = bannerAdCaptor.capture()
    val bannerLayout = bannerAdCaptor.firstValue.view
    assertThat(bannerLayout.layoutParams.width).isEqualTo(BANNER.getWidthInPixels(context))
    assertThat(bannerLayout.layoutParams.height).isEqualTo(BANNER.getHeightInPixels(context))
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
  }

  @Test
  fun loadRtbBannerAd_withoutAppId_callsLoadFailure() {
    val bannerAdLoadCallback =
      mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()

    adapter.loadRtbBannerAd(
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
      ),
      bannerAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding banner ad from Liftoff Monetize. " +
          "Missing or invalid App ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(bannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_withoutPlacementId_callsLoadFailure() {
    val bannerAdLoadCallback =
      mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()

    adapter.loadRtbBannerAd(
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
      ),
      bannerAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding banner ad from Liftoff Monetize. " +
          "Missing or Invalid Placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(bannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val bannerAdLoadCallback =
      mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbBannerAd(
        createMediationBannerAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
        ),
        bannerAdLoadCallback,
      )
    }

    verify(bannerAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadRtbInterstitialAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbInterstitialAd(
        createMediationInterstitialAdConfiguration(context = context),
        mock(),
      )
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRtbInterstitialAd_loadsLiftoffInterstitialAdWithBidResponse() {
    stubVungleInitializerToSucceed()
    val vungleInterstitialAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleInterstitialAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbInterstitialAd(
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
    verify(vungleFactory).createInterstitialAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleInterstitialAd).adListener = any()
    verify(vungleInterstitialAd).load(TEST_BID_RESPONSE)
  }

  @Test
  fun loadRtbInterstitialAd_withoutAppId_callsLoadFailure() {
    val interstitialAdLoadCallback =
      mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()

    adapter.loadRtbInterstitialAd(
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      interstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding interstitial ad from Liftoff Monetize. " +
          "Missing or invalid App ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(interstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbInterstitialAd_withoutPlacementId_callsLoadFailure() {
    val interstitialAdLoadCallback =
      mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()

    adapter.loadRtbInterstitialAd(
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      interstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding interstitial ad from Liftoff Monetize. " +
          "Missing or Invalid Placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(interstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val interstitialAdLoadCallback =
      mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbInterstitialAd(
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        interstitialAdLoadCallback,
      )
    }

    verify(interstitialAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadRtbNativeAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(createMediationNativeAdConfiguration(context = context), mock())
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRtbNativeAd_loadsLiftoffNativeAdWithBidResponse() {
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(
        createMediationNativeAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleFactory).createNativeAd(context, TEST_PLACEMENT_ID)
    verify(vungleNativeAd).adOptionsPosition = TOP_RIGHT
    verify(vungleNativeAd).adListener = any()
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
    verify(vungleNativeAd).load(TEST_BID_RESPONSE)
  }

  @Test
  fun loadRtbNativeAd_forTopLeftAdChoicesPlacement_setsTopLeftPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_TOP_LEFT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = TOP_LEFT
  }

  @Test
  fun loadRtbNativeAd_forBottomLeftAdChoicesPlacement_setsBottomLeftPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_BOTTOM_LEFT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = BOTTOM_LEFT
  }

  @Test
  fun loadRtbNativeAd_forBottomRightAdChoicesPlacement_setsBottomRightPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_BOTTOM_RIGHT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = BOTTOM_RIGHT
  }

  @Test
  fun loadRtbNativeAd_forTopRightAdChoicesPlacement_setsTopRightPositionOnLiftoffSdk() {
    stubVungleInitializerToSucceed()
    whenever(mediationNativeAdConfiguration.nativeAdOptions) doReturn
      NativeAdOptions.Builder().setAdChoicesPlacement(ADCHOICES_TOP_RIGHT).build()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, mock())
    }

    verify(vungleNativeAd).adOptionsPosition = TOP_RIGHT
  }

  @Test
  fun loadRtbNativeAd_withoutAppId_callsLoadFailure() {
    val nativeAdLoadCallback =
      mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()

    adapter.loadRtbNativeAd(
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
      ),
      nativeAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding native ad from Liftoff Monetize. " +
          "Missing or invalid app ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbNativeAd_withoutPlacementId_callsLoadFailure() {
    val nativeAdLoadCallback =
      mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()

    adapter.loadRtbNativeAd(
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
      ),
      nativeAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding native ad from Liftoff Monetize. " +
          "Missing or Invalid placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbNativeAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val nativeAdLoadCallback =
      mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(
        createMediationNativeAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
        ),
        nativeAdLoadCallback,
      )
    }

    verify(nativeAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadRtbRewardedInterstitialAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedInterstitialAd(
        createMediationRewardedAdConfiguration(context = context),
        mock(),
      )
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRtbRewardedInterstitialAd_loadsLiftoffRewardedAdWithBidResponse() {
    stubVungleInitializerToSucceed()
    val vungleRewardedAd = mock<RewardedAd>()
    whenever(vungleFactory.createRewardedAd(any(), any(), any())) doReturn vungleRewardedAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedInterstitialAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
    verify(vungleFactory).createRewardedAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleRewardedAd).adListener = any()
    verify(vungleRewardedAd).setUserId(TEST_USER_ID)
    verify(vungleRewardedAd).load(TEST_BID_RESPONSE)
  }

  @Test
  fun loadRtbRewardedInterstitialAd_withoutAppId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRtbRewardedInterstitialAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding rewarded ad from Liftoff Monetize. " +
          "Missing or invalid App ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbRewardedInterstitialAd_withoutPlacementId_callsLoadFailure() {
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

    adapter.loadRtbRewardedInterstitialAd(
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
      ),
      rewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load bidding rewarded ad from Liftoff Monetize. " +
          "Missing or invalid Placement ID configured for this ad source instance " +
          "in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbRewardedInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val rewardedAdLoadCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedInterstitialAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
        ),
        rewardedAdLoadCallback,
      )
    }

    verify(rewardedAdLoadCallback).onFailure(liftoffSdkInitError)
  }

  @Test
  fun loadRtbAppOpenAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbAppOpenAd(createMediationAppOpenAdConfiguration(context = context), mock())
    }

    verify(mockVungleInitializer).updateCoppaStatus(any())
  }

  @Test
  fun loadRtbAppOpenAd_loadsLiftoffInterstitialAdWithBidResponse() {
    stubVungleInitializerToSucceed()
    val vungleInterstitialAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleInterstitialAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        mock(),
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
    verify(vungleFactory).createInterstitialAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleInterstitialAd).adListener = any()
    verify(vungleInterstitialAd).load(TEST_BID_RESPONSE)
  }

  @Test
  fun loadRtbAppOpenAd_withoutAppId_callsLoadFailure() {
    val appOpenAdLoadCallback =
      mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>>()

    adapter.loadRtbAppOpenAd(
      createMediationAppOpenAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      appOpenAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load app open ad from Liftoff Monetize. Missing or invalid App ID " +
          "configured for this ad source instance in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(appOpenAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbAppOpenAd_withoutPlacementId_callsLoadFailure() {
    val appOpenAdLoadCallback =
      mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>>()

    adapter.loadRtbAppOpenAd(
      createMediationAppOpenAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        bidResponse = TEST_BID_RESPONSE,
        watermark = TEST_WATERMARK,
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      appOpenAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load app open ad from Liftoff Monetize. Missing or Invalid Placement " +
          "ID configured for this ad source instance in the AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(appOpenAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbAppOpenAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        VungleError.UNKNOWN_ERROR,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    val appOpenAdLoadCallback =
      mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>>()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(appOpenAdLoadCallback).onFailure(liftoffSdkInitError)
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
        VungleMediationAdapter.ERROR_DOMAIN,
      )
    whenever(mockSdkWrapper.getBiddingToken(any())) doReturn ""

    adapter.collectSignals(mockRtbSignalData, mockSignalCallbacks)

    verify(mockSignalCallbacks).onFailure(argThat(AdErrorMatcher(error)))
  }

  private fun stubVungleInitializerToSucceed() {
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
  }

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
    const val TEST_APP_ID_2 = "testAppId2"
    const val TEST_USER_ID = "testUserId"
  }
}
