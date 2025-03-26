package com.google.ads.mediation.maio

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.maio.MaioUtils.getVersionInfo
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_MEDIA_ID
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_ZONE_ID
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.getSdkVersion
import jp.maio.sdk.android.v2.Version
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Class containing unit tests for [MaioMediationAdapter] */
@RunWith(AndroidJUnit4::class)
class MaioMediationAdapterTest {

  private var adapter: MaioMediationAdapter = MaioMediationAdapter()

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()
  private val mockRewardedAdCallback = mock<MediationRewardedAdCallback>()
  private val mockMediationRewardedAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>> {
      on { onSuccess(any()) } doReturn mockRewardedAdCallback
    }

  // region version tests
  @Test
  fun getVersionInfo_validVersionFor4Digits_returnsTheSameVersion() {
    mockStatic(MaioUtils::class.java).use {
      whenever(getVersionInfo()) doReturn "7.3.2.1"

      adapter.assertGetVersionInfo(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getVersionInfo_validVersionFor5Digits_returnsTheValidVersion() {
    mockStatic(MaioUtils::class.java).use {
      whenever(getVersionInfo()) doReturn "7.3.2.1.5"

      adapter.assertGetVersionInfo(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZeros() {
    mockStatic(MaioUtils::class.java).use {
      whenever(getVersionInfo()) doReturn "3.2"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor3Digits_returnsTheSameVersion() {
    val mockVersion = mock<Version> { on { toString() } doReturn "7.3.2" }

    mockStatic(MaioAdsManager::class.java).use {
      whenever(getSdkVersion()) doReturn mockVersion
      adapter.assertGetSdkVersion(expectedValue = "7.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor4Digits_returnsTheValidVersion() {
    val mockVersion = mock<Version> { on { toString() } doReturn "7.3.2.1" }

    mockStatic(MaioAdsManager::class.java).use {
      whenever(getSdkVersion()) doReturn mockVersion
      adapter.assertGetSdkVersion(expectedValue = "7.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZeros() {
    val mockVersion = mock<Version> { on { toString() } doReturn "3.2" }

    mockStatic(MaioAdsManager::class.java).use {
      whenever(getSdkVersion()) doReturn mockVersion
      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region initialize tests
  @Test
  fun initialize_withNoMediaIdKeyInServerParameters_invokesOnInitializationFailed() {
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER)

    adapter.initialize(activity, mockInitializationCompleteCallback, listOf(mediationConfiguration))

    verify(mockInitializationCompleteCallback)
      .onInitializationFailed(MISSING_OR_INVALID_APP_KEY_MESSAGE)
  }

  @Test
  fun initialize_withEmptyAppKey_invokesOnInitializationFailed() {
    adapter.mediationAdapterInitializeVerifyFailure(
      activity,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(KEY_MEDIA_ID to ""),
      /* expectedError= */ MISSING_OR_INVALID_APP_KEY_MESSAGE,
    )
  }

  @Test
  fun initialize_withMediationConfigurations_invokesOnInitializationSucceeded() {
    adapter.mediationAdapterInitializeVerifySuccess(
      activity,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1),
    )
  }

  @Test
  fun initialize_withMultipleMediationConfigurations_invokesOnInitializationSucceededOnlyOnce() {
    val mediationConfiguration1 =
      createMediationConfiguration(
        AdFormat.BANNER,
        serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1),
      )
    val mediationConfiguration2 =
      createMediationConfiguration(
        AdFormat.BANNER,
        serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_2),
      )

    adapter.initialize(
      activity,
      mockInitializationCompleteCallback,
      listOf(mediationConfiguration1, mediationConfiguration2),
    )

    verify(mockInitializationCompleteCallback, times(1)).onInitializationSucceeded()
  }

  private fun createMediationConfiguration(
    adFormat: AdFormat,
    serverParameters: Bundle = bundleOf(),
  ) = MediationConfiguration(adFormat, serverParameters)

  // endregion

  // region Rewarded ad tests
  @Test
  fun loadRewardedAd_withNullKeyMedia_invokesOnFailure() {
    val rewardedAdConfiguration = createRewardedAdConfiguration()

    adapter.loadRewardedAd(rewardedAdConfiguration, mockMediationRewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_withEmptyKeyMedia_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to "")
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, mockMediationRewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_withNullZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1)
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, mockMediationRewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_withEmptyZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "")
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, mockMediationRewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  private fun createRewardedAdConfiguration(
    context: Context = activity,
    serverParameters: Bundle = bundleOf(),
  ) =
    MediationRewardedAdConfiguration(
      context,
      /*bidresponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  // endregion

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
    const val TEST_APP_ID_2 = "testAppId2"
    const val MISSING_OR_INVALID_APP_KEY_MESSAGE =
      "Initialization Failed: Missing or Invalid Media ID."
    const val INVALID_CONTEXT_MESSAGE = "Maio SDK requires an Activity context to initialize"
  }
}
