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

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adsurge.adn.ads.AdSurgeAdError
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdSurgeBannerAdTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var testView: View
  private lateinit var fakeAdViewWrapper: FakeAdSurgeBannerAdWrapper
  private lateinit var fakeSdkWrapper: FakeSdkWrapper
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)

  @Before
  fun setUp() {
    testView = View(context)
    fakeAdViewWrapper = FakeAdSurgeBannerAdWrapper(testView)
    fakeSdkWrapper = FakeSdkWrapper(bannerAdWrapper = fakeAdViewWrapper)
    AdSurgeFactory.delegate = fakeSdkWrapper
  }

  @After
  fun tearDown() {
    AdSurgeFactory.reset()
  }

  @Test
  fun newInstance_withValidConfiguration_returnsSuccess() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
        adSize = AdSize.BANNER,
      )

    val result = AdSurgeBannerAd.newInstance(adConfiguration, bannerAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
    val bannerAd = result.getOrNull()
    assertThat(bannerAd).isNotNull()
    assertThat(bannerAd?.view).isEqualTo(testView)
    assertThat(fakeSdkWrapper.lastCreatedAdUnitId).isEqualTo("test_ad_unit_id")
  }

  @Test
  fun newInstance_withMissingAdUnitId_returnsFailure() {
    val serverParameters = Bundle()
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
        adSize = AdSize.BANNER,
      )

    val result = AdSurgeBannerAd.newInstance(adConfiguration, bannerAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun newInstance_withEmptyBidResponse_returnsFailure() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "",
        adSize = AdSize.BANNER,
      )

    val result = AdSurgeBannerAd.newInstance(adConfiguration, bannerAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadAd_setsListenerAndLoadsAdOnAdView() {
    val bannerAd =
      AdSurgeBannerAd(
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adViewWrapper = fakeAdViewWrapper,
      )

    bannerAd.loadAd()

    assertThat(fakeAdViewWrapper.listener).isEqualTo(bannerAd)
    assertThat(fakeAdViewWrapper.loadedConfig?.token).isEqualTo("test_bid_response")
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    val bannerAd =
      AdSurgeBannerAd(
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adViewWrapper = fakeAdViewWrapper,
      )

    bannerAd.onAdLoaded(FakeAdSurgeAd())

    assertThat(bannerAdLoadCallback.isSuccess).isTrue()
  }

  @Test
  fun onAdFailed_invokesOnFailure() {
    val bannerAd =
      AdSurgeBannerAd(
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adViewWrapper = fakeAdViewWrapper,
      )
    val sdkError = AdSurgeAdError(300, "Network timeout")

    bannerAd.onAdFailed(sdkError)

    assertThat(bannerAdLoadCallback.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.adError?.code).isEqualTo(300)
    assertThat(bannerAdLoadCallback.adError?.message).isEqualTo("Network timeout")
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    val bannerAd =
      AdSurgeBannerAd(
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adViewWrapper = fakeAdViewWrapper,
      )
    val fakeAd = FakeAdSurgeAd()
    bannerAd.onAdLoaded(fakeAd)

    bannerAd.onAdImpression(fakeAd)

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    val bannerAd =
      AdSurgeBannerAd(
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adViewWrapper = fakeAdViewWrapper,
      )
    val fakeAd = FakeAdSurgeAd()
    bannerAd.onAdLoaded(fakeAd)

    bannerAd.onAdClicked(fakeAd)

    assertThat(bannerAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosed() {
    val bannerAd =
      AdSurgeBannerAd(
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adViewWrapper = fakeAdViewWrapper,
      )
    val fakeAd = FakeAdSurgeAd()
    bannerAd.onAdLoaded(fakeAd)

    bannerAd.onAdDismissed(fakeAd)

    assertThat(bannerAdCallback.isClosed).isTrue()
  }
}
