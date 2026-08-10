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
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeMediationRewardedAdCallbackTest {

  private val callback = FakeMediationRewardedAdCallback()

  @Test
  fun initialState_allValuesDefault() {
    assertThat(callback.isOnUserEarnedRewardInvoked).isFalse()
    assertThat(callback.onUserEarnedRewardInvokeCount).isEqualTo(0)
    assertThat(callback.rewardItem).isNull()
    assertThat(callback.isUserEarnedReward).isFalse()

    assertThat(callback.isOnVideoStartInvoked).isFalse()
    assertThat(callback.onVideoStartInvokeCount).isEqualTo(0)
    assertThat(callback.isVideoStarted).isFalse()

    assertThat(callback.isOnVideoCompleteInvoked).isFalse()
    assertThat(callback.onVideoCompleteInvokeCount).isEqualTo(0)
    assertThat(callback.isVideoCompleted).isFalse()

    assertThat(callback.isOnAdFailedToShowInvoked).isFalse()
    assertThat(callback.onAdFailedToShowInvokeCount).isEqualTo(0)
    assertThat(callback.isFailedToShow).isFalse()
    assertThat(callback.adError).isNull()
    assertThat(callback.error).isNull()
    assertThat(callback.adFailedToShowError).isNull()
  }

  @Test
  @Suppress("DEPRECATION")
  fun onUserEarnedReward_withRewardItem_recordsInvocationAndItem() {
    val reward =
      object : RewardItem {
        override fun getType(): String = "coins"

        override fun getAmount(): Int = 50
      }

    callback.onUserEarnedReward(reward)

    assertThat(callback.isOnUserEarnedRewardInvoked).isTrue()
    assertThat(callback.onUserEarnedRewardInvokeCount).isEqualTo(1)
    assertThat(callback.isUserEarnedReward).isTrue()
    assertThat(callback.rewardItem).isEqualTo(reward)
  }

  @Test
  fun onUserEarnedReward_parameterless_recordsInvocation() {
    callback.onUserEarnedReward()

    assertThat(callback.isOnUserEarnedRewardInvoked).isTrue()
    assertThat(callback.onUserEarnedRewardInvokeCount).isEqualTo(1)
    assertThat(callback.isUserEarnedReward).isTrue()
    assertThat(callback.rewardItem).isNull()
  }

  @Test
  fun onVideoStart_recordsInvocationAndCount() {
    callback.onVideoStart()

    assertThat(callback.isOnVideoStartInvoked).isTrue()
    assertThat(callback.onVideoStartInvokeCount).isEqualTo(1)
    assertThat(callback.isVideoStarted).isTrue()
  }

  @Test
  fun onVideoComplete_recordsInvocationAndCount() {
    callback.onVideoComplete()

    assertThat(callback.isOnVideoCompleteInvoked).isTrue()
    assertThat(callback.onVideoCompleteInvokeCount).isEqualTo(1)
    assertThat(callback.isVideoCompleted).isTrue()
  }

  @Test
  fun onAdFailedToShow_recordsInvocationAndError() {
    val error = AdError(102, "Failed to show rewarded", "com.google.ads.mediation")

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
