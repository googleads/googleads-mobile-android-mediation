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

import com.google.android.gms.ads.mediation.MediationNativeAdCallback

/**
 * Fake implementation of [MediationNativeAdCallback] that records invocations of native ad events.
 */
open class FakeMediationNativeAdCallback : FakeMediationAdCallback(), MediationNativeAdCallback {

  var isOnAdLeftApplicationInvoked: Boolean = false
    private set

  var onAdLeftApplicationInvokeCount: Int = 0
    private set

  var isOnVideoPauseInvoked: Boolean = false
    private set

  var onVideoPauseInvokeCount: Int = 0
    private set

  var isOnVideoPlayInvoked: Boolean = false
    private set

  var onVideoPlayInvokeCount: Int = 0
    private set

  var isOnVideoCompleteInvoked: Boolean = false
    private set

  var onVideoCompleteInvokeCount: Int = 0
    private set

  var isOnVideoMuteInvoked: Boolean = false
    private set

  var onVideoMuteInvokeCount: Int = 0
    private set

  var isOnVideoUnmuteInvoked: Boolean = false
    private set

  var onVideoUnmuteInvokeCount: Int = 0
    private set

  /** Returns true if [onAdLeftApplication] was invoked. */
  val isLeftApplication: Boolean
    get() = isOnAdLeftApplicationInvoked

  /** Returns true if [onVideoPlay] was invoked. */
  val isVideoPlaying: Boolean
    get() = isOnVideoPlayInvoked

  /** Returns true if [onVideoPause] was invoked. */
  val isVideoPaused: Boolean
    get() = isOnVideoPauseInvoked

  /** Returns true if [onVideoComplete] was invoked. */
  val isVideoCompleted: Boolean
    get() = isOnVideoCompleteInvoked

  /** Returns true if [onVideoMute] was invoked. */
  val isVideoMuted: Boolean
    get() = isOnVideoMuteInvoked

  /** Returns true if [onVideoUnmute] was invoked. */
  val isVideoUnmuted: Boolean
    get() = isOnVideoUnmuteInvoked

  override fun onAdLeftApplication() {
    isOnAdLeftApplicationInvoked = true
    onAdLeftApplicationInvokeCount++
  }

  override fun onVideoPause() {
    isOnVideoPauseInvoked = true
    onVideoPauseInvokeCount++
  }

  override fun onVideoPlay() {
    isOnVideoPlayInvoked = true
    onVideoPlayInvokeCount++
  }

  override fun onVideoComplete() {
    isOnVideoCompleteInvoked = true
    onVideoCompleteInvokeCount++
  }

  override fun onVideoMute() {
    isOnVideoMuteInvoked = true
    onVideoMuteInvokeCount++
  }

  override fun onVideoUnmute() {
    isOnVideoUnmuteInvoked = true
    onVideoUnmuteInvokeCount++
  }
}
