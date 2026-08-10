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
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

/**
 * Custom Truth [Subject] for [FakeSignalCallbacks] assertions.
 *
 * Provides concise and ergonomic assertions on signal collection states and error matching.
 */
class FakeSignalCallbacksSubject(
  failureMetadata: FailureMetadata,
  private val actual: FakeSignalCallbacks?,
) : Subject(failureMetadata, actual) {

  /** Asserts that signal collection failed with the given [expectedError]. */
  fun hasFailedWith(expectedError: AdError): FakeSignalCallbacksSubject {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).isEqualTo(expectedError)
    return this
  }

  /** Asserts that signal collection failed with the given [errorCode] and [domain]. */
  fun hasFailedWith(errorCode: Int, domain: String): FakeSignalCallbacksSubject {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasCode(errorCode)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasDomain(domain)
    return this
  }

  /** Asserts that signal collection failed with the given [errorCode], [message], and [domain]. */
  fun hasFailedWith(errorCode: Int, message: String, domain: String): FakeSignalCallbacksSubject {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasCode(errorCode)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasMessage(message)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasDomain(domain)
    return this
  }

  /** Asserts that [FakeSignalCallbacks.isFailure] is true. */
  fun hasFailed(): FakeSignalCallbacksSubject {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    return this
  }

  /** Asserts that [FakeSignalCallbacks.isFailure] is false. */
  fun hasNotFailed(): FakeSignalCallbacksSubject {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isFalse()
    return this
  }

  /** Alias for [hasNotFailed]. */
  fun hasNoFailure(): FakeSignalCallbacksSubject = hasNotFailed()

  /** Asserts that [FakeSignalCallbacks.isSuccess] is true. */
  fun hasSucceeded(): FakeSignalCallbacksSubject {
    isNotNull()
    check("isSuccess").that(actual?.isSuccess).isTrue()
    return this
  }

  /** Asserts that [FakeSignalCallbacks.isSuccess] is false. */
  fun hasNotSucceeded(): FakeSignalCallbacksSubject {
    isNotNull()
    check("isSuccess").that(actual?.isSuccess).isFalse()
    return this
  }

  /** Asserts that signal collection succeeded with the given [expectedSignals]. */
  fun hasSucceededWith(expectedSignals: String): FakeSignalCallbacksSubject {
    isNotNull()
    check("isSuccess").that(actual?.isSuccess).isTrue()
    check("signals").that(actual?.signals).isEqualTo(expectedSignals)
    return this
  }

  companion object {
    /** Factory for creating [FakeSignalCallbacksSubject] instances. */
    fun fakeSignalCallbacks(): Factory<FakeSignalCallbacksSubject, FakeSignalCallbacks> =
      Factory(::FakeSignalCallbacksSubject)

    /** Entry point for [FakeSignalCallbacks] assertions. */
    @JvmStatic
    fun assertThat(actual: FakeSignalCallbacks?): FakeSignalCallbacksSubject =
      assertAbout(fakeSignalCallbacks()).that(actual)
  }
}

/** Top-level entry point for [FakeSignalCallbacks] assertions. */
fun assertThat(actual: FakeSignalCallbacks?): FakeSignalCallbacksSubject =
  FakeSignalCallbacksSubject.assertThat(actual)
