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
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks

/**
 * Fake implementation of [SignalCallbacks] that records RTB signal collection success and failure
 * events.
 */
open class FakeSignalCallbacks : SignalCallbacks {

  var isSuccessInvoked: Boolean = false
    private set

  var successInvokeCount: Int = 0
    private set

  var isFailureInvoked: Boolean = false
    private set

  var failureInvokeCount: Int = 0
    private set

  var signals: String? = null
    private set

  var adError: AdError? = null
    private set

  /** The error passed to [onFailure], or null if not invoked. */
  val error: AdError?
    get() = adError

  /** Returns true if [onSuccess] was invoked. */
  val isSuccess: Boolean
    get() = isSuccessInvoked

  /** Returns true if [onFailure] was invoked. */
  val isFailure: Boolean
    get() = isFailureInvoked

  override fun onSuccess(signals: String) {
    isSuccessInvoked = true
    successInvokeCount++
    this.signals = signals
  }

  override fun onFailure(adError: AdError) {
    isFailureInvoked = true
    failureInvokeCount++
    this.adError = adError
  }
}
