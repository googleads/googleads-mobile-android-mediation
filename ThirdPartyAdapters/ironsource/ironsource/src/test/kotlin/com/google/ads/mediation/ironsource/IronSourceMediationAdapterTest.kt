package com.google.ads.mediation.ironsource

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.adaptertestkit.loadBannerAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadInterstitialAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRewardedAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRewardedInterstitialAdWithFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.ironsource.IronSourceAdapterUtils.getAdapterVersion
import com.google.ads.mediation.ironsource.IronSourceConstants.KEY_APP_KEY
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_SDK_NOT_INITIALIZED
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSource.createBannerForDemandOnly
import com.ironsource.mediationsdk.IronSource.initISDemandOnly
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerLayout
import com.ironsource.mediationsdk.utils.IronSourceUtils
import com.ironsource.mediationsdk.utils.IronSourceUtils.getSDKVersion
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [IronSourceMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class IronSourceMediationAdapterTest {

  private lateinit var adapter: IronSourceMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()

  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()
  private val mockRtbSignalData = mock<RtbSignalData> { on { context } doReturn context }
  private val mockSignalCallbacks = mock<SignalCallbacks>()
  private val bannerAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
  private val interstitialAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
  private val rewardedAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()

  @Before
  fun setUp() {
    adapter = IronSourceMediationAdapter()
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor3Digits_returnsTheSameVersion() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "7.3.2"

      adapter.assertGetSdkVersion(expectedValue = "7.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor4Digits_returnsTheSameVersion() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "7.3.2.1"

      adapter.assertGetSdkVersion(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZeros() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "3.2"

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

    adapter.initialize(context, mockInitializationCompleteCallback, listOf(mediationConfiguration))

    verify(mockInitializationCompleteCallback)
      .onInitializationFailed(MISSING_OR_INVALID_APP_KEY_MESSAGE)
  }

  @Test
  fun initialize_withEmptyAppKey_invokesOnInitializationFailed() {
    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(KEY_APP_KEY to ""),
      /* expectedError= */ MISSING_OR_INVALID_APP_KEY_MESSAGE,
    )
  }

  @Test
  fun initialize_withMediationConfigurations_invokesOnInitializationSucceeded() {
    adapter.mediationAdapterInitializeVerifySuccess(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
    )
  }

  @Test
  fun initialize_withMultipleMediationConfigurations_invokesOnInitializationSucceededOnlyOnce() {
    mockStatic(IronSource::class.java).use {
      val mediationConfiguration1 =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1),
        )
      val mediationConfiguration2 =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_2),
        )

      adapter.initialize(
        context,
        mockInitializationCompleteCallback,
        listOf(mediationConfiguration1, mediationConfiguration2),
      )

      verify(mockInitializationCompleteCallback).onInitializationSucceeded()
      it.verify {
        initISDemandOnly(
          any(),
          argThat { appKey -> listOf(TEST_APP_ID_1, TEST_APP_ID_2).contains(appKey) },
          eq(IronSource.AD_UNIT.INTERSTITIAL),
          eq(IronSource.AD_UNIT.REWARDED_VIDEO),
          eq(IronSource.AD_UNIT.BANNER),
        )
      }
    }
  }

  @Test
  fun initialize_alreadyInitialized_invokesOnInitializationSucceededOnlyOnce() {
    adapter.setIsInitialized(true)

    adapter.initialize(
      context,
      mockInitializationCompleteCallback,
      /* mediationConfigurations= */ listOf(),
    )

    verify(mockInitializationCompleteCallback).onInitializationSucceeded()
  }

  @Test
  fun collectSignals_invokesOnSuccess() {
    mockStatic(IronSource::class.java).use {
      whenever(IronSource.getISDemandOnlyBiddingData(context)) doReturn TEST_BID_RESPONSE

      adapter.collectSignals(mockRtbSignalData, mockSignalCallbacks)

      verify(mockSignalCallbacks).onSuccess(TEST_BID_RESPONSE)
    }
  }

  @Test
  fun loadBannerAd_notInitialized_expectOnFailureCallbackWithAdError() {
    val mediationAdConfiguration = createMediationBannerAdConfiguration(context)

    adapter.loadBannerAdWithFailure(
      mediationAdConfiguration,
      bannerAdLoadCallback,
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "banner"),
        IRONSOURCE_SDK_ERROR_DOMAIN,
      ),
    )
  }

  @Test
  fun loadBannerAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationBannerAdConfiguration(context)

    adapter.loadBannerAdWithFailure(
      mediationAdConfiguration,
      bannerAdLoadCallback,
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ERROR_DOMAIN),
    )
  }

  @Test
  fun loadBannerAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationBannerAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )

    adapter.loadBannerAdWithFailure(
      mediationAdConfiguration,
      bannerAdLoadCallback,
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ERROR_DOMAIN),
    )
  }

  @Test
  fun loadBannerAd_invalidBannerSize_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationBannerAdConfiguration(activity, adSize = AdSize.SEARCH)

    adapter.loadBannerAdWithFailure(
      mediationAdConfiguration,
      bannerAdLoadCallback,
      AdError(
        ERROR_BANNER_SIZE_MISMATCH,
        "There is no matching IronSource banner ad size for Google ad size: ${AdSize.SEARCH}",
        ERROR_DOMAIN,
      ),
    )
  }

  @Test
  fun loadBannerAd_validInput_loadsSuccessfully() {
    mockStatic(IronSource::class.java).use {
      val mockISBannerLayout = mock<ISDemandOnlyBannerLayout>()
      whenever(createBannerForDemandOnly(any(), any())) doReturn mockISBannerLayout
      adapter.setIsInitialized(true)
      val mediationAdConfiguration = createMediationBannerAdConfiguration(activity)

      adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyBanner(activity, mockISBannerLayout, "0") }
    }
  }

  @Test
  fun loadBannerAd_alreadyLoadedInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationBannerAdConfiguration(activity)
    adapter.loadBannerAd(mediationAdConfiguration, bannerAdLoadCallback)

    adapter.loadBannerAdWithFailure(
      mediationAdConfiguration,
      bannerAdLoadCallback,
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource banner is already loaded for instance ID: 0",
        ERROR_DOMAIN,
      ),
    )
  }

  @Test
  fun loadBannerAd_referenceToPreviouslyLoadedAdCleared_loadsSuccessfully() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationBannerAdConfiguration(activity)
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

    adapter.loadInterstitialAdWithFailure(
      mediationAdConfiguration,
      interstitialAdLoadCallback,
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "interstitial"),
        IRONSOURCE_SDK_ERROR_DOMAIN,
      ),
    )
  }

  @Test
  fun loadInterstitialAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(context)

    adapter.loadInterstitialAdWithFailure(
      mediationAdConfiguration,
      interstitialAdLoadCallback,
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ERROR_DOMAIN),
    )
  }

  @Test
  fun loadInterstitialAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationInterstitialAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )

    adapter.loadInterstitialAdWithFailure(
      mediationAdConfiguration,
      interstitialAdLoadCallback,
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ERROR_DOMAIN),
    )
  }

  @Test
  fun loadInterstitialAd_validInput_loadsSuccessfully() {
    mockStatic(IronSource::class.java).use {
      adapter.setIsInitialized(true)
      val mediationAdConfiguration = createMediationInterstitialAdConfiguration(activity)

      adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyInterstitial(activity, "0") }
    }
  }

  @Test
  fun loadInterstitialAd_alreadyLoadedInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationInterstitialAdConfiguration(activity)
    adapter.loadInterstitialAd(mediationAdConfiguration, interstitialAdLoadCallback)

    adapter.loadInterstitialAdWithFailure(
      mediationAdConfiguration,
      interstitialAdLoadCallback,
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource interstitial ad is already loading for instance ID: 0",
        ERROR_DOMAIN,
      ),
    )
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

    adapter.loadRewardedAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "rewarded"),
        ERROR_DOMAIN,
      ),
    )
  }

  @Test
  fun loadRewardedAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(context)

    adapter.loadRewardedAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ERROR_DOMAIN),
    )
  }

  @Test
  fun loadRewardedAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )

    adapter.loadRewardedAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ERROR_DOMAIN),
    )
  }

  @Test
  fun loadRewardedAd_validInput_loadsSuccessfully() {
    mockStatic(IronSource::class.java).use {
      adapter.setIsInitialized(true)
      val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)

      adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

      it.verify { IronSource.loadISDemandOnlyRewardedVideo(activity, "0") }
    }
  }

  @Test
  fun loadRewardedAd_alreadyLoadedInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(activity)
    adapter.loadRewardedAd(mediationAdConfiguration, rewardedAdLoadCallback)

    adapter.loadRewardedAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource Rewarded ad is already loading for instance ID: 0",
        ERROR_DOMAIN,
      ),
    )
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

    adapter.loadRewardedInterstitialAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(
        ERROR_SDK_NOT_INITIALIZED,
        getUninitializedErrorMessage(adFormat = "rewarded"),
        ERROR_DOMAIN,
      ),
    )
  }

  @Test
  fun loadRewardedInterstitialAd_invalidContext_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration = createMediationRewardedAdConfiguration(context)

    adapter.loadRewardedInterstitialAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, INVALID_CONTEXT_MESSAGE, ERROR_DOMAIN),
    )
  }

  @Test
  fun loadRewardedInterstitialAd_emptyInstanceId_expectOnFailureCallbackWithAdError() {
    adapter.setIsInitialized(true)
    val mediationAdConfiguration =
      createMediationRewardedAdConfiguration(
        activity,
        serverParameters = bundleOf(IronSourceConstants.KEY_INSTANCE_ID to ""),
      )

    adapter.loadRewardedInterstitialAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(ERROR_INVALID_SERVER_PARAMETERS, INVALID_INSTANCE_ID_MESSAGE, ERROR_DOMAIN),
    )
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

    adapter.loadRewardedInterstitialAdWithFailure(
      mediationAdConfiguration,
      rewardedAdLoadCallback,
      AdError(
        ERROR_AD_ALREADY_LOADED,
        "An IronSource Rewarded ad is already loading for instance ID: 0",
        ERROR_DOMAIN,
      ),
    )
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
