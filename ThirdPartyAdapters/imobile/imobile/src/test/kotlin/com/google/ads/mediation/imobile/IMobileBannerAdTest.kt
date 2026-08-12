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
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.FailNotificationReason
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the APIs implemented by [IMobileBannerAd]. */
@RunWith(AndroidJUnit4::class)
class IMobileBannerAdTest {

  private val bannerAdCallback = FakeMediationBannerAdCallback()

  private val adLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)

  private val iMobileBannerAd = IMobileBannerAd(adLoadCallback)

  // region ImobileSdkAdListener implementation tests
  @Test
  fun onAdReadyCompleted_callsLoadSuccessCallback() {
    iMobileBannerAd.onAdReadyCompleted()

    assertThat(adLoadCallback).hasSucceededWith(iMobileBannerAd)
  }

  @Test
  fun onAdCliclkCompleted_reportsAdClickedAndAdOpenedAndAdLeftApplication() {
    // Call onAdReadyCompleted to set iMobileBannerAd.bannerAdCallback
    iMobileBannerAd.onAdReadyCompleted()

    iMobileBannerAd.onAdCliclkCompleted()

    assertThat(bannerAdCallback.isClicked).isTrue()
    assertThat(bannerAdCallback.isOpened).isTrue()
    assertThat(bannerAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onDismissAdScreen_callsOnAdClosed() {
    // Call onAdReadyCompleted to set iMobileBannerAd.bannerAdCallback
    iMobileBannerAd.onAdReadyCompleted()

    iMobileBannerAd.onDismissAdScreen()

    assertThat(bannerAdCallback.isClosed).isTrue()
  }

  @Test
  fun onFailed_callsLoadFailureCallback() {
    val expectedError = AdapterHelper.getAdError(FailNotificationReason.RESPONSE)

    iMobileBannerAd.onFailed(FailNotificationReason.RESPONSE)

    assertThat(adLoadCallback).hasFailedWith(expectedError)
  }
  // endregion
}
