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
import com.adsurge.adn.ads.AdSurgeAdError
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class AdSurgeInterstitialAdTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var fakeInterstitialAd: FakeInterstitialAd
  private lateinit var fakeSdkWrapper: FakeSdkWrapper
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )

  @Before
  fun setUp() {
    fakeInterstitialAd = FakeInterstitialAd()
    fakeSdkWrapper = FakeSdkWrapper(interstitialAd = fakeInterstitialAd)
    AdSurgeFactory.delegate = fakeSdkWrapper
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
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
      )

    val result = AdSurgeInterstitialAd.newInstance(adConfiguration, interstitialAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isNotNull()
    assertThat(fakeSdkWrapper.lastCreatedAdUnitId).isEqualTo("test_ad_unit_id")
  }

  @Test
  fun newInstance_withMissingAdUnitId_returnsFailure() {
    val serverParameters = Bundle()
    val adConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
      )

    val result = AdSurgeInterstitialAd.newInstance(adConfiguration, interstitialAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    assertThat(interstitialAdLoadCallback.isFailure).isTrue()
    assertThat(interstitialAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun newInstance_withEmptyBidResponse_returnsFailure() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "",
      )

    val result = AdSurgeInterstitialAd.newInstance(adConfiguration, interstitialAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    assertThat(interstitialAdLoadCallback.isFailure).isTrue()
    assertThat(interstitialAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadAd_setsListenerAndLoadsAdOnInterstitialAd() {
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )

    interstitialAd.loadAd()

    assertThat(fakeInterstitialAd.listener).isEqualTo(interstitialAd)
    assertThat(fakeInterstitialAd.loadedConfig?.token).isEqualTo("test_bid_response")
  }

  @Test
  fun showAd_whenAdNotValid_invokesOnAdFailedToShow() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    fakeInterstitialAd.isAdValid = false
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )
    interstitialAd.onAdLoaded(FakeAdSurgeAd())

    interstitialAd.showAd(activity)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adError?.code).isEqualTo(AdSurgeUtils.ERROR_AD_NOT_READY)
    assertThat(fakeInterstitialAd.shownActivity).isNull()
  }

  @Test
  fun showAd_withValidActivity_invokesShowAdOnInterstitialAd() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    fakeInterstitialAd.isAdValid = true
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )
    interstitialAd.onAdLoaded(FakeAdSurgeAd())

    interstitialAd.showAd(activity)

    assertThat(fakeInterstitialAd.shownActivity).isEqualTo(activity)
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )

    interstitialAd.onAdLoaded(FakeAdSurgeAd())

    assertThat(interstitialAdLoadCallback.isSuccess).isTrue()
  }

  @Test
  fun onAdFailed_invokesOnFailure() {
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )
    val sdkError = AdSurgeAdError(300, "Network timeout")

    interstitialAd.onAdFailed(sdkError)

    assertThat(interstitialAdLoadCallback.isFailure).isTrue()
    assertThat(interstitialAdLoadCallback.adError?.code).isEqualTo(300)
    assertThat(interstitialAdLoadCallback.adError?.message).isEqualTo("Network timeout")
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )
    val fakeAd = FakeAdSurgeAd()
    interstitialAd.onAdLoaded(fakeAd)

    interstitialAd.onAdImpression(fakeAd)

    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )
    val fakeAd = FakeAdSurgeAd()
    interstitialAd.onAdLoaded(fakeAd)

    interstitialAd.onAdClicked(fakeAd)

    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosed() {
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )
    val fakeAd = FakeAdSurgeAd()
    interstitialAd.onAdLoaded(fakeAd)

    interstitialAd.onAdDismissed(fakeAd)

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdShowFailed_invokesOnAdFailedToShow() {
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = fakeInterstitialAd,
      )
    val fakeAd = FakeAdSurgeAd()
    interstitialAd.onAdLoaded(fakeAd)
    val sdkError = AdSurgeAdError(400, "Display error")

    interstitialAd.onAdShowFailed(fakeAd, sdkError)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adError?.code).isEqualTo(400)
    assertThat(interstitialAdCallback.adError?.message).isEqualTo("Display error")
  }
}
