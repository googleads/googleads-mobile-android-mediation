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
import com.adsurge.adn.ads.interstitial.IInterstitialAd
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
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
class AdSurgeInterstitialAdTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mockSdkWrapper: SdkWrapper = mock()
  private val mockInterstitialAd: IInterstitialAd = mock()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )

  @Before
  fun setUp() {
    AdSurgeFactory.delegate = mockSdkWrapper
    whenever(mockSdkWrapper.createInterstitialAd(any(), any())).thenReturn(mockInterstitialAd)
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
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
      )
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )

    interstitialAd.loadAd()

    val captor = argumentCaptor<AdConfig>()
    verify(mockInterstitialAd).loadAd(captor.capture(), eq(interstitialAd))
    assertThat(captor.firstValue.token).isEqualTo("test_bid_response")
  }

  @Test
  fun showAd_withNonActivityContext_invokesOnAdFailedToShow() {
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()
    interstitialAd.onAdLoaded(mockAd)

    interstitialAd.showAd(context)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_CONTEXT_NOT_ACTIVITY)
  }

  @Test
  fun showAd_whenAdNotValid_invokesOnAdFailedToShow() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    whenever(mockInterstitialAd.isValid).thenReturn(false)
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()
    interstitialAd.onAdLoaded(mockAd)

    interstitialAd.showAd(activity)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adError?.code).isEqualTo(AdSurgeUtils.ERROR_AD_NOT_READY)
  }

  @Test
  fun showAd_withValidActivity_invokesShowAdOnInterstitialAd() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    whenever(mockInterstitialAd.isValid).thenReturn(true)
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()
    interstitialAd.onAdLoaded(mockAd)

    interstitialAd.showAd(activity)

    verify(mockInterstitialAd).showAd(activity)
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()

    interstitialAd.onAdLoaded(mockAd)

    assertThat(interstitialAdLoadCallback.isSuccess).isTrue()
  }

  @Test
  fun onAdFailed_invokesOnFailure() {
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val sdkError = AdSurgeAdError(300, "Network timeout")

    interstitialAd.onAdFailed(sdkError)

    assertThat(interstitialAdLoadCallback.isFailure).isTrue()
    assertThat(interstitialAdLoadCallback.adError?.code).isEqualTo(300)
    assertThat(interstitialAdLoadCallback.adError?.message).isEqualTo("Network timeout")
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()
    interstitialAd.onAdLoaded(mockAd)

    interstitialAd.onAdImpression(mockAd)

    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()
    interstitialAd.onAdLoaded(mockAd)

    interstitialAd.onAdClicked(mockAd)

    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosed() {
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()
    interstitialAd.onAdLoaded(mockAd)

    interstitialAd.onAdDismissed(mockAd)

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdShowFailed_invokesOnAdFailedToShow() {
    val adConfiguration: MediationInterstitialAdConfiguration = mock()
    val interstitialAd =
      AdSurgeInterstitialAd(
        mediationInterstitialAdConfiguration = adConfiguration,
        mediationAdLoadCallback = interstitialAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        interstitialAd = mockInterstitialAd,
      )
    val mockAd: AdSurgeAd = mock()
    interstitialAd.onAdLoaded(mockAd)
    val sdkError = AdSurgeAdError(400, "Display error")

    interstitialAd.onAdShowFailed(mockAd, sdkError)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adError?.code).isEqualTo(400)
    assertThat(interstitialAdCallback.adError?.message).isEqualTo("Display error")
  }
}
