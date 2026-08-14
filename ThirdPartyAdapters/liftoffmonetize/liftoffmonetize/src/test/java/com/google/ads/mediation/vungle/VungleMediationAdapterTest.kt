package com.google.ads.mediation.vungle

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_BACK_BUTTON_IMMEDIATELY_ENABLED
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
import com.google.ads.mediation.vungle.renderers.VungleBannerAd
import com.google.ads.mediation.vungle.rtb.VungleRtbBannerAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdSize.BANNER
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
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_LEFT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_RIGHT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.AdConfig
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.BidTokenCallback
import com.vungle.ads.InterstitialAd
import com.vungle.ads.NativeAd
import com.vungle.ads.NativeAd.Companion.BOTTOM_LEFT
import com.vungle.ads.NativeAd.Companion.BOTTOM_RIGHT
import com.vungle.ads.NativeAd.Companion.TOP_LEFT
import com.vungle.ads.NativeAd.Companion.TOP_RIGHT
import com.vungle.ads.RewardedAd
import com.vungle.ads.VungleAdSize
import com.vungle.ads.VungleBannerView
import com.vungle.ads.internal.protos.Sdk.SDKError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
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
  private val initializationCompleteCallback = FakeInitializationCompleteCallback()
  private val signalCallbacks = FakeSignalCallbacks()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>()
  private val appOpenAdLoadCallback =
    FakeMediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>()
  private val mockRtbSignalData = mock<RtbSignalData>() { on { context } doReturn context }
  private val mockSdkWrapper = mock<SdkWrapper>()
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

    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(
        createMediationConfiguration(
          serverParameters = bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1)
        )
      ),
    )

    assertThat(initializationCompleteCallback).hasSucceeded()
  }

  @Test
  fun initialize_zeroMediationConfigurations_callsOnFailure() {
    val error = AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.", ERROR_DOMAIN)
    adapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(createMediationConfiguration(serverParameters = bundleOf())),
    )

    assertThat(initializationCompleteCallback).hasFailedWith(error.toString())
  }

  @Test
  fun initialize_oneMediationConfiguration_callsOnSuccess() {
    val serverParameters = bundleOf(VungleConstants.KEY_APP_ID to TEST_APP_ID_1)
    val configs = listOf(createMediationConfiguration(serverParameters = serverParameters))
    val listener = argumentCaptor<VungleInitializer.VungleInitializationListener>()

    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.initialize(context, initializationCompleteCallback, configs)

      verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), any(), listener.capture())
      listener.firstValue.onInitializeSuccess()
      assertThat(initializationCompleteCallback).hasSucceeded()
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
      adapter.initialize(context, initializationCompleteCallback, configs)

      verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), any(), listener.capture())
      listener.firstValue.onInitializeSuccess()
      assertThat(initializationCompleteCallback).hasSucceeded()
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
      adapter.initialize(context, initializationCompleteCallback, configs)

      verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), any(), listener.capture())
      listener.firstValue.onInitializeError(error)
      assertThat(initializationCompleteCallback).hasFailedWith(error.toString())
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

      adapter.initialize(context, initializationCompleteCallback, configs)
    }

    val appIdCaptor = argumentCaptor<String>()
    verify(mockVungleInitializer, times(1)).initialize(appIdCaptor.capture(), any(), any())
    assertThat(appIdCaptor.firstValue).isAnyOf(TEST_APP_ID_1, TEST_APP_ID_2)
  }

  @Test
  fun loadBannerAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadBannerAd(
        createMediationBannerAdConfiguration(context = context),
        bannerAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
  }

  @Test
  fun loadBannerAd_loadsLiftoffBannerAd() {
    stubVungleInitializerToSucceed()
    val requestAdSize: AdSize = BANNER
    val vungleBannerAd =
      mock<VungleBannerView> {
        on { adConfig } doReturn vungleAdConfig
        on { getAdViewSize() } doReturn VungleAdSize.BANNER
      }
    whenever(vungleFactory.createBannerAd(any(), any(), any())) doReturn vungleBannerAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadBannerAd(
        createMediationBannerAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          adSize = requestAdSize,
        ),
        bannerAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleFactory).createBannerAd(context, TEST_PLACEMENT_ID, VungleAdSize.BANNER)
    verify(vungleBannerAd).load(null)
    val bannerAdCaptor = argumentCaptor<VungleBannerAd>()
    verify(vungleBannerAd).adListener = bannerAdCaptor.capture()
    val bannerAdView = bannerAdCaptor.firstValue.view as VungleBannerView
    assertThat(bannerAdView.getAdViewSize().width).isEqualTo(BANNER.getWidthInPixels(context))
    assertThat(bannerAdView.getAdViewSize().height).isEqualTo(BANNER.getHeightInPixels(context))
  }

  @Test
  fun loadBannerAd_forAdaptiveBanner_loadsLiftoffBannerAd() {
    stubVungleInitializerToSucceed()
    val requestAdaptiveAdSize: AdSize = AdSize.getInlineAdaptiveBannerAdSize(400, 240)
    val vungleAdSize =
      VungleAdSize.getValidAdSizeFromSize(
        requestAdaptiveAdSize.width,
        requestAdaptiveAdSize.height,
        TEST_PLACEMENT_ID,
      )
    val vungleBannerAd =
      mock<VungleBannerView> {
        on { adConfig } doReturn vungleAdConfig
        on { getAdViewSize() } doReturn vungleAdSize
      }
    whenever(vungleFactory.createBannerAd(any(), any(), any())) doReturn vungleBannerAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadBannerAd(
        createMediationBannerAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          adSize = requestAdaptiveAdSize,
        ),
        bannerAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleBannerAd).load(null)
    val bannerAdCaptor = argumentCaptor<VungleBannerAd>()
    verify(vungleBannerAd).adListener = bannerAdCaptor.capture()
    val bannerAdView = bannerAdCaptor.firstValue.view as VungleBannerView
    assertThat(bannerAdView.getAdViewSize().width)
      .isEqualTo(requestAdaptiveAdSize.getWidthInPixels(context))
    assertThat(bannerAdView.getAdViewSize().height)
      .isEqualTo(requestAdaptiveAdSize.getHeightInPixels(context))
  }

  @Test
  fun loadBannerAd_withoutAppId_callsLoadFailure() {
    adapter.loadBannerAd(
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
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
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withoutPlacementId_callsLoadFailure() {
    adapter.loadBannerAd(
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
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
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadBannerAd(
        createMediationBannerAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        ),
        bannerAdLoadCallback,
      )
    }

    assertThat(bannerAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  // region Waterfall Interstitial tests
  @Test
  fun loadWaterfallInterstitialAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadInterstitialAd(
        createMediationInterstitialAdConfiguration(context = context),
        interstitialAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
  }

  @Test
  fun loadWaterfallInterstitialAd_loadsLiftoffInterstitialAd() {
    stubVungleInitializerToSucceed()
    val vungleInterstitialAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleInterstitialAd
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadInterstitialAd(
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        interstitialAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleFactory).createInterstitialAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleInterstitialAd).adListener = any()
    verify(vungleInterstitialAd).load()
  }

  @Test
  fun loadWaterfallInterstitialAd_withoutAppId_callsLoadFailure() {
    adapter.loadInterstitialAd(
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      interstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load interstitial ad from Liftoff Monetize. Missing or invalid App ID.",
        ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadWaterfallInterstitialAd_withoutPlacementId_callsLoadFailure() {
    adapter.loadInterstitialAd(
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID_1),
        mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
      ),
      interstitialAdLoadCallback,
    )

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load interstitial ad from Liftoff Monetize. " +
          "Missing or Invalid Placement ID.",
        ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadWaterfallInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())

    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadInterstitialAd(
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        interstitialAdLoadCallback,
      )
    }

    assertThat(interstitialAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  // endregion

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
        rewardedAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        rewardedAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleAdConfig).adOrientation = LANDSCAPE
    verify(vungleFactory).createRewardedAd(context, TEST_PLACEMENT_ID, vungleAdConfig)
    verify(vungleRewardedAd).adListener = adapter
    verify(vungleRewardedAd).setUserId(TEST_USER_ID)
    verify(vungleRewardedAd).load()
  }

  @Test
  fun loadRewardedAd_withoutAppId_callsLoadFailure() {
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(rewardedAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadNativeAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadNativeAd(
        createMediationNativeAdConfiguration(context = context),
        nativeAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        nativeAdLoadCallback,
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

      adapter.loadNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
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

      adapter.loadNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
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

      adapter.loadNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
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

      adapter.loadNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
    }

    verify(vungleNativeAd).adOptionsPosition = TOP_RIGHT
  }

  @Test
  fun loadNativeAd_withoutAppId_callsLoadFailure() {
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
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadNativeAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadNativeAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(nativeAdLoadCallback).hasFailedWith(liftoffSdkInitError)
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

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
    verify(vungleRewardedAd).load()
  }

  @Test
  fun loadRewardedInterstitialAd_withoutAppId_callsLoadFailure() {
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedInterstitialAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(rewardedAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadAppOpenAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadAppOpenAd(
        createMediationAppOpenAdConfiguration(context = context),
        appOpenAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        appOpenAdLoadCallback,
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
    assertThat(appOpenAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadAppOpenAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(appOpenAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadAppOpenAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(appOpenAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadAppOpenAd_ifBackButtonImmediatelyIsSetToTrueInMediationExtras_setsItTrueInLiftoffAdConfig() {
    val vungleAppOpenAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleAppOpenAd
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_BACK_BUTTON_IMMEDIATELY_ENABLED to true),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(vungleAdConfig).setBackButtonImmediatelyEnabled(true)
  }

  @Test
  fun loadAppOpenAd_ifBackButtonImmediatelyIsSetToFalseInMediationExtras_setsItFalseInLiftoffAdConfig() {
    val vungleAppOpenAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleAppOpenAd
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_BACK_BUTTON_IMMEDIATELY_ENABLED to false),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(vungleAdConfig).setBackButtonImmediatelyEnabled(false)
  }

  @Test
  fun loadAppOpenAd_ifBackButtonImmediatelyIsNotSetInMediationExtras_doesNotSetItInLiftoffAdConfig() {
    val vungleAppOpenAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleAppOpenAd
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(vungleAdConfig, times(0)).setBackButtonImmediatelyEnabled(any())
  }

  @Test
  fun loadRtbRewardedAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedAd(
        createMediationRewardedAdConfiguration(context = context),
        rewardedAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        rewardedAdLoadCallback,
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbRewardedAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbRewardedAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(rewardedAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadRtbBannerAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbBannerAd(
        createMediationBannerAdConfiguration(context = context),
        bannerAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
  }

  @Test
  fun loadRtbBannerAd_loadsLiftoffBannerAdWithBidResponse() {
    stubVungleInitializerToSucceed()
    val requestAdSize: AdSize = BANNER
    val vungleBannerAd =
      mock<VungleBannerView> {
        on { adConfig } doReturn vungleAdConfig
        on { getAdViewSize() } doReturn VungleAdSize.BANNER
      }
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
          adSize = requestAdSize,
        ),
        bannerAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleFactory).createBannerAd(context, TEST_PLACEMENT_ID, VungleAdSize.BANNER)
    verify(vungleBannerAd).load(TEST_BID_RESPONSE)
    val bannerAdCaptor = argumentCaptor<VungleRtbBannerAd>()
    verify(vungleBannerAd).adListener = bannerAdCaptor.capture()
    val bannerAdView = bannerAdCaptor.firstValue.view as VungleBannerView
    assertThat(bannerAdView.getAdViewSize().width).isEqualTo(BANNER.getWidthInPixels(context))
    assertThat(bannerAdView.getAdViewSize().height).isEqualTo(BANNER.getHeightInPixels(context))
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
  }

  @Test
  fun loadRtbBannerAd_loadsLiftoffBannerAdWithBidResponse_adaptiveBanner() {
    stubVungleInitializerToSucceed()
    val requestAdaptiveAdSize: AdSize = AdSize.getInlineAdaptiveBannerAdSize(400, 240)
    val vungleAdSize =
      VungleAdSize.getValidAdSizeFromSize(
        requestAdaptiveAdSize.width,
        requestAdaptiveAdSize.height,
        TEST_PLACEMENT_ID,
      )
    val vungleBannerAd =
      mock<VungleBannerView> {
        on { adConfig } doReturn vungleAdConfig
        on { getAdViewSize() } doReturn vungleAdSize
      }
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
          adSize = requestAdaptiveAdSize,
        ),
        bannerAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).initialize(eq(TEST_APP_ID_1), eq(context), any())
    verify(vungleBannerAd).load(TEST_BID_RESPONSE)
    val bannerAdCaptor = argumentCaptor<VungleRtbBannerAd>()
    verify(vungleBannerAd).adListener = bannerAdCaptor.capture()
    val bannerAdView = bannerAdCaptor.firstValue.view as VungleBannerView
    assertThat(bannerAdView.getAdViewSize().width)
      .isEqualTo(requestAdaptiveAdSize.getWidthInPixels(context))
    assertThat(bannerAdView.getAdViewSize().height)
      .isEqualTo(requestAdaptiveAdSize.getHeightInPixels(context))
    verify(vungleAdConfig).setWatermark(TEST_WATERMARK)
  }

  @Test
  fun loadRtbBannerAd_withoutAppId_callsLoadFailure() {
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
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbBannerAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbBannerAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(bannerAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadRtbInterstitialAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbInterstitialAd(
        createMediationInterstitialAdConfiguration(context = context),
        interstitialAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        interstitialAdLoadCallback,
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
        "Failed to load interstitial ad from Liftoff Monetize. " + "Missing or invalid App ID.",
        ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbInterstitialAd_withoutPlacementId_callsLoadFailure() {
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
        "Failed to load interstitial ad from Liftoff Monetize. " +
          "Missing or Invalid Placement ID.",
        ERROR_DOMAIN,
      )
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(interstitialAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadRtbNativeAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbNativeAd(
        createMediationNativeAdConfiguration(context = context),
        nativeAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        nativeAdLoadCallback,
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

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
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

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
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

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
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

      adapter.loadRtbNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback)
    }

    verify(vungleNativeAd).adOptionsPosition = TOP_RIGHT
  }

  @Test
  fun loadRtbNativeAd_withoutAppId_callsLoadFailure() {
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
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbNativeAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbNativeAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(nativeAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadRtbRewardedInterstitialAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbRewardedInterstitialAd(
        createMediationRewardedAdConfiguration(context = context),
        rewardedAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        rewardedAdLoadCallback,
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbRewardedInterstitialAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbRewardedInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(rewardedAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadRtbAppOpenAd_updatesCoppaStatus() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbAppOpenAd(
        createMediationAppOpenAdConfiguration(context = context),
        appOpenAdLoadCallback,
      )
    }

    verify(mockVungleInitializer).updateCoppaAndUnderageConsentStatus(any())
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
        appOpenAdLoadCallback,
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
    assertThat(appOpenAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbAppOpenAd_withoutPlacementId_callsLoadFailure() {
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
    assertThat(appOpenAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRtbAppOpenAd_onLiftoffSdkInitializationError_callsLoadFailure() {
    val liftoffSdkInitError =
      AdError(
        SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.",
        VUNGLE_SDK_ERROR_DOMAIN,
      )
    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializationListener).onInitializeError(liftoffSdkInitError)
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
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

    assertThat(appOpenAdLoadCallback).hasFailedWith(liftoffSdkInitError)
  }

  @Test
  fun loadRtbAppOpenAd_ifBackButtonImmediatelyIsSetToTrueInMediationExtras_setsItTrueInLiftoffAdConfig() {
    val vungleAppOpenAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleAppOpenAd
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_BACK_BUTTON_IMMEDIATELY_ENABLED to true),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(vungleAdConfig).setBackButtonImmediatelyEnabled(true)
  }

  @Test
  fun loadRtbAppOpenAd_ifBackButtonImmediatelyIsSetToFalseInMediationExtras_setsItFalseInLiftoffAdConfig() {
    val vungleAppOpenAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleAppOpenAd
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(KEY_BACK_BUTTON_IMMEDIATELY_ENABLED to false),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(vungleAdConfig).setBackButtonImmediatelyEnabled(false)
  }

  @Test
  fun loadRtbAppOpenAd_ifBackButtonImmediatelyIsNotSetInMediationExtras_doesNotSetItInLiftoffAdConfig() {
    val vungleAppOpenAd = mock<InterstitialAd>()
    whenever(vungleFactory.createInterstitialAd(any(), any(), any())) doReturn vungleAppOpenAd
    stubVungleInitializerToSucceed()
    mockStatic(VungleInitializer::class.java).use {
      whenever(getInstance()) doReturn mockVungleInitializer

      adapter.loadRtbAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID_1, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          mediationExtras = bundleOf(),
        ),
        appOpenAdLoadCallback,
      )
    }

    verify(vungleAdConfig, times(0)).setBackButtonImmediatelyEnabled(any())
  }

  @Test
  fun collectSignals_onSuccessCalled() {
    val biddingToken = "token"
    whenever(mockSdkWrapper.getBiddingToken(any(), any())).doAnswer {
      val callback = it.arguments[1] as BidTokenCallback
      callback.onBidTokenCollected(biddingToken)
    }

    adapter.collectSignals(mockRtbSignalData, signalCallbacks)

    assertThat(signalCallbacks).hasSucceededWith(biddingToken)
  }

  @Test
  fun collectSignals_emptyBidToken_onFailureCalled() {
    val error =
      AdError(
        VungleMediationAdapter.ERROR_CANNOT_GET_BID_TOKEN,
        "Liftoff Monetize returned an empty bid token.",
        VungleMediationAdapter.ERROR_DOMAIN,
      )
    whenever(mockSdkWrapper.getBiddingToken(any(), any())).doAnswer {
      val callback = it.arguments[1] as BidTokenCallback
      callback.onBidTokenError("empty bid token")
    }

    adapter.collectSignals(mockRtbSignalData, signalCallbacks)

    assertThat(signalCallbacks).hasFailedWith(error)
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
