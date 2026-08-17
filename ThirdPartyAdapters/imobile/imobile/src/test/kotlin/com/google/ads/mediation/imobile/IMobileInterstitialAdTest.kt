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

package com.google.ads.mediation.imobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.FailNotificationReason
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/** Tests for the APIs implemented by [IMobileInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class IMobileInterstitialAdTest {

  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()

  private val adLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )

  private val iMobileSdkWrapper: IMobileSdkWrapper = mock()

  private val iMobileInterstitialAd = IMobileInterstitialAd(adLoadCallback, iMobileSdkWrapper)

  // region ImobileSdkAdListener implementation tests
  @Test
  fun onAdReadyCompleted_callsLoadSuccessCallback() {
    iMobileInterstitialAd.onAdReadyCompleted()

    assertThat(adLoadCallback).hasSucceededWith(iMobileInterstitialAd)
  }

  @Test
  fun onAdShowCompleted_callsOnAdOpened() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdShowCompleted()

    assertThat(interstitialAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdCliclkCompleted_reportsAdClickedAndAdOpenedAndAdLeftApplication() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdCliclkCompleted()

    assertThat(interstitialAdCallback.isClicked).isTrue()
    assertThat(interstitialAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onAdCloseCompleted_callsOnAdClosed() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdCloseCompleted()

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onFailed_callsLoadFailureCallback() {
    val expectedError = AdapterHelper.getAdError(FailNotificationReason.RESPONSE)

    iMobileInterstitialAd.onFailed(FailNotificationReason.RESPONSE)

    assertThat(adLoadCallback).hasFailedWith(expectedError)
  }
  // endregion
}
