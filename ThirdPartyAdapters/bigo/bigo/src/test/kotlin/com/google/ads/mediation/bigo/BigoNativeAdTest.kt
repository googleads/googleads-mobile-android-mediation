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
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_BODY
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_CALL_TO_ACTION
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_HEADLINE
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdOptionsView
import sg.bigo.ads.api.AdTag
import sg.bigo.ads.api.MediaView
import sg.bigo.ads.api.NativeAd
import sg.bigo.ads.api.NativeAdRequest
import sg.bigo.ads.api.VideoController

@RunWith(AndroidJUnit4::class)
class BigoNativeAdTest {
  // Subject of testing
  private lateinit var bigoNativeAd: BigoNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val adLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val mockNativeAdRequest = mock<NativeAdRequest>()
  private val mockNativeAdLoader = mock<BigoNativeAdLoaderWrapper>()
  private val mockBigoFactory =
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
    BigoNativeAd.newInstance(adConfiguration, adLoadCallback).onSuccess { bigoNativeAd = it }
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

    assertThat(adLoadCallback).hasSucceededWith(bigoNativeAd)
    assertThat(bigoNativeAd.headline).isEqualTo("testTitle")
    assertThat(bigoNativeAd.body).isEqualTo("testDescription")
    assertThat(bigoNativeAd.callToAction).isEqualTo("testCallToAction")
    assertThat(bigoNativeAd.advertiser).isEqualTo("testAdvertiser")
    assertThat(bigoNativeAd.hasVideoContent()).isFalse()
    assertThat(bigoNativeAd.mediaContentAspectRatio).isEqualTo(1.0f)
    assertThat(bigoNativeAd.adChoicesContent).isInstanceOf(AdOptionsView::class.java)
    assertThat(bigoNativeAd.overrideClickHandling).isTrue()
    assertThat(bigoNativeAd.overrideImpressionRecording).isTrue()
  }

  @Test
  fun onError_invokesOnFailure() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)

    bigoNativeAd.onError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    assertThat(adLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdImpression()

    assertThat(nativeAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdClicked()

    assertThat(nativeAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdOpened_invokesOnAdOpened() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdOpened()

    assertThat(nativeAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onAdClosed()

    assertThat(nativeAdCallback.isClosed).isTrue()
  }

  @Test
  fun onVideoPlay_invokesOnVideoPlay() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onVideoPlay()

    assertThat(nativeAdCallback.isVideoPlaying).isTrue()
  }

  @Test
  fun onVideoPause_invokesOnVideoPause() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onVideoPause()

    assertThat(nativeAdCallback.isVideoPaused).isTrue()
  }

  @Test
  fun onVideoEnd_invokesOnVideoComplete() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onVideoEnd()

    assertThat(nativeAdCallback.isVideoCompleted).isTrue()
  }

  @Test
  fun onMuteChange_invokesOnVideoMute() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onMuteChange(true)

    assertThat(nativeAdCallback.isVideoMuted).isTrue()
  }

  @Test
  fun onMuteChange_invokesOnVideoUnmute() {
    bigoNativeAd.onAdLoaded(mock())

    bigoNativeAd.onMuteChange(false)

    assertThat(nativeAdCallback.isVideoUnmuted).isTrue()
  }

  @Test
  fun trackViews_tagsViewsAndRegistersWithBigoNativeAd() {
    val mockNativeAd = mock<NativeAd>()
    bigoNativeAd.onAdLoaded(mockNativeAd)

    val container = FrameLayout(context)
    val headline = TextView(context)
    val body = TextView(context)
    val cta = Button(context)
    val clickableAssetViews =
      mapOf(ASSET_HEADLINE to headline, ASSET_BODY to body, ASSET_CALL_TO_ACTION to cta)
    val nonClickableAssetViews = emptyMap<String, View>()

    val mediaViewCaptor = argumentCaptor<MediaView>()
    val iconViewCaptor = argumentCaptor<ImageView>()
    val adOptionsViewCaptor = argumentCaptor<AdOptionsView>()

    bigoNativeAd.trackViews(container, clickableAssetViews, nonClickableAssetViews)

    verify(mockNativeAd)
      .registerViewForInteraction(
        eq(container),
        mediaViewCaptor.capture(),
        iconViewCaptor.capture(),
        adOptionsViewCaptor.capture(),
        eq(clickableAssetViews.values.toList()),
      )

    assertThat(container.tag).isEqualTo(AdTag.NATIVE_AD_VIEW)
    assertThat(iconViewCaptor.firstValue.tag).isEqualTo(AdTag.ICON_VIEW)
    assertThat(mediaViewCaptor.firstValue.tag).isEqualTo(AdTag.MEDIA_VIEW)
    assertThat(adOptionsViewCaptor.firstValue.tag).isEqualTo(AdTag.OPTION_VIEW)
    assertThat(headline.tag).isEqualTo(AdTag.TITLE)
    assertThat(body.tag).isEqualTo(AdTag.DESCRIPTION)
    assertThat(cta.tag).isEqualTo(AdTag.CALL_TO_ACTION)
  }

  @Test
  fun onAdLoaded_withVideoCreativeType_setsHasVideoContentAndVideoLifeCallback() {
    val mockVideoController = mock<VideoController>()
    val mockNativeAd =
      mock<NativeAd> {
        on { creativeType } doReturn NativeAd.CreativeType.VIDEO
        on { videoController } doReturn mockVideoController
      }

    bigoNativeAd.onAdLoaded(mockNativeAd)

    assertThat(bigoNativeAd.hasVideoContent()).isTrue()
    verify(mockVideoController).videoLifeCallback = bigoNativeAd
  }

  @Test
  fun onAdLoaded_withIcon_setsIconImage() {
    val mockNativeAd = mock<NativeAd> { on { hasIcon() } doReturn true }

    bigoNativeAd.onAdLoaded(mockNativeAd)

    assertThat(bigoNativeAd.icon).isNotNull()
  }

  @Test
  fun onAdLoaded_setsMediaView() {
    val mockNativeAd = mock<NativeAd>()

    bigoNativeAd.onAdLoaded(mockNativeAd)

    assertThat(bigoNativeAd.adChoicesContent).isInstanceOf(AdOptionsView::class.java)
  }

  @Test
  fun newInstance_withMissingSlotId_invokesOnFailure() {
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = bundleOf(),
        watermark = TEST_WATERMARK,
      )
    val expectedAdError =
      BigoUtils.getGmaAdError(
        BigoMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
        BigoMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
        BigoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )

    val result = BigoNativeAd.newInstance(adConfiguration, adLoadCallback)

    assertThat(result.isFailure).isTrue()
    assertThat(adLoadCallback).hasFailedWith(expectedAdError)
  }

  private companion object {
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_ERROR_CODE = 123
    const val TEST_ERROR_MSG = "testError"
    const val TEST_VERSION_STRING = "testVersionString"
  }
}
