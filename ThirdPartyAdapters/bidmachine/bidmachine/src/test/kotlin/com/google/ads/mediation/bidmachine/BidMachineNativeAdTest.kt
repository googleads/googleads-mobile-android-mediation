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
import android.os.Bundle
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
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import io.bidmachine.Ad
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.nativead.NativeAd
import io.bidmachine.format.nativead.NativeClickableView
import io.bidmachine.model.ImageData
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BidMachineNativeAdTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val mockBidMachineInstance = mock<BidMachineInstance>()
  private val mediaView = View(context)
  private val adChoiceView = View(context)
  private val iconDrawable = mock<Drawable>()
  private val iconUri = Uri.parse("https://example.com/icon.png")
  private val mockIcon =
    mock<ImageData> {
      on { getImage() } doReturn iconDrawable
      on { getLocalUri() } doReturn iconUri
    }
  private val mainImageDrawable = mock<Drawable>()
  private val mainImageUri = Uri.parse("file:///cache/main.png")
  private val mockMainImage =
    mock<ImageData> {
      on { getImage() } doReturn mainImageDrawable
      on { getLocalUri() } doReturn mainImageUri
    }
  private val mockNativeAd =
    mock<NativeAd> {
      on { getTitle() } doReturn TEST_TITLE
      on { getBody() } doReturn TEST_BODY
      on { getCallToAction() } doReturn TEST_CALL_TO_ACTION
      on { getAdvertiser() } doReturn TEST_ADVERTISER
      on { getStarRating() } doReturn TEST_STAR_RATING
      on { getIcon() } doReturn mockIcon
      on { getMainImage() } doReturn mockMainImage
      on { getMediaView() } doReturn mediaView
      on { hasVideoContent() } doReturn true
      on { getAdChoiceView() } doReturn adChoiceView
    }

  // Subject of testing.
  private val bidMachineNativeAd: BidMachineNativeAd = createNativeAd()

  // region newInstance tests
  @Test
  fun newInstance_createsPlacementWithPlacementId() {
    assertThat(bidMachineNativeAd.placement?.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun newInstance_withoutPlacementId_createsNoPlacement() {
    assertThat(createNativeAd(serverParameters = bundleOf()).placement).isNull()
  }

  // endregion

  // region load tests
  @Test
  fun loadWaterfallAd_loadsNativeWithPlacement() {
    bidMachineNativeAd.loadWaterfallAd(mockBidMachineInstance)

    verify(mockBidMachineInstance)
      .loadNative(same(bidMachineNativeAd.placement), same(bidMachineNativeAd))
  }

  @Test
  fun loadRtbAd_loadsNativeWithBidResponse() {
    bidMachineNativeAd.loadRtbAd(mockBidMachineInstance)

    verify(mockBidMachineInstance).loadNative(eq(TEST_BID_RESPONSE), same(bidMachineNativeAd))
  }

  // endregion

  // region AdLoadCallback tests
  @Test
  fun onAdLoaded_setsWatermarkAndListenerAndInvokesOnSuccess() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    verify(mockNativeAd as Ad).setExtras(eq(mapOf(WATERMARK_KEY to TEST_WATERMARK)))
    verify(mockNativeAd).setListener(same(bidMachineNativeAd))
    assertThat(nativeAdLoadCallback).hasSucceededWith(bidMachineNativeAd)
    assertThat(bidMachineNativeAd.overrideClickHandling).isTrue()
    assertThat(bidMachineNativeAd.overrideImpressionRecording).isTrue()
  }

  @Test
  fun onAdLoaded_mapsAllNativeAdAssets() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    assertThat(bidMachineNativeAd.headline).isEqualTo(TEST_TITLE)
    assertThat(bidMachineNativeAd.body).isEqualTo(TEST_BODY)
    assertThat(bidMachineNativeAd.callToAction).isEqualTo(TEST_CALL_TO_ACTION)
    assertThat(bidMachineNativeAd.advertiser).isEqualTo(TEST_ADVERTISER)
    assertThat(bidMachineNativeAd.starRating).isEqualTo(TEST_STAR_RATING)
    assertThat(bidMachineNativeAd.icon.drawable).isSameInstanceAs(iconDrawable)
    assertThat(bidMachineNativeAd.icon.uri).isEqualTo(iconUri)
    assertThat(bidMachineNativeAd.icon.scale).isEqualTo(1.0)
    assertThat(bidMachineNativeAd.images).hasSize(1)
    val mainImage = bidMachineNativeAd.images.single()
    assertThat(mainImage.drawable).isSameInstanceAs(mainImageDrawable)
    assertThat(mainImage.uri).isEqualTo(mainImageUri)
    assertThat(mainImage.scale).isEqualTo(1.0)
    assertThat(bidMachineNativeAd.adChoicesContent).isSameInstanceAs(adChoiceView)
    assertThat(bidMachineNativeAd.hasVideoContent()).isTrue()
  }

  @Test
  fun onAdLoaded_withUriOnlyMainImage_mapsImageUri() {
    whenever(mockMainImage.getImage()).thenReturn(null)

    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    assertThat(bidMachineNativeAd.images).hasSize(1)
    val mainImage = bidMachineNativeAd.images.single()
    assertThat(mainImage.drawable).isNull()
    assertThat(mainImage.uri).isEqualTo(mainImageUri)
  }

  @Test
  fun onAdLoaded_withoutVideo_setsHasVideoContentToFalse() {
    whenever(mockNativeAd.hasVideoContent()).thenReturn(false)

    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    assertThat(bidMachineNativeAd.hasVideoContent()).isFalse()
  }

  @Test
  fun onAdLoaded_withMissingAssets_leavesThemUnset() {
    val nativeAd =
      mock<NativeAd> {
        // Mockito returns 0.0 for an unstubbed Double?, while the SDK returns null for no rating.
        on { getStarRating() } doReturn null
        on { getMediaView() } doReturn mediaView
        on { getAdChoiceView() } doReturn adChoiceView
      }

    bidMachineNativeAd.onAdLoaded(nativeAd)

    assertThat(bidMachineNativeAd.headline).isNull()
    assertThat(bidMachineNativeAd.body).isNull()
    assertThat(bidMachineNativeAd.callToAction).isNull()
    assertThat(bidMachineNativeAd.advertiser).isNull()
    assertThat(bidMachineNativeAd.starRating).isNull()
    assertThat(bidMachineNativeAd.icon).isNull()
    assertThat(bidMachineNativeAd.images).isNull()
    assertThat(nativeAdLoadCallback).hasSucceededWith(bidMachineNativeAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bmError = createBMError()

    bidMachineNativeAd.onAdLoadFailed(bmError)

    assertThat(nativeAdLoadCallback)
      .hasFailedWith(AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN))
  }

  // endregion

  // region View tracking tests
  @Test
  fun trackViews_bindsNativeAdWithContainerAndClickableViews() {
    val container = FrameLayout(context)
    val titleView = View(context)
    val ctaView = View(context)
    val clickableViewsCaptor = argumentCaptor<List<NativeClickableView>>()
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.trackViews(
      container,
      mapOf("title" to titleView, "cta" to ctaView),
      mapOf("body" to View(context)),
    )

    verify(mockNativeAd).bind(same(container), clickableViewsCaptor.capture())
    assertThat(clickableViewsCaptor.firstValue.map { it.getView() })
      .containsExactly(titleView, ctaView)
  }

  @Test
  fun untrackView_unbindsNativeAd() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.untrackView(FrameLayout(context))

    verify(mockNativeAd).unbind()
    verify(mockNativeAd, never()).destroy()
  }

  @Test
  fun trackAndUntrackViews_beforeLoad_doNotThrow() {
    bidMachineNativeAd.trackViews(FrameLayout(context), mapOf(), mapOf())
    bidMachineNativeAd.untrackView(FrameLayout(context))
  }

  // endregion

  // region destroy tests
  @Test
  fun destroy_calledTwice_releasesNativeAdOnce() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.destroy()
    bidMachineNativeAd.destroy()

    verify(mockNativeAd).setListener(null)
    verify(mockNativeAd).destroy()
  }

  @Test
  fun destroy_beforeLoad_doesNotThrow() {
    bidMachineNativeAd.destroy()
  }

  // endregion

  // region NativeListener tests
  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.onAdImpression(mockNativeAd)

    assertThat(nativeAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClickedOnAdOpenedAndOnAdLeftApplication() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.onAdClicked(mockNativeAd)

    assertThat(nativeAdCallback.isClicked).isTrue()
    assertThat(nativeAdCallback.isOpened).isTrue()
    assertThat(nativeAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun listenerEvents_afterDestroy_doNotInvokeCallbacks() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)
    bidMachineNativeAd.destroy()

    bidMachineNativeAd.onAdImpression(mockNativeAd)
    bidMachineNativeAd.onAdClicked(mockNativeAd)

    assertThat(nativeAdCallback.isImpressionReported).isFalse()
    assertThat(nativeAdCallback.isClicked).isFalse()
    assertThat(nativeAdCallback.isOpened).isFalse()
    assertThat(nativeAdCallback.isLeftApplication).isFalse()
  }

  @Test
  fun onAdPresentedShowFailedAndExpired_doNotInvokeCallbacks() {
    bidMachineNativeAd.onAdLoaded(mockNativeAd)

    bidMachineNativeAd.onAdPresented(mockNativeAd)
    bidMachineNativeAd.onAdShowFailed(mockNativeAd, createBMError())
    bidMachineNativeAd.onAdExpired(mockNativeAd)

    assertThat(nativeAdCallback.isImpressionReported).isFalse()
    assertThat(nativeAdCallback.isClicked).isFalse()
    assertThat(nativeAdCallback.isOpened).isFalse()
  }

  // endregion

  private fun createNativeAd(
    serverParameters: Bundle = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID)
  ): BidMachineNativeAd {
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParameters,
        watermark = TEST_WATERMARK,
      )
    return BidMachineNativeAd.newInstance(adConfiguration, nativeAdLoadCallback).getOrThrow()
  }

  private companion object {
    const val TEST_TITLE = "testTitle"
    const val TEST_BODY = "testBody"
    const val TEST_CALL_TO_ACTION = "testCallToAction"
    const val TEST_ADVERTISER = "testAdvertiser"
    const val TEST_STAR_RATING = 4.5
  }
}
