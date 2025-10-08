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
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
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
import sg.bigo.ads.ad.banner.BigoAdView
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdSize
import sg.bigo.ads.api.BannerAdRequest

@RunWith(AndroidJUnit4::class)
class BigoBannerAdTest {
  // Subject of testing
  private lateinit var bigoBannerAd: BigoBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockBannerAdCallback: MediationBannerAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }
  private val mockBannerAdRequest = mock<BannerAdRequest>()
  private val mockBigoAdView = mock<BigoAdView>()
  private var mockBigoFactory =
    mock<SdkFactory> {
      on {
        createBannerAdRequest(eq(TEST_BID_RESPONSE), eq(TEST_SLOT_ID), eq(AdSize.BANNER))
      } doReturn mockBannerAdRequest
      on { createBigoAdView(context) } doReturn mockBigoAdView
    }

  @Before
  fun setUp() {
    val serverParams = bundleOf(SLOT_ID_KEY to TEST_SLOT_ID)
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParams,
        watermark = TEST_WATERMARK,
        adSize = com.google.android.gms.ads.AdSize.BANNER,
      )
    BigoFactory.delegate = mockBigoFactory
    BigoBannerAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess { bigoBannerAd = it }
  }

  @Test
  fun loadAd_invokesSetAdLoaderListenerSetAdInteractorListenerAndLoadAd() {
    bigoBannerAd.loadAd()

    inOrder(mockBigoAdView) {
      verify(mockBigoAdView).setAdLoadListener(bigoBannerAd)
      verify(mockBigoAdView).setAdInteractionListener(bigoBannerAd)
      verify(mockBigoAdView).loadAd(mockBannerAdRequest)
    }
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    bigoBannerAd.onAdLoaded(mockBigoAdView)

    verify(mockAdLoadCallback).onSuccess(bigoBannerAd)
  }

  @Test
  fun onError_invokesOnFailure() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)

    bigoBannerAd.onError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdError_throwsNoException() {
    bigoBannerAd.onAdError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bigoBannerAd.onAdLoaded(mock())

    bigoBannerAd.onAdImpression()

    verify(mockBannerAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bigoBannerAd.onAdLoaded(mock())

    bigoBannerAd.onAdClicked()

    verify(mockBannerAdCallback).reportAdClicked()
  }

  @Test
  fun onAdOpened_invokesOnAdOpened() {
    bigoBannerAd.onAdLoaded(mock())

    bigoBannerAd.onAdOpened()

    verify(mockBannerAdCallback).onAdOpened()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bigoBannerAd.onAdLoaded(mock())

    bigoBannerAd.onAdClosed()

    verify(mockBannerAdCallback).onAdClosed()
  }

  private companion object {
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_ERROR_CODE = 123
    const val TEST_ERROR_MSG = "testError"
  }
}
