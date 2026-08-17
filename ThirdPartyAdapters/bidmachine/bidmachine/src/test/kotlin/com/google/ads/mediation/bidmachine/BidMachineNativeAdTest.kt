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

package com.google.ads.mediation.bidmachine

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_EMPTY_NATIVE_AD_DATA
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_EMPTY_NATIVE_AD_DATA
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import io.bidmachine.ImageData
import io.bidmachine.RendererConfiguration
import io.bidmachine.nativead.NativeAd
import io.bidmachine.nativead.NativePublicData
import io.bidmachine.nativead.NativeRequest
import io.bidmachine.nativead.view.NativeMediaView
import io.bidmachine.utils.BMError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BidMachineNativeAdTest {
  // Subject of testing.
  private lateinit var bidMachineNativeAd: BidMachineNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val mockNativeRequest = mock<NativeRequest> { on { isExpired } doReturn false }
  private val nativePublicData =
    mock<NativePublicData> {
      on { title } doReturn TEST_TITLE
      on { description } doReturn TEST_DESCRIPTION
      on { callToAction } doReturn TEST_CALL_TO_ACTION
      on { icon } doReturn mock()
      on { rating } doReturn 5.0f
    }
  private val mockNativeAd = mock<NativeAd> { on { adData } doReturn nativePublicData }

  @Before
  fun setUp() {
    val serverParams = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID)
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParams,
        watermark = TEST_WATERMARK,
      )
    BidMachineNativeAd.newInstance(adConfiguration, nativeAdLoadCallback).onSuccess {
      bidMachineNativeAd = it
    }
  }

  @Test
  fun newInstance_correctlyCreatesAdPlacementConfig() {
    assertThat(bidMachineNativeAd.adPlacementConfig.placementId).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun loadWaterfallAd_invokesBidMachineRequest() {
    val mockNativeRequestBuilder = configureNativeRequestBuilder()

    bidMachineNativeAd.loadWaterfallAd(mockNativeAd)

    verify(mockNativeAd).setListener(eq(bidMachineNativeAd))
    verify(mockNativeRequestBuilder, never()).setBidPayload(any())
    verify(mockNativeRequestBuilder).setListener(eq(bidMachineNativeAd))
    verify(mockNativeRequest).request(eq(context))
  }

  @Test
  fun loadRtbAd_invokesBidMachineRequest() {
    val mockNativeRequestBuilder = configureNativeRequestBuilder()

    bidMachineNativeAd.loadRtbAd(mockNativeAd)

    verify(mockNativeAd).setListener(eq(bidMachineNativeAd))
    verify(mockNativeRequestBuilder).setBidPayload(eq(TEST_BID_RESPONSE))
    verify(mockNativeRequestBuilder).setListener(eq(bidMachineNativeAd))
    verify(mockNativeRequest).request(eq(context))
  }

  @Test
  fun trackViews_registersViewsWithBidMachineNativeAd() {
    val container = FrameLayout(context)
    val view1 = View(context)
    val view2 = View(context)
    val clickableAssetViews = mapOf("asset1" to view1)
    val nonClickableAssetViews = mapOf("asset2" to view2)
    bidMachineNativeAd.loadRtbAd(mockNativeAd)
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.trackViews(container, clickableAssetViews, nonClickableAssetViews)

    verify(mockNativeAd)
      .registerView(
        eq(container),
        eq(bidMachineNativeAd.adChoicesContent),
        any<NativeMediaView>(),
        eq(clickableAssetViews.values.toSet()),
      )
  }

  @Test
  fun onRequestSuccess_invokesLoad() {
    bidMachineNativeAd.loadRtbAd(mockNativeAd)
    val rendererConfigCaptor = argumentCaptor<RendererConfiguration>()

    bidMachineNativeAd.onRequestSuccess(mockNativeRequest, mock())

    verify(mockNativeAd).setRendererConfiguration(rendererConfigCaptor.capture())
    assertThat(rendererConfigCaptor.firstValue.getWatermark()).isEqualTo(TEST_WATERMARK)
    verify(mockNativeAd).load(mockNativeRequest)
  }

  @Test
  fun onRequestSuccess_withExpiredAdRequest_invokesOnFailure() {
    whenever(mockNativeRequest.isExpired) doReturn true
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    bidMachineNativeAd.loadRtbAd(mockNativeAd)

    bidMachineNativeAd.onRequestSuccess(mockNativeRequest, mock())

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockNativeRequest).destroy()
    verify(mockNativeAd, never()).load(mockNativeRequest)
  }

  @Test
  fun onRequestFailed_invokesOnFailure() {
    val bMError = BMError.BMServerNoFill
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)

    bidMachineNativeAd.onRequestFailed(mockNativeRequest, bMError)

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockNativeRequest).destroy()
  }

  @Test
  fun onRequestExpired_invokesOnFailure() {
    val expectedAdError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)

    bidMachineNativeAd.onRequestExpired(mockNativeRequest)

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockNativeRequest).destroy()
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    configureNativeRequestBuilder()
    bidMachineNativeAd.loadRtbAd(mockNativeAd)

    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    assertThat(nativeAdLoadCallback).hasSucceededWith(bidMachineNativeAd)
  }

  @Test
  fun onAdLoaded_mapsAllNativeAdAssets() {
    val mockDrawable = mock<Drawable>()
    val mockUri = mock<Uri>()
    val mockImageData =
      mock<ImageData> {
        on { image } doReturn mockDrawable
        on { localUri } doReturn mockUri
      }
    whenever(nativePublicData.icon) doReturn mockImageData
    bidMachineNativeAd.loadRtbAd(mockNativeAd)

    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    assertThat(bidMachineNativeAd.headline).isEqualTo(TEST_TITLE)
    assertThat(bidMachineNativeAd.body).isEqualTo(TEST_DESCRIPTION)
    assertThat(bidMachineNativeAd.callToAction).isEqualTo(TEST_CALL_TO_ACTION)
    assertThat(bidMachineNativeAd.starRating).isEqualTo(5.0)
    val icon = checkNotNull(bidMachineNativeAd.icon)
    assertThat(icon.drawable).isEqualTo(mockDrawable)
    assertThat(icon.uri).isEqualTo(mockUri)
  }

  @Test
  fun onAdLoaded_withNullAdData_invokesOnFailureWithError106() {
    whenever(mockNativeAd.adData) doReturn null
    val expectedAdError =
      AdError(ERROR_CODE_EMPTY_NATIVE_AD_DATA, ERROR_MSG_EMPTY_NATIVE_AD_DATA, ADAPTER_ERROR_DOMAIN)
    bidMachineNativeAd.loadRtbAd(mockNativeAd)

    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bMError = BMError.AlreadyShown
    val expectedAdError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    bidMachineNativeAd.loadRtbAd(mockNativeAd)

    bidMachineNativeAd.onAdLoadFailed(mockNativeAd, bMError)

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
    verify(mockNativeAd).destroy()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    configureNativeRequestBuilder()
    bidMachineNativeAd.loadRtbAd(mockNativeAd)
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.onAdImpression(mockNativeAd)

    assertThat(nativeAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesOnAdOpenedOnAdLeftApplicationAndReportAdClicked() {
    configureNativeRequestBuilder()
    bidMachineNativeAd.loadRtbAd(mockNativeAd)
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.onAdClicked(mockNativeAd)

    assertThat(nativeAdCallback.isOpened).isTrue()
    assertThat(nativeAdCallback.isLeftApplication).isTrue()
    assertThat(nativeAdCallback.isClicked).isTrue()
  }

  private fun configureNativeRequestBuilder(): NativeRequest.Builder {
    val mockNativeRequestBuilder =
      mock<NativeRequest.Builder> {
        on { setBidPayload(eq(TEST_BID_RESPONSE)) } doReturn it
        on { setListener(any()) } doReturn it
        on { build() } doReturn mockNativeRequest
      }
    bidMachineNativeAd.nativeRequestBuilder = mockNativeRequestBuilder
    return mockNativeRequestBuilder
  }

  private companion object {
    const val TEST_TITLE = "testTitle"
    const val TEST_DESCRIPTION = "testDescription"
    const val TEST_CALL_TO_ACTION = "testCallToAction"
  }
}
