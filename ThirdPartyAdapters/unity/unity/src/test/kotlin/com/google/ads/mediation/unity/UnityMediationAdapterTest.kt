package com.google.ads.mediation.unity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode
import com.google.ads.mediation.unity.UnityInitializer.ADMOB
import com.google.ads.mediation.unity.UnityInitializer.KEY_ADAPTER_VERSION
import com.google.ads.mediation.unity.UnityInterstitialAd.ERROR_MSG_INTERSTITIAL_INITIALIZATION_FAILED
import com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_INITIALIZATION_FAILURE
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_MISSING_PARAMETERS
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_NON_ACTIVITY
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.ads.mediation.unity.UnityMediationBannerAd.ERROR_MSG_INITIALIZATION_FAILED_FOR_GAME_ID
import com.google.ads.mediation.unity.UnityMediationBannerAd.ERROR_MSG_NO_MATCHING_AD_SIZE
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
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
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.metadata.MediationMetaData
import com.unity3d.services.banners.UnityBannerSize
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.notNull
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Unit tests for [UnityMediationAdapter] */
@RunWith(AndroidJUnit4::class)
class UnityMediationAdapterTest {

  // Subject of tests
  private var unityMediationAdapter = UnityMediationAdapter()
  private var mediationConfigurations: List<MediationConfiguration> = emptyList()
  private var serverParameters: Bundle = Bundle()
  private var adSize: AdSize = AdSize.BANNER
  private lateinit var mediationBannerAdConfiguration: MediationBannerAdConfiguration
  private lateinit var mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration
  private lateinit var mediationRewardedAdConfiguration: MediationRewardedAdConfiguration

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val nonActivityContext: Context = ApplicationProvider.getApplicationContext()

  private val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val mediationBannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val mediationInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mediationRewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()
  private val unityAdsWrapper: UnityAdsWrapper = mock()
  private val unityInitializer: UnityInitializer = spy(UnityInitializer(unityAdsWrapper))
  private val unityAdsLoader: UnityAdsLoader = mock()
  private val mediationMetadata: MediationMetaData = mock()
  private val unityBannerViewWrapper: UnityBannerViewWrapper = mock()
  private val unityBannerViewFactory: UnityBannerViewFactory = mock()
  private val signalCallbacks: SignalCallbacks = mock()

  @Before
  fun setUp() {
    unityMediationAdapter =
      UnityMediationAdapter(unityInitializer, unityBannerViewFactory, unityAdsLoader)
    mediationConfigurations = emptyList()

    adSize = AdSize.BANNER
    serverParameters =
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )

