// Copyright 2025 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.facebook.rtb

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.Ad
import com.facebook.ads.AdError.AD_PRESENTATION_ERROR
import com.facebook.ads.AdError.NO_FILL
import com.facebook.ads.InterstitialAd
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FAILED_TO_PRESENT_AD
import com.google.ads.mediation.facebook.FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN
import com.google.ads.mediation.facebook.FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER
import com.google.ads.mediation.facebook.MetaFactory
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [MetaRtbAppOpenAd]. */
@RunWith(AndroidJUnit4::class)
class MetaRtbAppOpenAdTest {

  /** The unit under test. */
  private lateinit var adapterAppOpenAd: MetaRtbAppOpenAd

  private lateinit var mediationAppOpenAdConfig: MediationAppOpenAdConfiguration
  private val serverParameters = Bundle()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mediationAppOpenAdCallback: MediationAppOpenAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mediationAppOpenAdCallback
    }
  // Meta SDK uses InterstitialAd for displaying app open ads.
  private val metaAppOpenAdLoadConfigBuilder: InterstitialAd.InterstitialAdLoadConfigBuilder =
    mock {
      on { withBid(any()) } doReturn this.mock
      on { withAdListener(any()) } doReturn this.mock
    }
  private val metaAppOpenAd: InterstitialAd = mock {
    on { buildLoadAdConfig() } doReturn metaAppOpenAdLoadConfigBuilder
  }
  private val metaFactory: MetaFactory = mock {
    on { createAppOpenAd(any(), any()) } doReturn metaAppOpenAd
  }
  private val metaAd: Ad = mock()

  @Before
  fun setUp() {
    serverParameters.putString(RTB_PLACEMENT_PARAMETER, TEST_AD_UNIT)
    mediationAppOpenAdConfig =
      createMediationAppOpenAdConfiguration(context, serverParameters = serverParameters)
    adapterAppOpenAd = MetaRtbAppOpenAd(mediationAdLoadCallback, metaFactory)
  }

  @Test
  fun onError_onShowAdAlreadyCalled_invokesOnAdFailedToShowCallback() {
    renderAndLoadSuccessfully()
    // Stub metaAppOpenAd.show() to return true (i.e. we are able to successfully ask Meta's app
    // open ad to be shown).
    whenever(metaAppOpenAd.show()) doReturn true
    // As part of setting this test up, show the ad.
    adapterAppOpenAd.showAd(context)

    // Simulate Meta reporting a show error.
    adapterAppOpenAd.onError(metaAd, AD_PRESENTATION_ERROR)

    val expectedAdError =
      AdError(
        AD_PRESENTATION_ERROR.errorCode,
        AD_PRESENTATION_ERROR.errorMessage,
        FACEBOOK_SDK_ERROR_DOMAIN,
      )
    verify(mediationAppOpenAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onError_onShowAdNotYetCalled_invokesAdLoadCallbackWithFailure() {
    renderAndLoadSuccessfully()

    // Simulate a no-fill error.
    adapterAppOpenAd.onError(metaAd, NO_FILL)

    val expectedAdError =
      AdError(NO_FILL.errorCode, NO_FILL.errorMessage, FACEBOOK_SDK_ERROR_DOMAIN)
    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onShowAd_ifAppOpenAdShowFailed_callsOnAdFailedToShow() {
    renderAndLoadSuccessfully()
    // Stub metaAppOpenAd.show() to return false (i.e. we are able to unsuccessful in Meta's app
    // open ad to be shown).
    whenever(metaAppOpenAd.show()) doReturn false
    val expectedAdError =
      AdError(ERROR_FAILED_TO_PRESENT_AD, "Failed to present app open ad.", ERROR_DOMAIN)

    // invoke the showAd callback
    adapterAppOpenAd.showAd(context)

    verify(mediationAppOpenAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAppOpenDisplayed_invokesOnAdOpenedCallback() {
    renderAndLoadSuccessfully()

    adapterAppOpenAd.onInterstitialDisplayed(metaAd)

    verify(mediationAppOpenAdCallback).onAdOpened()
  }

  @Test
  fun onAppOpenDismissed_invokesOnAdClosedCallback() {
    renderAndLoadSuccessfully()

    adapterAppOpenAd.onInterstitialDismissed(metaAd)

    verify(mediationAppOpenAdCallback).onAdClosed()
  }

  @Test
  fun onAppOpenDismissed_adAlreadyClosed_doesNotInvokeOnAdClosedCallbackTwice() {
    renderAndLoadSuccessfully()

    // simulate dismissed already called.
    adapterAppOpenAd.onInterstitialDismissed(metaAd)
    // make a second dismissed call.
    adapterAppOpenAd.onInterstitialDismissed(metaAd)

    verify(mediationAppOpenAdCallback, times(1)).onAdClosed()
  }

  @Test
  fun onAdClicked_invokesReportAdClickedAndOnAdLeftApplicationCallback() {
    renderAndLoadSuccessfully()

    adapterAppOpenAd.onAdClicked(metaAd)

    verify(mediationAppOpenAdCallback).reportAdClicked()
  }

  @Test
  fun onLoggingImpression_invokesReportAdImpressionCallback() {
    renderAndLoadSuccessfully()

    adapterAppOpenAd.onLoggingImpression(metaAd)

    verify(mediationAppOpenAdCallback).reportAdImpression()
  }

  @Test
  fun onAppOpenActivityDestroyed_invokesOnAdClosed() {
    renderAndLoadSuccessfully()

    adapterAppOpenAd.onInterstitialActivityDestroyed()

    verify(mediationAppOpenAdCallback).onAdClosed()
  }

  @Test
  fun onAppOpenActivityDestroyed_adAlreadyClosed_doesNotInvokeOnAdClosedCallbackTwice() {
    renderAndLoadSuccessfully()

    // simulate activity destroyed call
    adapterAppOpenAd.onInterstitialActivityDestroyed()
    // make a second destroyed call
    adapterAppOpenAd.onInterstitialActivityDestroyed()

    verify(mediationAppOpenAdCallback, times(1)).onAdClosed()
  }

  /**
   * Verify there are no exceptions when onRewardedAd callbacks are invoked. Note: All onRewardedAd
   * callbacks are no-ops since this is app open ad.
   */
  @Test
  fun onRewardedAdCallbacks_noException() {
    adapterAppOpenAd.onRewardedAdCompleted()
    adapterAppOpenAd.onRewardedAdServerSucceeded()
    adapterAppOpenAd.onRewardedAdServerFailed()
  }

  private fun renderAndLoadSuccessfully() {
    // As part of setting this test up, render the ad.
    adapterAppOpenAd.loadAd(adConfiguration = mediationAppOpenAdConfig)
    // Simulate Meta ad load success.
    adapterAppOpenAd.onAdLoaded(metaAd)
  }
}
