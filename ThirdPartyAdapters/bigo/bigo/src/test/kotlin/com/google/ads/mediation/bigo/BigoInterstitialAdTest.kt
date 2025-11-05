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
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
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
import sg.bigo.ads.api.InterstitialAd
import sg.bigo.ads.api.InterstitialAdRequest

@RunWith(AndroidJUnit4::class)
class BigoInterstitialAdTest {
  // Subject of testing
  private lateinit var bigoInterstitialAd: BigoInterstitialAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }
  private val mockInterstitialAdRequest = mock<InterstitialAdRequest>()
  private val mockInterstitialAdLoader = mock<BigoInterstitialAdLoaderWrapper>()
  private var mockBigoFactory =
    mock<SdkFactory> {
      on {
        createInterstitialAdRequest(eq(TEST_BID_RESPONSE), eq(TEST_SLOT_ID), eq(TEST_WATERMARK))
      } doReturn mockInterstitialAdRequest
      on { createInterstitialAdLoader() } doReturn mockInterstitialAdLoader
    }

  @Before
  fun setUp() {
    val serverParams = bundleOf(SLOT_ID_KEY to TEST_SLOT_ID)
    val adConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParams,
        watermark = TEST_WATERMARK,
      )
    BigoFactory.delegate = mockBigoFactory
    BigoInterstitialAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess {
      bigoInterstitialAd = it
    }
  }

  @Test
  fun loadAd_invokesWrapperInitializeAdLoaderAndLoadAd() {
    bigoInterstitialAd.loadAd(TEST_VERSION_STRING)

    inOrder(mockInterstitialAdLoader) {
      verify(mockInterstitialAdLoader).initializeAdLoader(bigoInterstitialAd, TEST_VERSION_STRING)
      verify(mockInterstitialAdLoader).loadAd(mockInterstitialAdRequest)
    }
  }

  @Test
  fun onAdLoaded_setsListenerAndInvokesOnSuccess() {
    val mockInterstitialAd = mock<InterstitialAd>()

    bigoInterstitialAd.onAdLoaded(mockInterstitialAd)

    verify(mockInterstitialAd).setAdInteractionListener(bigoInterstitialAd)
    verify(mockAdLoadCallback).onSuccess(bigoInterstitialAd)
  }

  @Test
  fun onError_invokesOnFailure() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)

    bigoInterstitialAd.onError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun showAd_invokesShow() {
    val mockInterstitialAd = mock<InterstitialAd>()
    bigoInterstitialAd.onAdLoaded(mockInterstitialAd)

    bigoInterstitialAd.showAd(context)

    verify(mockInterstitialAd).show()
  }

  @Test
  fun onAdError_invokesOnAdFailedToShow() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdImpression()

    verify(mockInterstitialAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdClicked()

    verify(mockInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAdOpened_invokesOnAdOpened() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdOpened()

    verify(mockInterstitialAdCallback).onAdOpened()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdClosed()

    verify(mockInterstitialAdCallback).onAdClosed()
  }

  private companion object {
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_ERROR_CODE = 123
    const val TEST_ERROR_MSG = "testError"
    const val TEST_VERSION_STRING = "testVersionString"
  }
}
