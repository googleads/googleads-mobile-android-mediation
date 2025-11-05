package com.google.ads.mediation.applovin

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.adview.AppLovinInterstitialAdDialog
import com.applovin.mediation.AppLovinExtras.Keys.KEY_WATERMARK
import com.applovin.mediation.AppLovinUtils
import com.applovin.mediation.AppLovinUtils.ERROR_MSG_CHILD_USER
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinAdSize
import com.applovin.sdk.AppLovinSdk
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_AD_ALREADY_REQUESTED
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_CHILD_USER
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MISSING_AD_UNIT_ID
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MISSING_SDK_KEY
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MSG_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MSG_MISSING_SDK
import com.google.ads.mediation.applovin.AppLovinRewardedRenderer.ERROR_MSG_MULTIPLE_REWARDED_AD
import com.google.ads.mediation.applovin.AppLovinWaterfallInterstitialAd.ERROR_MSG_MULTIPLE_INTERSTITIAL_AD
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
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
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [AppLovinMediationAdapter] */
@RunWith(AndroidJUnit4::class)
class AppLovinMediationAdapterTest {

  // Subject of tests
  private lateinit var appLovinMediationAdapter: AppLovinMediationAdapter
  private var serverParameters: Bundle = Bundle()
  private var adSize: AdSize = AdSize.BANNER
  private lateinit var mediationBannerAdConfiguration: MediationBannerAdConfiguration
  private lateinit var mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration
  private lateinit var mediationRewardedAdConfiguration: MediationRewardedAdConfiguration
  private lateinit var appLovinInitializer: AppLovinInitializer

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val appOpenAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock()
  private val mediationBannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val mediationInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mediationRewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()
  private val appLovinAdService: AppLovinAdService = mock()
  private val appLovinSdk: AppLovinSdk = mock { on { getAdService() } doReturn appLovinAdService }
  private val appLovinSdkWrapper: AppLovinSdkWrapper = mock {
    on { getInstance(any()) } doReturn appLovinSdk
  }
  private val appLovinAdViewWrapper: AppLovinAdViewWrapper = mock()
  private val appLovinSdkUtilsWrapper: AppLovinSdkUtilsWrapper = mock()
  private val appLovinInterstitialAdDialog: AppLovinInterstitialAdDialog = mock()
  private val appLovinIncentivizedInterstitial: AppLovinIncentivizedInterstitial = mock()
  private val appLovinAppOpenAd: MaxAppOpenAd = mock()
  private val appLovinAdFactory: AppLovinAdFactory = mock {
    on { createAdView(any(), any(), any(), any()) } doReturn appLovinAdViewWrapper
    on { createInterstitialAdDialog(any(), any()) } doReturn appLovinInterstitialAdDialog
    on { createIncentivizedInterstitial(any(), any()) } doReturn appLovinIncentivizedInterstitial
    on { createIncentivizedInterstitial(any()) } doReturn appLovinIncentivizedInterstitial
    on { createMaxAppOpenAd(TEST_AD_UNIT_ID) } doReturn appLovinAppOpenAd
  }
  private val signalCallbacks: SignalCallbacks = mock()
  private val appOpenAdWaterfallConfig =
    MediationAppOpenAdConfiguration(
      context,
      /*bidResponse=*/ "",
      bundleOf(
        AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
        AppLovinUtils.ServerParameterKeys.AD_UNIT_ID to TEST_AD_UNIT_ID,
      ),
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      /*watermark=*/ "",
    )

  @Before
  fun setUp() {
    adSize = AdSize.BANNER
    serverParameters =
      bundleOf(
        AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY,
        AppLovinUtils.ServerParameterKeys.ZONE_ID to TEST_ZONE_ID,
      )

    appLovinInitializer = spy(AppLovinInitializer(appLovinSdkWrapper))
    appLovinMediationAdapter =
      AppLovinMediationAdapter(
        appLovinInitializer,
        appLovinAdFactory,
        appLovinSdkWrapper,
        appLovinSdkUtilsWrapper,
      )
  }

