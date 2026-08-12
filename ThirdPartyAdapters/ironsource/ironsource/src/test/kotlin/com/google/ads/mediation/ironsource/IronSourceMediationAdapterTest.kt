package com.google.ads.mediation.ironsource

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeSignalCallbacks
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.ironsource.IronSourceAdapterUtils.getAdapterVersion
import com.google.ads.mediation.ironsource.IronSourceConstants.KEY_APP_KEY
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_SDK_NOT_INITIALIZED
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSource.createBannerForDemandOnly
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerLayout
import com.unity3d.ironsourceads.InitListener
import com.unity3d.ironsourceads.IronSourceAds
import com.unity3d.ironsourceads.IronSourceAds.getSdkVersion
import com.unity3d.mediation.LevelPlay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [IronSourceMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class IronSourceMediationAdapterTest {

  private lateinit var adapter: IronSourceMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val initializationCompleteCallback = FakeInitializationCompleteCallback()
  private val rtbSignalData = mock<RtbSignalData> { on { context } doReturn context }
  private val signalCallbacks = FakeSignalCallbacks()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    adapter = IronSourceMediationAdapter(mediationUtils)
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor3Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAds::class.java).use {
      whenever(getSdkVersion()) doReturn "8.3.2"

      adapter.assertGetSdkVersion(expectedValue = "8.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor4Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAds::class.java).use {
      whenever(getSdkVersion()) doReturn "7.3.2.1"

      adapter.assertGetSdkVersion(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZeros() {
    mockStatic(IronSourceAds::class.java).use {
      whenever(getSdkVersion()) doReturn "3.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_validVersionWith4Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2.1"

      adapter.assertGetVersionInfo(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getVersionInfo_validVersionWith5Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2.1.8"

      adapter.assertGetVersionInfo(expectedValue = "7.3.20108")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZeros() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun initialize_withNoAppKeyInServerParameters_invokesOnInitializationFailed() {
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER)

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    assertThat(initializationCompleteCallback).hasFailedWith(MISSING_OR_INVALID_APP_KEY_MESSAGE)
  }

  @Test
  fun initialize_withEmptyAppKey_invokesOnInitializationFailed() {
    val mediationConfiguration =
      createMediationConfiguration(AdFormat.BANNER, serverParameters = bundleOf(KEY_APP_KEY to ""))

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    assertThat(initializationCompleteCallback).hasFailedWith(MISSING_OR_INVALID_APP_KEY_MESSAGE)
  }

  @Test
  fun initialize_withTFCDTrue_setsLevelPlayMetaDataToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val mockStaticLevelPlay = mockStatic(LevelPlay::class.java)
    mockStatic(IronSourceAds::class.java).use { _ ->
      val mediationConfiguration =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockStaticLevelPlay.verify { LevelPlay.setMetaData("is_child_directed", "true") }
    }
    mockStaticLevelPlay.close()
  }

  @Test
  fun initialize_withTFUATrue_setsLevelPlayMetaDataToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val mockStaticLevelPlay = mockStatic(LevelPlay::class.java)
    mockStatic(IronSourceAds::class.java).use { _ ->
      val mediationConfiguration =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockStaticLevelPlay.verify { LevelPlay.setMetaData("is_child_directed", "true") }
    }
    mockStaticLevelPlay.close()
  }

  @Test
  fun initialize_withTFCDFalse_setsLevelPlayMetaDataToFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val mockStaticLevelPlay = mockStatic(LevelPlay::class.java)
    mockStatic(IronSourceAds::class.java).use { _ ->
      val mediationConfiguration =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockStaticLevelPlay.verify { LevelPlay.setMetaData("is_child_directed", "false") }
    }
    mockStaticLevelPlay.close()
  }

  @Test
  fun initialize_withTFUAFalse_setsLevelPlayMetaDataToFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val mockStaticLevelPlay = mockStatic(LevelPlay::class.java)
    mockStatic(IronSourceAds::class.java).use { _ ->
      val mediationConfiguration =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockStaticLevelPlay.verify { LevelPlay.setMetaData("is_child_directed", "false") }
    }
    mockStaticLevelPlay.close()
  }

  @Test
  fun initialize_withAgeRestrictedTreatmentChild_setsLevelPlayMetaDataToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD).build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val mockStaticLevelPlay = mockStatic(LevelPlay::class.java)
    mockStatic(IronSourceAds::class.java).use { _ ->
      val mediationConfiguration =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockStaticLevelPlay.verify { LevelPlay.setMetaData("is_child_directed", "true") }
    }
    mockStaticLevelPlay.close()
  }

  @Test
  fun initialize_withAgeRestrictedTreatmentTeen_doesNotChangeLevelPlayMetaData() {
    val requestConfiguration =
      RequestConfiguration.Builder().setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN).build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val mockStaticLevelPlay = mockStatic(LevelPlay::class.java)
    mockStatic(IronSourceAds::class.java).use { _ ->
      val mediationConfiguration =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      mockStaticLevelPlay.verify(
        { LevelPlay.setMetaData(eq("is_child_directed"), any<String>()) },
        never(),
      )
    }
    mockStaticLevelPlay.close()
  }

  @Test
  fun initialize_withMediationConfigurations_invokesOnInitializationSucceeded() {
    val mockStaticLevelPlay = mockStatic(LevelPlay::class.java)
    mockStatic(IronSourceAds::class.java).use {
      whenever(IronSourceAds.init(any(), any(), any())).thenAnswer { invocation ->
        val listener = invocation.getArgument<InitListener>(2)
        listener.onInitSuccess()
        null
      }
      val mediationConfiguration =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      assertThat(initializationCompleteCallback).hasSucceeded()
      mockStaticLevelPlay.verify(
        { LevelPlay.setMetaData(eq("is_child_directed"), any<String>()) },
        never(),
      )
    }
    mockStaticLevelPlay.close()
  }

  @Test
  fun initialize_withMultipleMediationConfigurations_invokesOnInitializationSucceededOnlyOnce() {
    mockStatic(IronSourceAds::class.java).use { mockedStatic ->
      val expectedAdFormats =
        listOf(
          IronSourceAds.AdFormat.BANNER,
          IronSourceAds.AdFormat.INTERSTITIAL,
          IronSourceAds.AdFormat.REWARDED,
        )
      val mediationConfiguration1 =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )
      val mediationConfiguration2 =
        createMediationConfiguration(
          AdFormat.INTERSTITIAL,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_2),
        )
      whenever(IronSourceAds.init(any(), any(), any())).thenAnswer { invocation ->
        val listener = invocation.getArgument<InitListener>(2)
        listener.onInitSuccess()
        null
      }

      adapter.initialize(
        context,
        initializationCompleteCallback,
        listOf(mediationConfiguration1, mediationConfiguration2),
      )

      assertThat(initializationCompleteCallback).hasSucceeded()
      mockedStatic.verify {
        IronSourceAds.init(
          eq(context),
          argThat { initRequest ->
            val appKeyMatches = initRequest.appKey == TEST_APP_ID_2
            val adFormatsMatch =
              initRequest.legacyAdFormats.containsAll(expectedAdFormats) &&
                initRequest.legacyAdFormats.size == expectedAdFormats.size

            appKeyMatches && adFormatsMatch
          },
          any(),
        )
      }
    }
  }

  @Test
  fun initialize_alreadyInitialized_invokesOnInitializationSucceededOnlyOnce() {
    adapter.setIsInitialized(true)

    adapter.initialize(
      context,
      initializationCompleteCallback,
      /* mediationConfigurations= */ listOf(),
    )

    assertThat(initializationCompleteCallback).hasSucceeded()
  }

  @Test
  fun collectSignals_invokesOnSuccess() {
    mockStatic(IronSource::class.java).use {
      whenever(IronSource.getISDemandOnlyBiddingData(context)) doReturn TEST_BID_RESPONSE

      adapter.collectSignals(rtbSignalData, signalCallbacks)

      assertThat(signalCallbacks).hasSucceededWith(TEST_BID_RESPONSE)
    }
  }

  @Test
  fun loadBannerAd_notInitialized_expectOnFailureCallbackWithAdError() {
    val mediationAdConfiguration = createMediationBannerAdConfiguration(context)
    val expectedError =
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "banner"),
        IRONSOURCE_SDK_ERROR_DOMAIN,
      )

    adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationBannerAdConfiguration(context)
    val expectedError =
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationBannerAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )
    val expectedError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_invalidBannerSize_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationBannerAdConfiguration(activity, adSize = AdSize.WIDE_SKYSCRAPER)
    val expectedError =
      AdError(
        ERROR_BANNER_SIZE_MISMATCH,
        "There is no matching IronSource banner ad size for Google ad size: ${AdSize.WIDE_SKYSCRAPER}",
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_validInput_loadsSuccessfully() {
    mockStatic(IronSource::class.java).use {
      val mockISBannerLayout = mock<ISDemandOnlyBannerLayout>()
      whenever(createBannerForDemandOnly(any(), any())) doReturn mockISBannerLayout
      adapter.setIsInitialized(true)
      val mediationAdConfiguration = createMediationBannerAdConfiguration(activity)
      whenever(mediationUtils.findClosestSize(eq(activity), eq(AdSize.BANNER), any())) doReturn
        AdSize.BANNER

      adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyBanner(activity, mockISBannerLayout, "0") }
    }
  }

  @Test
  fun loadBannerAd_alreadyLoadedInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationBannerAdConfiguration(activity)
    whenever(mediationUtils.findClosestSize(eq(activity), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

    val secondBannerAdLoadCallback =
      FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    val expectedError =
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource banner is already loaded for instance ID: 0",
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadBannerAd(mediationAdConfiguration, secondBannerAdLoadCallback)

    assertThat(secondBannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadBannerAd_referenceToPreviouslyLoadedAdCleared_loadsSuccessfully() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationBannerAdConfiguration(activity)
    whenever(mediationUtils.findClosestSize(eq(activity), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)
    // Clear the ad reference's reference to the ad object.
    IronSourceBannerAd.availableBannerInstances["0"]?.clear()
    mockStatic(IronSource::class.java).use {
      val mockISBannerLayout = mock<ISDemandOnlyBannerLayout>()
      whenever(createBannerForDemandOnly(any(), any())) doReturn mockISBannerLayout

      // Reload an ad for the same instance ID (i.e. "0") as above.
      adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyBanner(activity, mockISBannerLayout, "0") }
    }
  }

  @Test
  fun loadInterstitialAd_notInitialized_expectOnFailureCallbackWithAdError() {
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(context)
    val expectedError =
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "interstitial"),
        IRONSOURCE_SDK_ERROR_DOMAIN,
      )

    adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(context)
    val expectedError =
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationInterstitialAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )
    val expectedError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_validInput_invokesLoadISDemandOnlyInterstitial() {
    mockStatic(IronSource::class.java).use {
      adapter.setIsInitialized(true)
      val mediationAdConfiguration = createMediationInterstitialAdConfiguration(activity)

      adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyInterstitial(activity, "0") }
    }
  }

  @Test
  fun loadRtbInterstitialAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationInterstitialAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )
    val expectedError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_alreadyLoadedInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(activity)
    adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

    val secondInterstitialAdLoadCallback =
      FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>()
    val expectedError =
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource interstitial ad is already loading for instance ID: 0",
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadInterstitialAd(mediationAdConfiguration, secondInterstitialAdLoadCallback)

    assertThat(secondInterstitialAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadInterstitialAd_referenceToPreviouslyLoadedAdCleared_loadsSuccessfully() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(activity)
    adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)
    // Clear the ad reference's reference to the ad object.
    IronSourceInterstitialAd.availableInterstitialInstances["0"]?.clear()

    mockStatic(IronSource::class.java).use {
      // Reload an ad for the same instance ID (i.e. "0") as above.
      adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyInterstitial(activity, "0") }
    }
  }

  @Test
  fun loadRewardedAd_notInitialized_expectOnFailureCallbackWithAdError() {
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(context)
    val expectedError =
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "rewarded"),
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(context)
    val expectedError =
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )
    val expectedError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_validInput_invokesLoadISDemandOnlyRewardedVideo() {
    mockStatic(IronSource::class.java).use {
      adapter.setIsInitialized(true)
      val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)

      adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyRewardedVideo(activity, "0") }
    }
  }

  @Test
  fun loadRtbRewardedAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )
    val expectedError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRtbRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_alreadyLoadedInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)
    adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

    val secondRewardedAdLoadCallback =
      FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
    val expectedError =
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource Rewarded ad is already loading for instance ID: 0",
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadRewardedAd(mediationAdConfiguration, secondRewardedAdLoadCallback)

    assertThat(secondRewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedAd_referenceToPreviouslyLoadedAdCleared_loadsSuccessfully() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)
    adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)
    // Clear the ad reference's reference to the ad object.
    IronSourceRewardedAd.availableInstances["0"]?.clear()

    mockStatic(IronSource::class.java).use {
      // Reload an ad for the same instance ID (i.e. "0") as above.
      adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyRewardedVideo(activity, "0") }
    }
  }

  @Test
  fun loadRewardedInterstitialAd_notInitialized_expectOnFailureCallbackWithAdError() {
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(context)
    val expectedError =
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "rewarded"),
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadRewardedInterstitialAd(mediationAdConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedInterstitialAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(context)
    val expectedError =
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRewardedInterstitialAd(mediationAdConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedInterstitialAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )
    val expectedError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ADAPTER_ERROR_DOMAIN)

    adapter.loadRewardedInterstitialAd(mediationAdConfiguration, rewardedAdLoadCallback)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedInterstitialAd_validInput_loadsSuccessfully() {
    mockStatic(IronSource::class.java).use {
      adapter.setIsInitialized(true)
      val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)

      adapter.loadRewardedInterstitialAd(mediationAdConfiguration, rewardedAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyRewardedVideo(activity, "0") }
    }
  }

  @Test
  fun loadRewardedInterstitialAd_alreadyLoadedInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)
    adapter.loadRewardedInterstitialAd(mediationAdConfiguration, rewardedAdLoadCallback)

    val secondRewardedAdLoadCallback =
      FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
    val expectedError =
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource Rewarded ad is already loading for instance ID: 0",
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadRewardedInterstitialAd(mediationAdConfiguration, secondRewardedAdLoadCallback)

    assertThat(secondRewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun loadRewardedInterstitialAd_referenceToPreviouslyLoadedAdCleared_loadsSuccessfully() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)
    adapter.loadRewardedInterstitialAd(mediationAdConfiguration, rewardedAdLoadCallback)
    // Clear the ad reference's reference to the ad object.
    IronSourceRewardedAd.availableInstances["0"]?.clear()

    mockStatic(IronSource::class.java).use {
      // Reload an ad for the same instance ID (i.e. "0") as above.
      adapter.loadRewardedInterstitialAd(mediationAdConfiguration, rewardedAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyRewardedVideo(activity, "0") }
    }
  }

  @After
  fun tearDown() {
    adapter.setIsInitialized(false)
    IronSourceBannerAd.removeFromAvailableInstances(/* instanceId= */ "0")
    IronSourceInterstitialAd.removeFromAvailableInstances(/* instanceId= */ "0")
    IronSourceRewardedAd.removeFromAvailableInstances(/* instanceId= */ "0")
  }

  private fun createMediationConfiguration(
    adFormat: AdFormat,
    serverParameters: Bundle = bundleOf(),
  ) = MediationConfiguration(adFormat, serverParameters)

  private fun getUninitializedErrorMessage(adFormat: String) =
    "Failed to load IronSource $adFormat ad since IronSource SDK is not initialized."

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
    const val TEST_APP_ID_2 = "testAppId2"
    const val MISSING_OR_INVALID_APP_KEY_MESSAGE = "Missing or invalid app key."
    const val INVALID_CONTEXT_MESSAGE = "IronSource requires an Activity context to load ads."
    const val INVALID_INSTANCE_ID_MESSAGE = "Missing or invalid instance ID."
  }
}
