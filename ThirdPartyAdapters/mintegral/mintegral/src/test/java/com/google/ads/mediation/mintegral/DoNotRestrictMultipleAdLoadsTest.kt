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

package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.MintegralMediationAdapterTest.SynchronousExecutorService
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import kotlin.use
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Tests the relevant behavior when [FlagValueGetter].shouldRestrictMultipleAdLoads() is false. */
@RunWith(RobolectricTestRunner::class)
class DoNotRestrictMultipleAdLoadsTest {

  private lateinit var adapter: MintegralMediationAdapter

  private val context = Robolectric.buildActivity(Activity::class.java).get()

  private val splashAdWrapper: MintegralSplashAdWrapper = mock()

  private val interstitialAdWrapper: MintegralNewInterstitialAdWrapper = mock()

  private val bidInterstitialAdWrapper: MintegralBidNewInterstitialAdWrapper = mock()

  private val rewardedAdWrapper: MintegralRewardedAdWrapper = mock()

  private val bidRewardedAdWrapper: MintegralBidRewardedAdWrapper = mock()

  private val appOpenAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock()

  private val interstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()

  private val rewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()

  private val appOpenAdWaterfallConfig =
    createMediationAppOpenAdConfiguration(
      context = context,
      serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
    )

  private val interstitialAdWaterfallConfig =
    createMediationInterstitialAdConfiguration(
      context = context,
      serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
    )

  private val rewardedAdWaterfallConfig =
    createMediationRewardedAdConfiguration(
      context = context,
      serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
    )

  private val appOpenAdRtbConfig =
    createMediationAppOpenAdConfiguration(
      context = context,
      serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
      bidResponse = TEST_BID_RESPONSE,
    )

  private val interstitialAdRtbConfig =
    createMediationInterstitialAdConfiguration(
      context = context,
      serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
      bidResponse = TEST_BID_RESPONSE,
    )

  private val rewardedAdRtbConfig =
    createMediationRewardedAdConfiguration(
      context = context,
      serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
      bidResponse = TEST_BID_RESPONSE,
    )

  private val signalCallbacks: SignalCallbacks = mock()

  private val appOpenRtbSignalData =
    RtbSignalData(
      context,
      listOf(
        createMediationConfiguration(
          AdFormat.APP_OPEN_AD,
          bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        )
      ),
      bundleOf(),
      null,
    )

  private val interstitialRtbSignalData =
    RtbSignalData(
      context,
      listOf(
        createMediationConfiguration(
          AdFormat.INTERSTITIAL,
          bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        )
      ),
      bundleOf(),
      null,
    )

  private val rewardedRtbSignalData =
    RtbSignalData(
      context,
      listOf(
        createMediationConfiguration(
          AdFormat.REWARDED,
          bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID),
        )
      ),
      bundleOf(),
      null,
    )

  @Before
  fun setUp() {
    adapter =
      MintegralMediationAdapter(
        SynchronousExecutorService(),
        /*flagValueGetter=*/ mock { on { shouldRestrictMultipleAdLoads() } doReturn false },
      )
  }

  @Test
  fun loadSecondAppOpenWaterfallAd_ifPreviousAdIsLoadedAndNotYetShown_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

      verify(splashAdWrapper, times(2)).preLoad()
    }
  }

  @Test
  fun collectSignalsForSecondAppOpenRtbAd_ifPreviousAdLoadedAndNotYetShown_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadRtbAppOpenAd(appOpenAdRtbConfig, appOpenAdLoadCallback)

      adapter.collectSignals(appOpenRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun loadSecondInterstitialWaterfallAd_ifPreviousAdIsLoadedAndNotYetShown_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createInterstitialHandler()) doReturn interstitialAdWrapper
      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)

      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)

      verify(interstitialAdWrapper, times(2)).load()
    }
  }

  @Test
  fun collectSignalsForSecondInterstitialRtbAd_ifPreviousAdLoadedAndNotYetShown_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createBidInterstitialHandler()) doReturn bidInterstitialAdWrapper
      adapter.loadRtbInterstitialAd(interstitialAdRtbConfig, interstitialAdLoadCallback)

      adapter.collectSignals(interstitialRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun loadSecondRewardedWaterfallAd_ifPreviousAdIsLoadedAndNotYetShown_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralRewardedAdWrapper()) doReturn rewardedAdWrapper
      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)

      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)

      verify(rewardedAdWrapper, times(2)).load()
    }
  }

  @Test
  fun collectSignalsForSecondRewardedRtbAd_ifPreviousAdLoadedAndNotYetShown_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralBidRewardedAdWrapper()) doReturn bidRewardedAdWrapper
      adapter.loadRtbRewardedAd(rewardedAdRtbConfig, rewardedAdLoadCallback)

      adapter.collectSignals(rewardedRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }
}
