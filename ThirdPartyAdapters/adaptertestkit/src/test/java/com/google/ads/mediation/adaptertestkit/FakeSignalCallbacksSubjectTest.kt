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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeSignalCallbacksSubjectTest {

  private val expectedError = AdError(301, "Signal failure", "com.google.ads.mediation")

  @Test
  fun hasFailedWith_matchingAdError_succeeds() {
    val callback = FakeSignalCallbacks()
    val actualError = AdError(301, "Signal failure", "com.google.ads.mediation")
    callback.onFailure(actualError)

    assertThat(callback).hasFailedWith(expectedError)
    assertThat(callback).hasFailed()
    assertThat(callback).hasNotSucceeded()
  }

  @Test
  fun hasFailedWith_matchingProperties_succeeds() {
    val callback = FakeSignalCallbacks()
    callback.onFailure(expectedError)

    assertThat(callback).hasFailedWith(301, "com.google.ads.mediation")
    assertThat(callback).hasFailedWith(301, "Signal failure", "com.google.ads.mediation")
  }

  @Test
  fun hasFailedWith_mismatchingError_fails() {
    val callback = FakeSignalCallbacks()
    val differentError = AdError(302, "Different message", "com.google.ads.mediation")
    callback.onFailure(differentError)

    assertThrows(AssertionError::class.java) { assertThat(callback).hasFailedWith(expectedError) }
    assertThrows(AssertionError::class.java) {
      assertThat(callback).hasFailedWith(301, "com.google.ads.mediation")
    }
  }

  @Test
  fun hasSucceededWith_matchingSignals_succeeds() {
    val callback = FakeSignalCallbacks()
    callback.onSuccess("sample_signals_data")

    assertThat(callback).hasSucceeded()
    assertThat(callback).hasSucceededWith("sample_signals_data")
    assertThat(callback).hasNotFailed()
    assertThat(callback).hasNoFailure()
  }

  @Test
  fun hasSucceededWith_mismatchingSignals_fails() {
    val callback = FakeSignalCallbacks()
    callback.onSuccess("sample_signals_data")

    assertThrows(AssertionError::class.java) {
      assertThat(callback).hasSucceededWith("different_signals")
    }
  }

  @Test
  fun hasFailed_whenSuccessInvoked_fails() {
    val callback = FakeSignalCallbacks()
    callback.onSuccess("sample_signals_data")

    assertThrows(AssertionError::class.java) { assertThat(callback).hasFailed() }
    assertThrows(AssertionError::class.java) { assertThat(callback).hasFailedWith(expectedError) }
  }
}
