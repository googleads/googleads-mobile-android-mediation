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

package com.google.ads.mediation.vungle.waterfall

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_CANNOT_PLAY_AD
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleError
import com.vungle.ads.internal.protos.Sdk.SDKError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Tests for [VungleWaterfallInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class VungleWaterfallInterstitialAdTest {

  /** Unit under test. */
  private lateinit var adapterWaterfallInterstitialAd: VungleWaterfallInterstitialAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val interstitialAdCallback = mock<MediationInterstitialAdCallback>()
  private val interstitialAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>> {
      on { onSuccess(any()) } doReturn interstitialAdCallback
    }
  private val vungleInitializer = mock<VungleInitializer>()
  private val vungleInterstitialAd = mock<InterstitialAd>()
  private val vungleFactory =
    mock<VungleFactory> {
      on { createInterstitialAd(any(), any(), any()) } doReturn vungleInterstitialAd
      on { createAdConfig() } doReturn mock()
    }
  private val mediationInterstitialAdConfiguration =
    createMediationInterstitialAdConfiguration(
      context = context,
      serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
    )

  @Before
  fun setUp() {
    adapterWaterfallInterstitialAd =
      VungleWaterfallInterstitialAd(interstitialAdLoadCallback, vungleFactory)

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(vungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_callsLoadSuccess() {
    adapterWaterfallInterstitialAd.onAdLoaded(vungleInterstitialAd)

    verify(interstitialAdLoadCallback).onSuccess(adapterWaterfallInterstitialAd)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.API_REQUEST_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK interstitial ad load failed."
      }

    adapterWaterfallInterstitialAd.onAdFailedToLoad(vungleInterstitialAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(interstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun showAd_playsLiftoffAd() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterWaterfallInterstitialAd.render(mediationInterstitialAdConfiguration)
    }
    adapterWaterfallInterstitialAd.showAd(context)

    verify(vungleInterstitialAd).play(context)
  }

  @Test
  fun showAd_whenInterstitialAdIsNull_invokesOnAdFailedToShow() {
    adapterWaterfallInterstitialAd.onAdLoaded(vungleInterstitialAd)

    adapterWaterfallInterstitialAd.showAd(context)

    val expectedError =
      AdError(
        ERROR_CANNOT_PLAY_AD,
        "Failed to show interstitial ad from Liftoff Monetize.",
        ERROR_DOMAIN,
      )
    verify(interstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  private fun renderAdAndMockLoadSuccess() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterWaterfallInterstitialAd.render(mediationInterstitialAdConfiguration)
    }
    adapterWaterfallInterstitialAd.onAdLoaded(vungleInterstitialAd)
  }

  @Test
  fun onAdStart_callsOnAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallInterstitialAd.onAdStart(vungleInterstitialAd)

    verify(interstitialAdCallback).onAdOpened()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdEnd_callsOnAdClosed() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallInterstitialAd.onAdEnd(vungleInterstitialAd)

    verify(interstitialAdCallback).onAdClosed()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallInterstitialAd.onAdClicked(vungleInterstitialAd)

    verify(interstitialAdCallback).reportAdClicked()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdLeftApplication_callsOnAdLeftApplication() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallInterstitialAd.onAdLeftApplication(vungleInterstitialAd)
    verify(interstitialAdCallback).onAdLeftApplication()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdFailedToPlay_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.AD_NOT_LOADED_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK ad play failed."
      }

    adapterWaterfallInterstitialAd.onAdFailedToPlay(vungleInterstitialAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(interstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallInterstitialAd.onAdImpression(vungleInterstitialAd)

    verify(interstitialAdCallback).reportAdImpression()
    verifyNoMoreInteractions(interstitialAdCallback)
  }
}
