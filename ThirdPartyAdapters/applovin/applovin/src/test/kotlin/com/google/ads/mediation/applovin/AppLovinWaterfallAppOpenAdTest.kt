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

package com.google.ads.mediation.applovin

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.mediation.AppLovinUtils
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_PRESENTATION_AD_NOT_READY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [AppLovinWaterfallAppOpenAd]. */
@RunWith(AndroidJUnit4::class)
class AppLovinWaterfallAppOpenAdTest {

  private val appLovinInitializer: AppLovinInitializer = mock()
  private val appLovinAd: MaxAppOpenAd = mock()
  private val appLovinAdFactory: AppLovinAdFactory = mock {
    on { createMaxAppOpenAd(any()) } doReturn appLovinAd
  }
  private val appOpenAdCallback: MediationAppOpenAdCallback = mock()
  private val adLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn appOpenAdCallback
    }

  /** Unit under test. */
  private val appLovinWaterfallAppOpenAd =
    AppLovinWaterfallAppOpenAd(adLoadCallback, appLovinInitializer, appLovinAdFactory)

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val appOpenAdWaterfallConfig =
    MediationAppOpenAdConfiguration(
      context,
      /*bidResponse=*/ "",
      bundleOf(
        AppLovinUtils.ServerParameterKeys.SDK_KEY to "fake_sdk_key",
        AppLovinUtils.ServerParameterKeys.AD_UNIT_ID to "fake_ad_unit_id",
      ),
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      /*watermark=*/ "",
    )

  // region MediationAppOpenAd implementation tests

  @Test
  fun showAd_ifAdIsReady_showsAppLovinAd() {
    // Mock init success for code to proceed to ad load.
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    // Load ad so that appLovinWaterfallAppOpenAd.appLovinAd is set.
    appLovinWaterfallAppOpenAd.loadAd(appOpenAdWaterfallConfig)
    whenever(appLovinAd.isReady) doReturn true

    appLovinWaterfallAppOpenAd.showAd(context)

    verify(appLovinAd).showAd()
  }

  @Test
  fun showAd_ifAdIsNotReady_invokesShowFailureCallback() {
    // Mock init success for code to proceed to ad load.
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as OnInitializeSuccessListener).onInitializeSuccess()
      }
      .whenever(appLovinInitializer)
      .initialize(any(), any(), any())
    // Load ad so that appLovinWaterfallAppOpenAd.appLovinAd is set.
    appLovinWaterfallAppOpenAd.loadAd(appOpenAdWaterfallConfig)
    // Invoke onAdLoaded() so that appLovinWaterfallAppOpenAd.appOpenAdCallback is set.
    appLovinWaterfallAppOpenAd.onAdLoaded(mock())
    whenever(appLovinAd.isReady) doReturn false

    appLovinWaterfallAppOpenAd.showAd(context)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(appOpenAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    assertThat(adErrorCaptor.firstValue.code).isEqualTo(ERROR_PRESENTATION_AD_NOT_READY)
    assertThat(adErrorCaptor.firstValue.domain).isEqualTo(ERROR_DOMAIN)
  }

  // endregion

  // region MaxAdListener implementation tests

  @Test
  fun onAdLoaded_invokesLoadSuccessCallback() {
    appLovinWaterfallAppOpenAd.onAdLoaded(mock())

    verify(adLoadCallback).onSuccess(appLovinWaterfallAppOpenAd)
  }

  @Test
  fun onAdLoadFailed_invokesLoadFailureCallback() {
    val appLovinAdLoadError = mock<MaxError>()
    // Return a fake error code of 1001 for ad load error.
    whenever(appLovinAdLoadError.code) doReturn 1001

    appLovinWaterfallAppOpenAd.onAdLoadFailed("Ad load failed", appLovinAdLoadError)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(adLoadCallback).onFailure(adErrorCaptor.capture())
    assertThat(adErrorCaptor.firstValue.code).isEqualTo(1001)
    assertThat(adErrorCaptor.firstValue.domain).isEqualTo(APPLOVIN_SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdDisplayed_invokesAdOpenedCallbackAndReportsImpression() {
    // Invoke onAdLoaded() so that appLovinWaterfallAppOpenAd.appOpenAdCallback is set.
    appLovinWaterfallAppOpenAd.onAdLoaded(mock())

    appLovinWaterfallAppOpenAd.onAdDisplayed(mock())

    verify(appOpenAdCallback).onAdOpened()
    verify(appOpenAdCallback).reportAdImpression()
  }

  @Test
  fun onAdHidden_invokesAdClosedCallback() {
    // Invoke onAdLoaded() so that appLovinWaterfallAppOpenAd.appOpenAdCallback is set.
    appLovinWaterfallAppOpenAd.onAdLoaded(mock())

    appLovinWaterfallAppOpenAd.onAdHidden(mock())

    verify(appOpenAdCallback).onAdClosed()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    // Invoke onAdLoaded() so that appLovinWaterfallAppOpenAd.appOpenAdCallback is set.
    appLovinWaterfallAppOpenAd.onAdLoaded(mock())

    appLovinWaterfallAppOpenAd.onAdClicked(mock())

    verify(appOpenAdCallback).reportAdClicked()
  }

  @Test
  fun onAdDisplayFailed_invokesShowFailureCallback() {
    // Invoke onAdLoaded() so that appLovinWaterfallAppOpenAd.appOpenAdCallback is set.
    appLovinWaterfallAppOpenAd.onAdLoaded(mock())
    val appLovinAdDisplayError = mock<MaxError>()
    // Return a fake error code of 1002 for ad display error.
    whenever(appLovinAdDisplayError.code) doReturn 1002

    appLovinWaterfallAppOpenAd.onAdDisplayFailed(mock(), appLovinAdDisplayError)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(appOpenAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    assertThat(adErrorCaptor.firstValue.code).isEqualTo(1002)
    assertThat(adErrorCaptor.firstValue.domain).isEqualTo(APPLOVIN_SDK_ERROR_DOMAIN)
  }

  // endregion
}
