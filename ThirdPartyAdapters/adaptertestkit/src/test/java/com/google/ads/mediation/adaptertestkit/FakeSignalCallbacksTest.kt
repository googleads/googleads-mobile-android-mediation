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
class FakeSignalCallbacksTest {

  private val callback = FakeSignalCallbacks()

  @Test
  fun initialState_allValuesDefault() {
    assertThat(callback.isSuccessInvoked).isFalse()
    assertThat(callback.isSuccess).isFalse()
    assertThat(callback.successInvokeCount).isEqualTo(0)
    assertThat(callback.isFailureInvoked).isFalse()
    assertThat(callback.isFailure).isFalse()
    assertThat(callback.failureInvokeCount).isEqualTo(0)
    assertThat(callback.signals).isNull()
    assertThat(callback.adError).isNull()
    assertThat(callback.error).isNull()
  }

  @Test
  fun onSuccess_recordsSuccessAndSignals() {
    val signals = "encoded_signal_token_12345"

    callback.onSuccess(signals)

    assertThat(callback.isSuccessInvoked).isTrue()
    assertThat(callback.isSuccess).isTrue()
    assertThat(callback.successInvokeCount).isEqualTo(1)
    assertThat(callback.signals).isEqualTo(signals)

    callback.onSuccess("second_signals")

    assertThat(callback.isSuccessInvoked).isTrue()
    assertThat(callback.successInvokeCount).isEqualTo(2)
    assertThat(callback.signals).isEqualTo("second_signals")
  }

  @Test
  fun onFailure_recordsFailureAndAdError() {
    val error = AdError(301, "Failed to collect RTB signals", "com.google.ads.mediation")

    callback.onFailure(error)

    assertThat(callback.isFailureInvoked).isTrue()
    assertThat(callback.isFailure).isTrue()
    assertThat(callback.failureInvokeCount).isEqualTo(1)
    assertThat(callback.adError).isEqualTo(error)
    assertThat(callback.error).isEqualTo(error)

    val secondError = AdError(302, "Another signal error", "com.google.ads.mediation")
    callback.onFailure(secondError)

    assertThat(callback.isFailureInvoked).isTrue()
    assertThat(callback.failureInvokeCount).isEqualTo(2)
    assertThat(callback.adError).isEqualTo(secondError)
    assertThat(callback.error).isEqualTo(secondError)
  }
}
