// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.maio

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeInitializationCompleteCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_USER_IS_AGE_RESTRICTED
import com.google.ads.mediation.maio.MaioMediationAdapter.MAIO_IS_AGE_RESTRICTED_ERROR
import com.google.ads.mediation.maio.MaioMediationAdapter.MAIO_SDK_ERROR_DOMAIN
import com.google.ads.mediation.maio.MaioUtils.getVersionInfo
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_MEDIA_ID
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_ZONE_ID
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.getSdkVersion
import jp.maio.sdk.android.v2.Version
import jp.maio.sdk.android.v2.banner.MaioBannerView
import jp.maio.sdk.android.v2.interstitial.Interstitial
import jp.maio.sdk.android.v2.rewarded.Rewarded
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Class containing unit tests for [MaioMediationAdapter] */
@RunWith(AndroidJUnit4::class)
class MaioMediationAdapterTest {

  private val mediationUtils: MediationUtilsWrapper = mock()

  private var adapter: MaioMediationAdapter = MaioMediationAdapter(mediationUtils)

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val initializationCompleteCallback = FakeInitializationCompleteCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()

  @Before
  fun setUp() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
  }

  // region constructor tests
  @Test
  fun constructor_defaultConstructor_initializesAdapter() {
    val defaultAdapter = MaioMediationAdapter()
    assertThat(defaultAdapter).isNotNull()
  }

  // endregion

  // region getAdError tests
  @Test
  fun getAdError_withErrorCode_returnsExpectedAdError() {
    val errorCode = 10700
    val adError = MaioMediationAdapter.getAdError(errorCode)

    assertThat(adError.code).isEqualTo(errorCode)
    assertThat(adError.message).isEqualTo("Failed to request ad from Maio: $errorCode")
    assertThat(adError.domain).isEqualTo(MAIO_SDK_ERROR_DOMAIN)
  }

  // endregion

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
  fun initialize_withMediationConfigurations_invokesOnInitializationSucceeded() {
    adapter.initialize(
      activity,
      initializationCompleteCallback,
      /* mediationConfigurations= */ emptyList(),
    )

    assertThat(initializationCompleteCallback).hasSucceeded()
  }

  @Test
  fun initialize_withTFCDAndTFUAFalse_invokesOnInitializationSucceeded() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.initialize(
      activity,
      initializationCompleteCallback,
      /* mediationConfigurations= */ emptyList(),
    )

    assertThat(initializationCompleteCallback).hasSucceeded()
  }

  @Test
  fun initialize_withTFCDTrue_invokesOnInitializationFailed() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.initialize(
      activity,
      initializationCompleteCallback,
      /* mediationConfigurations= */ emptyList(),
    )

    assertThat(initializationCompleteCallback).hasFailedWith(MAIO_IS_AGE_RESTRICTED_ERROR)
  }

  @Test
  fun initialize_withTFUATrue_invokesOnInitializationFailed() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.initialize(
      activity,
      initializationCompleteCallback,
      /* mediationConfigurations= */ emptyList(),
    )

    assertThat(initializationCompleteCallback).hasFailedWith(MAIO_IS_AGE_RESTRICTED_ERROR)
  }

  @Test
  fun initialize_withAgeRestrictedTreatmentChild_invokesOnInitializationFailed() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.initialize(
      activity,
      initializationCompleteCallback,
      /* mediationConfigurations= */ emptyList(),
    )

    assertThat(initializationCompleteCallback).hasFailedWith(MAIO_IS_AGE_RESTRICTED_ERROR)
  }

  // endregion

  // region Interstitial ad tests
  @Test
  fun loadInterstitialAd_withTFCDTrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val interstitialAdConfiguration = createInterstitialAdConfiguration()

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadInterstitialAd_withTFUATrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val interstitialAdConfiguration = createInterstitialAdConfiguration()

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadInterstitialAd_withAgeRestrictedTreatmentChild_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val interstitialAdConfiguration = createInterstitialAdConfiguration()

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadInterstitialAd_withNullKeyMedia_invokesOnFailure() {
    val interstitialAdConfiguration = createInterstitialAdConfiguration()

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadInterstitialAd_withEmptyKeyMedia_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to "")
    val interstitialAdConfiguration =
      createInterstitialAdConfiguration(serverParameters = serverParameters)

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadInterstitialAd_withNullZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1)
    val interstitialAdConfiguration =
      createInterstitialAdConfiguration(serverParameters = serverParameters)

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadInterstitialAd_withEmptyZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "")
    val interstitialAdConfiguration =
      createInterstitialAdConfiguration(serverParameters = serverParameters)

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadInterstitialAd_withValidParameters_invokesInterstitialLoadAd() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "testZoneId")
    val interstitialAdConfiguration =
      createInterstitialAdConfiguration(serverParameters = serverParameters)

    mockStatic(Interstitial::class.java).use { mockInterstitialStatic ->
      val mockInterstitial = mock<Interstitial>()
      mockInterstitialStatic
        .`when`<Interstitial> { MaioTestHelper.loadInterstitialAd(any(), any(), any()) }
        .thenReturn(mockInterstitial)

      adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

      mockInterstitialStatic.verify {
        MaioTestHelper.loadInterstitialAd(any(), eq(activity), any())
      }
    }
  }

  private fun createInterstitialAdConfiguration(
    context: Context = activity,
    serverParameters: Bundle = bundleOf(),
  ) =
    MediationInterstitialAdConfiguration(
      context,
      /*bidresponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      /*watermark=*/ "",
    )

  // endregion

  // region Rewarded ad tests
  @Test
  fun loadRewardedAd_withTFCDTrue_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "testZoneId")
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_withTFUATrue_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "testZoneId")
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_withAgeRestrictedTreatmentChild_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "testZoneId")
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_withValidParameters_invokesRewardedLoadAd() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "testZoneId")
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    mockStatic(Rewarded::class.java).use { mockRewardedStatic ->
      val mockRewarded = mock<Rewarded>()
      mockRewardedStatic
        .`when`<Rewarded> { MaioTestHelper.loadRewardedAd(any(), any(), any()) }
        .thenReturn(mockRewarded)

      adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

      mockRewardedStatic.verify { MaioTestHelper.loadRewardedAd(any(), eq(activity), any()) }
    }
  }

  @Test
  fun loadRewardedAd_withNullKeyMedia_invokesOnFailure() {
    val rewardedAdConfiguration = createRewardedAdConfiguration()

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_withEmptyKeyMedia_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to "")
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_withNullZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1)
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadRewardedAd_withEmptyZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "")
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
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

  // region Banner ad tests
  @Test
  fun loadBannerAd_withTFCDTrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val bannerAdConfiguration = createBannerAdConfiguration()

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withTFUATrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val bannerAdConfiguration = createBannerAdConfiguration()

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withAgeRestrictedTreatmentChild_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val bannerAdConfiguration = createBannerAdConfiguration()

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, MAIO_IS_AGE_RESTRICTED_ERROR, ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withNullKeyMedia_invokesOnFailure() {
    val bannerAdConfiguration = createBannerAdConfiguration()

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withEmptyKeyMedia_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to "")
    val bannerAdConfiguration = createBannerAdConfiguration(serverParameters = serverParameters)

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withNullZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1)
    val bannerAdConfiguration = createBannerAdConfiguration(serverParameters = serverParameters)

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withEmptyZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "")
    val bannerAdConfiguration = createBannerAdConfiguration(serverParameters = serverParameters)

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withInvalidSize_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "testZoneId")
    val bannerAdConfiguration =
      createBannerAdConfiguration(serverParameters = serverParameters, adSize = AdSize(300, 50))
    whenever(mediationUtils.findClosestSize(eq(context), eq(AdSize(300, 50)), any())) doReturn null

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "The requested ad size is not supported by maio SDK.",
        ERROR_DOMAIN,
      )
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadBannerAd_withValidParameters_loadsBannerAd() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_APP_ID_1, KEY_ZONE_ID to "testZoneId")
    val bannerAdConfiguration =
      createBannerAdConfiguration(serverParameters = serverParameters, adSize = AdSize.BANNER)
    whenever(mediationUtils.findClosestSize(any(), any(), any())) doReturn AdSize.BANNER

    mockConstruction(MaioBannerView::class.java).use { mockBannerViewConstruction ->
      adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

      assertThat(mockBannerViewConstruction.constructed()).hasSize(1)
    }
  }

  private fun createBannerAdConfiguration(
    context: Context = activity,
    serverParameters: Bundle = bundleOf(),
    adSize: AdSize = AdSize.BANNER,
  ) =
    MediationBannerAdConfiguration(
      context,
      /*bidresponse=*/ "",
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

  // endregion

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
    const val TEST_APP_ID_2 = "testAppId2"
    const val MISSING_OR_INVALID_APP_KEY_MESSAGE =
      "Initialization Failed: Missing or Invalid Media ID."
    const val INVALID_CONTEXT_MESSAGE = "Maio SDK requires an Activity context to initialize"
  }
}
