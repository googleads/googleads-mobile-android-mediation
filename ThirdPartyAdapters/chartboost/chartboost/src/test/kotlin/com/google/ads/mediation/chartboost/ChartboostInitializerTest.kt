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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.events.StartError
import com.chartboost.sdk.privacy.model.COPPA
import com.chartboost.sdk.privacy.model.GDPR
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.chartboost.ChartboostConstants.AD_TECHNOLOGY_PROVIDER_ID
import com.google.ads.mediation.chartboost.ChartboostConstants.CHARTBOOST_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [ChartboostInitializer]. */
@RunWith(AndroidJUnit4::class)
class ChartboostInitializerTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val listener: ChartboostInitializer.Listener = mock()
  private val listener2: ChartboostInitializer.Listener = mock()
  private val startError: StartError = mock()
  private val startErrorCode: StartError.Code = mock()

  private lateinit var mockChartboost: MockedStatic<Chartboost>
  private lateinit var chartboostParams: ChartboostParams

  @Before
  fun setUp() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    )
    mockChartboost = mockStatic(Chartboost::class.java)
    ChartboostInitializer.clearInstance()

    chartboostParams =
      ChartboostParams().apply {
        appId = TEST_APP_ID
        appSignature = TEST_APP_SIGNATURE
        location = TEST_LOCATION
      }
  }

  @After
  fun tearDown() {
    mockChartboost.close()
  }

  @Test
  fun getInstance_returnsSameInstance() {
    val instance1 = ChartboostInitializer.getInstance()
    val instance2 = ChartboostInitializer.getInstance()

    assertIs<ChartboostInitializer>(instance1)
    assertThat(instance1).isSameInstanceAs(instance2)
  }

  @Test
  fun initialize_initializationSuccess_invokesOnInitializationSucceeded() {
    val callbackCaptor = argumentCaptor<StartCallback>()

    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)

    mockChartboost.verify {
      Chartboost.startWithAppId(
        eq(context),
        eq(TEST_APP_ID),
        eq(TEST_APP_SIGNATURE),
        callbackCaptor.capture(),
      )
    }
    callbackCaptor.firstValue.onStartCompleted(null)

    verify(listener).onInitializationSucceeded()
    verify(listener, never()).onInitializationFailed(any())
  }

  @Test
  fun initialize_initializationFailure_invokesOnInitializationFailed() {
    val callbackCaptor = argumentCaptor<StartCallback>()
    whenever(startError.code) doReturn startErrorCode
    whenever(startErrorCode.errorCode) doReturn ERROR_CODE
    whenever(startError.toString()) doReturn ERROR_MESSAGE
    val expectedAdError = AdError(ERROR_CODE, ERROR_MESSAGE, CHARTBOOST_SDK_ERROR_DOMAIN)

    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)

    mockChartboost.verify {
      Chartboost.startWithAppId(
        eq(context),
        eq(TEST_APP_ID),
        eq(TEST_APP_SIGNATURE),
        callbackCaptor.capture(),
      )
    }
    callbackCaptor.firstValue.onStartCompleted(startError)

    verify(listener).onInitializationFailed(argThat(AdErrorMatcher(expectedAdError)))
    verify(listener, never()).onInitializationSucceeded()
  }

  @Test
  fun initialize_alreadyInitialized_invokesOnInitializationSucceededImmediately() {
    val callbackCaptor = argumentCaptor<StartCallback>()

    // First initialization succeeds.
    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)
    mockChartboost.verify {
      Chartboost.startWithAppId(any(), any(), any(), callbackCaptor.capture())
    }
    callbackCaptor.firstValue.onStartCompleted(null)
    verify(listener).onInitializationSucceeded()

    // Second initialization with new listener.
    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener2)

    verify(listener2).onInitializationSucceeded()
    // Verify startWithAppId was only called once in total.
    mockChartboost.verify({ Chartboost.startWithAppId(any(), any(), any(), any()) }, times(1))
  }

  @Test
  fun initialize_alreadyInitializing_queuesListenerAndNotifiesOnSuccess() {
    val callbackCaptor = argumentCaptor<StartCallback>()

    // Start first initialization (do not complete callback yet).
    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)
    mockChartboost.verify {
      Chartboost.startWithAppId(any(), any(), any(), callbackCaptor.capture())
    }

    // Call initialize again while first is in progress.
    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener2)

    // startWithAppId should not be called again.
    mockChartboost.verify({ Chartboost.startWithAppId(any(), any(), any(), any()) }, times(1))

    // Complete the initialization callback with success.
    callbackCaptor.firstValue.onStartCompleted(null)

    verify(listener).onInitializationSucceeded()
    verify(listener2).onInitializationSucceeded()
  }

  @Test
  fun initialize_alreadyInitializing_queuesListenerAndNotifiesOnFailure() {
    val callbackCaptor = argumentCaptor<StartCallback>()
    whenever(startError.code) doReturn startErrorCode
    whenever(startErrorCode.errorCode) doReturn ERROR_CODE
    whenever(startError.toString()) doReturn ERROR_MESSAGE
    val expectedAdError = AdError(ERROR_CODE, ERROR_MESSAGE, CHARTBOOST_SDK_ERROR_DOMAIN)

    // Start first initialization.
    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)
    mockChartboost.verify {
      Chartboost.startWithAppId(any(), any(), any(), callbackCaptor.capture())
    }

    // Call initialize again while first is in progress.
    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener2)

    // Complete the callback with failure.
    callbackCaptor.firstValue.onStartCompleted(startError)

    verify(listener).onInitializationFailed(argThat(AdErrorMatcher(expectedAdError)))
    verify(listener2).onInitializationFailed(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun initialize_withACConsentTrue_addsBehavioralConsent() {
    mockStatic(ChartboostAdapterUtils::class.java).use { mockAdapterUtils ->
      mockAdapterUtils
        .`when`<ChartboostAdapterUtils.ConsentResult> {
          ChartboostAdapterUtils.hasACConsent(any(), eq(AD_TECHNOLOGY_PROVIDER_ID))
        }
        .thenReturn(ChartboostAdapterUtils.ConsentResult.TRUE)

      val gdprCaptor = argumentCaptor<GDPR>()
      ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)

      mockChartboost.verify { Chartboost.addDataUseConsent(eq(context), gdprCaptor.capture()) }
      assertThat(gdprCaptor.firstValue.consent).isEqualTo(GDPR.GDPR_CONSENT.BEHAVIORAL.value)
    }
  }

  @Test
  fun initialize_withACConsentFalse_addsNonBehavioralConsent() {
    mockStatic(ChartboostAdapterUtils::class.java).use { mockAdapterUtils ->
      mockAdapterUtils
        .`when`<ChartboostAdapterUtils.ConsentResult> {
          ChartboostAdapterUtils.hasACConsent(any(), eq(AD_TECHNOLOGY_PROVIDER_ID))
        }
        .thenReturn(ChartboostAdapterUtils.ConsentResult.FALSE)

      val gdprCaptor = argumentCaptor<GDPR>()
      ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)

      mockChartboost.verify { Chartboost.addDataUseConsent(eq(context), gdprCaptor.capture()) }
      assertThat(gdprCaptor.firstValue.consent).isEqualTo(GDPR.GDPR_CONSENT.NON_BEHAVIORAL.value)
    }
  }

  @Test
  fun initialize_withACConsentUnknown_doesNotAddGdprConsent() {
    mockStatic(ChartboostAdapterUtils::class.java).use { mockAdapterUtils ->
      mockAdapterUtils
        .`when`<ChartboostAdapterUtils.ConsentResult> {
          ChartboostAdapterUtils.hasACConsent(any(), eq(AD_TECHNOLOGY_PROVIDER_ID))
        }
        .thenReturn(ChartboostAdapterUtils.ConsentResult.UNKNOWN)

      ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)

      mockChartboost.verify({ Chartboost.addDataUseConsent(any(), any<GDPR>()) }, never())
    }
  }

  @Test
  fun initialize_updatesCoppaStatus() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )
    val coppaCaptor = argumentCaptor<COPPA>()

    ChartboostInitializer.getInstance().initialize(context, chartboostParams, listener)

    mockChartboost.verify { Chartboost.addDataUseConsent(eq(context), coppaCaptor.capture()) }
    assertThat(coppaCaptor.firstValue.consent).isTrue()
  }

  private companion object {
    const val TEST_APP_ID = "test_app_id"
    const val TEST_APP_SIGNATURE = "test_app_signature"
    const val TEST_LOCATION = "test_location"
    const val ERROR_CODE = 10
    const val ERROR_MESSAGE = "test_error_message"
  }
}
