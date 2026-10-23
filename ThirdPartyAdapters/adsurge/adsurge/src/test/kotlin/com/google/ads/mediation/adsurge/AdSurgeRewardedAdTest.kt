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

package com.google.ads.mediation.adsurge

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adsurge.adn.ads.AdConfig
import com.adsurge.adn.ads.AdSurgeAd
import com.adsurge.adn.ads.AdSurgeAdError
import com.adsurge.adn.ads.rewarded.IRewardedAd
import com.adsurge.adn.ads.rewarded.RewardItem as AdSurgeRewardItem
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class AdSurgeRewardedAdTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mockSdkWrapper: SdkWrapper = mock()
  private val mockRewardedAd: IRewardedAd = mock()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )

  @Before
  fun setUp() {
    AdSurgeFactory.delegate = mockSdkWrapper
    whenever(mockSdkWrapper.createRewardedAd(any(), any())).thenReturn(mockRewardedAd)
  }

  @After
  fun tearDown() {
    AdSurgeFactory.reset()
  }

  @Test
  fun newInstance_withValidParameters_returnsSuccess() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
      )

    val result = AdSurgeRewardedAd.newInstance(adConfiguration, rewardedAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isNotNull()
  }

  @Test
  fun newInstance_withMissingAdUnitId_returnsFailure() {
    val serverParameters = Bundle()
    val adConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
      )

    val result = AdSurgeRewardedAd.newInstance(adConfiguration, rewardedAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    assertThat(rewardedAdLoadCallback.isFailure).isTrue()
    assertThat(rewardedAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun newInstance_withEmptyBidResponse_returnsFailure() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "",
      )

    val result = AdSurgeRewardedAd.newInstance(adConfiguration, rewardedAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    assertThat(rewardedAdLoadCallback.isFailure).isTrue()
    assertThat(rewardedAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadAd_setsListenerAndLoadsAdOnRewardedAd() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
      )
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )

    rewardedAd.loadAd()

    val captor = argumentCaptor<AdConfig>()
    verify(mockRewardedAd).loadAd(captor.capture(), eq(rewardedAd))
    assertThat(captor.firstValue.token).isEqualTo("test_bid_response")
  }

  @Test
  fun showAd_withNonActivityContext_invokesOnAdFailedToShow() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)

    rewardedAd.showAd(context)

    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adError?.code).isEqualTo(AdSurgeUtils.ERROR_CONTEXT_NOT_ACTIVITY)
  }

  @Test
  fun showAd_whenAdNotValid_invokesOnAdFailedToShow() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    whenever(mockRewardedAd.isValid).thenReturn(false)
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)

    rewardedAd.showAd(activity)

    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adError?.code).isEqualTo(AdSurgeUtils.ERROR_AD_NOT_READY)
  }

  @Test
  fun showAd_withValidActivity_invokesShowAdOnRewardedAd() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    whenever(mockRewardedAd.isValid).thenReturn(true)
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)

    rewardedAd.showAd(activity)

    verify(mockRewardedAd).showAd(activity)
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()

    rewardedAd.onAdLoaded(mockAd)

    assertThat(rewardedAdLoadCallback.isSuccess).isTrue()
  }

  @Test
  fun onAdFailed_invokesOnFailure() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val sdkError = AdSurgeAdError(300, "Network timeout")

    rewardedAd.onAdFailed(sdkError)

    assertThat(rewardedAdLoadCallback.isFailure).isTrue()
    assertThat(rewardedAdLoadCallback.adError?.code).isEqualTo(300)
    assertThat(rewardedAdLoadCallback.adError?.message).isEqualTo("Network timeout")
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)

    rewardedAd.onAdImpression(mockAd)

    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)

    rewardedAd.onAdClicked(mockAd)

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosed() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)

    rewardedAd.onAdDismissed(mockAd)

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdShowFailed_invokesOnAdFailedToShow() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)
    val sdkError = AdSurgeAdError(400, "Display error")

    rewardedAd.onAdShowFailed(mockAd, sdkError)

    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adError?.code).isEqualTo(400)
    assertThat(rewardedAdCallback.adError?.message).isEqualTo("Display error")
  }

  @Test
  fun onUserEarnedReward_invokesOnUserEarnedRewardWithRewardItem() {
    val adConfiguration: MediationRewardedAdConfiguration = mock()
    val rewardedAd =
      AdSurgeRewardedAd(
        mediationRewardedAdConfiguration = adConfiguration,
        mediationAdLoadCallback = rewardedAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        rewardedAd = mockRewardedAd,
      )
    val mockAd: AdSurgeAd = mock()
    rewardedAd.onAdLoaded(mockAd)

    val mockRewardItem: AdSurgeRewardItem = mock()
    whenever(mockRewardItem.amount).thenReturn(10)
    whenever(mockRewardItem.type).thenReturn("coins")

    rewardedAd.onUserEarnedReward(mockAd, mockRewardItem)

    assertThat(rewardedAdCallback.rewardItem?.amount).isEqualTo(10)
    assertThat(rewardedAdCallback.rewardItem?.type).isEqualTo("coins")
  }
}
