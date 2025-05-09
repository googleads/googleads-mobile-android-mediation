package com.google.ads.mediation.pangle

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGDoNotSellType
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGGDPRConsentType
import com.bytedance.sdk.openadsdk.api.init.BiddingTokenCallback
import com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.pangle.PangleMediationAdapter.ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID
import com.google.ads.mediation.pangle.renderer.PangleAppOpenAd
import com.google.ads.mediation.pangle.renderer.PangleBannerAd
import com.google.ads.mediation.pangle.renderer.PangleInterstitialAd
import com.google.ads.mediation.pangle.renderer.PangleNativeAd
import com.google.ads.mediation.pangle.renderer.PangleRewardedAd
import com.google.ads.mediation.pangle.utils.DoNotSellTypesProvider
import com.google.ads.mediation.pangle.utils.GDPRConsentTypesProvider
import com.google.ads.mediation.pangle.utils.GmaChildDirectedTagsProvider
import com.google.ads.mediation.pangle.utils.TestConstants.APP_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_MESSAGE
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationFailure
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationSuccess
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector

/** Unit Test class for [PangleMediationAdapter]. */
@RunWith(RobolectricTestParameterInjector::class)
class PangleMediationAdapterTest {

  // Test subject.
  private lateinit var pangleMediationAdapter: PangleMediationAdapter

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val pangleInitializer: PangleInitializer = mock()
  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val appOpenAd: PangleAppOpenAd = mock()
  private val bannerAd: PangleBannerAd = mock()
  private val interstitialAd: PangleInterstitialAd = mock()
  private val nativeAd: PangleNativeAd = mock()
  private val rewardedAd: PangleRewardedAd = mock()
  private val pangleFactory: PangleFactory = mock {
    on { createPangleAppOpenAd(any(), any(), any(), any(), any()) } doReturn appOpenAd
    on { createPangleBannerAd(any(), any(), any(), any(), any()) } doReturn bannerAd
    on { createPangleInterstitialAd(any(), any(), any(), any(), any()) } doReturn interstitialAd
    on { createPangleNativeAd(any(), any(), any(), any(), any()) } doReturn nativeAd
    on { createPangleRewardedAd(any(), any(), any(), any(), any()) } doReturn rewardedAd
  }
  private val panglePrivacyConfig: PanglePrivacyConfig = mock()
  private val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val appOpenAdConfig: MediationAppOpenAdConfiguration = mock()
  private val appOpenAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock()
  private val bannerAdConfig: MediationBannerAdConfiguration = mock()
  private val bannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val interstitialAdConfig: MediationInterstitialAdConfiguration = mock()
  private val interstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val nativeAdConfig: MediationNativeAdConfiguration = mock()
  private val nativeAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val rewardedAdConfig: MediationRewardedAdConfiguration = mock()
  private val rewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()

  @Before
  fun setUp() {
    // Resetting the GDPR and the DoNotSellInformation to their default value.
    PangleMediationAdapter.setGDPRConsent(PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_DEFAULT)
    PangleMediationAdapter.setDoNotSell(PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_DEFAULT)

    pangleMediationAdapter =
      PangleMediationAdapter(
        pangleInitializer,
        pangleSdkWrapper,
        pangleFactory,
        panglePrivacyConfig,
      )
  }

  @Test
  fun collectSignals_callsOnSuccessWithBiddingToken() {
    val signalCallbacks: SignalCallbacks = mock()
    val networkExtras = bundleOf(PangleExtras.Keys.USER_DATA to USER_DATA_VALUE)
    val biddingTokenCallbackCaptor = argumentCaptor<BiddingTokenCallback>()

    // When collectSignals is called
    pangleMediationAdapter.collectSignals(
      RtbSignalData(context, emptyList(), networkExtras, AdSize(1, 1)),
      signalCallbacks,
    )

    // Verify that user data is set on the Pangle SDK *before* getting the bidding token from the
    // Pangle SDK.
    inOrder(pangleSdkWrapper) {
      verify(pangleSdkWrapper).setUserData(USER_DATA_VALUE)
      verify(pangleSdkWrapper).getBiddingToken(biddingTokenCallbackCaptor.capture())
    }
    val biddingTokenCallback = biddingTokenCallbackCaptor.firstValue
    biddingTokenCallback.onBiddingTokenCollected(BIDDING_TOKEN)
    // Then signalCallbacks onSuccess is called with the PAGSdk biddingToken.
    verify(signalCallbacks).onSuccess(BIDDING_TOKEN)
  }

  @Test
  fun initialize_ifAppIdsAreMissing_callsFailureCallback() {
    // Create server parameters without app ID.
    val serverParameters = bundleOf()
    // Create a mediation config with the above server parameters
    val mediationConfig = MediationConfiguration(AdFormat.BANNER, serverParameters)

    pangleMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(mediationConfig),
    )

