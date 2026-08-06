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
class AdErrorSubjectTest {

  @Test
  fun isEqualTo_matchingAdErrors_succeeds() {
    val error1 = AdError(101, "Error message", "com.google.ads.mediation")
    val error2 = AdError(101, "Error message", "com.google.ads.mediation")

    assertThat(error1).isEqualTo(error2)
  }

  @Test
  fun isEqualTo_matchingAdErrorsWithMatchingCauses_succeeds() {
    val cause1 = AdError(500, "Underlying cause", "com.thirdparty.sdk")
    val cause2 = AdError(500, "Underlying cause", "com.thirdparty.sdk")
    val error1 = AdError(101, "Error message", "com.google.ads.mediation", cause1)
    val error2 = AdError(101, "Error message", "com.google.ads.mediation", cause2)

    assertThat(error1).isEqualTo(error2)
  }

  @Test
  fun isEqualTo_codeMismatch_fails() {
    val error1 = AdError(101, "Error message", "com.google.ads.mediation")
    val error2 = AdError(102, "Error message", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error1).isEqualTo(error2) }
  }

  @Test
  fun isEqualTo_domainMismatch_fails() {
    val error1 = AdError(101, "Error message", "com.google.ads.mediation")
    val error2 = AdError(101, "Error message", "com.pangle.ads")

    assertThrows(AssertionError::class.java) { assertThat(error1).isEqualTo(error2) }
  }

  @Test
  fun isEqualTo_messageMismatch_fails() {
    val error1 = AdError(101, "Error message 1", "com.google.ads.mediation")
    val error2 = AdError(101, "Error message 2", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error1).isEqualTo(error2) }
  }

  @Test
  fun isEqualTo_causeMismatch_fails() {
    val cause1 = AdError(500, "Cause 1", "com.thirdparty.sdk")
    val cause2 = AdError(501, "Cause 2", "com.thirdparty.sdk")
    val error1 = AdError(101, "Error message", "com.google.ads.mediation", cause1)
    val error2 = AdError(101, "Error message", "com.google.ads.mediation", cause2)

    assertThrows(AssertionError::class.java) { assertThat(error1).isEqualTo(error2) }
  }

  @Test
  fun isEqualTo_actualHasCauseButExpectedHasNullCause_fails() {
    val cause = AdError(500, "Cause", "com.thirdparty.sdk")
    val error1 = AdError(101, "Error message", "com.google.ads.mediation", cause)
    val error2 = AdError(101, "Error message", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error1).isEqualTo(error2) }
  }

  @Test
  fun isEqualTo_bothNull_succeeds() {
    val nullError: AdError? = null

    assertThat(nullError).isEqualTo(null)
  }

  @Test
  fun isEqualTo_actualNullExpectedNonNull_fails() {
    val nullError: AdError? = null
    val expected = AdError(101, "Error", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(nullError).isEqualTo(expected) }
  }

  @Test
  fun isEqualTo_actualNonNullExpectedNull_fails() {
    val error = AdError(101, "Error", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error).isEqualTo(null) }
  }

  @Test
  fun isEqualTo_differentType_fails() {
    val error = AdError(101, "Error", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error).isEqualTo("Not an AdError") }
  }

  @Test
  fun hasCode_matchingCode_succeeds() {
    val error = AdError(101, "Error message", "com.google.ads.mediation")

    assertThat(error).hasCode(101)
  }

  @Test
  fun hasCode_mismatchingCode_fails() {
    val error = AdError(101, "Error message", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error).hasCode(102) }
  }

  @Test
  fun hasDomain_matchingDomain_succeeds() {
    val error = AdError(101, "Error message", "com.google.ads.mediation")

    assertThat(error).hasDomain("com.google.ads.mediation")
  }

  @Test
  fun hasDomain_mismatchingDomain_fails() {
    val error = AdError(101, "Error message", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error).hasDomain("com.other.domain") }
  }

  @Test
  fun hasMessage_matchingMessage_succeeds() {
    val error = AdError(101, "Error message", "com.google.ads.mediation")

    assertThat(error).hasMessage("Error message")
  }

  @Test
  fun hasMessage_mismatchingMessage_fails() {
    val error = AdError(101, "Error message", "com.google.ads.mediation")

    assertThrows(AssertionError::class.java) { assertThat(error).hasMessage("Different message") }
  }

  @Test
  fun hasMessageThat_chainedAssertions_succeeds() {
    val error = AdError(101, "Invalid placement id was provided", "com.google.ads.mediation")

    assertThat(error).hasMessageThat().contains("placement id")
    assertThat(error).hasMessageThat().startsWith("Invalid")
  }

  @Test
  fun hasCause_and_hasNoCause_succeeds() {
    val cause = AdError(500, "Underlying error", "com.thirdparty.sdk")
    val errorWithCause = AdError(101, "Error message", "com.google.ads.mediation", cause)
    val errorWithoutCause = AdError(101, "Error message", "com.google.ads.mediation")

    assertThat(errorWithCause).hasCause(cause)
    assertThat(errorWithoutCause).hasNoCause()

    assertThrows(AssertionError::class.java) { assertThat(errorWithCause).hasNoCause() }
    assertThrows(AssertionError::class.java) { assertThat(errorWithoutCause).hasCause(cause) }
  }

  @Test
  fun hasCauseThat_navigatesToCause_succeeds() {
    val cause = AdError(500, "Underlying error", "com.thirdparty.sdk")
    val error = AdError(101, "Error message", "com.google.ads.mediation", cause)

    assertThat(error).hasCauseThat().hasCode(500)
    assertThat(error).hasCauseThat().hasDomain("com.thirdparty.sdk")
  }
}
