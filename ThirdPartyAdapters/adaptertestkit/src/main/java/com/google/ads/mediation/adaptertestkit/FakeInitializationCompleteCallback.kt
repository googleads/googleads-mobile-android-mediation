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

import com.google.android.gms.ads.mediation.InitializationCompleteCallback

/**
 * Fake implementation of [InitializationCompleteCallback] that records adapter initialization
 * callback invocations.
 */
open class FakeInitializationCompleteCallback : InitializationCompleteCallback {

  var isInitializationSucceededInvoked: Boolean = false
    private set

  var initializationSucceededInvokeCount: Int = 0
    private set

  var isInitializationFailedInvoked: Boolean = false
    private set

  var initializationFailedInvokeCount: Int = 0
    private set

  var error: String? = null
    private set

  /** Returns true if [onInitializationSucceeded] was invoked. */
  val isInitializationSucceeded: Boolean
    get() = isInitializationSucceededInvoked

  /** Returns true if [onInitializationFailed] was invoked. */
  val isInitializationFailed: Boolean
    get() = isInitializationFailedInvoked

  override fun onInitializationSucceeded() {
    isInitializationSucceededInvoked = true
    initializationSucceededInvokeCount++
  }

  override fun onInitializationFailed(error: String) {
    isInitializationFailedInvoked = true
    initializationFailedInvokeCount++
    this.error = error
  }
}
