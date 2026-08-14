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
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_BID_RESPONSE
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.mintegral.MintegralConstants.MINTEGRAL_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.rtb.MintegralRtbRewardedAd
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallRewardedAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardInfo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [MintegralWaterfallRewardedAd] and [MintegralRtbRewardedAd]. */
@RunWith(AndroidJUnit4::class)
class MintegralRewardedAdTest {

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockRewardedAdCallback: MediationRewardedAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockRewardedAdCallback
    }
  private val mockFlagValueGetter: FlagValueGetter = mock {
    on { shouldRestrictMultipleAdLoads() } doReturn false
  }
  private val mockRewardedAdWrapper: MintegralRewardedAdWrapper = mock()
  private val mockBidRewardedAdWrapper: MintegralBidRewardedAdWrapper = mock()

  private lateinit var mockMintegralFactoryStatic: MockedStatic<MintegralFactory>
  private lateinit var waterfallRewardedAd: MintegralWaterfallRewardedAd
  private lateinit var rtbRewardedAd: MintegralRtbRewardedAd

  @Before
  fun setUp() {
    mockMintegralFactoryStatic = mockStatic(MintegralFactory::class.java)
    whenever(MintegralFactory.createMintegralRewardedAdWrapper()) doReturn mockRewardedAdWrapper
    whenever(MintegralFactory.createMintegralBidRewardedAdWrapper()) doReturn
      mockBidRewardedAdWrapper

    val waterfallConfig = createDefaultRewardedAdConfiguration()
    waterfallRewardedAd =
      MintegralWaterfallRewardedAd(waterfallConfig, mockAdLoadCallback, mockFlagValueGetter)

    val rtbConfig = createDefaultRtbRewardedAdConfiguration()
    rtbRewardedAd = MintegralRtbRewardedAd(rtbConfig, mockAdLoadCallback, mockFlagValueGetter)
  }

  @After
  fun tearDown() {
    mockMintegralFactoryStatic.close()
  }

  // region Waterfall Rewarded Ad Tests

  @Test
  fun loadWaterfallRewardedAd_withNullAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(PLACEMENT_ID to TEST_PLACEMENT_ID)
    val config =
      createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallRewardedAd.loadAd(config)

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
  fun loadWaterfallRewardedAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "", PLACEMENT_ID to TEST_PLACEMENT_ID)
    val config =
      createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallRewardedAd.loadAd(config)

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
  fun loadWaterfallRewardedAd_withNullPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val config =
      createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallRewardedAd.loadAd(config)

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
  fun loadWaterfallRewardedAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val config =
      createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)

    waterfallRewardedAd.loadAd(config)

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
  fun loadWaterfallRewardedAd_withValidParameters_createsWrapperAndLoads() {
    val config = createDefaultRewardedAdConfiguration()

    waterfallRewardedAd.loadAd(config)

    verify(mockRewardedAdWrapper).createAd(eq(context), eq(TEST_PLACEMENT_ID), eq(TEST_AD_UNIT))
    verify(mockRewardedAdWrapper).setRewardVideoListener(eq(waterfallRewardedAd))
    verify(mockRewardedAdWrapper).load()
  }

  @Test
  fun showAd_forWaterfallRewardedAd_whenMuted_playsVideoMutedAndShows() {
    val mutedConfig =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        mediationExtras = bundleOf(MintegralExtras.Keys.MUTE_AUDIO to true),
      )
    val mutedRewardedAd =
      MintegralWaterfallRewardedAd(mutedConfig, mockAdLoadCallback, mockFlagValueGetter)
    mutedRewardedAd.loadAd(mutedConfig)

    mutedRewardedAd.showAd(context)

    verify(mockRewardedAdWrapper).playVideoMute(MBridgeConstans.REWARD_VIDEO_PLAY_MUTE)
    verify(mockRewardedAdWrapper).show()
  }

  @Test
  fun showAd_forWaterfallRewardedAd_whenNotMuted_playsVideoNotMutedAndShows() {
    val unmutedConfig = createDefaultRewardedAdConfiguration()
    waterfallRewardedAd.loadAd(unmutedConfig)

    waterfallRewardedAd.showAd(context)

    verify(mockRewardedAdWrapper).playVideoMute(MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE)
    verify(mockRewardedAdWrapper).show()
  }

  // endregion

  // region RTB Rewarded Ad Tests

  @Test
  fun loadRtbRewardedAd_withNullAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(PLACEMENT_ID to TEST_PLACEMENT_ID)
    val config =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    rtbRewardedAd.loadAd(config)

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
  fun loadRtbRewardedAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "", PLACEMENT_ID to TEST_PLACEMENT_ID)
    val config =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    rtbRewardedAd.loadAd(config)

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
  fun loadRtbRewardedAd_withNullPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val config =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    rtbRewardedAd.loadAd(config)

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
  fun loadRtbRewardedAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val config =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = TEST_BID_RESPONSE,
      )

    rtbRewardedAd.loadAd(config)

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
  fun loadRtbRewardedAd_withEmptyBidResponse_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    val config =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "",
      )

    rtbRewardedAd.loadAd(config)

    val expectedAdError =
      AdError(
        ERROR_INVALID_BID_RESPONSE,
        "Missing or invalid Mintegral bidding signal in this ad request.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRtbRewardedAd_withValidParameters_createsWrapperSetsWatermarkAndLoadsFromBid() {
    val config = createDefaultRtbRewardedAdConfiguration()

    rtbRewardedAd.loadAd(config)

    verify(mockBidRewardedAdWrapper).createAd(eq(context), eq(TEST_PLACEMENT_ID), eq(TEST_AD_UNIT))
    verify(mockBidRewardedAdWrapper).setExtraInfo(any())
    verify(mockBidRewardedAdWrapper).setRewardVideoListener(eq(rtbRewardedAd))
    verify(mockBidRewardedAdWrapper).loadFromBid(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun showAd_forRtbRewardedAd_whenMuted_playsVideoMutedAndShowsFromBid() {
    val mutedConfig =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        bidResponse = TEST_BID_RESPONSE,
        mediationExtras = bundleOf(MintegralExtras.Keys.MUTE_AUDIO to true),
      )
    val mutedRtbAd = MintegralRtbRewardedAd(mutedConfig, mockAdLoadCallback, mockFlagValueGetter)
    mutedRtbAd.loadAd(mutedConfig)

    mutedRtbAd.showAd(context)

    verify(mockBidRewardedAdWrapper).playVideoMute(MBridgeConstans.REWARD_VIDEO_PLAY_MUTE)
    verify(mockBidRewardedAdWrapper).showFromBid()
  }

  @Test
  fun showAd_forRtbRewardedAd_whenNotMuted_playsVideoNotMutedAndShowsFromBid() {
    val unmutedConfig = createDefaultRtbRewardedAdConfiguration()
    rtbRewardedAd.loadAd(unmutedConfig)

    rtbRewardedAd.showAd(context)

    verify(mockBidRewardedAdWrapper).playVideoMute(MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE)
    verify(mockBidRewardedAdWrapper).showFromBid()
  }

  // endregion

  // region Callback / Listener Tests

  @Test
  fun onVideoLoadSuccess_invokesOnSuccess() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onVideoLoadSuccess(ids)

    verify(mockAdLoadCallback).onSuccess(eq(waterfallRewardedAd))
  }

  @Test
  fun onLoadSuccess_throwsNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onLoadSuccess(ids)
  }

  @Test
  fun onVideoLoadFailWithCode_invokesOnFailureWithGivenAdError() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onVideoLoadFailWithCode(ids, 123, TEST_ERROR_MESSAGE)

    val expectedAdError = AdError(123, TEST_ERROR_MESSAGE, MINTEGRAL_SDK_ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdShow_invokesOnAdOpenedAndReportAdImpression() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallRewardedAd.onVideoLoadSuccess(ids)

    waterfallRewardedAd.onAdShow(ids)

    verify(mockRewardedAdCallback).onAdOpened()
    verify(mockRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onAdShow_withoutRewardedAdCallback_throwsNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onAdShow(ids)
  }

  @Test
  fun onAdClose_withCompletedReward_invokesOnUserEarnedRewardAndOnAdClosed() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallRewardedAd.onVideoLoadSuccess(ids)
    val mockRewardInfo =
      mock<RewardInfo> {
        on { isCompleteView } doReturn true
        on { rewardName } doReturn "Coins"
        on { rewardAmount } doReturn "10"
      }

    waterfallRewardedAd.onAdClose(ids, mockRewardInfo)

    verify(mockRewardedAdCallback).onUserEarnedReward()
    verify(mockRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onAdClose_withIncompleteReward_invokesOnAdClosedWithoutReward() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallRewardedAd.onVideoLoadSuccess(ids)
    val mockRewardInfo = mock<RewardInfo> { on { isCompleteView } doReturn false }

    waterfallRewardedAd.onAdClose(ids, mockRewardInfo)

    verify(mockRewardedAdCallback, never()).onUserEarnedReward(any())
    verify(mockRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onAdClose_withNullReward_invokesOnAdClosedWithoutReward() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallRewardedAd.onVideoLoadSuccess(ids)

    waterfallRewardedAd.onAdClose(ids, null)

    verify(mockRewardedAdCallback, never()).onUserEarnedReward(any())
    verify(mockRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onAdClose_withoutRewardedAdCallback_throwsNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onAdClose(ids, null)
  }

  @Test
  fun onShowFailWithCode_invokesOnAdFailedToShow() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallRewardedAd.onVideoLoadSuccess(ids)

    waterfallRewardedAd.onShowFailWithCode(ids, 456, TEST_ERROR_MESSAGE)

    val expectedAdError = AdError(456, TEST_ERROR_MESSAGE, MINTEGRAL_SDK_ERROR_DOMAIN)
    verify(mockRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onShowFailWithCode_withoutRewardedAdCallback_throwsNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onShowFailWithCode(ids, 456, TEST_ERROR_MESSAGE)
  }

  @Test
  fun onVideoAdClicked_invokesReportAdClicked() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)
    waterfallRewardedAd.onVideoLoadSuccess(ids)

    waterfallRewardedAd.onVideoAdClicked(ids)

    verify(mockRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onVideoAdClicked_withoutRewardedAdCallback_throwsNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onVideoAdClicked(ids)
  }

  @Test
  fun onVideoComplete_throwsNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onVideoComplete(ids)
  }

  @Test
  fun onEndcardShow_throwsNoException() {
    val ids = MBridgeIds(TEST_PLACEMENT_ID, TEST_AD_UNIT)

    waterfallRewardedAd.onEndcardShow(ids)
  }

  // endregion

  private fun createDefaultRewardedAdConfiguration(): MediationRewardedAdConfiguration {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    return createMediationRewardedAdConfiguration(
      context = context,
      serverParameters = serverParameters,
    )
  }

  private fun createDefaultRtbRewardedAdConfiguration(): MediationRewardedAdConfiguration {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    return createMediationRewardedAdConfiguration(
      context = context,
      serverParameters = serverParameters,
      bidResponse = TEST_BID_RESPONSE,
      watermark = TEST_WATERMARK,
    )
  }
}
