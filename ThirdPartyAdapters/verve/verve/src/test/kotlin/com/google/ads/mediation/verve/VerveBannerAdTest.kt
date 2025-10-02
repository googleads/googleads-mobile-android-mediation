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

package com.google.ads.mediation.verve

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import net.pubnative.lite.sdk.views.HyBidAdView
import net.pubnative.lite.sdk.views.HyBidBannerAdView
import net.pubnative.lite.sdk.views.HyBidLeaderboardAdView
import net.pubnative.lite.sdk.views.HyBidMRectAdView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VerveBannerAdTest {
  // Subject of testing.
  private lateinit var verveBannerAd: VerveBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockHyBidAdView = mock<HyBidAdView>()
  private val mockBannerAdCallback: MediationBannerAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }

  @Before
  fun setUp() {
    verveBannerAd =
      VerveBannerAd(mockAdLoadCallback, TEST_BID_RESPONSE, mockHyBidAdView, AdSize.BANNER)
  }

  @Test
  fun getView_afterBannerAdCreated_createsHyBidBannerAd() {
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.BANNER,
      )
    VerveBannerAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess { verveBannerAd = it }

    val verveAdView = verveBannerAd.view as HyBidAdView

    assertThat(verveAdView).isInstanceOf(HyBidBannerAdView::class.java)
  }

  @Test
  fun getView_afterMediumRectangleAdCreated_createsHyBidBannerAd() {
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.MEDIUM_RECTANGLE,
      )
    VerveBannerAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess { verveBannerAd = it }

    val verveAdView = verveBannerAd.view as HyBidAdView

    assertThat(verveAdView).isInstanceOf(HyBidMRectAdView::class.java)
  }

  @Test
  fun getView_afterLeaderboardAdCreated_createsHyBidBannerAd() {
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        adSize = AdSize.LEADERBOARD,
      )
    VerveBannerAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess { verveBannerAd = it }

    val verveAdView = verveBannerAd.view as HyBidAdView

    assertThat(verveAdView).isInstanceOf(HyBidLeaderboardAdView::class.java)
  }

  @Test
  fun loadAd_invokesHyBidRenderAd() {
    verveBannerAd.loadAd(context)

    verify(mockHyBidAdView).renderAd(eq(TEST_BID_RESPONSE), eq(verveBannerAd))
  }

  @Test
  fun onAdLoaded_invokesOnSuccess() {
    verveBannerAd.onAdLoaded()

    verify(mockAdLoadCallback).onSuccess(eq(verveBannerAd))
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val testError = Throwable("TestError")
    val expectedAdError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "Could not load banner ad. Error: TestError",
        SDK_ERROR_DOMAIN,
      )

    verveBannerAd.onAdLoadFailed(testError)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    verveBannerAd.onAdLoaded()

    verveBannerAd.onAdImpression()

    verify(mockBannerAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClick_invokesReportAdClickedOnAdOpenedAndOnAdLeftApplication() {
    verveBannerAd.onAdLoaded()

    verveBannerAd.onAdClick()

    verify(mockBannerAdCallback).reportAdClicked()
    verify(mockBannerAdCallback).onAdOpened()
    verify(mockBannerAdCallback).onAdLeftApplication()
  }
}
