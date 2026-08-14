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

package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_ERROR_MESSAGE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_BANNER_SIZE_UNSUPPORTED
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_BID_RESPONSE
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.mintegral.MintegralConstants.MINTEGRAL_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.mediation.MintegralBannerAd
import com.google.ads.mediation.mintegral.rtb.MintegralRtbBannerAd
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallBannerAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.mbridge.msdk.out.MBBannerView
import com.mbridge.msdk.out.MBridgeIds
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [MintegralWaterfallBannerAd] and [MintegralRtbBannerAd]. */
@RunWith(AndroidJUnit4::class)
class MintegralBannerAdTest {

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockBannerAdCallback: MediationBannerAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }
  private val mockMediationUtils: MediationUtilsWrapper = mock()
  private val mockMBBannerView: MBBannerView = mock()

  private lateinit var mockMintegralFactoryStatic: MockedStatic<MintegralFactory>
  private lateinit var mockMBBannerViewConstruction: MockedConstruction<MBBannerView>
  private lateinit var waterfallBannerAd: MintegralWaterfallBannerAd
  private lateinit var rtbBannerAd: MintegralRtbBannerAd

  @Before
  fun setUp() {
    mockMintegralFactoryStatic = mockStatic(MintegralFactory::class.java)
    whenever(MintegralFactory.createMBBannerView(any())) doReturn mockMBBannerView

    mockMBBannerViewConstruction = mockConstruction(MBBannerView::class.java)

    whenever(mockMediationUtils.findClosestSize(eq(context), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    whenever(
      mockMediationUtils.findClosestSize(eq(context), eq(AdSize.MEDIUM_RECTANGLE), any())
    ) doReturn AdSize.MEDIUM_RECTANGLE
    whenever(
      mockMediationUtils.findClosestSize(eq(context), eq(AdSize.LEADERBOARD), any())
    ) doReturn AdSize.LEADERBOARD

    waterfallBannerAd = MintegralWaterfallBannerAd(mockAdLoadCallback)
    rtbBannerAd = MintegralRtbBannerAd(mockAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockMintegralFactoryStatic.close()
    mockMBBannerViewConstruction.close()
  }

  // region Banner Size Mapping Tests

  @Test
  fun getMintegralBannerSizeFromAdMobAdSize_forBanner_returnsStandardType() {
    val bannerSize =
      MintegralBannerAd.getMintegralBannerSizeFromAdMobAdSize(
        AdSize.BANNER,
        context,
        /* isRtb= */ false,
        mockMediationUtils,
      )

    assertNotNull(bannerSize)
    assertThat(bannerSize.width).isEqualTo(320)
    assertThat(bannerSize.height).isEqualTo(50)
  }

  @Test
  fun getMintegralBannerSizeFromAdMobAdSize_forMediumRectangle_returnsMediumType() {
    val bannerSize =
      MintegralBannerAd.getMintegralBannerSizeFromAdMobAdSize(
        AdSize.MEDIUM_RECTANGLE,
        context,
        /* isRtb= */ false,
        mockMediationUtils,
      )

    assertNotNull(bannerSize)
    assertThat(bannerSize.width).isEqualTo(300)
    assertThat(bannerSize.height).isEqualTo(250)
  }

  @Test
  fun getMintegralBannerSizeFromAdMobAdSize_forLeaderboard_returnsSmartType() {
    val bannerSize =
      MintegralBannerAd.getMintegralBannerSizeFromAdMobAdSize(
        AdSize.LEADERBOARD,
        context,
        /* isRtb= */ false,
        mockMediationUtils,
      )

    assertNotNull(bannerSize)
    assertThat(bannerSize.width).isEqualTo(320)
    assertThat(bannerSize.height).isEqualTo(50)
  }

  @Test
  fun getMintegralBannerSizeFromAdMobAdSize_forDevSetSize_returnsDevSetType() {
    val customSize = AdSize(160, 600)
    whenever(mockMediationUtils.findClosestSize(eq(context), eq(customSize), any())) doReturn
      customSize

    val bannerSize =
      MintegralBannerAd.getMintegralBannerSizeFromAdMobAdSize(
        customSize,
        context,
        /* isRtb= */ false,
        mockMediationUtils,
      )

    assertNotNull(bannerSize)
    assertThat(bannerSize.width).isEqualTo(160)
    assertThat(bannerSize.height).isEqualTo(600)
  }

  @Test
  fun getMintegralBannerSizeFromAdMobAdSize_forWaterfall_withUnsupportedSize_returnsNull() {
    val unsupportedSize = AdSize(160, 600)
    whenever(mockMediationUtils.findClosestSize(eq(context), eq(unsupportedSize), any())) doReturn
      null

    val bannerSize =
      MintegralBannerAd.getMintegralBannerSizeFromAdMobAdSize(
        unsupportedSize,
        context,
        /* isRtb= */ false,
        mockMediationUtils,
      )

    assertNull(bannerSize)
  }

  @Test
  fun getMintegralBannerSizeFromAdMobAdSize_forRtb_withUnsupportedSize_returnsDevSetType() {
    val unsupportedSize = AdSize(160, 600)
    whenever(mockMediationUtils.findClosestSize(eq(context), eq(unsupportedSize), any())) doReturn
      null

    val bannerSize =
      MintegralBannerAd.getMintegralBannerSizeFromAdMobAdSize(
        unsupportedSize,
        context,
        /* isRtb= */ true,
        mockMediationUtils,
      )

    assertNotNull(bannerSize)
    assertThat(bannerSize.width).isEqualTo(160)
    assertThat(bannerSize.height).isEqualTo(600)
  }

  // endregion

  // region Waterfall Banner Ad Tests

  @Test
  fun loadWaterfallBannerAd_withUnsupportedSize_invokesOnFailure() {
    val unsupportedSize = AdSize(160, 600)
    whenever(mockMediationUtils.findClosestSize(eq(context), eq(unsupportedSize), any())) doReturn
      null
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        adSize = unsupportedSize,
      )

    waterfallBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_BANNER_SIZE_UNSUPPORTED,
        "The requested banner size: $unsupportedSize is not supported by Mintegral.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadWaterfallBannerAd_withNullAdUnitId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(PLACEMENT_ID to TEST_PLACEMENT_ID),
        adSize = AdSize.BANNER,
      )

    waterfallBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadWaterfallBannerAd_withEmptyAdUnitId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to "", PLACEMENT_ID to TEST_PLACEMENT_ID),
        adSize = AdSize.BANNER,
      )

    waterfallBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadWaterfallBannerAd_withNullPlacementId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT),
        adSize = AdSize.BANNER,
      )

    waterfallBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadWaterfallBannerAd_withEmptyPlacementId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to ""),
        adSize = AdSize.BANNER,
      )

    waterfallBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadWaterfallBannerAd_withValidParameters_initializesBannerViewAndLoads() {
    val config = createDefaultWaterfallBannerAdConfiguration()

    waterfallBannerAd.loadAd(config, mockMediationUtils)

    assertThat(mockMBBannerViewConstruction.constructed()).hasSize(1)
    val constructedView = mockMBBannerViewConstruction.constructed().first()
    verify(constructedView).init(any(), eq(TEST_PLACEMENT_ID), eq(TEST_AD_UNIT))
    verify(constructedView).setBannerAdListener(eq(waterfallBannerAd))
    verify(constructedView).load()
  }

  @Test
  fun getView_returnsMBBannerView() {
    val config = createDefaultWaterfallBannerAdConfiguration()
    waterfallBannerAd.loadAd(config, mockMediationUtils)

    val view = waterfallBannerAd.view

    assertIs<MBBannerView>(view)
  }

  // endregion

  // region RTB Banner Ad Tests

  @Test
  fun loadRtbBannerAd_withNullAdUnitId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.BANNER,
      )

    rtbBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_withEmptyAdUnitId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to "", PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.BANNER,
      )

    rtbBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid ad Unit ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_withNullPlacementId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT),
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.BANNER,
      )

    rtbBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_withEmptyPlacementId_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to ""),
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.BANNER,
      )

    rtbBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_withEmptyBidResponse_invokesOnFailure() {
    val config =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = "",
        adSize = AdSize.BANNER,
      )

    rtbBannerAd.loadAd(config, mockMediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_BID_RESPONSE,
        "Missing or invalid Mintegral bidding signal in this ad request.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbBannerAd_withValidParameters_initializesBannerViewSetsWatermarkAndLoadsFromBid() {
    val config = createDefaultRtbBannerAdConfiguration()

    rtbBannerAd.loadAd(config, mockMediationUtils)

    verify(mockMBBannerView).init(any(), eq(TEST_PLACEMENT_ID), eq(TEST_AD_UNIT))
    verify(mockMBBannerView).setExtraInfo(any())
    verify(mockMBBannerView).setBannerAdListener(eq(rtbBannerAd))
    verify(mockMBBannerView).loadFromBid(eq(TEST_BID_RESPONSE))
  }

  // endregion

  // region Callback / Listener Tests

  @Test
  fun onLoadSuccessed_invokesOnSuccess() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallBannerAd.onLoadSuccessed(ids)

    verify(mockAdLoadCallback).onSuccess(eq(waterfallBannerAd))
  }

  @Test
  fun onLoadFailedWithCode_invokesOnFailureWithGivenAdError() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallBannerAd.onLoadFailedWithCode(ids, 123, TEST_ERROR_MESSAGE)

    val expectedAdError = AdError(123, TEST_ERROR_MESSAGE, MINTEGRAL_SDK_ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onLogImpression_invokesReportAdImpression() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallBannerAd.onLoadSuccessed(ids)

    waterfallBannerAd.onLogImpression(ids)

    verify(mockBannerAdCallback).reportAdImpression()
  }

  @Test
  fun onClick_invokesReportAdClicked() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallBannerAd.onLoadSuccessed(ids)

    waterfallBannerAd.onClick(ids)

    verify(mockBannerAdCallback).reportAdClicked()
  }

  @Test
  fun onLeaveApp_invokesOnAdLeftApplication() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallBannerAd.onLoadSuccessed(ids)

    waterfallBannerAd.onLeaveApp(ids)

    verify(mockBannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun showFullScreen_invokesOnAdOpened() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallBannerAd.onLoadSuccessed(ids)

    waterfallBannerAd.showFullScreen(ids)

    verify(mockBannerAdCallback).onAdOpened()
  }

  @Test
  fun closeFullScreen_invokesOnAdClosed() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallBannerAd.onLoadSuccessed(ids)

    waterfallBannerAd.closeFullScreen(ids)

    verify(mockBannerAdCallback).onAdClosed()
  }

  @Test
  fun onCloseBanner_invokesOnAdClosed() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallBannerAd.onLoadSuccessed(ids)

    waterfallBannerAd.onCloseBanner(ids)

    verify(mockBannerAdCallback).onAdClosed()
  }

  @Test
  fun callbacks_withoutBannerAdCallback_throwNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallBannerAd.onLogImpression(ids)
    waterfallBannerAd.onClick(ids)
    waterfallBannerAd.onLeaveApp(ids)
    waterfallBannerAd.showFullScreen(ids)
    waterfallBannerAd.closeFullScreen(ids)
    waterfallBannerAd.onCloseBanner(ids)
  }

  // endregion

  private fun createDefaultWaterfallBannerAdConfiguration(): MediationBannerAdConfiguration {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    return createMediationBannerAdConfiguration(
      context = context,
      serverParameters = serverParameters,
      adSize = AdSize.BANNER,
    )
  }

  private fun createDefaultRtbBannerAdConfiguration(): MediationBannerAdConfiguration {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    return createMediationBannerAdConfiguration(
      context = context,
      serverParameters = serverParameters,
      bidResponse = TEST_BID_RESPONSE,
      watermark = TEST_WATERMARK,
      adSize = AdSize.BANNER,
    )
  }
}
