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
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
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
import sg.bigo.ads.api.NativeAd
import sg.bigo.ads.api.NativeAdRequest

@RunWith(AndroidJUnit4::class)
class BigoNativeAdTest {
  // Subject of testing
  private lateinit var bigoNativeAd: BigoNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockNativeAdCallback: MediationNativeAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockNativeAdCallback
    }
  private val mockNativeAdRequest = mock<NativeAdRequest>()
  private val mockNativeAdLoader = mock<BigoNativeAdLoaderWrapper>()
  private var mockBigoFactory =
    mock<SdkFactory> {
      on {
        createNativeAdRequest(eq(TEST_BID_RESPONSE), eq(TEST_SLOT_ID), eq(TEST_WATERMARK))
      } doReturn mockNativeAdRequest
      on { createNativeAdLoader() } doReturn mockNativeAdLoader
    }

  @Before
  fun setUp() {
    val serverParams = bundleOf(SLOT_ID_KEY to TEST_SLOT_ID)
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParams,
        watermark = TEST_WATERMARK,
      )
    BigoFactory.delegate = mockBigoFactory
    BigoNativeAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess { bigoNativeAd = it }
  }

  @Test
  fun loadAd_invokesSetAdLoaderListenerSetAdInteractorListenerAndLoadAd() {
    bigoNativeAd.loadAd(TEST_VERSION_STRING)

    inOrder(mockNativeAdLoader) {
      verify(mockNativeAdLoader).initializeAdLoader(bigoNativeAd, TEST_VERSION_STRING)
      verify(mockNativeAdLoader).loadAd(mockNativeAdRequest)
    }
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    val mockNativeAd =
      mock<NativeAd> {
        on { title } doReturn "testTitle"
        on { description } doReturn "testDescription"
        on { callToAction } doReturn "testCallToAction"
        on { advertiser } doReturn "testAdvertiser"
        on { creativeType } doReturn NativeAd.CreativeType.IMAGE
        on { mediaContentAspectRatio } doReturn 1.0f
      }

    bigoNativeAd.onAdLoaded(mockNativeAd)

    verify(mockAdLoadCallback).onSuccess(bigoNativeAd)
    assertThat(bigoNativeAd.headline).isEqualTo("testTitle")
    assertThat(bigoNativeAd.body).isEqualTo("testDescription")
    assertThat(bigoNativeAd.callToAction).isEqualTo("testCallToAction")
    assertThat(bigoNativeAd.advertiser).isEqualTo("testAdvertiser")
    assertThat(bigoNativeAd.hasVideoContent()).isFalse()
    assertThat(bigoNativeAd.mediaContentAspectRatio).isEqualTo(1.0f)
    assertThat(bigoNativeAd.overrideClickHandling).isTrue()
    assertThat(bigoNativeAd.overrideImpressionRecording).isTrue()
  }

  @Test
  fun onError_invokesOnFailure() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)

    bigoNativeAd.onError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdError_throwsNoException() {
    bigoNativeAd.onAdError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdImpression()

    verify(mockNativeAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdClicked()

    verify(mockNativeAdCallback).reportAdClicked()
  }

  @Test
  fun onAdOpened_invokesOnAdOpened() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdOpened()

    verify(mockNativeAdCallback).onAdOpened()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdClosed()

    verify(mockNativeAdCallback).onAdClosed()
  }

  @Test
  fun onVideoStart_throwsNoException() {
    bigoNativeAd.onVideoStart()
  }

  @Test
  fun onVideoPlay_invokesOnVideoPlay() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onVideoPlay()

    verify(mockNativeAdCallback).onVideoPlay()
  }

  @Test
  fun onVideoPause_invokesOnVideoPause() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onVideoPause()

    verify(mockNativeAdCallback).onVideoPause()
  }

  @Test
  fun onVideoEnd_invokesOnVideoComplete() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onVideoEnd()

    verify(mockNativeAdCallback).onVideoComplete()
  }

  @Test
  fun onMuteChange_invokesOnVideoMute() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onMuteChange(true)

    verify(mockNativeAdCallback).onVideoMute()
  }

  @Test
  fun onMuteChange_invokesOnVideoUnmute() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onMuteChange(false)

    verify(mockNativeAdCallback).onVideoUnmute()
  }

  private companion object {
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_ERROR_CODE = 123
    const val TEST_ERROR_MSG = "testError"
    const val TEST_VERSION_STRING = "testVersionString"
  }
}
