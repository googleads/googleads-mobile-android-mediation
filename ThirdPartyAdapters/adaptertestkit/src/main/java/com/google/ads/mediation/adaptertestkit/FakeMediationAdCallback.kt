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

import com.google.android.gms.ads.mediation.MediationAdCallback

/**
 * Base fake implementation of [MediationAdCallback] that records invocations of ad lifecycle
 * events.
 */
open class FakeMediationAdCallback : MediationAdCallback {

  var isReportAdClickedInvoked: Boolean = false
    private set

  var reportAdClickedInvokeCount: Int = 0
    private set

  var isReportAdImpressionInvoked: Boolean = false
    private set

  var reportAdImpressionInvokeCount: Int = 0
    private set

  var isOnAdOpenedInvoked: Boolean = false
    private set

  var onAdOpenedInvokeCount: Int = 0
    private set

  var isOnAdClosedInvoked: Boolean = false
    private set

  var onAdClosedInvokeCount: Int = 0
    private set

  /** Returns true if [reportAdClicked] was invoked. */
  val isClicked: Boolean
    get() = isReportAdClickedInvoked

  /** The number of times [reportAdClicked] was invoked. */
  val clickInvokeCount: Int
    get() = reportAdClickedInvokeCount

  /** Returns true if [reportAdImpression] was invoked. */
  val isImpressionReported: Boolean
    get() = isReportAdImpressionInvoked

  /** The number of times [reportAdImpression] was invoked. */
  val impressionInvokeCount: Int
    get() = reportAdImpressionInvokeCount

  /** Returns true if [onAdOpened] was invoked. */
  val isOpened: Boolean
    get() = isOnAdOpenedInvoked

  /** Returns true if [onAdClosed] was invoked. */
  val isClosed: Boolean
    get() = isOnAdClosedInvoked

  override fun reportAdClicked() {
    isReportAdClickedInvoked = true
    reportAdClickedInvokeCount++
  }

  override fun reportAdImpression() {
    isReportAdImpressionInvoked = true
    reportAdImpressionInvokeCount++
  }

  override fun onAdOpened() {
    isOnAdOpenedInvoked = true
    onAdOpenedInvokeCount++
  }

  override fun onAdClosed() {
    isOnAdClosedInvoked = true
    onAdClosedInvokeCount++
  }
}
