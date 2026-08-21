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
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.ads.Interstitial
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ClickEvent
import com.chartboost.sdk.events.DismissEvent
import com.chartboost.sdk.events.ExpirationEvent
import com.chartboost.sdk.events.ImpressionEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.events.StartError
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_AD_LOCATION
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_APP_ID
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_APP_SIGNATURE
import com.google.ads.mediation.chartboost.ChartboostConstants.CHARTBOOST_SDK_ERROR_DOMAIN
import com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_DOMAIN
import com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
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

/** Tests for [ChartboostInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class ChartboostInterstitialAdTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val interstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn interstitialAdCallback
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
  private val dismissEvent: DismissEvent = mock()
  private val impressionEvent: ImpressionEvent = mock()
  private val expirationEvent: ExpirationEvent = mock()
  private val startError: StartError = mock()
  private val startErrorCode: StartError.Code = mock()

  private lateinit var mockChartboost: MockedStatic<Chartboost>
  private lateinit var interstitialAd: ChartboostInterstitialAd

  @Before
  fun setUp() {
    mockChartboost = mockStatic(Chartboost::class.java)
    ChartboostInitializer.clearInstance()

    mockChartboost
      .`when`<Unit> { Chartboost.startWithAppId(any(), any(), any(), any()) }
      .thenAnswer {
        val callback = it.arguments[3] as StartCallback
        callback.onStartCompleted(null)
      }
    mockChartboost.`when`<String> { Chartboost.getSDKVersion() }.thenReturn(TEST_SDK_VERSION)

    interstitialAd = ChartboostInterstitialAd(mediationAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockChartboost.close()
  }

  @Test
  fun loadAd_invalidServerParameters_invokesOnFailureWithInvalidServerParametersError() {
    val serverParameters =
      bundleOf(KEY_APP_SIGNATURE to TEST_APP_SIGNATURE, KEY_AD_LOCATION to TEST_LOCATION)
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load interstitial ad from Chartboost. Missing or invalid server parameters.",
        ERROR_DOMAIN,
      )

    interstitialAd.loadAd(config)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyLocation_invokesOnFailureWithInvalidServerParametersError() {
    val serverParameters =
      bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_APP_SIGNATURE to TEST_APP_SIGNATURE)
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid location.", ERROR_DOMAIN)

    mockStatic(ChartboostAdapterUtils::class.java).use { mockAdapterUtils ->
      val params =
        ChartboostParams().apply {
          appId = TEST_APP_ID
          appSignature = TEST_APP_SIGNATURE
          location = ""
        }
      mockAdapterUtils
        .`when`<ChartboostParams> { ChartboostAdapterUtils.createChartboostParams(any()) }
        .thenReturn(params)
      mockAdapterUtils
        .`when`<Boolean> { ChartboostAdapterUtils.isValidChartboostParams(params) }
        .thenReturn(true)

      interstitialAd.loadAd(config)

      verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun loadAd_sdkInitializationFails_invokesOnFailureWithSdkError() {
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
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )

    interstitialAd.loadAd(config)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_validServerParameters_initializesViaSdkAndCachesInterstitial() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )

    mockConstruction(Interstitial::class.java).use { mockedInterstitialConstruction ->
      interstitialAd.loadAd(config)

      assertThat(mockedInterstitialConstruction.constructed()).hasSize(1)
      val createdInterstitial = mockedInterstitialConstruction.constructed().first()
      verify(createdInterstitial).cache()
    }
  }

  @Test
  fun showAd_adNotCached_stillCallsShow() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )

    mockConstruction(Interstitial::class.java) { mock, _ ->
        whenever(mock.isCached()) doReturn false
      }
      .use { mockedInterstitialConstruction ->
        interstitialAd.loadAd(config)
        interstitialAd.showAd(context)

        val createdInterstitial = mockedInterstitialConstruction.constructed().first()
        verify(createdInterstitial).show()
      }
  }

  @Test
  fun showAd_adIsNull_doesNotThrowException() {
    interstitialAd.showAd(context)
  }

  @Test
  fun showAd_adCached_callsShow() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )

    mockConstruction(Interstitial::class.java) { mock, _ ->
        whenever(mock.isCached()) doReturn true
      }
      .use { mockedInterstitialConstruction ->
        interstitialAd.loadAd(config)
        interstitialAd.showAd(context)

        val createdInterstitial = mockedInterstitialConstruction.constructed().first()
        verify(createdInterstitial).show()
      }
  }

  @Test
  fun onAdDismiss_invokesOnAdClosed() {
    interstitialAd.onAdLoaded(cacheEvent, null)

    interstitialAd.onAdDismiss(dismissEvent)

    verify(interstitialAdCallback).onAdClosed()
  }

  @Test
  fun onAdDismiss_doesNotDestroyInterstitialAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )

    mockConstruction(Interstitial::class.java).use { mockedInterstitialConstruction ->
      interstitialAd.loadAd(config)
      val createdInterstitial = mockedInterstitialConstruction.constructed().first()

      interstitialAd.onAdDismiss(dismissEvent)

      verify(createdInterstitial, never()).destroy()
    }
  }

  @Test
  fun onImpressionRecorded_invokesReportAdImpression() {
    interstitialAd.onAdLoaded(cacheEvent, null)

    interstitialAd.onImpressionRecorded(impressionEvent)

    verify(interstitialAdCallback).reportAdImpression()
  }

  @Test
  fun onAdShown_withShowError_invokesOnAdFailedToShowWithSdkError() {
    interstitialAd.onAdLoaded(cacheEvent, null)
    whenever(showError.code) doReturn showErrorCode
    whenever(showErrorCode.errorCode) doReturn ERROR_CODE
    whenever(showError.toString()) doReturn ERROR_MESSAGE
    val expectedAdError = AdError(ERROR_CODE, ERROR_MESSAGE, CHARTBOOST_SDK_ERROR_DOMAIN)

    interstitialAd.onAdShown(showEvent, showError)

    verify(interstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdShown_withShowError_destroysInterstitialAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    whenever(showError.code) doReturn showErrorCode
    whenever(showErrorCode.errorCode) doReturn ERROR_CODE
    whenever(showError.toString()) doReturn ERROR_MESSAGE

    mockConstruction(Interstitial::class.java).use { mockedInterstitialConstruction ->
      interstitialAd.loadAd(config)
      val createdInterstitial = mockedInterstitialConstruction.constructed().first()

      interstitialAd.onAdShown(showEvent, showError)

      verify(createdInterstitial).destroy()
    }
  }

  @Test
  fun onAdShown_withoutShowError_invokesOnAdOpened() {
    interstitialAd.onAdLoaded(cacheEvent, null)

    interstitialAd.onAdShown(showEvent, null)

    verify(interstitialAdCallback).onAdOpened()
  }

  @Test
  fun onAdLoaded_withCacheError_invokesOnFailureWithSdkError() {
    whenever(cacheError.code) doReturn cacheErrorCode
    whenever(cacheErrorCode.errorCode) doReturn ERROR_CODE
    whenever(cacheError.toString()) doReturn ERROR_MESSAGE
    val expectedAdError = AdError(ERROR_CODE, ERROR_MESSAGE, CHARTBOOST_SDK_ERROR_DOMAIN)

    interstitialAd.onAdLoaded(cacheEvent, cacheError)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mediationAdLoadCallback, never()).onSuccess(any())
  }

  @Test
  fun onAdLoaded_withCacheError_destroysInterstitialAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )
    whenever(cacheError.code) doReturn cacheErrorCode
    whenever(cacheErrorCode.errorCode) doReturn ERROR_CODE
    whenever(cacheError.toString()) doReturn ERROR_MESSAGE

    mockConstruction(Interstitial::class.java).use { mockedInterstitialConstruction ->
      interstitialAd.loadAd(config)
      val createdInterstitial = mockedInterstitialConstruction.constructed().first()

      interstitialAd.onAdLoaded(cacheEvent, cacheError)

      verify(createdInterstitial).destroy()
    }
  }

  @Test
  fun onAdLoaded_withoutCacheError_doesNotDestroyInterstitialAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )

    mockConstruction(Interstitial::class.java).use { mockedConstruction ->
      interstitialAd.loadAd(config)
      val createdInterstitial = mockedConstruction.constructed().first()

      interstitialAd.onAdLoaded(cacheEvent, null)

      verify(createdInterstitial, never()).destroy()
    }
  }

  @Test
  fun onAdShown_withoutShowError_doesNotDestroyInterstitialAd() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )
    val config =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
      )

    mockConstruction(Interstitial::class.java).use { mockedConstruction ->
      interstitialAd.loadAd(config)
      val createdInterstitial = mockedConstruction.constructed().first()

      interstitialAd.onAdShown(showEvent, null)

      verify(createdInterstitial, never()).destroy()
    }
  }

  @Test
  fun onAdLoaded_withoutCacheError_invokesOnSuccess() {
    interstitialAd.onAdLoaded(cacheEvent, null)

    verify(mediationAdLoadCallback).onSuccess(interstitialAd)
  }

  @Test
  fun onAdClicked_withClickError_doesNotInvokeReportAdClicked() {
    interstitialAd.onAdLoaded(cacheEvent, null)
    whenever(clickError.code) doReturn clickErrorCode
    whenever(clickErrorCode.errorCode) doReturn ERROR_CODE
    whenever(clickError.toString()) doReturn ERROR_MESSAGE

    interstitialAd.onAdClicked(clickEvent, clickError)

    verify(interstitialAdCallback, never()).reportAdClicked()
  }

  @Test
  fun onAdClicked_withoutClickError_invokesReportAdClicked() {
    interstitialAd.onAdLoaded(cacheEvent, null)

    interstitialAd.onAdClicked(clickEvent, null)

    verify(interstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAdRequestedToShow_doesNotThrowException() {
    interstitialAd.onAdRequestedToShow(showEvent)
  }

  @Test
  fun onAdExpired_doesNotThrowException() {
    interstitialAd.onAdExpired(expirationEvent)
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