  @After
  fun tearDown() {
    // Reset child-directed and under-age tags.
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .build()
    )
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    )
    AppLovinWaterfallInterstitialAd.appLovinWaterfallInterstitialAds.clear()
    AppLovinWaterfallRewardedRenderer.incentivizedAdsMap.clear()
  }

  @Test
  fun initialize_ifUserIsTaggedAsChild_invokesOnFailure() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )

    appLovinMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      /*mediationConfigurations=*/ emptyList(),
    )

    verify(initializationCompleteCallback).onInitializationFailed(ERROR_MSG_CHILD_USER)
  }

  @Test
  fun initialize_ifUserIsTaggedAsUnderAge_invokesOnFailure() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )

    appLovinMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      /*mediationConfigurations=*/ emptyList(),
    )

    verify(initializationCompleteCallback).onInitializationFailed(ERROR_MSG_CHILD_USER)
  }

  @Test
  fun initialize_withoutSdkKeys_invokesOnFailure() {

    appLovinMediationAdapter.initialize(
      context,
      initializationCompleteCallback,
      /*mediationConfigurations=*/ emptyList(),
    )

    verify(initializationCompleteCallback).onInitializationFailed(ERROR_MSG_MISSING_SDK)
    verify(appLovinInitializer, never()).initialize(any(), any(), any())
  }

  @Test
  fun initialize_invokesInitializerOnlyOnce() {
    doReturn(appLovinSdk).whenever(appLovinSdkWrapper).getInstance(any())
    val serverParameters1 = bundleOf(AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY)
    val serverParameters2 = bundleOf(AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY_2)
    val configuration1 = MediationConfiguration(AdFormat.BANNER, serverParameters1)
    val configuration2 = MediationConfiguration(AdFormat.INTERSTITIAL, serverParameters2)
    val configurationsList = listOf(configuration1, configuration2)

    appLovinMediationAdapter.initialize(context, initializationCompleteCallback, configurationsList)

    verify(appLovinInitializer, times(1)).initialize(any(), any(), any())
  }

  @Test
  fun initialize_initializesSuccessfully_invokesOnInitializationSucceededWhenFinished() {
    doReturn(appLovinSdk).whenever(appLovinSdkWrapper).getInstance(any())
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    val serverParameters = bundleOf(AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY)
    val configuration = MediationConfiguration(AdFormat.BANNER, serverParameters)
    val configurationList = listOf(configuration)

    appLovinMediationAdapter.initialize(context, initializationCompleteCallback, configurationList)

    verify(initializationCompleteCallback).onInitializationSucceeded()
  }

  @Test
  fun getVersion_withLessThanFourNumbers_returnsZeroVersionInfo() {
    // An invalid Adapter Version is made out of less than 4 numbers separated by dots.
    val adapterVersion = "3.2.1"

    val versionInfo = appLovinMediationAdapter.getVersionInfo(adapterVersion)

    assertThat(versionInfo.majorVersion).isEqualTo(0)
    assertThat(versionInfo.minorVersion).isEqualTo(0)
    assertThat(versionInfo.microVersion).isEqualTo(0)
  }

  @Test
  fun getVersionInfo_withValidVersion_returnsProperFromattedVersionInfo() {
    // A valid Adapter Version is made out of at least 4 numbers separated by dots.
    val adapterVersion = "4.3.2.1"

    val versionInfo = appLovinMediationAdapter.getVersionInfo(adapterVersion)

    assertThat(versionInfo.majorVersion).isEqualTo(4)
    assertThat(versionInfo.minorVersion).isEqualTo(3)
    assertThat(versionInfo.microVersion).isEqualTo(201)
  }

  @Test
  fun getSDKVersionInfo_withLessThanThreeNumbers_returnsZeroVersionInfo() {
    val sdkVersion = "2.1"
    whenever(appLovinSdkWrapper.sdkVersion) doReturn sdkVersion

    val sdkVersionInfo = appLovinMediationAdapter.getSDKVersionInfo()

    assertThat(sdkVersionInfo.majorVersion).isEqualTo(0)
    assertThat(sdkVersionInfo.minorVersion).isEqualTo(0)
    assertThat(sdkVersionInfo.microVersion).isEqualTo(0)
  }

  @Test
  fun getSDKVersionInfo_withValidVersion_returnsProperFromattedVersionInfo() {
    // A valid Sdk Version is made out of at least 3 numbers separated by dots.
    val sdkVersion = "3.2.1"
    whenever(appLovinSdkWrapper.sdkVersion) doReturn sdkVersion

    val sdkVersionInfo = appLovinMediationAdapter.getSDKVersionInfo()

    assertThat(sdkVersionInfo.majorVersion).isEqualTo(3)
    assertThat(sdkVersionInfo.minorVersion).isEqualTo(2)
    assertThat(sdkVersionInfo.microVersion).isEqualTo(1)
  }

  @Test
  fun getSdkSettings_fromAppLovinMediationAdapter_initializesSdkSettingsAndReturnsThatInstance() {
    val sdkSettings = AppLovinMediationAdapter.getSdkSettings(context)

    assertThat(sdkSettings).isNotNull()
  }

  @Test
  fun collectSignals_ifUserIsTaggedAsChild_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )

    appLovinMediationAdapter.collectSignals(mock<RtbSignalData>(), signalCallbacks)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(signalCallbacks).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun collectSignals_ifUserIsTaggedAsUnderAge_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )

    appLovinMediationAdapter.collectSignals(mock<RtbSignalData>(), signalCallbacks)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(signalCallbacks).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  // region loadAppOpenAd tests

  @Test
  fun loadAppOpenAd_ifUserIsTaggedAsChild_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )

    appLovinMediationAdapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(appOpenAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadAppOpenAd_ifUserIsTaggedAsUnderAge_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )

    appLovinMediationAdapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(appOpenAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadAppOpenAd_withoutSdkKey_failsWithCallback() {
    val appOpenAdConfigWithoutSdkKey =
      MediationAppOpenAdConfiguration(
        context,
        /*bidResponse=*/ "",
        bundleOf(AppLovinUtils.ServerParameterKeys.AD_UNIT_ID to TEST_AD_UNIT_ID),
        /*mediationExtras=*/ Bundle(),
        /*isTesting=*/ true,
        /*location=*/ null,
        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        /*watermark=*/ "",
      )

    appLovinMediationAdapter.loadAppOpenAd(appOpenAdConfigWithoutSdkKey, appOpenAdLoadCallback)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(appOpenAdLoadCallback).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_MISSING_SDK_KEY)
    assertThat(errorCaptured.message).isEqualTo(ERROR_MSG_MISSING_SDK)
    assertThat(errorCaptured.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadAppOpenAd_ifAdUnitIsMissing_failsWithCallback() {
    val appOpenAdConfigWithoutAdUnitId =
      MediationAppOpenAdConfiguration(
        context,
        /*bidResponse=*/ "",
        bundleOf(AppLovinUtils.ServerParameterKeys.SDK_KEY to TEST_SDK_KEY),
        /*mediationExtras=*/ Bundle(),
        /*isTesting=*/ true,
        /*location=*/ null,
        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        /*watermark=*/ "",
      )

    appLovinMediationAdapter.loadAppOpenAd(appOpenAdConfigWithoutAdUnitId, appOpenAdLoadCallback)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(appOpenAdLoadCallback).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_MISSING_AD_UNIT_ID)
    assertThat(errorCaptured.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadAppOpenAd_withCorrectParameters_invokesSdkInitialization() {
    appLovinMediationAdapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

    verify(appLovinSdk).initialize(any(), any())
  }

  @Test
  fun loadAppOpenAd_withCorrectParametersAndInitSuccess_loadsAppLovinAd() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())

    appLovinMediationAdapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

    verify(appLovinAppOpenAd).loadAd()
  }

  // endregion

  @Test
  fun loadBannerAd_ifUserIsTaggedAsChild_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    mediationBannerAdConfiguration = initializeBannerAd()

    appLovinMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_ifUserIsTaggedAsUnderAge_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    mediationBannerAdConfiguration = initializeBannerAd()

    appLovinMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withoutSdkKey_failsWithCallback() {
    serverParameters.remove(AppLovinUtils.ServerParameterKeys.SDK_KEY)
    mediationBannerAdConfiguration = initializeBannerAd()

    appLovinMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_MISSING_SDK_KEY)
    assertThat(errorCaptured.message).isEqualTo(ERROR_MSG_MISSING_SDK)
    assertThat(errorCaptured.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withUnsupportedAdSize_invokesOnFailure() {
    adSize = AdSize.WIDE_SKYSCRAPER
    mediationBannerAdConfiguration = initializeBannerAd()

    appLovinMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_BANNER_SIZE_MISMATCH)
    assertThat(errorCaptured.message).isEqualTo(ERROR_MSG_BANNER_SIZE_MISMATCH)
    assertThat(errorCaptured.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withCorrectArguments_invokesSdkInitializationMethods() {
    mediationBannerAdConfiguration = initializeBannerAd()

    appLovinMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    verify(appLovinSdk).initialize(any(), any())
  }

  @Test
  fun loadBannerAd_withCorrectParametersAndInitSuccess_invokesLoadNextAdForZoneId() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    mediationBannerAdConfiguration = initializeBannerAd()

    appLovinMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    verify(appLovinAdService).loadNextAdForZoneId(eq(TEST_ZONE_ID), any())
  }

  @Test
  fun loadBannerAd_withNullZoneIdAndInitSuccess_invokesLoadNextAd() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    serverParameters.remove(AppLovinUtils.ServerParameterKeys.ZONE_ID)
    mediationBannerAdConfiguration = initializeBannerAd()

    appLovinMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    // Since adSize is set as BANNER it is expector for AppLovinAdSize to be BANNER too.
    verify(appLovinAdService).loadNextAd(eq(AppLovinAdSize.BANNER), any())
  }

  private fun initializeBannerAd() =
    MediationBannerAdConfiguration(
      context,
      /*bidResponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      adSize,
      TEST_WATERMARK,
    )

  @Test
  fun loadInterstitialAd_ifUserIsTaggedAsChild_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadInterstitialAd_ifUserIsTaggedAsUnderAge_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadInterstitialAd_withoutSdkKey_failsWithCallback() {
    serverParameters.remove(AppLovinUtils.ServerParameterKeys.SDK_KEY)
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_MISSING_SDK_KEY)
    assertThat(errorCaptured.message).isEqualTo(ERROR_MSG_MISSING_SDK)
    assertThat(errorCaptured.domain).isEqualTo(APPLOVIN_SDK_ERROR_DOMAIN)
    verify(appLovinSdk, never()).initialize(any(), any())
  }

  @Test
  fun loadInterstitialAd_withCorrectArguments_invokesSdkInitializationMethods() {
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(appLovinSdk).initialize(any(), any())
  }

  @Test
  fun loadInterstitialAd_multipleTimesWithSameZoneId_invokesOnFailure() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    mediationInterstitialAdConfiguration = initializeInterstitialAd()
    val adErrorCaptor = argumentCaptor<AdError>()

    appLovinMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )
    appLovinMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(mediationInterstitialAdLoadCallback, times(1)).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_AD_ALREADY_REQUESTED)
    assertThat(errorCaptured.message).isEqualTo(ERROR_MSG_MULTIPLE_INTERSTITIAL_AD)
    assertThat(errorCaptured.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRtbInterstitialAd_ifUserIsTaggedAsChild_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRtbInterstitialAd_ifUserIsTaggedAsUnderAge_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRtbInterstitialAd_invokesLoadNextAdForAdToken() {
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(appLovinAdService).loadNextAdForAdToken(eq(TEST_BIDRESPONSE), any())
  }

  @Test
  fun loadRtbInterstitialAd_initializedInterstitialAd() {
    mediationInterstitialAdConfiguration = initializeInterstitialAd()

    appLovinMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(appLovinInterstitialAdDialog).setAdDisplayListener(any())
    verify(appLovinInterstitialAdDialog).setAdClickListener(any())
    verify(appLovinInterstitialAdDialog).setAdVideoPlaybackListener(any())
    verify(appLovinInterstitialAdDialog).setExtraInfo(eq(KEY_WATERMARK), eq(TEST_WATERMARK))
  }

  private fun initializeInterstitialAd() =
    MediationInterstitialAdConfiguration(
      context,
      TEST_BIDRESPONSE,
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  @Test
  fun loadRewardedAd_ifUserIsTaggedAsChild_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRewardedAd_ifUserIsTaggedAsUnderAge_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRewardedAd_withoutSdkKey_failsWithCallback() {
    serverParameters.remove(AppLovinUtils.ServerParameterKeys.SDK_KEY)
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_MISSING_SDK_KEY)
    assertThat(errorCaptured.message).isEqualTo(ERROR_MSG_MISSING_SDK)
    assertThat(errorCaptured.domain).isEqualTo(APPLOVIN_SDK_ERROR_DOMAIN)
    verify(appLovinSdk, never()).initialize(any(), any())
  }

  @Test
  fun loadRewardedAd_withCorrectArguments_invokesSdkInitializationMethods() {
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(appLovinSdk).initialize(any(), any())
  }

  @Test
  fun loadRewardedAd_whenInitilizedMultipleTimesWithSameZoneId_invokesOnFailure() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    mediationRewardedAdConfiguration = initializeRewardedAd()
    val adErrorCaptor = argumentCaptor<AdError>()

    appLovinMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )
    appLovinMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(mediationRewardedAdLoadCallback, times(1)).onFailure(adErrorCaptor.capture())
    val errorCaptured = adErrorCaptor.firstValue
    assertThat(errorCaptured.code).isEqualTo(ERROR_AD_ALREADY_REQUESTED)
    assertThat(errorCaptured.message).isEqualTo(ERROR_MSG_MULTIPLE_REWARDED_AD)
    assertThat(errorCaptured.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRtbRewardedAd_ifUserIsTaggedAsChild_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRtbRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRtbRewardedAd_ifUserIsTaggedAsUnderAge_failsWithCallback() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRtbRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CHILD_USER)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadRtbRewardedAd_invokesLoadNextAdForAdToken() {
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRtbRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(appLovinAdService).loadNextAdForAdToken(eq(TEST_BIDRESPONSE), any())
  }

  @Test
  fun loadRtbRewardedAd_setExtraInfoForIncentivizedInterstitial() {
    mediationRewardedAdConfiguration = initializeRewardedAd()

    appLovinMediationAdapter.loadRtbRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(appLovinIncentivizedInterstitial).setExtraInfo(eq(KEY_WATERMARK), eq(TEST_WATERMARK))
  }

  private fun initializeRewardedAd() =
    MediationRewardedAdConfiguration(
      context,
      TEST_BIDRESPONSE,
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  companion object {
    private const val TEST_SDK_KEY = "sdkKey"
    private const val TEST_SDK_KEY_2 = "sdkKey2"
    private const val TEST_ZONE_ID = "zoneId"
    private const val TEST_WATERMARK = "watermark"
    private const val TEST_BIDRESPONSE = "bidResponse"
    private const val TEST_AD_UNIT_ID = "fake_ad_unit_id"
  }
}
