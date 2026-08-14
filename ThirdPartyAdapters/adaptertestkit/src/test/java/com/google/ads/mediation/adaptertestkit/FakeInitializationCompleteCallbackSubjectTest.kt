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

import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeInitializationCompleteCallbackSubjectTest {

  @Test
  fun hasSucceeded_whenSuccessInvoked_succeeds() {
    val callback = FakeInitializationCompleteCallback()
    callback.onInitializationSucceeded()

    assertThat(callback).hasSucceeded()
    assertThat(callback).hasNotFailed()
    assertThat(callback).hasNoFailure()
  }

  @Test
  fun hasSucceeded_whenFailedInvoked_fails() {
    val callback = FakeInitializationCompleteCallback()
    callback.onInitializationFailed("Init failed")

    assertThrows(AssertionError::class.java) { assertThat(callback).hasSucceeded() }
  }

  @Test
  fun hasFailedWith_whenFailedInvoked_succeeds() {
    val callback = FakeInitializationCompleteCallback()
    callback.onInitializationFailed("Init failed")

    assertThat(callback).hasFailed()
    assertThat(callback).hasFailedWith("Init failed")
    assertThat(callback).hasNotSucceeded()
  }

  @Test
  fun hasFailedWith_mismatchingError_fails() {
    val callback = FakeInitializationCompleteCallback()
    callback.onInitializationFailed("Init failed")

    assertThrows(AssertionError::class.java) {
      assertThat(callback).hasFailedWith("Different error")
    }
  }

  @Test
  fun hasNotFailed_whenFailed_fails() {
    val callback = FakeInitializationCompleteCallback()
    callback.onInitializationFailed("Init failed")

    assertThrows(AssertionError::class.java) { assertThat(callback).hasNotFailed() }
    assertThrows(AssertionError::class.java) { assertThat(callback).hasNoFailure() }
  }
}
