// Copyright 2024 Google LLC
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

package com.google.ads.mediation.adaptertestkit

import com.google.android.gms.ads.AdError
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeMediationInterstitialAdCallbackTest {

  private val callback = FakeMediationInterstitialAdCallback()

  @Test
  fun initialState_allValuesDefault() {
    assertThat(callback.isOnAdLeftApplicationInvoked).isFalse()
    assertThat(callback.onAdLeftApplicationInvokeCount).isEqualTo(0)
    assertThat(callback.isOnAdFailedToShowInvoked).isFalse()
    assertThat(callback.onAdFailedToShowInvokeCount).isEqualTo(0)
    assertThat(callback.adError).isNull()
    assertThat(callback.error).isNull()
    assertThat(callback.isLeftApplication).isFalse()
    assertThat(callback.isFailedToShow).isFalse()
    assertThat(callback.adFailedToShowError).isNull()
  }

  @Test
  fun onAdLeftApplication_recordsInvocationAndCount() {
    callback.onAdLeftApplication()

    assertThat(callback.isOnAdLeftApplicationInvoked).isTrue()
    assertThat(callback.onAdLeftApplicationInvokeCount).isEqualTo(1)
    assertThat(callback.isLeftApplication).isTrue()
  }

  @Test
  fun onAdFailedToShow_recordsInvocationAndError() {
    val error = AdError(101, "Failed to show interstitial", "com.google.ads.mediation")

    callback.onAdFailedToShow(error)

    assertThat(callback.isOnAdFailedToShowInvoked).isTrue()
    assertThat(callback.onAdFailedToShowInvokeCount).isEqualTo(1)
    assertThat(callback.isFailedToShow).isTrue()
    assertThat(callback.adError).isEqualTo(error)
    assertThat(callback.error).isEqualTo(error)
    assertThat(callback.adFailedToShowError).isEqualTo(error)
  }

  @Test
  fun inheritsBaseEvents() {
    callback.reportAdClicked()
    callback.reportAdImpression()
    callback.onAdOpened()
    callback.onAdClosed()

    assertThat(callback.isReportAdClickedInvoked).isTrue()
    assertThat(callback.isReportAdImpressionInvoked).isTrue()
    assertThat(callback.isOnAdOpenedInvoked).isTrue()
    assertThat(callback.isOnAdClosedInvoked).isTrue()
  }
}
