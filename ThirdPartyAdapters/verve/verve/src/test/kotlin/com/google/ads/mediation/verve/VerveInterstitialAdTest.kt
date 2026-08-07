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
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VerveInterstitialAdTest {
  // Subject of testing.
  private lateinit var verveInterstitialAd: VerveInterstitialAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockHyBidInterstitialAd = mock<HyBidInterstitialAd>()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )

  @Before
  fun setUp() {
    val adConfiguration =
      createMediationInterstitialAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
    VerveInterstitialAd.newInstance(adConfiguration, interstitialAdLoadCallback).onSuccess {
      verveInterstitialAd = it
    }
    VerveSdkFactory.delegate = mock {
      on { createHyBidInterstitialAd(context, verveInterstitialAd) } doReturn
        mockHyBidInterstitialAd
    }
  }

  @Test
  fun loadAd_invokesHyBidPrepareAd() {
    verveInterstitialAd.loadAd(context)

    verify(mockHyBidInterstitialAd).prepareAd(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun showAd_invokesHyBidShow() {
    verveInterstitialAd.loadAd(context)

    verveInterstitialAd.showAd(context)

    verify(mockHyBidInterstitialAd).show()
  }

  @Test
  fun showAd_withNullHyBidInterstitialAd_invokesOnAdFailedToShow() {
    val expectedAdError =
      AdError(
        ERROR_CODE_FULLSCREEN_AD_IS_NULL,
        ERROR_MSG_FULLSCREEN_AD_IS_NULL,
        ADAPTER_ERROR_DOMAIN,
      )
    verveInterstitialAd.onInterstitialLoaded()

    verveInterstitialAd.showAd(context)

    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
    verify(mockHyBidInterstitialAd, never()).show()
  }

  @Test
  fun onInterstitialLoaded_invokesOnSuccess() {
    verveInterstitialAd.onInterstitialLoaded()

    assertThat(interstitialAdLoadCallback).hasSucceededWith(verveInterstitialAd)
  }

  @Test
  fun onInterstitialLoadFailed_invokesOnFailure() {
    val testError = Throwable("TestError")
    val expectedAdError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "Could not load interstitial ad Error: TestError",
        SDK_ERROR_DOMAIN,
      )

    verveInterstitialAd.onInterstitialLoadFailed(testError)

    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onInterstitialDismissed_invokesOnAdClosed() {
    verveInterstitialAd.onInterstitialLoaded()

    verveInterstitialAd.onInterstitialDismissed()

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onInterstitialImpression_invokesReportAdImpression() {
    verveInterstitialAd.onInterstitialLoaded()

    verveInterstitialAd.onInterstitialImpression()

    assertThat(interstitialAdCallback.isOpened).isTrue()
    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onInterstitialClick_invokesOnAdOpenedReportAdClickedAndOnAdLeftApplication() {
    verveInterstitialAd.onInterstitialLoaded()

    verveInterstitialAd.onInterstitialClick()

    assertThat(interstitialAdCallback.isClicked).isTrue()
    assertThat(interstitialAdCallback.isLeftApplication).isTrue()
  }
}
