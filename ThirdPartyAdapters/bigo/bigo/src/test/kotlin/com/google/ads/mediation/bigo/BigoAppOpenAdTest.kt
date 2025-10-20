// Copyright 2025 Google LLC
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

package com.google.ads.mediation.bigo

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.SplashAd
import sg.bigo.ads.api.SplashAdRequest

@RunWith(AndroidJUnit4::class)
class BigoAppOpenAdTest {
  // Subject of testing
  private lateinit var bigoAppOpenAd: BigoAppOpenAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockAppOpenAdCallback: MediationAppOpenAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockAppOpenAdCallback
    }
  private val mockSplashAdRequest = mock<SplashAdRequest>()
  private val mockSplashAdLoader = mock<BigoSplashAdLoaderWrapper>()
  private var mockBigoFactory =
    mock<SdkFactory> {
      on {
        createSplashAdRequest(eq(TEST_BID_RESPONSE), eq(TEST_SLOT_ID), eq(TEST_WATERMARK))
      } doReturn mockSplashAdRequest
      on { createSplashAdLoader() } doReturn mockSplashAdLoader
    }

  @Before
  fun setUp() {
    val serverParams = bundleOf(SLOT_ID_KEY to TEST_SLOT_ID)
    val adConfiguration =
      createMediationAppOpenAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParams,
        watermark = TEST_WATERMARK,
      )
    BigoFactory.delegate = mockBigoFactory
    BigoAppOpenAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess { bigoAppOpenAd = it }
  }

  @Test
  fun loadAd_invokesWrapperInitializeAdLoaderAndLoadAd() {
    bigoAppOpenAd.loadAd(TEST_VERSION_STRING)

    inOrder(mockSplashAdLoader) {
      verify(mockSplashAdLoader).initializeAdLoader(bigoAppOpenAd, TEST_VERSION_STRING)
      verify(mockSplashAdLoader).loadAd(mockSplashAdRequest)
    }
  }

  @Test
  fun onAdLoaded_setsListenerAndInvokesOnSuccess() {
    val mockSplashAd = mock<SplashAd>()

    bigoAppOpenAd.onAdLoaded(mockSplashAd)

    verify(mockSplashAd).setAdInteractionListener(bigoAppOpenAd)
    verify(mockAdLoadCallback).onSuccess(bigoAppOpenAd)
  }

  @Test
  fun onError_invokesOnFailure() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)

    bigoAppOpenAd.onError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun showAd_invokesShow() {
    val mockSplashAd = mock<SplashAd>()
    bigoAppOpenAd.onAdLoaded(mockSplashAd)

    bigoAppOpenAd.showAd(context)

    verify(mockSplashAd).show()
  }

  @Test
  fun onAdError_invokesOnAdFailedToShow() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)
    bigoAppOpenAd.onAdLoaded(mock())

    bigoAppOpenAd.onAdError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    verify(mockAppOpenAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bigoAppOpenAd.onAdLoaded(mock())

    bigoAppOpenAd.onAdImpression()

    verify(mockAppOpenAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bigoAppOpenAd.onAdLoaded(mock())

    bigoAppOpenAd.onAdClicked()

    verify(mockAppOpenAdCallback).reportAdClicked()
  }

  @Test
  fun onAdOpened_invokesOnAdOpened() {
    bigoAppOpenAd.onAdLoaded(mock())

    bigoAppOpenAd.onAdOpened()

    verify(mockAppOpenAdCallback).onAdOpened()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bigoAppOpenAd.onAdLoaded(mock())

    bigoAppOpenAd.onAdClosed()

    verify(mockAppOpenAdCallback).onAdClosed()
  }

  @Test
  fun onAdSkipped_throwsNoException() {
    bigoAppOpenAd.onAdSkipped()
  }

  @Test
  fun onAdFinished_throwsNoException() {
    bigoAppOpenAd.onAdFinished()
  }

  private companion object {
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_ERROR_CODE = 123
    const val TEST_ERROR_MSG = "testError"
    const val TEST_VERSION_STRING = "testVersionString"
  }
}