    whenever(unityBannerViewFactory.createBannerView(any(), eq(TEST_PLACEMENT_ID), any())) doReturn
      unityBannerViewWrapper
    whenever(unityAdsWrapper.getMediationMetaData(any())) doReturn mediationMetadata
  }

  @Test
  fun initialize_withEmptyParameters_callsOnInitializationFailed() {
    unityMediationAdapter.initialize(
      activity,
      initializationCompleteCallback,
      mediationConfigurations,
    )

    val adError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Game ID.", ADAPTER_ERROR_DOMAIN)
    verify(initializationCompleteCallback).onInitializationFailed(adError.toString())
  }

  @Test
  fun initialize_withCorrectParametersAndSuccess_invokesOnInitializationSucceeded() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    mediationConfigurations = listOf(MediationConfiguration(AdFormat.BANNER, serverParameters))

    unityMediationAdapter.initialize(
      activity,
      initializationCompleteCallback,
      mediationConfigurations,
    )

    verify(initializationCompleteCallback).onInitializationSucceeded()
  }

  @Test
  fun initialize_withCorrectParametersAndFailure_invokesOnInitializationFailed() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationFailed(
          UnityAdsInitializationError.INTERNAL_ERROR,
          TEST_ERROR_MESSAGE,
        )
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    mediationConfigurations = listOf(MediationConfiguration(AdFormat.BANNER, serverParameters))
    val errorCode = getMediationErrorCode(UnityAdsInitializationError.INTERNAL_ERROR)
    val adError: AdError =
      AdError(
        errorCode,
        ERROR_MSG_INITIALIZATION_FAILURE.format(
          UnityAdsInitializationError.INTERNAL_ERROR,
          TEST_ERROR_MESSAGE,
        ),
        SDK_ERROR_DOMAIN,
      )

    unityMediationAdapter.initialize(
      activity,
      initializationCompleteCallback,
      mediationConfigurations,
    )

    verify(initializationCompleteCallback).onInitializationFailed(eq(adError.toString()))
  }

  @Test
  fun initialize_withCorrectParameters_commitsUnityMediationMetaDataBeforeInitialization() {
    doReturn(TEST_VERSION_NUMBER).whenever(unityAdsWrapper).getVersion()
    mediationConfigurations = listOf(MediationConfiguration(AdFormat.BANNER, serverParameters))

    unityMediationAdapter.initialize(
      activity,
      initializationCompleteCallback,
      mediationConfigurations,
    )

    inOrder(mediationMetadata) {
      verify(mediationMetadata).setName(ADMOB)
      verify(mediationMetadata).commit()
    }
    inOrder(mediationMetadata) {
      verify(mediationMetadata).setVersion(TEST_VERSION_NUMBER)
      verify(mediationMetadata).commit()
    }
    inOrder(mediationMetadata) {
      verify(mediationMetadata).set(KEY_ADAPTER_VERSION, BuildConfig.ADAPTER_VERSION)
      verify(mediationMetadata).commit()
    }
    inOrder(mediationMetadata, unityAdsWrapper) {
      verify(mediationMetadata).commit()
      verify(unityAdsWrapper).initialize(any(), any(), any())
    }
  }

  @Test
  fun collectSignals_forBannerFormatAndNonActivityContext_fails() {
    val rtbSignalData =
      RtbSignalData(
        nonActivityContext,
        listOf(MediationConfiguration(AdFormat.BANNER, /* serverParameters= */ bundleOf())),
        /* networkExtras= */ bundleOf(),
        AdSize.BANNER,
      )

    unityMediationAdapter.collectSignals(rtbSignalData, signalCallbacks)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(signalCallbacks).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_CONTEXT_NOT_ACTIVITY)
    assertThat(adError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
    verifyNoMoreInteractions(signalCallbacks)
  }

  @Test
  fun initialize_afterUnityAdsInitialized_invokesOnInitializationSucceeded() {
    doReturn(true).whenever(unityAdsWrapper).isInitialized()
    mediationConfigurations = listOf(MediationConfiguration(AdFormat.BANNER, serverParameters))

    unityMediationAdapter.initialize(
      activity,
      initializationCompleteCallback,
      mediationConfigurations,
    )

    verify(initializationCompleteCallback).onInitializationSucceeded()
    verify(unityAdsWrapper, never()).initialize(any(), any(), any())
  }

  @Test
  fun loadBannerAd_withEmptyServerParameters_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_GAME_ID)
    serverParameters.remove(UnityMediationAdapter.KEY_PLACEMENT_ID)
    mediationBannerAdConfiguration = initializeBannerAd()

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withInvalidGameId_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_GAME_ID)
    mediationBannerAdConfiguration = initializeBannerAd()

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withInvalidPlacementId_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_PLACEMENT_ID)
    mediationBannerAdConfiguration = initializeBannerAd()

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withIncompatibleSize_failsWithAdError() {
    adSize = AdSize.MEDIUM_RECTANGLE
    mediationBannerAdConfiguration = initializeBannerAd()

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_BANNER_SIZE_MISMATCH)
    assertThat(capturedError.message).isEqualTo("${ERROR_MSG_NO_MATCHING_AD_SIZE}${adSize}")
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withNonActivityContext_failsWithAdError() {
    mediationBannerAdConfiguration = initializeBannerAd(ApplicationProvider.getApplicationContext())

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_CONTEXT_NOT_ACTIVITY)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_NON_ACTIVITY)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withCorrectParameters_callsInitializeUnityAds() {
    mediationBannerAdConfiguration = initializeBannerAd()

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    verify(unityInitializer).initializeUnityAds(any(), any(), any())
  }

  @Test
  fun loadBannerAd_withCorrectParametersAndInitFailure_callsOnLoadFailed() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationFailed(
          UnityAdsInitializationError.INTERNAL_ERROR,
          TEST_ERROR_MESSAGE,
        )
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    mediationBannerAdConfiguration = initializeBannerAd()
    val errorCaptor = argumentCaptor<AdError>()
    val errorCode = getMediationErrorCode(UnityAdsInitializationError.INTERNAL_ERROR)

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    verify(mediationBannerAdLoadCallback).onFailure(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message)
      .isEqualTo(
        ERROR_MSG_INITIALIZATION_FAILED_FOR_GAME_ID.format(TEST_GAME_ID, TEST_ERROR_MESSAGE)
      )
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_withCorrectParametersAndInitSuccess_invokesBannerViewLoad() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    mediationBannerAdConfiguration = initializeBannerAd()
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions

    unityMediationAdapter.loadBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    verify(unityAdsLoader).createUnityAdsLoadOptionsWithId(any())
    inOrder(unityBannerViewWrapper) {
      verify(unityBannerViewWrapper).setListener(any())
      verify(unityBannerViewWrapper).load(any())
    }
  }

  @Test
  fun loadRtbBannerAd_withCorrectParametersAndInitSuccess_invokesBannerViewLoadWithBidResponse() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    mediationBannerAdConfiguration = initializeBannerAd(activity, "testBidResponse")
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions

    unityMediationAdapter.loadRtbBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    verify(unityAdsLoader).createUnityAdsLoadOptionsWithId(any())
    verify(unityAdsLoadOptions).set(UnityMediationAdapter.KEY_WATERMARK, "watermark")
    verify(unityAdsLoadOptions).setAdMarkup("testBidResponse")
    inOrder(unityBannerViewWrapper) {
      verify(unityBannerViewWrapper).setListener(any())
      verify(unityBannerViewWrapper).load(any())
    }
  }

  @Test
  fun loadRtbBannerAd_evenForNonStandardUnityBannerSize_loads() {
    // Medium rectangle is a "non-standard" size for Unity Ads SDK.
    adSize = AdSize.MEDIUM_RECTANGLE
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    mediationBannerAdConfiguration = initializeBannerAd(activity, "testBidResponse")
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn mock()

    unityMediationAdapter.loadRtbBannerAd(
      mediationBannerAdConfiguration,
      mediationBannerAdLoadCallback,
    )

    val unityBannerSizeCaptor = argumentCaptor<UnityBannerSize>()
    verify(unityBannerViewFactory)
      .createBannerView(eq(activity), eq(TEST_PLACEMENT_ID), unityBannerSizeCaptor.capture())
    val unityBannerSize = unityBannerSizeCaptor.firstValue
    assertThat(unityBannerSize.width).isEqualTo(AdSize.MEDIUM_RECTANGLE.width)
    assertThat(unityBannerSize.height).isEqualTo(AdSize.MEDIUM_RECTANGLE.height)
    verify(unityBannerViewWrapper).load(any())
  }

  private fun initializeBannerAd(context: Context, bidResponse: String) =
    MediationBannerAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      adSize,
      /*watermark=*/ "watermark",
    )

  private fun initializeBannerAd(context: Context) = initializeBannerAd(context, bidResponse = "")

  private fun initializeBannerAd() = initializeBannerAd(activity)

  @Test
  fun loadInterstitialAd_withEmptyServerParameters_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_GAME_ID)
    serverParameters.remove(UnityMediationAdapter.KEY_PLACEMENT_ID)
    initializeInterstitialAd()

    unityMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadInterstitialAd_withInvalidGameId_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_GAME_ID)
    initializeInterstitialAd()

    unityMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadInterstitialAd_withInvalidPlacementId_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_PLACEMENT_ID)
    initializeInterstitialAd()

    unityMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadInterstitialAd_withCorrectParameters_invokesInitializeUnityAds() {
    initializeInterstitialAd()

    unityMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(unityInitializer).initializeUnityAds(any(), any(), any())
  }

  @Test
  fun loadInterstitialAd_evenWithNonActivityContext_invokesInitializeUnityAds() {
    initializeInterstitialAd(ApplicationProvider.getApplicationContext())

    unityMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(unityInitializer).initializeUnityAds(any(), any(), any())
  }

  @Test
  fun loadInterstitialAd_withCorrectParametersAndInitFailure_callsOnLoadFailed() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationFailed(
          UnityAdsInitializationError.INTERNAL_ERROR,
          TEST_ERROR_MESSAGE,
        )
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    initializeInterstitialAd()
    val adErrorCaptor = argumentCaptor<AdError>()
    val errorCode = getMediationErrorCode(UnityAdsInitializationError.INTERNAL_ERROR)

    unityMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(mediationInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message)
      .isEqualTo(
        ERROR_MSG_INTERSTITIAL_INITIALIZATION_FAILED.format(TEST_GAME_ID, TEST_ERROR_MESSAGE)
      )
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun loadInterstitialAd_withCorrectParametersAndInitSuccess_invokesUnityAdsLoad() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    initializeInterstitialAd()

    unityMediationAdapter.loadInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(unityAdsLoader).createUnityAdsLoadOptionsWithId(notNull())
    verify(unityAdsLoadOptions).setAdMarkup(eq(""))
    verify(unityAdsLoader).load(any(), any(), any())
  }

  @Test
  fun loadRtbInterstitialAd_withCorrectParametersAndInitSuccess_invokesUnityAdsLoad() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    initializeInterstitialAd(activity, "testBidToken")

    unityMediationAdapter.loadRtbInterstitialAd(
      mediationInterstitialAdConfiguration,
      mediationInterstitialAdLoadCallback,
    )

    verify(unityAdsLoader).createUnityAdsLoadOptionsWithId(notNull())
    verify(unityAdsLoadOptions).setAdMarkup("testBidToken")
    verify(unityAdsLoader).load(any(), any(), any())
  }

  private fun initializeInterstitialAd(context: Context, bidToken: String) {
    mediationInterstitialAdConfiguration =
      MediationInterstitialAdConfiguration(
        context,
        bidToken,
        serverParameters,
        /*mediationExtras=*/ Bundle(),
        /*isTesting=*/ true,
        /*location=*/ null,
        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        /*watermark=*/ "watermark",
      )
  }

  private fun initializeInterstitialAd(context: Context) {
    initializeInterstitialAd(context, bidToken = "")
  }

  private fun initializeInterstitialAd() {
    initializeInterstitialAd(activity)
  }

  @Test
  fun loadRewardedAd_withEmptyServerParameters_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_GAME_ID)
    serverParameters.remove(UnityMediationAdapter.KEY_PLACEMENT_ID)
    initializeRewardedAd()

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadRewardedAd_withInvalidGameId_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_GAME_ID)
    initializeRewardedAd()

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadRewardedAd_withInvalidPlacementId_failsWithAdError() {
    serverParameters.remove(UnityMediationAdapter.KEY_PLACEMENT_ID)
    initializeRewardedAd()

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_MISSING_PARAMETERS)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun loadRewardedAd_withCorrectParameters_invokesInitializeUnityAds() {
    initializeRewardedAd()

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(unityInitializer).initializeUnityAds(any(), any(), any())
  }

  @Test
  fun loadRewardedAd_evenWithNonActivityContext_invokesInitializeUnityAds() {
    initializeRewardedAd(ApplicationProvider.getApplicationContext())

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(unityInitializer).initializeUnityAds(any(), any(), any())
  }

  @Test
  fun loadRewardedAd_withCorrectParametersAndInitFailure_callsOnLoadFailed() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationFailed(
          UnityAdsInitializationError.INTERNAL_ERROR,
          TEST_ERROR_MESSAGE,
        )
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    initializeRewardedAd()
    val adErrorCaptor = argumentCaptor<AdError>()
    val errorCode = getMediationErrorCode(UnityAdsInitializationError.INTERNAL_ERROR)

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(mediationRewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message)
      .isEqualTo(
        ERROR_MSG_INTERSTITIAL_INITIALIZATION_FAILED.format(TEST_GAME_ID, TEST_ERROR_MESSAGE)
      )
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun loadRewardedAd_withCorrectParametersAndInitSuccess_invokesUnityAdsLoad() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    initializeRewardedAd()

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(unityAdsLoader).createUnityAdsLoadOptionsWithId(notNull())
    verify(unityAdsLoadOptions).setAdMarkup(eq(""))
    verify(unityAdsLoader).load(any(), any(), any())
  }

  @Test
  fun loadRtbRewardedAd_withCorrectParametersAndInitSuccess_invokesUnityAdsLoad() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    initializeRewardedAd(activity, "testBidToken")

    unityMediationAdapter.loadRewardedAd(
      mediationRewardedAdConfiguration,
      mediationRewardedAdLoadCallback,
    )

    verify(unityAdsLoader).createUnityAdsLoadOptionsWithId(notNull())
    verify(unityAdsLoadOptions).setAdMarkup("testBidToken")
    verify(unityAdsLoader).load(any(), any(), any())
  }

  private fun initializeRewardedAd(context: Context, bidToken: String) {
    mediationRewardedAdConfiguration =
      MediationRewardedAdConfiguration(
        context,
        bidToken,
        serverParameters,
        /*mediationExtras=*/ Bundle(),
        /*isTesting=*/ true,
        /*location=*/ null,
        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        /*watermark=*/ "watermark",
      )
  }

  private fun initializeRewardedAd(context: Context) {
    initializeRewardedAd(context, bidToken = "")
  }

  private fun initializeRewardedAd() {
    initializeRewardedAd(activity)
  }

  companion object {
    const val TEST_ERROR_MESSAGE = "Test Error Message"
    const val TEST_VERSION_NUMBER = "100"
    const val TEST_GAME_ID = "gameId"
    const val TEST_PLACEMENT_ID = "placementId"
  }
}
