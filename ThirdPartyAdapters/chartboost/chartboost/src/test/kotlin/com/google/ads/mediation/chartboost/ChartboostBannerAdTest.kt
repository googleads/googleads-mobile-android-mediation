// Copyright 2026 Google LLC
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

package com.google.ads.mediation.chartboost

import android.content.Context
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.ads.Banner
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ClickEvent
import com.chartboost.sdk.events.ExpirationEvent
import com.chartboost.sdk.events.ImpressionEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.events.StartError
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_AD_LOCATION
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_APP_ID
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_APP_SIGNATURE
import com.google.ads.mediation.chartboost.ChartboostConstants.CHARTBOOST_SDK_ERROR_DOMAIN
import com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_DOMAIN
import com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MediationUtils
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [ChartboostBannerAd]. */
@RunWith(AndroidJUnit4::class)
class ChartboostBannerAdTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val bannerAdCallback: MediationBannerAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn bannerAdCallback
    }

  private val cacheEvent: CacheEvent = mock()
  private val cacheError: CacheError = mock()
  private val cacheErrorCode: CacheError.Code = mock()
  private val showEvent: ShowEvent = mock()
  private val showError: ShowError = mock()
  private val showErrorCode: ShowError.Code = mock()
  private val clickEvent: ClickEvent = mock()
  private val clickError: ClickError = mock()
  private val clickErrorCode: ClickError.Code = mock()
  private val impressionEvent: ImpressionEvent = mock()
  private val expirationEvent: ExpirationEvent = mock()
  private val startError: StartError = mock()
  private val startErrorCode: StartError.Code = mock()
  private val mockBanner: Banner = mock()

  private lateinit var mockChartboost: MockedStatic<Chartboost>
  private lateinit var mockMediationUtils: MockedStatic<MediationUtils>
  private lateinit var bannerAd: ChartboostBannerAd

  @Before
  fun setUp() {
    mockChartboost = mockStatic(Chartboost::class.java)
    mockMediationUtils = mockStatic(MediationUtils::class.java)
    ChartboostInitializer.clearInstance()

    mockChartboost
      .`when`<Unit> { Chartboost.startWithAppId(any(), any(), any(), any()) }
      .thenAnswer {
        val callback = it.arguments[3] as StartCallback
        callback.onStartCompleted(null)
      }
    mockChartboost.`when`<String> { Chartboost.getSDKVersion() }.thenReturn(TEST_SDK_VERSION)

    mockMediationUtils
      .`when`<AdSize> { MediationUtils.findClosestSize(any(), any(), any()) }
      .thenAnswer { (it.arguments[2] as List<AdSize>)[0] }

    whenever(cacheEvent.ad) doReturn mockBanner

    bannerAd = ChartboostBannerAd(mediationAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockChartboost.close()
    mockMediationUtils.close()
  }

  @Test
  fun loadAd_missingAppId_invokesOnFailure() {
    val serverParameters =
      bundleOf(KEY_APP_SIGNATURE to TEST_APP_SIGNATURE, KEY_AD_LOCATION to TEST_LOCATION)
    val config =
      createMediationBannerAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load banner ad from Chartboost. Missing or invalid server parameters.",
        ERROR_DOMAIN,
      )

    bannerAd.loadAd(config)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyAppId_invokesOnFailure() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to "",
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load banner ad from Chartboost. Missing or invalid server parameters.",
        ERROR_DOMAIN,
      )

    bannerAd.loadAd(config)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_missingAppSignature_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_AD_LOCATION to TEST_LOCATION)
    val config =
      createMediationBannerAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load banner ad from Chartboost. Missing or invalid server parameters.",
        ERROR_DOMAIN,
      )

    bannerAd.loadAd(config)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_unsupportedBannerSize_invokesOnFailure() {
    mockMediationUtils
      .`when`<AdSize> { MediationUtils.findClosestSize(any(), any(), any()) }
      .thenReturn(null)

    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val unsupportedSize = AdSize(123, 456)
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        adSize = unsupportedSize,
      )
    val expectedAdError =
      AdError(
        ERROR_BANNER_SIZE_MISMATCH,
        "The requested banner size: $unsupportedSize is not supported by Chartboost SDK.",
        ERROR_DOMAIN,
      )

    bannerAd.loadAd(config)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_initializationFailure_invokesOnFailure() {
    mockChartboost
      .`when`<Unit> { Chartboost.startWithAppId(any(), any(), any(), any()) }
      .thenAnswer {
        val callback = it.arguments[3] as StartCallback
        callback.onStartCompleted(startError)
      }
    whenever(startError.code) doReturn startErrorCode
    whenever(startErrorCode.errorCode) doReturn ERROR_CODE
    whenever(startError.toString()) doReturn ERROR_MESSAGE
    val expectedAdError = AdError(ERROR_CODE, ERROR_MESSAGE, CHARTBOOST_SDK_ERROR_DOMAIN)

    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(context = context, serverParameters = serverParameters)

    bannerAd.loadAd(config)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_validParameters_createsBannerAndCallsCache() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    mockConstruction(Banner::class.java).use { mockedBannerConstruction ->
      bannerAd.loadAd(config)

      assertThat(mockedBannerConstruction.constructed()).hasSize(1)
      val createdBanner = mockedBannerConstruction.constructed().first()
      verify(createdBanner).cache()
    }
  }

  @Test
  fun onAdLoaded_success_invokesOnSuccessAndShowsAd() {
    bannerAd.onAdLoaded(cacheEvent, null)

    verify(mediationAdLoadCallback).onSuccess(bannerAd)
    verify(mockBanner).show()
  }

  @Test
  fun onAdLoaded_withoutCacheError_doesNotDetachBannerAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    mockConstruction(Banner::class.java).use { mockedBannerConstruction ->
      bannerAd.loadAd(config)
      val createdBanner = mockedBannerConstruction.constructed().first()

      bannerAd.onAdLoaded(cacheEvent, null)

      verify(createdBanner, never()).detach()
    }
  }

  @Test
  fun onAdLoaded_withCacheError_invokesOnFailure() {
    whenever(cacheError.code) doReturn cacheErrorCode
    whenever(cacheErrorCode.errorCode) doReturn ERROR_CODE
    whenever(cacheError.toString()) doReturn ERROR_MESSAGE
    val expectedAdError = AdError(ERROR_CODE, ERROR_MESSAGE, CHARTBOOST_SDK_ERROR_DOMAIN)

    bannerAd.onAdLoaded(cacheEvent, cacheError)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mediationAdLoadCallback, never()).onSuccess(any())
  }

  @Test
  fun onAdLoaded_withCacheError_detachesBannerAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )
    whenever(cacheError.code) doReturn cacheErrorCode
    whenever(cacheErrorCode.errorCode) doReturn ERROR_CODE
    whenever(cacheError.toString()) doReturn ERROR_MESSAGE

    mockConstruction(Banner::class.java).use { mockedBannerConstruction ->
      bannerAd.loadAd(config)
      val createdBanner = mockedBannerConstruction.constructed().first()

      bannerAd.onAdLoaded(cacheEvent, cacheError)

      verify(createdBanner).detach()
    }
  }

  @Test
  fun onAdShown_success_invokesOnAdOpened() {
    bannerAd.onAdLoaded(cacheEvent, null)

    bannerAd.onAdShown(showEvent, null)

    verify(bannerAdCallback).onAdOpened()
  }

  @Test
  fun onAdShown_withoutShowError_doesNotDetachBannerAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    mockConstruction(Banner::class.java).use { mockedBannerConstruction ->
      bannerAd.loadAd(config)
      val createdBanner = mockedBannerConstruction.constructed().first()

      bannerAd.onAdShown(showEvent, null)

      verify(createdBanner, never()).detach()
    }
  }

  @Test
  fun onAdShown_withShowError_doesNotInvokeOnAdOpened() {
    bannerAd.onAdLoaded(cacheEvent, null)
    whenever(showError.code) doReturn showErrorCode
    whenever(showErrorCode.errorCode) doReturn ERROR_CODE
    whenever(showError.toString()) doReturn ERROR_MESSAGE

    bannerAd.onAdShown(showEvent, showError)

    verify(bannerAdCallback, never()).onAdOpened()
  }

  @Test
  fun onAdShown_withShowError_detachesBannerAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )
    whenever(showError.code) doReturn showErrorCode
    whenever(showErrorCode.errorCode) doReturn ERROR_CODE
    whenever(showError.toString()) doReturn ERROR_MESSAGE

    mockConstruction(Banner::class.java).use { mockedBannerConstruction ->
      bannerAd.loadAd(config)
      val createdBanner = mockedBannerConstruction.constructed().first()

      bannerAd.onAdShown(showEvent, showError)

      verify(createdBanner).detach()
    }
  }

  @Test
  fun onImpressionRecorded_invokesReportAdImpression() {
    bannerAd.onAdLoaded(cacheEvent, null)

    bannerAd.onImpressionRecorded(impressionEvent)

    verify(bannerAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_success_invokesReportAdClicked() {
    bannerAd.onAdLoaded(cacheEvent, null)

    bannerAd.onAdClicked(clickEvent, null)

    verify(bannerAdCallback).reportAdClicked()
  }

  @Test
  fun onAdClicked_withClickError_doesNotInvokeReportAdClicked() {
    bannerAd.onAdLoaded(cacheEvent, null)
    whenever(clickError.code) doReturn clickErrorCode
    whenever(clickErrorCode.errorCode) doReturn ERROR_CODE
    whenever(clickError.toString()) doReturn ERROR_MESSAGE

    bannerAd.onAdClicked(clickEvent, clickError)

    verify(bannerAdCallback, never()).reportAdClicked()
  }

  @Test
  fun onAdRequestedToShow_doesNotThrow() {
    bannerAd.onAdRequestedToShow(showEvent)
  }

  @Test
  fun onAdExpired_doesNotThrow() {
    bannerAd.onAdExpired(expirationEvent)
  }

  @Test
  fun getView_afterLoadAd_returnsFrameLayoutContainingBanner() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    mockConstruction(Banner::class.java).use { mockedBannerConstruction ->
      bannerAd.loadAd(config)

      val view = bannerAd.view
      assertIs<FrameLayout>(view)
      assertThat(view.childCount).isEqualTo(1)
      assertThat(view.getChildAt(0)).isEqualTo(mockedBannerConstruction.constructed().first())
    }
  }

  private companion object {
    const val TEST_APP_ID = "test_app_id"
    const val TEST_APP_SIGNATURE = "test_app_signature"
    const val TEST_LOCATION = "test_location"
    const val TEST_SDK_VERSION = "9.13.0"
    const val ERROR_CODE = 10
    const val ERROR_MESSAGE = "test_error_message"
  }
}
