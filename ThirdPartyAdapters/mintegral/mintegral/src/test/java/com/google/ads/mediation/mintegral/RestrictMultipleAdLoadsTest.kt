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
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_CODE_AD_ALREADY_LOADED
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.MintegralMediationAdapterTest.SynchronousExecutorService
import com.google.android.gms.ads.AdError
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
import com.google.common.truth.Truth.assertThat
import com.mbridge.msdk.newinterstitial.out.NewInterstitialWithCodeListener
import com.mbridge.msdk.out.MBSplashLoadWithCodeListener
import com.mbridge.msdk.out.MBSplashShowListener
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardVideoWithCodeListener
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Tests the relevant behavior when [FlagValueGetter].shouldRestrictMultipleAdLoads() is true. */
@RunWith(RobolectricTestRunner::class)
class RestrictMultipleAdLoadsTest {

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
        /*flagValueGetter=*/ mock { on { shouldRestrictMultipleAdLoads() } doReturn true },
      )
  }

  @After
  fun tearDown() {
    // Clear the map of loaded slot identifiers after each test (to avoid one test affecting
    // another).
    MintegralMediationAdapter.loadedSlotIdentifiers.clear()
  }

  @Test
  fun loadSecondAppOpenWaterfallAd_ifPreviousAdIsLoadedAndNotYetShown_failsToLoad() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(appOpenAdLoadCallback).onFailure(adErrorCaptor.capture())
      assertThat(adErrorCaptor.firstValue.code).isEqualTo(ERROR_CODE_AD_ALREADY_LOADED)
      assertThat(adErrorCaptor.firstValue.domain).isEqualTo(ERROR_DOMAIN)
    }
  }

  @Test
  fun loadSecondAppOpenWaterfallAd_ifPreviousAdIsLoadedAndShown_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)
      val mbSplashShowListenerCaptor = argumentCaptor<MBSplashShowListener>()
      verify(splashAdWrapper).setSplashShowListener(mbSplashShowListenerCaptor.capture())
      mbSplashShowListenerCaptor.firstValue.onShowSuccessed(MBridgeIds())

      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

      verify(splashAdWrapper, times(2)).preLoad()
    }
  }

  @Test
  fun loadSecondAppOpenWaterfallAd_ifPreviousAdLoadFailed_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)
      val mbSplashLoadListenerCaptor = argumentCaptor<MBSplashLoadWithCodeListener>()
      verify(splashAdWrapper).setSplashLoadListener(mbSplashLoadListenerCaptor.capture())
      mbSplashLoadListenerCaptor.firstValue.onLoadFailedWithCode(
        MBridgeIds(),
        1001,
        "Load failed",
        1,
      )

      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

      verify(splashAdWrapper, times(2)).preLoad()
    }
  }

  @Test
  fun loadSecondAppOpenWaterfallAd_ifPreviousAdLoadedButShowFailed_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)
      val mbSplashShowListenerCaptor = argumentCaptor<MBSplashShowListener>()
      verify(splashAdWrapper).setSplashShowListener(mbSplashShowListenerCaptor.capture())
      mbSplashShowListenerCaptor.firstValue.onShowFailed(MBridgeIds(), "Show failed")

      adapter.loadAppOpenAd(appOpenAdWaterfallConfig, appOpenAdLoadCallback)

      verify(splashAdWrapper, times(2)).preLoad()
    }
  }

  @Test
  fun collectSignalsForSecondAppOpenRtbAd_ifPreviousAdLoadedAndNotYetShown_fails() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadRtbAppOpenAd(appOpenAdRtbConfig, appOpenAdLoadCallback)

      adapter.collectSignals(appOpenRtbSignalData, signalCallbacks)

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(signalCallbacks).onFailure(adErrorCaptor.capture())
      assertThat(adErrorCaptor.firstValue.code).isEqualTo(ERROR_CODE_AD_ALREADY_LOADED)
      assertThat(adErrorCaptor.firstValue.domain).isEqualTo(ERROR_DOMAIN)
    }
  }

  @Test
  fun collectSignalsForSecondAppOpenRtbAd_ifPreviousAdLoadedAndShown_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadRtbAppOpenAd(appOpenAdRtbConfig, appOpenAdLoadCallback)
      val mbSplashShowListenerCaptor = argumentCaptor<MBSplashShowListener>()
      verify(splashAdWrapper).setSplashShowListener(mbSplashShowListenerCaptor.capture())
      mbSplashShowListenerCaptor.firstValue.onShowSuccessed(MBridgeIds())

      adapter.collectSignals(appOpenRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun collectSignalsForSecondAppOpenRtbAd_ifPreviousAdLoadFailed_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadRtbAppOpenAd(appOpenAdRtbConfig, appOpenAdLoadCallback)
      val mbSplashLoadListenerCaptor = argumentCaptor<MBSplashLoadWithCodeListener>()
      verify(splashAdWrapper).setSplashLoadListener(mbSplashLoadListenerCaptor.capture())
      mbSplashLoadListenerCaptor.firstValue.onLoadFailedWithCode(
        MBridgeIds(),
        1001,
        "Load failed",
        1,
      )

      adapter.collectSignals(appOpenRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun collectSignalsForSecondAppOpenRtbAd_ifPreviousAdLoadedButShowFailed_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn splashAdWrapper
      adapter.loadRtbAppOpenAd(appOpenAdRtbConfig, appOpenAdLoadCallback)
      val mbSplashShowListenerCaptor = argumentCaptor<MBSplashShowListener>()
      verify(splashAdWrapper).setSplashShowListener(mbSplashShowListenerCaptor.capture())
      mbSplashShowListenerCaptor.firstValue.onShowFailed(MBridgeIds(), "Show failed")

      adapter.collectSignals(appOpenRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun loadSecondInterstitialWaterfallAd_ifPreviousAdIsLoadedAndNotYetShown_failsToLoad() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createInterstitialHandler()) doReturn interstitialAdWrapper
      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)

      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(interstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
      assertThat(adErrorCaptor.firstValue.code).isEqualTo(ERROR_CODE_AD_ALREADY_LOADED)
      assertThat(adErrorCaptor.firstValue.domain).isEqualTo(ERROR_DOMAIN)
    }
  }

  @Test
  fun loadSecondInterstitialWaterfallAd_ifPreviousAdIsLoadedAndShown_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createInterstitialHandler()) doReturn interstitialAdWrapper
      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)
      val interstitialAdListenerCaptor = argumentCaptor<NewInterstitialWithCodeListener>()
      verify(interstitialAdWrapper)
        .setInterstitialVideoListener(interstitialAdListenerCaptor.capture())
      interstitialAdListenerCaptor.firstValue.onAdShow(MBridgeIds())

      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)

      verify(interstitialAdWrapper, times(2)).load()
    }
  }

  @Test
  fun loadSecondInterstitialWaterfallAd_ifPreviousAdLoadFailed_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createInterstitialHandler()) doReturn interstitialAdWrapper
      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)
      val interstitialAdListenerCaptor = argumentCaptor<NewInterstitialWithCodeListener>()
      verify(interstitialAdWrapper)
        .setInterstitialVideoListener(interstitialAdListenerCaptor.capture())
      interstitialAdListenerCaptor.firstValue.onResourceLoadFailWithCode(
        MBridgeIds(),
        1001,
        "Load failed",
      )

      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)

      verify(interstitialAdWrapper, times(2)).load()
    }
  }

  @Test
  fun loadSecondInterstitialWaterfallAd_ifPreviousAdLoadedButShowFailed_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createInterstitialHandler()) doReturn interstitialAdWrapper
      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)
      val interstitialAdListenerCaptor = argumentCaptor<NewInterstitialWithCodeListener>()
      verify(interstitialAdWrapper)
        .setInterstitialVideoListener(interstitialAdListenerCaptor.capture())
      interstitialAdListenerCaptor.firstValue.onShowFailWithCode(MBridgeIds(), 1002, "Show failed")

      adapter.loadInterstitialAd(interstitialAdWaterfallConfig, interstitialAdLoadCallback)

      verify(interstitialAdWrapper, times(2)).load()
    }
  }

  @Test
  fun collectSignalsForSecondInterstitialRtbAd_ifPreviousAdLoadedAndNotYetShown_fails() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createBidInterstitialHandler()) doReturn bidInterstitialAdWrapper
      adapter.loadRtbInterstitialAd(interstitialAdRtbConfig, interstitialAdLoadCallback)

      adapter.collectSignals(interstitialRtbSignalData, signalCallbacks)

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(signalCallbacks).onFailure(adErrorCaptor.capture())
      assertThat(adErrorCaptor.firstValue.code).isEqualTo(ERROR_CODE_AD_ALREADY_LOADED)
      assertThat(adErrorCaptor.firstValue.domain).isEqualTo(ERROR_DOMAIN)
    }
  }

  @Test
  fun collectSignalsForSecondInterstitialRtbAd_ifPreviousAdLoadedAndShown_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createBidInterstitialHandler()) doReturn bidInterstitialAdWrapper
      adapter.loadRtbInterstitialAd(interstitialAdRtbConfig, interstitialAdLoadCallback)
      val interstitialAdListenerCaptor = argumentCaptor<NewInterstitialWithCodeListener>()
      verify(bidInterstitialAdWrapper)
        .setInterstitialVideoListener(interstitialAdListenerCaptor.capture())
      interstitialAdListenerCaptor.firstValue.onAdShow(MBridgeIds())

      adapter.collectSignals(interstitialRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun collectSignalsForSecondInterstitialRtbAd_ifPreviousAdLoadFailed_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createBidInterstitialHandler()) doReturn bidInterstitialAdWrapper
      adapter.loadRtbInterstitialAd(interstitialAdRtbConfig, interstitialAdLoadCallback)
      val interstitialAdListenerCaptor = argumentCaptor<NewInterstitialWithCodeListener>()
      verify(bidInterstitialAdWrapper)
        .setInterstitialVideoListener(interstitialAdListenerCaptor.capture())
      interstitialAdListenerCaptor.firstValue.onResourceLoadFailWithCode(
        MBridgeIds(),
        1001,
        "Load failed",
      )

      adapter.collectSignals(interstitialRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun collectSignalsForSecondInterstitialRtbAd_ifPreviousAdLoadedButShowFailed_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createBidInterstitialHandler()) doReturn bidInterstitialAdWrapper
      adapter.loadRtbInterstitialAd(interstitialAdRtbConfig, interstitialAdLoadCallback)
      val interstitialAdListenerCaptor = argumentCaptor<NewInterstitialWithCodeListener>()
      verify(bidInterstitialAdWrapper)
        .setInterstitialVideoListener(interstitialAdListenerCaptor.capture())
      interstitialAdListenerCaptor.firstValue.onShowFailWithCode(MBridgeIds(), 1002, "Show failed")

      adapter.collectSignals(interstitialRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun loadSecondRewardedWaterfallAd_ifPreviousAdIsLoadedAndNotYetShown_failsToLoad() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralRewardedAdWrapper()) doReturn rewardedAdWrapper
      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)

      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(rewardedAdLoadCallback).onFailure(adErrorCaptor.capture())
      assertThat(adErrorCaptor.firstValue.code).isEqualTo(ERROR_CODE_AD_ALREADY_LOADED)
      assertThat(adErrorCaptor.firstValue.domain).isEqualTo(ERROR_DOMAIN)
    }
  }

  @Test
  fun loadSecondRewardedWaterfallAd_ifPreviousAdIsLoadedAndShown_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralRewardedAdWrapper()) doReturn rewardedAdWrapper
      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)
      val rewardedAdListenerCaptor = argumentCaptor<RewardVideoWithCodeListener>()
      verify(rewardedAdWrapper).setRewardVideoListener(rewardedAdListenerCaptor.capture())
      rewardedAdListenerCaptor.firstValue.onAdShow(MBridgeIds())

      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)

      verify(rewardedAdWrapper, times(2)).load()
    }
  }

  @Test
  fun loadSecondRewardedWaterfallAd_ifPreviousAdLoadFailed_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralRewardedAdWrapper()) doReturn rewardedAdWrapper
      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)
      val rewardedAdListenerCaptor = argumentCaptor<RewardVideoWithCodeListener>()
      verify(rewardedAdWrapper).setRewardVideoListener(rewardedAdListenerCaptor.capture())
      rewardedAdListenerCaptor.firstValue.onVideoLoadFailWithCode(MBridgeIds(), 1001, "Load failed")

      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)

      verify(rewardedAdWrapper, times(2)).load()
    }
  }

  @Test
  fun loadSecondRewardedWaterfallAd_ifPreviousAdLoadedButShowFailed_loads() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralRewardedAdWrapper()) doReturn rewardedAdWrapper
      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)
      val rewardedAdListenerCaptor = argumentCaptor<RewardVideoWithCodeListener>()
      verify(rewardedAdWrapper).setRewardVideoListener(rewardedAdListenerCaptor.capture())
      rewardedAdListenerCaptor.firstValue.onShowFailWithCode(MBridgeIds(), 1002, "Show failed")

      adapter.loadRewardedAd(rewardedAdWaterfallConfig, rewardedAdLoadCallback)

      verify(rewardedAdWrapper, times(2)).load()
    }
  }

  @Test
  fun collectSignalsForSecondRewardedRtbAd_ifPreviousAdLoadedAndNotYetShown_fails() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralBidRewardedAdWrapper()) doReturn bidRewardedAdWrapper
      adapter.loadRtbRewardedAd(rewardedAdRtbConfig, rewardedAdLoadCallback)

      adapter.collectSignals(rewardedRtbSignalData, signalCallbacks)

      val adErrorCaptor = argumentCaptor<AdError>()
      verify(signalCallbacks).onFailure(adErrorCaptor.capture())
      assertThat(adErrorCaptor.firstValue.code).isEqualTo(ERROR_CODE_AD_ALREADY_LOADED)
      assertThat(adErrorCaptor.firstValue.domain).isEqualTo(ERROR_DOMAIN)
    }
  }

  @Test
  fun collectSignalsForSecondRewardedRtbAd_ifPreviousAdLoadedAndShown_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralBidRewardedAdWrapper()) doReturn bidRewardedAdWrapper
      adapter.loadRtbRewardedAd(rewardedAdRtbConfig, rewardedAdLoadCallback)
      val rewardedAdListenerCaptor = argumentCaptor<RewardVideoWithCodeListener>()
      verify(bidRewardedAdWrapper).setRewardVideoListener(rewardedAdListenerCaptor.capture())
      rewardedAdListenerCaptor.firstValue.onAdShow(MBridgeIds())

      adapter.collectSignals(rewardedRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun collectSignalsForSecondRewardedRtbAd_ifPreviousAdLoadFailed_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralBidRewardedAdWrapper()) doReturn bidRewardedAdWrapper
      adapter.loadRtbRewardedAd(rewardedAdRtbConfig, rewardedAdLoadCallback)
      val rewardedAdListenerCaptor = argumentCaptor<RewardVideoWithCodeListener>()
      verify(bidRewardedAdWrapper).setRewardVideoListener(rewardedAdListenerCaptor.capture())
      rewardedAdListenerCaptor.firstValue.onVideoLoadFailWithCode(MBridgeIds(), 1001, "Load failed")

      adapter.collectSignals(rewardedRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }

  @Test
  fun collectSignalsForSecondRewardedRtbAd_ifPreviousAdLoadedButShowFailed_succeeds() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createMintegralBidRewardedAdWrapper()) doReturn bidRewardedAdWrapper
      adapter.loadRtbRewardedAd(rewardedAdRtbConfig, rewardedAdLoadCallback)
      val rewardedAdListenerCaptor = argumentCaptor<RewardVideoWithCodeListener>()
      verify(bidRewardedAdWrapper).setRewardVideoListener(rewardedAdListenerCaptor.capture())
      rewardedAdListenerCaptor.firstValue.onShowFailWithCode(MBridgeIds(), 1002, "Show failed")

      adapter.collectSignals(rewardedRtbSignalData, signalCallbacks)

      verify(signalCallbacks).onSuccess(any())
    }
  }
}
