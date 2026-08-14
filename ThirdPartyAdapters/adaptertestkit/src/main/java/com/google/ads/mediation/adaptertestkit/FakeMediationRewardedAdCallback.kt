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
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardItem

/**
 * Fake implementation of [MediationRewardedAdCallback] that records invocations of rewarded ad
 * events.
 */
open class FakeMediationRewardedAdCallback :
  FakeMediationAdCallback(), MediationRewardedAdCallback {

  var isOnUserEarnedRewardInvoked: Boolean = false
    private set

  var onUserEarnedRewardInvokeCount: Int = 0
    private set

  var rewardItem: RewardItem? = null
    private set

  var isOnVideoStartInvoked: Boolean = false
    private set

  var onVideoStartInvokeCount: Int = 0
    private set

  var isOnVideoCompleteInvoked: Boolean = false
    private set

  var onVideoCompleteInvokeCount: Int = 0
    private set

  var isOnAdFailedToShowInvoked: Boolean = false
    private set

  var onAdFailedToShowInvokeCount: Int = 0
    private set

  var adError: AdError? = null
    private set

  /** Returns true if [onUserEarnedReward] was invoked. */
  val isUserEarnedReward: Boolean
    get() = isOnUserEarnedRewardInvoked

  /** Returns true if [onVideoStart] was invoked. */
  val isVideoStarted: Boolean
    get() = isOnVideoStartInvoked

  /** Returns true if [onVideoComplete] was invoked. */
  val isVideoCompleted: Boolean
    get() = isOnVideoCompleteInvoked

  /** Returns true if [onAdFailedToShow] was invoked. */
  val isFailedToShow: Boolean
    get() = isOnAdFailedToShowInvoked

  /** The error passed to [onAdFailedToShow], or null if not invoked. */
  val adFailedToShowError: AdError?
    get() = adError

  /** Alias for [adError] to support alternative naming conventions. */
  val error: AdError?
    get() = adError

  @Deprecated("Use onUserEarnedReward() instead.", ReplaceWith("onUserEarnedReward()"))
  override fun onUserEarnedReward(rewardItem: RewardItem) {
    isOnUserEarnedRewardInvoked = true
    onUserEarnedRewardInvokeCount++
    this.rewardItem = rewardItem
  }

  override fun onUserEarnedReward() {
    isOnUserEarnedRewardInvoked = true
    onUserEarnedRewardInvokeCount++
  }

  override fun onVideoStart() {
    isOnVideoStartInvoked = true
    onVideoStartInvokeCount++
  }

  override fun onVideoComplete() {
    isOnVideoCompleteInvoked = true
    onVideoCompleteInvokeCount++
  }

  override fun onAdFailedToShow(adError: AdError) {
    isOnAdFailedToShowInvoked = true
    onAdFailedToShowInvokeCount++
    this.adError = adError
  }
}
