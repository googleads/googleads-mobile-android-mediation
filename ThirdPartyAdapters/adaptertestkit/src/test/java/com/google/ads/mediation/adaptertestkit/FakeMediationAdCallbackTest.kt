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
class FakeMediationAdCallbackTest {

  private val callback = FakeMediationAdCallback()

  @Test
  fun initialState_allValuesDefault() {
    assertThat(callback.isReportAdClickedInvoked).isFalse()
    assertThat(callback.reportAdClickedInvokeCount).isEqualTo(0)
    assertThat(callback.isClicked).isFalse()
    assertThat(callback.clickInvokeCount).isEqualTo(0)

    assertThat(callback.isReportAdImpressionInvoked).isFalse()
    assertThat(callback.reportAdImpressionInvokeCount).isEqualTo(0)
    assertThat(callback.isImpressionReported).isFalse()
    assertThat(callback.impressionInvokeCount).isEqualTo(0)

    assertThat(callback.isOnAdOpenedInvoked).isFalse()
    assertThat(callback.onAdOpenedInvokeCount).isEqualTo(0)
    assertThat(callback.isOpened).isFalse()

    assertThat(callback.isOnAdClosedInvoked).isFalse()
    assertThat(callback.onAdClosedInvokeCount).isEqualTo(0)
    assertThat(callback.isClosed).isFalse()
  }

  @Test
  fun reportAdClicked_recordsInvocationAndCount() {
    callback.reportAdClicked()

    assertThat(callback.isReportAdClickedInvoked).isTrue()
    assertThat(callback.reportAdClickedInvokeCount).isEqualTo(1)
    assertThat(callback.isClicked).isTrue()
    assertThat(callback.clickInvokeCount).isEqualTo(1)

    callback.reportAdClicked()

    assertThat(callback.isReportAdClickedInvoked).isTrue()
    assertThat(callback.reportAdClickedInvokeCount).isEqualTo(2)
    assertThat(callback.clickInvokeCount).isEqualTo(2)
  }

  @Test
  fun reportAdImpression_recordsInvocationAndCount() {
    callback.reportAdImpression()

    assertThat(callback.isReportAdImpressionInvoked).isTrue()
    assertThat(callback.reportAdImpressionInvokeCount).isEqualTo(1)
    assertThat(callback.isImpressionReported).isTrue()
    assertThat(callback.impressionInvokeCount).isEqualTo(1)

    callback.reportAdImpression()

    assertThat(callback.isReportAdImpressionInvoked).isTrue()
    assertThat(callback.reportAdImpressionInvokeCount).isEqualTo(2)
    assertThat(callback.impressionInvokeCount).isEqualTo(2)
  }

  @Test
  fun onAdOpened_recordsInvocationAndCount() {
    callback.onAdOpened()

    assertThat(callback.isOnAdOpenedInvoked).isTrue()
    assertThat(callback.onAdOpenedInvokeCount).isEqualTo(1)
    assertThat(callback.isOpened).isTrue()

    callback.onAdOpened()

    assertThat(callback.isOnAdOpenedInvoked).isTrue()
    assertThat(callback.onAdOpenedInvokeCount).isEqualTo(2)
  }

  @Test
  fun onAdClosed_recordsInvocationAndCount() {
    callback.onAdClosed()

    assertThat(callback.isOnAdClosedInvoked).isTrue()
    assertThat(callback.onAdClosedInvokeCount).isEqualTo(1)
    assertThat(callback.isClosed).isTrue()

    callback.onAdClosed()

    assertThat(callback.isOnAdClosedInvoked).isTrue()
    assertThat(callback.onAdClosedInvokeCount).isEqualTo(2)
  }
}
