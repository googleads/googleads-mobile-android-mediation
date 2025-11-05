// Copyright 2023 Google LLC
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
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.vungle.ads.BaseAd
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleError
import com.vungle.ads.internal.protos.Sdk.SDKError
import kotlin.use
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests [VungleWaterfallBannerAd]. */
@RunWith(AndroidJUnit4::class)
class VungleWaterfallBannerAdTest {

  /** Unit under test. */
  private lateinit var adapterWaterfallBannerAd: VungleWaterfallBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val bannerAdCallback = mock<MediationBannerAdCallback>()
  private val bannerAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>> {
      on { onSuccess(any()) } doReturn bannerAdCallback
    }
  private val mockVungleInitializer = mock<VungleInitializer>()
  private val vungleBannerView = mock<VungleBannerView>()
  private val baseAd = mock<BaseAd>()
  private val vungleFactory =
    mock<VungleFactory> { on { createBannerAd(any(), any(), any()) } doReturn vungleBannerView }
  private val bannerAdConfig =
    createMediationBannerAdConfiguration(
      context = context,
      serverParameters =
        bundleOf(
          VungleConstants.KEY_APP_ID to TEST_APP_ID,
          VungleConstants.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        ),
    )

  @Before
  fun setUp() {
    adapterWaterfallBannerAd = VungleWaterfallBannerAd(bannerAdLoadCallback, vungleFactory)

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_addsLiftoffBannerViewToBannerLayoutAndCallsLoadSuccess() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterWaterfallBannerAd.validateParamsAndLoadAd(bannerAdConfig)
    }

    adapterWaterfallBannerAd.onAdLoaded(baseAd)

    verify(bannerAdLoadCallback).onSuccess(adapterWaterfallBannerAd)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.API_REQUEST_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK banner ad load failed."
      }

    adapterWaterfallBannerAd.onAdFailedToLoad(baseAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(bannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  private fun renderAdAndMockLoadSuccess() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterWaterfallBannerAd.validateParamsAndLoadAd(bannerAdConfig)
    }
    adapterWaterfallBannerAd.onAdLoaded(baseAd)
  }

  @Test
  fun onAdClicked_reportsAdClickedAndAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallBannerAd.onAdClicked(baseAd)

    verify(bannerAdCallback).reportAdClicked()
    verify(bannerAdCallback).onAdOpened()
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallBannerAd.onAdImpression(baseAd)

    verify(bannerAdCallback).reportAdImpression()
  }

  @Test
  fun onAdLeftApplication_callsOnAdLeftApplication() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallBannerAd.onAdLeftApplication(baseAd)

    verify(bannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdEnd_noCrash() {
    adapterWaterfallBannerAd.onAdEnd(baseAd)

    // No matching callback exists on the GMA SDK. This test just verifies that there was no crash.
  }

  @Test
  fun onAdStart_noCrash() {
    adapterWaterfallBannerAd.onAdStart(baseAd)

    // No matching callback exists on the GMA SDK. This test just verifies that there was no crash.
  }

  @Test
  fun onAdFailedToPlay_noCrash() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.AD_NOT_LOADED_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK banner ad play failed."
      }

    adapterWaterfallBannerAd.onAdFailedToPlay(baseAd, liftoffError)

    // No matching callback exists on the GMA SDK. This test just verifies that there was no crash.
  }
}
