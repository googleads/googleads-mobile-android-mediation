package com.google.ads.mediation.facebook

import android.content.Context
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.AdExperienceType
import com.facebook.ads.AdSettings
import com.facebook.ads.AdView
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.BidderTokenProvider
import com.facebook.ads.BidderTokenProvider.getBidderToken
import com.facebook.ads.ExtraHints
import com.facebook.ads.InterstitialAd
import com.facebook.ads.NativeAdBase
import com.facebook.ads.NativeAdBase.fromBidPayload
import com.facebook.ads.NativeAdListener
import com.facebook.ads.RewardedVideoAd
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.adaptertestkit.loadRtbAppOpenAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbBannerAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbInterstitialAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbNativeAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbRewardedAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbRewardedInterstitialAdWithFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyNoFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.facebook.FacebookAdapterUtils.adapterVersion
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FACEBOOK_INITIALIZATION
import com.google.ads.mediation.facebook.FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER
import com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience
import com.google.ads.mediation.facebook.FacebookSdkWrapper.sdkVersion
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class FacebookMediationAdapterTest {
  private var facebookMediationAdapter: FacebookMediationAdapter = FacebookMediationAdapter()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rtbSignalData = mock<RtbSignalData>() { on { context } doReturn context }
  private val signalCallbacks = mock<SignalCallbacks>()
  private val metaFactory = mock<MetaFactory>()
  private val mockInitializationCompleteCallback: InitializationCompleteCallback = mock()
  private val appOpenAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock()
  // Meta SDK uses InterstitialAd for displaying app open ads.
  private val metaAppOpenAdLoadConfig: InterstitialAd.InterstitialLoadAdConfig = mock()
  private val metaAppOpenAdLoadConfigBuilder: InterstitialAd.InterstitialAdLoadConfigBuilder =
    mock {
      on { withBid(any()) } doReturn this.mock
      on { withAdListener(any()) } doReturn this.mock
      on { build() } doReturn metaAppOpenAdLoadConfig
    }
  private val metaAppOpenAd: InterstitialAd = mock {
    on { buildLoadAdConfig() } doReturn metaAppOpenAdLoadConfigBuilder
  }
  private val mockInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mockBannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val metaBannerAdLoadConfig: AdView.AdViewLoadConfig = mock()
  private val metaBannerAdLoadConfigBuilder: AdView.AdViewLoadConfigBuilder = mock {
    on { withBid(any()) } doReturn this.mock
    on { withAdListener(any()) } doReturn this.mock
    on { build() } doReturn metaBannerAdLoadConfig
  }
  private val metaBannerAd: AdView = mock {
    on { buildLoadAdConfig() } doReturn metaBannerAdLoadConfigBuilder
  }
  private val metaInterstitialAdLoadConfig: InterstitialAd.InterstitialLoadAdConfig = mock()
  private val metaInterstitialAdLoadConfigBuilder: InterstitialAd.InterstitialAdLoadConfigBuilder =
    mock {
      on { withBid(any()) } doReturn this.mock
      on { withAdListener(any()) } doReturn this.mock
      on { build() } doReturn metaInterstitialAdLoadConfig
    }
  private val metaInterstitialAd: InterstitialAd = mock {
    on { buildLoadAdConfig() } doReturn metaInterstitialAdLoadConfigBuilder
  }
  val mediationAdConfiguration: MediationAdConfiguration = mock()
  private val mockRewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()
  private val metaRewardedAdLoadConfig: RewardedVideoAd.RewardedVideoLoadAdConfig = mock()
  private val metaRewardedAdLoadConfigBuilder: RewardedVideoAd.RewardedVideoAdLoadConfigBuilder =
    mock {
      on { withBid(any()) } doReturn this.mock
      on { withAdListener(any()) } doReturn this.mock
      on { withAdExperience(any()) } doReturn this.mock
      on { build() } doReturn metaRewardedAdLoadConfig
    }
  private val metaRewardedAd: RewardedVideoAd = mock {
    on { buildLoadAdConfig() } doReturn metaRewardedAdLoadConfigBuilder
  }
  private val mockNativeAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val metaNativeAdLoadConfig: NativeAdBase.NativeLoadAdConfig = mock()
  private val metaNativeAdLoadConfigBuilder: NativeAdBase.NativeAdLoadConfigBuilder = mock {
    on { withBid(any()) } doReturn this.mock
    on { withAdListener(any()) } doReturn this.mock
    on { withMediaCacheFlag(any()) } doReturn this.mock
    on {
      withPreloadedIconView(
        NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
        NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
      )
    } doReturn this.mock
    on { build() } doReturn metaNativeAdLoadConfig
  }
  private val metaNativeAd: NativeAdBase = mock {
    on { buildLoadAdConfig() } doReturn metaNativeAdLoadConfigBuilder
  }

  @Before
  fun setUp() {
    facebookMediationAdapter = FacebookMediationAdapter(metaFactory)
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
    whenever(mediationAdConfiguration.taggedForUnderAgeTreatment()) doReturn
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED

    setMixedAudience(mediationAdConfiguration)

    assertThat(AdSettings.isMixedAudience()).isTrue()
  }

  @Test
  fun setMixedAudience_whenTfuaTrue_setsMixedAudienceTrue() {
    whenever(mediationAdConfiguration.taggedForChildDirectedTreatment()) doReturn
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
    whenever(mediationAdConfiguration.taggedForUnderAgeTreatment()) doReturn
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE

    setMixedAudience(mediationAdConfiguration)

    assertThat(AdSettings.isMixedAudience()).isTrue()
  }

  @Test
  fun setMixedAudience_whenTfcdFalse_setsMixedAudienceFalse() {
    whenever(mediationAdConfiguration.taggedForChildDirectedTreatment()) doReturn
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
    whenever(mediationAdConfiguration.taggedForUnderAgeTreatment()) doReturn
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED

    setMixedAudience(mediationAdConfiguration)

    assertThat(AdSettings.isMixedAudience()).isFalse()
  }

  @Test
  fun setMixedAudience_whenTfuaFalse_setsMixedAudienceFalse() {
    whenever(mediationAdConfiguration.taggedForChildDirectedTreatment()) doReturn
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
    whenever(mediationAdConfiguration.taggedForUnderAgeTreatment()) doReturn
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE

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
        bundleOf(RTB_PLACEMENT_PARAMETER to TEST_APP_ID),
      )

      verify(mockInitSettingsBuilder).initialize()
    }
  }

  @Test
  fun initialize_whenInitializerSucceeds_invokesOnInitializationSucceeded() {
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
        bundleOf(RTB_PLACEMENT_PARAMETER to TEST_PLACEMENT_ID),
      )
    }
  }

  @Test
  fun initialize_whenInitializerFails_invokesOnInitializationFailed() {
    val facebookInitializer: FacebookInitializer = mock()
    val initializerError =
      AdError(ERROR_FACEBOOK_INITIALIZATION, "Meta SDK initialization failed.", ERROR_DOMAIN)
    whenever(facebookInitializer.initialize(any(), any<ArrayList<String>>(), any())) doAnswer
      { invocation ->
        val arguments = invocation.arguments
        (arguments[2] as FacebookInitializer.Listener).onInitializeError(initializerError)
      }
    mockStatic(FacebookInitializer::class.java).use {
      whenever(FacebookInitializer.getInstance()) doReturn facebookInitializer

      facebookMediationAdapter.mediationAdapterInitializeVerifyFailure(
        context,
        mockInitializationCompleteCallback,
        bundleOf(RTB_PLACEMENT_PARAMETER to TEST_PLACEMENT_ID),
        expectedError = initializerError.message,
      )
    }
  }

  @Test
  fun initialize_withEmptyMediationConfigurations_succeeds() {
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
        bundleOf(),
      )
    }
  }

  @Test
  fun initialize_withEmptyPlacementId_succeeds() {
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
        bundleOf(RTB_PLACEMENT_PARAMETER to ""),
      )
    }
  }

  // endregion

  // region App Open ad load tests.

  @Test
  fun loadRtbAppOpenAd_withoutPlacementId_invokesOnFailure() {
    val mediationAppOpenAdConfiguration = createMediationAppOpenAdConfiguration(context = context)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty. ",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      appOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbAppOpenAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(RTB_PLACEMENT_PARAMETER to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty. ",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      appOpenAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbAppOpenAd_loadsAd() {
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        watermark = WATERMARK,
      )
    whenever(
      metaFactory.createAppOpenAd(context, AdapterTestKitConstants.TEST_PLACEMENT_ID)
    ) doReturn metaAppOpenAd

    facebookMediationAdapter.loadRtbAppOpenAd(
      mediationAppOpenAdConfiguration,
      appOpenAdLoadCallback,
    )

    val extraHintsCaptor = argumentCaptor<ExtraHints>()
    verify(metaAppOpenAd).setExtraHints(extraHintsCaptor.capture())
    assertThat(extraHintsCaptor.firstValue.mediationData).isEqualTo(WATERMARK)
    verify(metaAppOpenAd).loadAd(metaAppOpenAdLoadConfig)
  }

  // endregion

  // region Banner Ad load tests
  @Test
  fun loadRtbBannerAd_withoutPlacementId_invokesOnFailureCallback() {
    val mediationBannerAdConfiguration = createMediationBannerAdConfiguration(context = context)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbBannerAdWithFailure(
      mediationBannerAdConfiguration,
      mockBannerAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbBannerAd_withEmptyPlacementId_invokesOnFailureCallback() {
    val serverParameters = bundleOf(RTB_PLACEMENT_PARAMETER to "")
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbBannerAdWithFailure(
      mediationBannerAdConfiguration,
      mockBannerAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbBannerAd_adViewCreationException_invokesOnFailureCallback() {
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(context = context, serverParameters = serverParameters)
    val exception = Exception("foo error")
    whenever(
      metaFactory.createMetaAdView(
        context,
        AdapterTestKitConstants.TEST_PLACEMENT_ID,
        mediationBannerAdConfiguration.bidResponse,
      )
    ) doThrow exception
    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_ADVIEW_CONSTRUCTOR_EXCEPTION,
        "Failed to create banner ad: " + exception.message,
        ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbBannerAdWithFailure(
      mediationBannerAdConfiguration,
      mockBannerAdLoadCallback,
      expectedAdError,
    )
  }

  @Test
  fun loadRtbBannerAd_loadsAd() {
    val WATERMARK = "meta"
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        watermark = WATERMARK,
      )
    whenever(
      metaFactory.createMetaAdView(
        context,
        AdapterTestKitConstants.TEST_PLACEMENT_ID,
        mediationBannerAdConfiguration.bidResponse,
      )
    ) doReturn metaBannerAd

    facebookMediationAdapter.loadRtbBannerAd(
      mediationBannerAdConfiguration,
      mockBannerAdLoadCallback,
    )

    val extraHintsCaptor = argumentCaptor<ExtraHints>()
    verify(metaBannerAd).setExtraHints(extraHintsCaptor.capture())
    extraHintsCaptor.firstValue.mediationData.equals(WATERMARK)
    val frameLayoutParamsCaptor = argumentCaptor<FrameLayout.LayoutParams>()
    verify(metaBannerAd, times(2)).setLayoutParams(frameLayoutParamsCaptor.capture())
    frameLayoutParamsCaptor.firstValue.apply {
      assertThat(width).isEqualTo(AdSize.BANNER.getWidthInPixels(context))
    }
    verify(metaBannerAd).loadAd(metaBannerAdLoadConfig)
  }

  // endregion

  // region Interstitial Ad load tests
  @Test
  fun loadRtbInterstitialAd_withoutPlacementId_invokesOnFailure() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(context = context)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty. ",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbInterstitialAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(RTB_PLACEMENT_PARAMETER to "")
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty. ",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbInterstitialAdWithFailure(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbInterstitialAd_loadsAd() {
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        watermark = WATERMARK,
      )
    whenever(
      metaFactory.createInterstitialAd(context, AdapterTestKitConstants.TEST_PLACEMENT_ID)
    ) doReturn metaInterstitialAd

    facebookMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mockInterstitialAdLoadCallback,
    )

    val extraHintsCaptor = argumentCaptor<ExtraHints>()
    verify(metaInterstitialAd).setExtraHints(extraHintsCaptor.capture())
    assertThat(extraHintsCaptor.firstValue.mediationData).isEqualTo(WATERMARK)
    verify(metaInterstitialAd).loadAd(metaInterstitialAdLoadConfig)
  }

  // endregion

  // region Rewarded Ad load tests

  @Test
  fun loadRtbRewardedAd_withoutPlacementId_invokesOnFailureCallback() {
    val mediationRewardedAdConfiguration = createMediationRewardedAdConfiguration(context = context)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbRewardedAdWithFailure(
      mediationRewardedAdConfiguration,
      mockRewardedAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbRewardedAd_emptyPlacementId_invokesOnFailureCallback() {
    val serverParameters = bundleOf(RTB_PLACEMENT_PARAMETER to "")
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbRewardedAdWithFailure(
      mediationRewardedAdConfiguration,
      mockRewardedAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbRewardedAd_loadsAd() {
    AdSettings.setMixedAudience(false)
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        taggedForChildDirectedTreatment = 1,
        watermark = WATERMARK,
        bidResponse = AdapterTestKitConstants.TEST_BID_RESPONSE,
      )
    whenever(
      metaFactory.createRewardedAd(context, AdapterTestKitConstants.TEST_PLACEMENT_ID)
    ) doReturn metaRewardedAd

    facebookMediationAdapter.loadRtbRewardedAd(
      mediationRewardedAdConfiguration,
      mockRewardedAdLoadCallback,
    )

    val extraHintsCaptor = argumentCaptor<ExtraHints>()
    verify(metaRewardedAd).setExtraHints(extraHintsCaptor.capture())
    extraHintsCaptor.firstValue.mediationData.equals(WATERMARK)
    assertThat(AdSettings.isMixedAudience()).isTrue()
    verify(metaRewardedAdLoadConfigBuilder).apply {
      withAdListener(any(FacebookRewardedAd::class.java))
      withBid(mediationRewardedAdConfiguration.bidResponse)
      withAdExperience(AdExperienceType.AD_EXPERIENCE_TYPE_REWARDED)
    }
    verify(metaRewardedAd).loadAd(metaRewardedAdLoadConfig)
  }

  // endregion

  // region Native Ad load tests

  @Test
  fun loadRtbNativeAd_withoutPlacementId_invokesOnFailureCallback() {
    val mediationNativeAdConfiguration = createMediationNativeAdConfiguration(context = context)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbNativeAdWithFailure(
      mediationNativeAdConfiguration,
      mockNativeAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbNativeAd_emptyPlacementId_invokesOnFailureCallback() {
    val serverParameters = bundleOf(RTB_PLACEMENT_PARAMETER to "")
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbNativeAdWithFailure(
      mediationNativeAdConfiguration,
      mockNativeAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbNativeAd_errorCreatingNativeAdBase_invokesOnFailureCallback() {
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
    val exception = Exception("error foo")
    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_CREATE_NATIVE_AD_FROM_BID_PAYLOAD,
        "Failed to create native ad from bid payload: " + exception.message,
        ERROR_DOMAIN,
      )
    mockStatic(NativeAdBase::class.java).use {
      whenever(fromBidPayload(any(), any(), any())) doThrow exception

      facebookMediationAdapter.loadRtbNativeAdWithFailure(
        mediationNativeAdConfiguration,
        mockNativeAdLoadCallback,
        expectedAdError,
      )
    }
  }

  @Test
  fun loadRtbNativeAd_loadsAd() {
    AdSettings.setMixedAudience(false)
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        taggedForChildDirectedTreatment = 1,
        watermark = WATERMARK,
        bidResponse = AdapterTestKitConstants.TEST_BID_RESPONSE,
      )
    mockStatic(NativeAdBase::class.java).use {
      whenever(fromBidPayload(any(), any(), any())) doReturn metaNativeAd

      facebookMediationAdapter.loadRtbNativeAd(
        mediationNativeAdConfiguration,
        mockNativeAdLoadCallback,
      )

      val extraHintsCaptor = argumentCaptor<ExtraHints>()
      verify(metaNativeAd).setExtraHints(extraHintsCaptor.capture())
      extraHintsCaptor.firstValue.mediationData.equals(WATERMARK)
      assertThat(AdSettings.isMixedAudience()).isTrue()
      verify(metaNativeAdLoadConfigBuilder).apply {
        withAdListener(any(NativeAdListener::class.java))
        withBid(mediationAdConfiguration.bidResponse)
        withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
        withPreloadedIconView(
          NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
          NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
        )
      }
      verify(metaNativeAd).loadAd(metaNativeAdLoadConfig)
    }
  }

  // endregion

  // region Rewarded Interstitial Ad load tests

  @Test
  fun loadRtbRewardedInterstitialAd_withoutPlacementId_invokesOnFailureCallback() {
    val rewardedInterstitialAdConfiguration =
      createMediationRewardedAdConfiguration(context = context)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbRewardedInterstitialAdWithFailure(
      rewardedInterstitialAdConfiguration,
      mockRewardedAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbRewardedInterstitialAd_emptyPlacementId_invokesOnFailureCallback() {
    val serverParameters = bundleOf(RTB_PLACEMENT_PARAMETER to "")
    val rewardedInterstitialAdConfiguration =
      createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to request ad. PlacementID is null or empty.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    facebookMediationAdapter.loadRtbRewardedInterstitialAdWithFailure(
      rewardedInterstitialAdConfiguration,
      mockRewardedAdLoadCallback,
      expectedError,
    )
  }

  @Test
  fun loadRtbRewardedInterstitialAd_loadsAd() {
    AdSettings.setMixedAudience(false)
    val serverParameters =
      bundleOf(RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID)
    val rewardedInterstitialAdConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        taggedForChildDirectedTreatment = 1,
        watermark = WATERMARK,
        bidResponse = AdapterTestKitConstants.TEST_BID_RESPONSE,
      )
    whenever(
      metaFactory.createRewardedAd(context, AdapterTestKitConstants.TEST_PLACEMENT_ID)
    ) doReturn metaRewardedAd

    facebookMediationAdapter.loadRtbRewardedInterstitialAd(
      rewardedInterstitialAdConfiguration,
      mockRewardedAdLoadCallback,
    )

    val extraHintsCaptor = argumentCaptor<ExtraHints>()
    verify(metaRewardedAd).setExtraHints(extraHintsCaptor.capture())
    extraHintsCaptor.firstValue.mediationData.equals(WATERMARK)
    assertThat(AdSettings.isMixedAudience()).isTrue()
    verify(metaRewardedAdLoadConfigBuilder)
      .withAdListener(any(FacebookRewardedInterstitialAd::class.java))
    verify(metaRewardedAdLoadConfigBuilder).withBid(AdapterTestKitConstants.TEST_BID_RESPONSE)
    verify(metaRewardedAdLoadConfigBuilder)
      .withAdExperience(AdExperienceType.AD_EXPERIENCE_TYPE_REWARDED_INTERSTITIAL)
    verify(metaRewardedAd).loadAd(metaRewardedAdLoadConfig)
  }

  // endregion

  companion object {
    private const val WATERMARK = "meta"
  }
}
