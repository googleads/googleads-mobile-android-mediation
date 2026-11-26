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

package com.google.ads.mediation.bidmachine

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import io.bidmachine.Ad
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.interstitialad.InterstitialAd
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BidMachineInterstitialAdTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val mockBidMachineInstance = mock<BidMachineInstance>()
  private val mockInterstitialAd = mock<InterstitialAd>()

  // Subject of testing.
  private val bidMachineInterstitialAd: BidMachineInterstitialAd = createInterstitialAd()

  // region newInstance tests
  @Test
  fun newInstance_createsPlacementWithPlacementId() {
    assertThat(bidMachineInterstitialAd.placement?.id).isEqualTo(TEST_PLACEMENT_ID)
  }

  @Test
  fun newInstance_withoutPlacementId_createsNoPlacement() {
    assertThat(createInterstitialAd(serverParameters = bundleOf()).placement).isNull()
  }

  // endregion

  // region load tests
  @Test
  fun loadWaterfallAd_loadsInterstitialWithPlacement() {
    bidMachineInterstitialAd.loadWaterfallAd(mockBidMachineInstance)

    verify(mockBidMachineInstance)
      .loadInterstitial(same(bidMachineInterstitialAd.placement), same(bidMachineInterstitialAd))
  }

  @Test
  fun loadRtbAd_loadsInterstitialWithBidResponse() {
    bidMachineInterstitialAd.loadRtbAd(mockBidMachineInstance)

    verify(mockBidMachineInstance)
      .loadInterstitial(eq(TEST_BID_RESPONSE), same(bidMachineInterstitialAd))
  }

  // endregion

  // region AdLoadCallback tests
  @Test
  fun onAdLoaded_setsWatermarkAndListenerAndInvokesOnSuccess() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    verify(mockInterstitialAd as Ad).setExtras(eq(mapOf(WATERMARK_KEY to TEST_WATERMARK)))
    verify(mockInterstitialAd).setListener(same(bidMachineInterstitialAd))
    assertThat(interstitialAdLoadCallback).hasSucceededWith(bidMachineInterstitialAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val bmError = createBMError()

    bidMachineInterstitialAd.onAdLoadFailed(bmError)

    assertThat(interstitialAdLoadCallback)
      .hasFailedWith(AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN))
  }

  // endregion

  // region showAd tests
  @Test
  fun showAd_whenAdCanBeShown_showsInterstitial() {
    whenever((mockInterstitialAd as Ad).canShow()) doReturn true
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.showAd(context)

    verify(mockInterstitialAd).show()
    assertThat(interstitialAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun showAd_whenAdCanNotBeShown_invokesOnAdFailedToShow() {
    whenever((mockInterstitialAd as Ad).canShow()) doReturn false
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.showAd(context)

    verify(mockInterstitialAd, never()).show()
    assertThat(interstitialAdCallback.adFailedToShowError?.code)
      .isEqualTo(ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD)
    assertThat(interstitialAdCallback.adFailedToShowError?.message)
      .isEqualTo(ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD)
  }

  @Test
  fun showAd_beforeLoad_doesNotThrow() {
    bidMachineInterstitialAd.showAd(context)

    assertThat(interstitialAdCallback.isFailedToShow).isFalse()
  }

  // endregion

  // region InterstitialListener tests
  @Test
  fun onAdImpression_invokesReportAdImpressionAndOnAdOpened() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdImpression(mockInterstitialAd)

    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
    assertThat(interstitialAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdShowFailed_invokesOnAdFailedToShow() {
    val bmError = createBMError()
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdShowFailed(mockInterstitialAd, bmError)

    val adError = interstitialAdCallback.adFailedToShowError
    assertThat(adError?.code).isEqualTo(bmError.code)
    assertThat(adError?.message).isEqualTo(bmError.message)
    assertThat(adError?.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdClicked(mockInterstitialAd)

    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdClosed(mockInterstitialAd)

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdPresentedAndExpired_doNotInvokeCallbacks() {
    bidMachineInterstitialAd.onAdLoaded(mockInterstitialAd)

    bidMachineInterstitialAd.onAdPresented(mockInterstitialAd)
    bidMachineInterstitialAd.onAdExpired(mockInterstitialAd)

    assertThat(interstitialAdCallback.isOpened).isFalse()
    assertThat(interstitialAdCallback.isClosed).isFalse()
    assertThat(interstitialAdCallback.isFailedToShow).isFalse()
  }

  // endregion

  private fun createInterstitialAd(
    serverParameters: Bundle = bundleOf(PLACEMENT_ID_KEY to TEST_PLACEMENT_ID)
  ): BidMachineInterstitialAd {
    val adConfiguration =
      createMediationInterstitialAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParameters,
        watermark = TEST_WATERMARK,
      )
    return BidMachineInterstitialAd.newInstance(adConfiguration, interstitialAdLoadCallback)
      .getOrThrow()
  }
}
