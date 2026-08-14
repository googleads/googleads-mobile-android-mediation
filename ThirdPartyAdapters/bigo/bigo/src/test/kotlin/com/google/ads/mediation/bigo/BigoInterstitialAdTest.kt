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
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val adLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val mockInterstitialAdRequest = mock<InterstitialAdRequest>()
  private val mockInterstitialAdLoader = mock<BigoInterstitialAdLoaderWrapper>()
  private val mockBigoFactory =
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
    BigoInterstitialAd.newInstance(adConfiguration, adLoadCallback).onSuccess {
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
    assertThat(adLoadCallback).hasSucceededWith(bigoInterstitialAd)
  }

  @Test
  fun onError_invokesOnFailure() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)

    bigoInterstitialAd.onError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    assertThat(adLoadCallback).hasFailedWith(expectedAdError)
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

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdImpression()

    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdClicked()

    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdOpened_invokesOnAdOpened() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdOpened()

    assertThat(interstitialAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bigoInterstitialAd.onAdLoaded(mock())

    bigoInterstitialAd.onAdClosed()

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  private companion object {
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_ERROR_CODE = 123
    const val TEST_ERROR_MSG = "testError"
    const val TEST_VERSION_STRING = "testVersionString"
  }
}
