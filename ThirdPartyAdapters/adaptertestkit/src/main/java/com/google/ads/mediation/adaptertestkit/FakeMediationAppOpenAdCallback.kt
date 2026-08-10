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
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback

/**
 * Fake implementation of [MediationAppOpenAdCallback] that records invocations of app open ad
 * events.
 */
open class FakeMediationAppOpenAdCallback : FakeMediationAdCallback(), MediationAppOpenAdCallback {

  var isOnAdFailedToShowInvoked: Boolean = false
    private set

  var onAdFailedToShowInvokeCount: Int = 0
    private set

  var adError: AdError? = null
    private set

  /** Returns true if [onAdFailedToShow] was invoked. */
  val isFailedToShow: Boolean
    get() = isOnAdFailedToShowInvoked

  /** The error passed to [onAdFailedToShow], or null if not invoked. */
  val adFailedToShowError: AdError?
    get() = adError

  /** Alias for [adError] to support alternative naming conventions. */
  val error: AdError?
    get() = adError

  override fun onAdFailedToShow(adError: AdError) {
    isOnAdFailedToShowInvoked = true
    onAdFailedToShowInvokeCount++
    this.adError = adError
  }
}
