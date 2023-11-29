package com.google.ads.mediation.adcolony

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adcolony.sdk.AdColony
import com.adcolony.sdk.AdColony.getSDKVersion
import com.adcolony.sdk.AdColonyAppOptions
import com.adcolony.sdk.AdColonyInterstitial
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adcolony.AdColonyAdapterUtils.getAdapterVersion
import com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_ADCOLONY_NOT_INITIALIZED
import com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.jirbo.adcolony.AdColonyManager
import com.jirbo.adcolony.AdColonyManager.getInstance
import junit.framework.TestCase.assertEquals
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

/** Tests for [AdColonyMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class AdColonyMediationAdapterTest {
  private lateinit var adapter: AdColonyMediationAdapter

  private val mediationConfiguration: MediationConfiguration = mock()
  private val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val invalidContext: Context = mock()
  private val adColonyManager: AdColonyManager = mock()
  private val adColonyInterstitial: AdColonyInterstitial = mock()
  private val mediationInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mediationBannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val adColonyWrapper: AdColonyWrapper = mock()

  @Before
  fun setUp() {
    adapter = AdColonyMediationAdapter()
  }

  @Test
  fun getVersionInfo_returnsCorrectVersion() {
    mockStatic(AdColonyAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_valid5Digits_returnsCorrectVersion() {
    mockStatic(AdColonyAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4.5"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZero() {
    mockStatic(AdColonyAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSDKVersionInfo_valid3Digits_returnsCorrectVersion() {
    mockStatic(AdColony::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2.3"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSDKVersionInfo_valid4Digits_returnsCorrectVersion() {
    mockStatic(AdColony::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2.3.4"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZero() {
    mockStatic(AdColony::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun initializationFailure_invalidAppId() {
    val invalidServerParameters = bundleOf(AdColonyAdapterUtils.KEY_APP_ID to "")
    whenever(mediationConfiguration.serverParameters).doReturn(invalidServerParameters)
    // Create an AdError object so that it can be verified that this object's toString() matches the
    // error string that's passed to the initialization callback.
    val adError =
      AdColonyMediationAdapter.createAdapterError(
        AdColonyMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid AdColony app ID."
      )

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
    verify(initializationCompleteCallback).onInitializationFailed(adError.toString())
  }

  @Test
  fun initializationFailure_invalidContext() {
    val validServerParameters =
      bundleOf(AdColonyAdapterUtils.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID)
    whenever(mediationConfiguration.serverParameters).doReturn(validServerParameters)
    val adError =
      AdColonyMediationAdapter.createAdapterError(
        AdColonyMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY,
        "AdColony SDK requires an Activity or Application context to initialize."
      )

    adapter.initialize(
      invalidContext,
      initializationCompleteCallback,
      listOf(mediationConfiguration)
    )

    verify(initializationCompleteCallback).onInitializationFailed(adError.toString())
  }

  @Test
  fun initializationFailure_invalidZoneIds() {

    val validServerParameters =
      bundleOf(AdColonyAdapterUtils.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID)
    whenever(mediationConfiguration.serverParameters).doReturn(validServerParameters)
    val adError =
      AdColonyMediationAdapter.createAdapterError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "No zones provided to initialize the AdColony SDK."
      )

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    verify(initializationCompleteCallback).onInitializationFailed(adError.toString())
  }

  @Test
  fun initializationSuccess_onInitializeSuccessCallback() {
    val validServerParameters =
      bundleOf(
        AdColonyAdapterUtils.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
        AdColonyAdapterUtils.KEY_ZONE_ID to "123;456"
      )

    whenever(mediationConfiguration.serverParameters).doReturn(validServerParameters)

    mockStatic(AdColonyManager::class.java).use {
      whenever(getInstance()) doReturn adColonyManager
      whenever(adColonyManager.configureAdColony(any(), any(), any(), any(), any())).doAnswer {
        val initializationListener = it.arguments[4] as AdColonyManager.InitializationListener
        initializationListener.onInitializeSuccess()
      }
      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
    }
    val mediationInfo = AdColonyMediationAdapter.getAppOptions().getMediationInfo()
    assertEquals(AdColonyAppOptions.ADMOB, mediationInfo.optString("name"))
    assertEquals(getAdapterVersion(), mediationInfo.optString("version"))
    verify(initializationCompleteCallback).onInitializationSucceeded()
  }

  @Test
  fun initializationFailure_onInitializeFailed() {
    val validServerParameters =
      bundleOf(
        AdColonyAdapterUtils.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
        AdColonyAdapterUtils.KEY_ZONE_ID to "123;456"
      )
    val adError =
      AdColonyMediationAdapter.createAdapterError(
        ERROR_ADCOLONY_NOT_INITIALIZED,
        "AdColony SDK failed to initialize."
      )

    whenever(mediationConfiguration.serverParameters).doReturn(validServerParameters)

    mockStatic(AdColonyManager::class.java).use {
      whenever(getInstance()) doReturn adColonyManager
      whenever(adColonyManager.configureAdColony(any(), any(), any(), any(), any())).doAnswer {
        val initializationListener = it.arguments[4] as AdColonyManager.InitializationListener
        initializationListener.onInitializeFailed(adError)
      }
      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
    }
    verify(initializationCompleteCallback).onInitializationFailed(adError.toString())
  }

  @Test
  fun loadInterstitialAd_successful() {
    val validServerParameters =
      bundleOf(
        AdColonyAdapterUtils.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
        AdColonyAdapterUtils.KEY_ZONE_ID to "123;456"
      )
    val mediationBannerAdConfiguration =
      createMediationInterstitialAdConfiguration(context, serverParameters = validServerParameters)
    mockStatic(AdColonyWrapper::class.java).use {
      whenever(AdColonyWrapper.getInstance()) doReturn adColonyWrapper
      adapter.loadRtbInterstitialAd(
        mediationBannerAdConfiguration,
        mediationInterstitialAdLoadCallback
      )
    }
    verify(adColonyWrapper).requestInterstitial(any(), any(), any())
  }

  @Test
  fun loadBannerAd_successful() {
    val validServerParameters =
      bundleOf(
        AdColonyAdapterUtils.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
        AdColonyAdapterUtils.KEY_ZONE_ID to "123;456"
      )
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(context, serverParameters = validServerParameters)
    mockStatic(AdColonyWrapper::class.java).use {
      whenever(AdColonyWrapper.getInstance()) doReturn adColonyWrapper
      adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mediationBannerAdLoadCallback)
    }
    verify(adColonyWrapper).requestAdView(any(), any(), any(), any())
  }
}