    val expectedAdErrorString =
      PangleConstants.createAdapterError(
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID,
        )
        .toString()
    verify(initializationCompleteCallback).onInitializationFailed(expectedAdErrorString)
  }

  @Test
  fun initialize_callsInitializeOnPangleInitializer() {
    pangleMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(buildProperMediationConfig()),
    )

    verify(pangleInitializer).initialize(eq(context), eq(APP_ID_VALUE), any())
  }

  @Test
  fun initialize_ifPangleSdkInitializationSucceeds_callsSuccessCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)

    pangleMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(buildProperMediationConfig()),
    )

    verify(initializationCompleteCallback).onInitializationSucceeded()
  }

  @Test
  fun initialize_ifPangleSdkInitializationFails_callsFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)

    pangleMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(buildProperMediationConfig()),
    )

    verify(initializationCompleteCallback).onInitializationFailed(PANGLE_INIT_FAILURE_MESSAGE)
  }

  @Test
  fun initialize_setsCorrectChildDirectedTag(
    @TestParameter(valuesProvider = GmaChildDirectedTagsProvider::class) childDirectedTag: Int
  ) {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setTagForChildDirectedTreatment(childDirectedTag).build()
    )

    pangleMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      listOf(buildProperMediationConfig()),
    )

    // pangleInitializer reads the coppa value from panglePrivacyConfig. So, we should ensure that
    // panglePrivacyConfig.setCoppa() is called before pangleInitializer.initialize().
    inOrder(panglePrivacyConfig, pangleInitializer) {
      verify(panglePrivacyConfig).setCoppa(childDirectedTag)
      verify(pangleInitializer).initialize(any(), any(), any())
    }
  }

  @Test
  fun getGDPRConsent_returnsTheUpdatedValueWhenCalled() {
    // Given the initial PangleMediationAdapter state
    // When the GDPRConsent is updated
    PangleMediationAdapter.setGDPRConsent(PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_NO_CONSENT)

    // Then getGDPRConsent() must return the updated value.
    assertThat(PangleMediationAdapter.getGDPRConsent())
      .isEqualTo(PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_NO_CONSENT)
  }

  @Test
  fun setGDPRConsent_ignoresValuesOutsideTheThreeAccepted() {
    // Given the initial PangleMdiationAdapter state
    // When the GDPRConsent is updated to a different range of values that are not allowed.
    PangleMediationAdapter.setGDPRConsent(-2)
    // Then the value is only updated when valid options are sent (-1, 0 or 1).
    assertThat(PangleMediationAdapter.getGDPRConsent())
      .isEqualTo(PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_DEFAULT)
    PangleMediationAdapter.setGDPRConsent(2)
    assertThat(PangleMediationAdapter.getGDPRConsent())
      .isEqualTo(PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_DEFAULT)
    PangleMediationAdapter.setGDPRConsent(-1)
    assertThat(PangleMediationAdapter.getGDPRConsent()).isEqualTo(-1)
    PangleMediationAdapter.setGDPRConsent(0)
    assertThat(PangleMediationAdapter.getGDPRConsent()).isEqualTo(0)
    PangleMediationAdapter.setGDPRConsent(1)
    assertThat(PangleMediationAdapter.getGDPRConsent()).isEqualTo(1)
  }

  @Test
  fun setGDPRConsent_ifPangleSDKIsInitialized_setsGDPRConsentOnPangleSdk(
    @TestParameter(valuesProvider = GDPRConsentTypesProvider::class) gdprConsent: Int
  ) {
    whenever(pangleSdkWrapper.isInitSuccess()).thenReturn(true)

    PangleMediationAdapter.setGDPRConsent(gdprConsent, pangleSdkWrapper)

    verify(pangleSdkWrapper).setGdprConsent(gdprConsent)
  }

  @Test
  fun getDoNotSell_returnTheUpdatedValueWhenCalled() {
    // Given the initial PangleMediationAdapter state
    // When the DoNotSell is updated
    PangleMediationAdapter.setDoNotSell(PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_NOT_SELL)

    // Then getDoNotsell() must return the updated value
    assertThat(PangleMediationAdapter.getDoNotSell())
      .isEqualTo(PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_NOT_SELL)
  }

  @Test
  fun setDoNotSell_ignoresValuesOutsideTheThreeAccepted() {
    // Given the initial PangleMediationAdapter state
    // When DoNotSell is updated to a different range od values that are not allowed.
    PangleMediationAdapter.setDoNotSell(-2)
    // Then the value is only updated when valid options are sent (-1, 0, 1).
    assertThat(PangleMediationAdapter.getDoNotSell())
      .isEqualTo(PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_DEFAULT)
    PangleMediationAdapter.setDoNotSell(2)
    assertThat(PangleMediationAdapter.getDoNotSell())
      .isEqualTo(PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_DEFAULT)
    PangleMediationAdapter.setDoNotSell(-1)
    assertThat(PangleMediationAdapter.getDoNotSell()).isEqualTo(-1)
    PangleMediationAdapter.setDoNotSell(0)
    assertThat(PangleMediationAdapter.getDoNotSell()).isEqualTo(0)
    PangleMediationAdapter.setDoNotSell(1)
    assertThat(PangleMediationAdapter.getDoNotSell()).isEqualTo(1)
  }

  @Test
  fun setDoNotSell_ifPangleSDKIsInitialized_setsDoNotSellOnPangleSdk(
    @TestParameter(valuesProvider = DoNotSellTypesProvider::class) doNotSellType: Int
  ) {
    whenever(pangleSdkWrapper.isInitSuccess()).thenReturn(true)

    PangleMediationAdapter.setDoNotSell(doNotSellType, pangleSdkWrapper)

    verify(pangleSdkWrapper).setDoNotSell(doNotSellType)
  }

  @Test
  fun getVersionInfo_ifAdapterVersionHasLessThanFourParts_returnsZeros() {
    // "3.1.4" is invalid because adapter version should contain at least four parts delimited by
    // ".".
    val adapterVersion = "3.1.4"

    val versionInfo = pangleMediationAdapter.getVersionInfo(adapterVersion)

    assertThat(versionInfo.getMajorVersion()).isEqualTo(0)
    assertThat(versionInfo.getMinorVersion()).isEqualTo(0)
    assertThat(versionInfo.getMicroVersion()).isEqualTo(0)
  }

  @Test
  fun getVersionInfo_ifAdapterVersionIsValid_returnsTheAdapterVersion() {
    val adapterVersion = "3.1.4.19.7"

    val versionInfo = pangleMediationAdapter.getVersionInfo(adapterVersion)

    assertThat(versionInfo.getMajorVersion()).isEqualTo(3)
    assertThat(versionInfo.getMinorVersion()).isEqualTo(1)
    assertThat(versionInfo.getMicroVersion()).isEqualTo(41907)
  }

  @Test
  fun getSDKVersionInfo_ifSdkVersionIsMissingMicroVersion_returnsZeros() {
    // "3.1" is an invalid SDK version because it is missing micro version.
    whenever(pangleSdkWrapper.sdkVersion) doReturn "3.1"

    val sdkVersionInfo = pangleMediationAdapter.sdkVersionInfo

    assertThat(sdkVersionInfo.getMajorVersion()).isEqualTo(0)
    assertThat(sdkVersionInfo.getMinorVersion()).isEqualTo(0)
    assertThat(sdkVersionInfo.getMicroVersion()).isEqualTo(0)
  }

  @Test
  fun getSDKVersionInfo_ifSdkVersionIsValid_returnsTheSdkVersion() {
    whenever(pangleSdkWrapper.sdkVersion) doReturn "3.1.4.19"

    val sdkVersionInfo = pangleMediationAdapter.sdkVersionInfo

    assertThat(sdkVersionInfo.getMajorVersion()).isEqualTo(3)
    assertThat(sdkVersionInfo.getMinorVersion()).isEqualTo(1)
    assertThat(sdkVersionInfo.getMicroVersion()).isEqualTo(419)
  }

  @Test
  fun loadAppOpenAd_rendersAppOpenAd() {
    pangleMediationAdapter.loadAppOpenAd(appOpenAdConfig, appOpenAdLoadCallback)

    verify(pangleFactory)
      .createPangleAppOpenAd(
        appOpenAdConfig,
        appOpenAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        panglePrivacyConfig,
      )
    verify(appOpenAd).render()
  }

  @Test
  fun loadBannerAd_rendersBannerAd() {
    pangleMediationAdapter.loadBannerAd(bannerAdConfig, bannerAdLoadCallback)

    verify(pangleFactory)
      .createPangleBannerAd(
        bannerAdConfig,
        bannerAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        panglePrivacyConfig,
      )
    verify(bannerAd).render()
  }

  @Test
  fun loadInterstitialAd_rendersInterstitialAd() {
    pangleMediationAdapter.loadInterstitialAd(interstitialAdConfig, interstitialAdLoadCallback)

    verify(pangleFactory)
      .createPangleInterstitialAd(
        interstitialAdConfig,
        interstitialAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        panglePrivacyConfig,
      )
    verify(interstitialAd).render()
  }

  @Test
  fun loadNativeAd_rendersNativeAd() {
    pangleMediationAdapter.loadNativeAd(nativeAdConfig, nativeAdLoadCallback)

    verify(pangleFactory)
      .createPangleNativeAd(
        nativeAdConfig,
        nativeAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        panglePrivacyConfig,
      )
    verify(nativeAd).render()
  }

  @Test
  fun loadRewardedAd_rendersRewardedAd() {
    pangleMediationAdapter.loadRewardedAd(rewardedAdConfig, rewardedAdLoadCallback)

    verify(pangleFactory)
      .createPangleRewardedAd(
        rewardedAdConfig,
        rewardedAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        panglePrivacyConfig,
      )
    verify(rewardedAd).render()
  }

  /**
   * Builds and returns a proper MediationConfiguration that can be used for initializing
   * PangleMediationAdapter.
   */
  private fun buildProperMediationConfig() =
    MediationConfiguration(AdFormat.BANNER, bundleOf(PangleConstants.APP_ID to APP_ID_VALUE))

  companion object {
    private const val USER_DATA_VALUE = "example_user_data"
    private const val BIDDING_TOKEN = "example_bidding_token"
  }
}
