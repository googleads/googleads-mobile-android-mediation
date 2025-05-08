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
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
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
  private val mockInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }

  @Before
  fun setUp() {
    val adConfiguration =
      createMediationInterstitialAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
    VerveInterstitialAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess {
      verveInterstitialAd = it
    }
    VerveSdkFactory.delegate = mock {
      on { createHyBidInterstitialAd(context, verveInterstitialAd) } doReturn
        mockHyBidInterstitialAd
    }
  }

  @Test
  fun loadAd_invokesHyBidPrepareAd() {
    verveInterstitialAd.loadAd()

    verify(mockHyBidInterstitialAd).prepareAd(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun showAd_invokesHyBidShow() {
    verveInterstitialAd.loadAd()

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

    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockHyBidInterstitialAd, never()).show()
  }

  @Test
  fun onInterstitialLoaded_invokesOnSuccess() {
    verveInterstitialAd.onInterstitialLoaded()

    verify(mockAdLoadCallback).onSuccess(eq(verveInterstitialAd))
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

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onInterstitialDismissed_invokesOnAdClosed() {
    verveInterstitialAd.onInterstitialLoaded()

    verveInterstitialAd.onInterstitialDismissed()

    verify(mockInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun onInterstitialImpression_invokesReportAdImpression() {
    verveInterstitialAd.onInterstitialLoaded()

    verveInterstitialAd.onInterstitialImpression()

    verify(mockInterstitialAdCallback).onAdOpened()
    verify(mockInterstitialAdCallback).reportAdImpression()
  }

  @Test
  fun onInterstitialClick_invokesOnAdOpenedReportAdClickedAndOnAdLeftApplication() {
    verveInterstitialAd.onInterstitialLoaded()

    verveInterstitialAd.onInterstitialClick()

    verify(mockInterstitialAdCallback).reportAdClicked()
    verify(mockInterstitialAdCallback).onAdLeftApplication()
  }
}
