package com.google.ads.mediation.pangle

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGPAConsentType
import com.bytedance.sdk.openadsdk.api.init.PAGBidCallback
import com.bytedance.sdk.openadsdk.api.init.PAGBidError
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.pangle.PangleMediationAdapter.ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID
import com.google.ads.mediation.pangle.renderer.PangleAppOpenAd
import com.google.ads.mediation.pangle.renderer.PangleBannerAd
import com.google.ads.mediation.pangle.renderer.PangleInterstitialAd
import com.google.ads.mediation.pangle.renderer.PangleNativeAd
import com.google.ads.mediation.pangle.renderer.PangleRewardedAd
import com.google.ads.mediation.pangle.utils.TestConstants.APP_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_MESSAGE
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationFailure
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationSuccess
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
import com.google.common.truth.Truth.assertThat
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
    on { createPangleAppOpenAd(any(), any(), any()) } doReturn appOpenAd
    on { createPangleBannerAd(any(), any(), any()) } doReturn bannerAd
    on { createPangleInterstitialAd(any(), any(), any()) } doReturn interstitialAd
    on { createPangleNativeAd(any(), any(), any()) } doReturn nativeAd
    on { createPangleRewardedAd(any(), any(), any()) } doReturn rewardedAd
  }
  private val appOpenAdConfig: MediationAppOpenAdConfiguration = mock()
  private val appOpenAdLoadCallback =
    FakeMediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>()
  private val bannerAdConfig: MediationBannerAdConfiguration = mock()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
  private val interstitialAdConfig: MediationInterstitialAdConfiguration = mock()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>()
  private val nativeAdConfig: MediationNativeAdConfiguration = mock()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>()
  private val rewardedAdConfig: MediationRewardedAdConfiguration = mock()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()

  @Before
  fun setUp() {
    // Resetting the PA Consent Information to their default value.
    PangleMediationAdapter.setPAConsent(PAGPAConsentType.PAG_PA_CONSENT_TYPE_CONSENT)

    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    pangleMediationAdapter =
      PangleMediationAdapter(pangleInitializer, pangleSdkWrapper, pangleFactory)
  }

  @Test
  fun collectSignals_callsOnSuccessWithBiddingToken() {
    val signalCallbacks = FakeSignalCallbacks()
    val networkExtras = bundleOf(PangleExtras.Keys.USER_DATA to USER_DATA_VALUE)
    val biddingTokenCallbackCaptor = argumentCaptor<PAGBidCallback>()

    // When collectSignals is called
    pangleMediationAdapter.collectSignals(
      RtbSignalData(context, emptyList(), networkExtras, AdSize(1, 1)),
      signalCallbacks,
    )

    // Verify that user data is set on the Pangle SDK *before* getting the bidding token from the
    // Pangle SDK.
    inOrder(pangleSdkWrapper) {
      verify(pangleSdkWrapper).setUserData(USER_DATA_VALUE)
      verify(pangleSdkWrapper).getBiddingToken(any(), any(), biddingTokenCallbackCaptor.capture())
    }
    val biddingTokenCallback = biddingTokenCallbackCaptor.firstValue
    biddingTokenCallback.onBiddingTokenCollected(BIDDING_TOKEN)
    // Then signalCallbacks onSuccess is called with the PAGSdk biddingToken.
    assertThat(signalCallbacks).hasSucceededWith(BIDDING_TOKEN)
  }

  @Test
  fun collectSignals_ifBiddingTokenCallFails_callsOnFailure() {
    val signalCallbacks = FakeSignalCallbacks()
    val networkExtras = bundleOf(PangleExtras.Keys.USER_DATA to USER_DATA_VALUE)
    val biddingTokenCallbackCaptor = argumentCaptor<PAGBidCallback>()

    // When collectSignals is called
    pangleMediationAdapter.collectSignals(
      RtbSignalData(context, emptyList(), networkExtras, AdSize(1, 1)),
      signalCallbacks,
    )

    // Verify that user data is set on the Pangle SDK *before* getting the bidding token from the
    // Pangle SDK.
    inOrder(pangleSdkWrapper) {
      verify(pangleSdkWrapper).setUserData(USER_DATA_VALUE)
      verify(pangleSdkWrapper).getBiddingToken(any(), any(), biddingTokenCallbackCaptor.capture())
    }
    val biddingTokenCallback = biddingTokenCallbackCaptor.firstValue
    val pagBidError =
      PAGBidError(ERROR_CODE_BIDDING_TOKEN_FAILURE, "Bidding token collection failed")
    biddingTokenCallback.onBiddingTokenFailed(pagBidError)

    val expectedError =
      PangleConstants.createSdkError(
        ERROR_CODE_BIDDING_TOKEN_FAILURE,
        "Bidding token collection failed",
      )
    assertThat(signalCallbacks).hasFailedWith(expectedError)
  }

  @Test
  fun collectSignals_withAgeRestrictedTreatmentChild_callsOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val signalCallbacks = FakeSignalCallbacks()

    pangleMediationAdapter.collectSignals(
      RtbSignalData(context, emptyList(), bundleOf(), AdSize(1, 1)),
      signalCallbacks,
    )

    assertThat(signalCallbacks).hasFailedWith(PangleConstants.createChildUserError())
  }

  @Test
  fun initialize_withoutAppId_callsFailureCallback() {
    val fakeCallback = FakeInitializationCompleteCallback()
    val expectedAdErrorString =
      PangleConstants.createAdapterError(
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID,
        )
        .toString()

    pangleMediationAdapter.initialize(
      context,
      fakeCallback,
      listOf(MediationConfiguration(AdFormat.BANNER, bundleOf())),
    )

    assertThat(fakeCallback).hasFailedWith(expectedAdErrorString)
  }

  @Test
  fun initialize_callsInitializeOnPangleInitializer() {
    val fakeCallback = FakeInitializationCompleteCallback()

    pangleMediationAdapter.initialize(context, fakeCallback, listOf(buildProperMediationConfig()))

    verify(pangleInitializer).initialize(eq(context), eq(APP_ID_VALUE), any())
  }

  @Test
  fun initialize_ifPangleSdkInitializationSucceeds_callsSuccessCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    val fakeCallback = FakeInitializationCompleteCallback()

    pangleMediationAdapter.initialize(context, fakeCallback, listOf(buildProperMediationConfig()))

    assertThat(fakeCallback).hasSucceeded()
  }

  @Test
  fun initialize_ifPangleSdkInitializationFails_callsFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    val fakeCallback = FakeInitializationCompleteCallback()

    pangleMediationAdapter.initialize(context, fakeCallback, listOf(buildProperMediationConfig()))

    assertThat(fakeCallback).hasFailedWith(PANGLE_INIT_FAILURE_MESSAGE)
  }

  @Test
  fun initialize_withAgeRestrictedTreatmentChild_callsFailureCallback() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val fakeCallback = FakeInitializationCompleteCallback()

    pangleMediationAdapter.initialize(context, fakeCallback, listOf(buildProperMediationConfig()))

    assertThat(fakeCallback).hasFailedWith(PangleConstants.ERROR_MSG_CHILD_USER)
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
      .createPangleAppOpenAd(appOpenAdLoadCallback, pangleInitializer, pangleSdkWrapper)
    verify(appOpenAd).render(appOpenAdConfig)
  }

  @Test
  fun loadBannerAd_rendersBannerAd() {
    pangleMediationAdapter.loadBannerAd(bannerAdConfig, bannerAdLoadCallback)

    verify(pangleFactory)
      .createPangleBannerAd(bannerAdLoadCallback, pangleInitializer, pangleSdkWrapper)
    verify(bannerAd).render(bannerAdConfig)
  }

  @Test
  fun loadBannerAd_withAgeRestrictedTreatmentChild_callsOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    pangleMediationAdapter.loadBannerAd(bannerAdConfig, bannerAdLoadCallback)

    assertThat(bannerAdLoadCallback).hasFailedWith(PangleConstants.createChildUserError())
  }

  @Test
  fun loadInterstitialAd_rendersInterstitialAd() {
    pangleMediationAdapter.loadInterstitialAd(interstitialAdConfig, interstitialAdLoadCallback)

    verify(pangleFactory)
      .createPangleInterstitialAd(interstitialAdLoadCallback, pangleInitializer, pangleSdkWrapper)
    verify(interstitialAd).render(interstitialAdConfig)
  }

  @Test
  fun loadNativeAd_rendersNativeAd() {
    pangleMediationAdapter.loadNativeAd(nativeAdConfig, nativeAdLoadCallback)

    verify(pangleFactory)
      .createPangleNativeAd(nativeAdLoadCallback, pangleInitializer, pangleSdkWrapper)
    verify(nativeAd).render(nativeAdConfig)
  }

  @Test
  fun loadRewardedAd_rendersRewardedAd() {
    pangleMediationAdapter.loadRewardedAd(rewardedAdConfig, rewardedAdLoadCallback)

    verify(pangleFactory)
      .createPangleRewardedAd(rewardedAdLoadCallback, pangleInitializer, pangleSdkWrapper)
    verify(rewardedAd).render(rewardedAdConfig)
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
    // A fake error code for Pangle's bidding token collection failure.
    private const val ERROR_CODE_BIDDING_TOKEN_FAILURE = 1005
  }
}
