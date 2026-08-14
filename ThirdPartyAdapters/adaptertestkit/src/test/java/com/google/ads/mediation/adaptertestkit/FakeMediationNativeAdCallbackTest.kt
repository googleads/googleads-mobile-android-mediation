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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeMediationNativeAdCallbackTest {

  private val callback = FakeMediationNativeAdCallback()

  @Test
  fun initialState_allValuesDefault() {
    assertThat(callback.isOnAdLeftApplicationInvoked).isFalse()
    assertThat(callback.onAdLeftApplicationInvokeCount).isEqualTo(0)
    assertThat(callback.isLeftApplication).isFalse()

    assertThat(callback.isOnVideoPlayInvoked).isFalse()
    assertThat(callback.onVideoPlayInvokeCount).isEqualTo(0)
    assertThat(callback.isVideoPlaying).isFalse()

    assertThat(callback.isOnVideoPauseInvoked).isFalse()
    assertThat(callback.onVideoPauseInvokeCount).isEqualTo(0)
    assertThat(callback.isVideoPaused).isFalse()

    assertThat(callback.isOnVideoCompleteInvoked).isFalse()
    assertThat(callback.onVideoCompleteInvokeCount).isEqualTo(0)
    assertThat(callback.isVideoCompleted).isFalse()

    assertThat(callback.isOnVideoMuteInvoked).isFalse()
    assertThat(callback.onVideoMuteInvokeCount).isEqualTo(0)
    assertThat(callback.isVideoMuted).isFalse()

    assertThat(callback.isOnVideoUnmuteInvoked).isFalse()
    assertThat(callback.onVideoUnmuteInvokeCount).isEqualTo(0)
    assertThat(callback.isVideoUnmuted).isFalse()
  }

  @Test
  fun onAdLeftApplication_recordsInvocationAndCount() {
    callback.onAdLeftApplication()

    assertThat(callback.isOnAdLeftApplicationInvoked).isTrue()
    assertThat(callback.onAdLeftApplicationInvokeCount).isEqualTo(1)
    assertThat(callback.isLeftApplication).isTrue()
  }

  @Test
  fun videoEvents_recordsInvocationsAndCounts() {
    callback.onVideoPlay()
    assertThat(callback.isOnVideoPlayInvoked).isTrue()
    assertThat(callback.onVideoPlayInvokeCount).isEqualTo(1)
    assertThat(callback.isVideoPlaying).isTrue()

    callback.onVideoPause()
    assertThat(callback.isOnVideoPauseInvoked).isTrue()
    assertThat(callback.onVideoPauseInvokeCount).isEqualTo(1)
    assertThat(callback.isVideoPaused).isTrue()

    callback.onVideoComplete()
    assertThat(callback.isOnVideoCompleteInvoked).isTrue()
    assertThat(callback.onVideoCompleteInvokeCount).isEqualTo(1)
    assertThat(callback.isVideoCompleted).isTrue()

    callback.onVideoMute()
    assertThat(callback.isOnVideoMuteInvoked).isTrue()
    assertThat(callback.onVideoMuteInvokeCount).isEqualTo(1)
    assertThat(callback.isVideoMuted).isTrue()

    callback.onVideoUnmute()
    assertThat(callback.isOnVideoUnmuteInvoked).isTrue()
    assertThat(callback.onVideoUnmuteInvokeCount).isEqualTo(1)
    assertThat(callback.isVideoUnmuted).isTrue()
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
