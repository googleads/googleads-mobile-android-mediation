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
class FakeInitializationCompleteCallbackTest {

  private val callback = FakeInitializationCompleteCallback()

  @Test
  fun initialState_allValuesDefault() {
    assertThat(callback.isInitializationSucceededInvoked).isFalse()
    assertThat(callback.isInitializationSucceeded).isFalse()
    assertThat(callback.initializationSucceededInvokeCount).isEqualTo(0)
    assertThat(callback.isInitializationFailedInvoked).isFalse()
    assertThat(callback.isInitializationFailed).isFalse()
    assertThat(callback.initializationFailedInvokeCount).isEqualTo(0)
    assertThat(callback.error).isNull()
  }

  @Test
  fun onInitializationSucceeded_recordsSuccess() {
    callback.onInitializationSucceeded()

    assertThat(callback.isInitializationSucceededInvoked).isTrue()
    assertThat(callback.isInitializationSucceeded).isTrue()
    assertThat(callback.initializationSucceededInvokeCount).isEqualTo(1)

    callback.onInitializationSucceeded()

    assertThat(callback.isInitializationSucceededInvoked).isTrue()
    assertThat(callback.initializationSucceededInvokeCount).isEqualTo(2)
  }

  @Test
  fun onInitializationFailed_recordsFailureAndError() {
    val errorMessage = "Initialization failed due to missing appId"

    callback.onInitializationFailed(errorMessage)

    assertThat(callback.isInitializationFailedInvoked).isTrue()
    assertThat(callback.isInitializationFailed).isTrue()
    assertThat(callback.initializationFailedInvokeCount).isEqualTo(1)
    assertThat(callback.error).isEqualTo(errorMessage)

    callback.onInitializationFailed("Second failure")

    assertThat(callback.isInitializationFailedInvoked).isTrue()
    assertThat(callback.initializationFailedInvokeCount).isEqualTo(2)
    assertThat(callback.error).isEqualTo("Second failure")
  }
}
