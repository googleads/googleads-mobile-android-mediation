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
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.bannerad.BannerView
import io.bidmachine.model.BannerAdSize
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class BidMachineBannerAdTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private val mockBidMachineInstance = mock<BidMachineInstance>()
  private val mockBannerView = mock<BannerView>()

  // Subject of testing.
  private val bidMachineBannerAd: BidMachineBannerAd = createBannerAd()

  // region newInstance tests
  @Test
  fun newInstance_createsPlacementWithPlacementId() {
    assertThat(bidMachineBannerAd.placement?.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun newInstance_withoutPlacementId_createsNoPlacement() {
    val bannerAd = createBannerAd(serverParameters = bundleOf())

    assertThat(bannerAd.placement).isNull()
  }

  @Test
  fun newInstance_withBannerSize_mapsToBannerSize() {
    assertThat(createBannerAd(AdSize.BANNER).bannerAdSize).isEqualTo(BannerAdSize.Banner)
  }

  @Test
  fun newInstance_withMediumRectangleSize_mapsToMediumRectangleSize() {
    assertThat(createBannerAd(AdSize.MEDIUM_RECTANGLE).bannerAdSize)
      .isEqualTo(BannerAdSize.MediumRectangle)
  }

  @Test
  fun newInstance_withLeaderboardSize_mapsToLeaderboardSize() {
    assertThat(createBannerAd(AdSize.LEADERBOARD).bannerAdSize)
      .isEqualTo(BannerAdSize.Leaderboard)
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xhdpi")
  fun newInstance_withCustomSize_mapsToAdaptiveSize() {
    val bannerAdSize = createBannerAd(AdSize(330, 60)).bannerAdSize

    assertThat(bannerAdSize).isEqualTo(BannerAdSize.adaptive(330, 60))
    assertThat(bannerAdSize.isAdaptive).isTrue()
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xhdpi")
  fun newInstance_withFullWidthSize_mapsToScreenWidthInDp() {
    val bannerAdSize = createBannerAd(AdSize(AdSize.FULL_WIDTH, 50)).bannerAdSize

    assertThat(bannerAdSize).isEqualTo(BannerAdSize.adaptive(360, 50))
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xhdpi")
  fun newInstance_withZeroSize_mapsToScreenWidthWithoutHeightLimit() {
    val bannerAdSize = createBannerAd(AdSize(0, 0)).bannerAdSize

    assertThat(bannerAdSize).isEqualTo(BannerAdSize.adaptive(360, 0))
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xhdpi")
  fun newInstance_withFullWidthAndAutoHeight_mapsToScreenWidthWithoutHeightLimit() {
    val bannerAdSize =
      createBannerAd(AdSize(AdSize.FULL_WIDTH, AdSize.AUTO_HEIGHT)).bannerAdSize

    assertThat(bannerAdSize).isEqualTo(BannerAdSize.adaptive(360, 0))
  }

  @Test
  fun newInstance_withAutoHeight_mapsToAdaptiveSizeWithoutHeightLimit() {
    val bannerAdSize = createBannerAd(AdSize(330, AdSize.AUTO_HEIGHT)).bannerAdSize

    assertThat(bannerAdSize).isEqualTo(BannerAdSize.adaptive(330, 0))
  }

  @Test
  fun newInstance_withZeroHeight_mapsToAdaptiveSizeWithoutHeightLimit() {
    val bannerAdSize = createBannerAd(AdSize(330, 0)).bannerAdSize

    assertThat(bannerAdSize).isEqualTo(BannerAdSize.adaptive(330, 0))
  }

  @Test
  fun newInstance_doesNotInvokeLoadCallback() {
    assertThat(bannerAdLoadCallback.isSuccess).isFalse()
    assertThat(bannerAdLoadCallback.isFailure).isFalse()
  }

  // endregion

  // region load tests
  @Test
  fun loadWaterfallAd_loadsBannerWithPlacementAndSize() {
    bidMachineBannerAd.loadWaterfallAd(mockBidMachineInstance)

    verify(mockBidMachineInstance)
      .loadBanner(
        same(bidMachineBannerAd.placement),
        eq(BannerAdSize.Banner),
        same(bidMachineBannerAd),
      )
  }

  @Test
  fun loadRtbAd_loadsBannerWithBidResponseAndSize() {
    bidMachineBannerAd.loadRtbAd(mockBidMachineInstance)

    verify(mockBidMachineInstance)
      .loadBanner(eq(TEST_BID_RESPONSE), eq(BannerAdSize.Banner), same(bidMachineBannerAd))
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-xhdpi")
  fun loadRtbAd_withZeroSize_loadsBannerWithScreenWidth() {
    val bannerAd = createBannerAd(AdSize(0, 0))

    bannerAd.loadRtbAd(mockBidMachineInstance)

    verify(mockBidMachineInstance)
      .loadBanner(eq(TEST_BID_RESPONSE), eq(BannerAdSize.adaptive(360, 0)), same(bannerAd))
  }

  // endregion

  // region AdLoadCallback tests
  @Test
  fun onAdLoaded_setsWatermarkAndListenerAndInvokesOnSuccess() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    verify(mockBannerView).setExtras(eq(mapOf(WATERMARK_KEY to TEST_WATERMARK)))
    verify(mockBannerView).setListener(same(bidMachineBannerAd))
    assertThat(bannerAdLoadCallback).hasSucceededWith(bidMachineBannerAd)
  }

  @Test
  fun getView_afterLoad_returnsBannerView() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    assertThat(bidMachineBannerAd.view).isSameInstanceAs(mockBannerView)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bmError = createBMError()

    bidMachineBannerAd.onAdLoadFailed(bmError)

    assertThat(bannerAdLoadCallback)
      .hasFailedWith(AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN))
  }

  // endregion

  // region BannerListener tests
  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    bidMachineBannerAd.onAdImpression(mockBannerView)

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClickedOnAdOpenedAndOnAdLeftApplication() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    bidMachineBannerAd.onAdClicked(mockBannerView)

    assertThat(bannerAdCallback.isClicked).isTrue()
    assertThat(bannerAdCallback.isOpened).isTrue()
    assertThat(bannerAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun listenerEvents_beforeLoad_doNotInvokeCallbacks() {
    bidMachineBannerAd.onAdImpression(mockBannerView)
    bidMachineBannerAd.onAdClicked(mockBannerView)

    assertThat(bannerAdCallback.isImpressionReported).isFalse()
    assertThat(bannerAdCallback.isClicked).isFalse()
  }

  @Test
  fun onAdPresentedShowFailedAndExpired_doNotInvokeCallbacks() {
    bidMachineBannerAd.onAdLoaded(mockBannerView)

    bidMachineBannerAd.onAdPresented(mockBannerView)
    bidMachineBannerAd.onAdShowFailed(mockBannerView, createBMError())
    bidMachineBannerAd.onAdExpired(mockBannerView)

    assertThat(bannerAdCallback.isImpressionReported).isFalse()
    assertThat(bannerAdCallback.isClicked).isFalse()
    assertThat(bannerAdCallback.isOpened).isFalse()
  }

  // endregion

  private fun createBannerAd(
    adSize: AdSize = AdSize.BANNER,
    serverParameters: Bundle = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID),
  ): BidMachineBannerAd {
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        adSize = adSize,
        serverParameters = serverParameters,
        watermark = TEST_WATERMARK,
      )
    return BidMachineBannerAd.newInstance(adConfiguration, bannerAdLoadCallback).getOrThrow()
  }
}
