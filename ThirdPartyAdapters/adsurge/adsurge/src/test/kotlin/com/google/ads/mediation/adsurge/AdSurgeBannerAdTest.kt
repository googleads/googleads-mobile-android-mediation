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
import com.adsurge.adn.ads.AdConfig
import com.adsurge.adn.ads.AdSurgeAd
import com.adsurge.adn.ads.AdSurgeAdError
import com.adsurge.adn.ads.AdSurgeAdSize
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AdSurgeBannerAdTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mockSdkWrapper: SdkWrapper = mock()
  private val mockAdViewWrapper: AdSurgeBannerAdWrapper = mock()
  private val mockMediationUtils: MediationUtilsWrapper = mock()
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private lateinit var testView: View

  @Before
  fun setUp() {
    testView = View(context)
    whenever(mockAdViewWrapper.getView()).thenReturn(testView)
    AdSurgeFactory.delegate = mockSdkWrapper
    whenever(mockSdkWrapper.createAdView(any(), any())).thenReturn(mockAdViewWrapper)
  }

  @After
  fun tearDown() {
    AdSurgeFactory.reset()
  }

  @Test
  fun newInstance_withValidBannerSize_returnsSuccess() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
        adSize = AdSize.BANNER,
      )
    whenever(mockMediationUtils.findClosestSize(any(), any(), any())).thenReturn(AdSize.BANNER)

    val result =
      AdSurgeBannerAd.newInstance(adConfiguration, bannerAdLoadCallback, mockMediationUtils)

    assertThat(result.isSuccess).isTrue()
    val bannerAd = result.getOrNull()
    assertThat(bannerAd).isNotNull()
    assertThat(bannerAd?.view).isEqualTo(testView)
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

    val result =
      AdSurgeBannerAd.newInstance(adConfiguration, bannerAdLoadCallback, mockMediationUtils)

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

    val result =
      AdSurgeBannerAd.newInstance(adConfiguration, bannerAdLoadCallback, mockMediationUtils)

    assertThat(result.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun newInstance_withUnsupportedSize_returnsFailure() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
        adSize = AdSize.WIDE_SKYSCRAPER,
      )
    whenever(mockMediationUtils.findClosestSize(any(), any(), any())).thenReturn(null)

    val result =
      AdSurgeBannerAd.newInstance(adConfiguration, bannerAdLoadCallback, mockMediationUtils)

    assertThat(result.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.adError?.code)
      .isEqualTo(AdSurgeUtils.ERROR_BANNER_SIZE_MISMATCH)
  }

  @Test
  fun loadAd_setsListenerAndLoadsAdOnAdView() {
    val serverParameters =
      Bundle().apply { putString(AdSurgeUtils.KEY_AD_UNIT_ID, "test_ad_unit_id") }
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = "test_bid_response",
        adSize = AdSize.BANNER,
      )
    val bannerAd =
      AdSurgeBannerAd(
        mediationBannerAdConfiguration = adConfiguration,
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adSurgeAdSize = AdSurgeAdSize.BANNER,
        adViewWrapper = mockAdViewWrapper,
      )

    bannerAd.loadAd()

    verify(mockAdViewWrapper).setListener(bannerAd)
    val captor = argumentCaptor<AdConfig>()
    verify(mockAdViewWrapper).loadAd(captor.capture())
    assertThat(captor.firstValue.token).isEqualTo("test_bid_response")
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    val adConfiguration: MediationBannerAdConfiguration = mock()
    val bannerAd =
      AdSurgeBannerAd(
        mediationBannerAdConfiguration = adConfiguration,
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adSurgeAdSize = AdSurgeAdSize.BANNER,
        adViewWrapper = mockAdViewWrapper,
      )
    val mockAd: AdSurgeAd = mock()

    bannerAd.onAdLoaded(mockAd)

    assertThat(bannerAdLoadCallback.isSuccess).isTrue()
  }

  @Test
  fun onAdFailed_invokesOnFailure() {
    val adConfiguration: MediationBannerAdConfiguration = mock()
    val bannerAd =
      AdSurgeBannerAd(
        mediationBannerAdConfiguration = adConfiguration,
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adSurgeAdSize = AdSurgeAdSize.BANNER,
        adViewWrapper = mockAdViewWrapper,
      )
    val sdkError = AdSurgeAdError(300, "Network timeout")

    bannerAd.onAdFailed(sdkError)

    assertThat(bannerAdLoadCallback.isFailure).isTrue()
    assertThat(bannerAdLoadCallback.adError?.code).isEqualTo(300)
    assertThat(bannerAdLoadCallback.adError?.message).isEqualTo("Network timeout")
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    val adConfiguration: MediationBannerAdConfiguration = mock()
    val bannerAd =
      AdSurgeBannerAd(
        mediationBannerAdConfiguration = adConfiguration,
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adSurgeAdSize = AdSurgeAdSize.BANNER,
        adViewWrapper = mockAdViewWrapper,
      )
    val mockAd: AdSurgeAd = mock()
    bannerAd.onAdLoaded(mockAd)

    bannerAd.onAdImpression(mockAd)

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    val adConfiguration: MediationBannerAdConfiguration = mock()
    val bannerAd =
      AdSurgeBannerAd(
        mediationBannerAdConfiguration = adConfiguration,
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adSurgeAdSize = AdSurgeAdSize.BANNER,
        adViewWrapper = mockAdViewWrapper,
      )
    val mockAd: AdSurgeAd = mock()
    bannerAd.onAdLoaded(mockAd)

    bannerAd.onAdClicked(mockAd)

    assertThat(bannerAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosed() {
    val adConfiguration: MediationBannerAdConfiguration = mock()
    val bannerAd =
      AdSurgeBannerAd(
        mediationBannerAdConfiguration = adConfiguration,
        mediationAdLoadCallback = bannerAdLoadCallback,
        adUnitId = "test_ad_unit_id",
        bidResponse = "test_bid_response",
        adSurgeAdSize = AdSurgeAdSize.BANNER,
        adViewWrapper = mockAdViewWrapper,
      )
    val mockAd: AdSurgeAd = mock()
    bannerAd.onAdLoaded(mockAd)

    bannerAd.onAdDismissed(mockAd)

    assertThat(bannerAdCallback.isClosed).isTrue()
  }
}
