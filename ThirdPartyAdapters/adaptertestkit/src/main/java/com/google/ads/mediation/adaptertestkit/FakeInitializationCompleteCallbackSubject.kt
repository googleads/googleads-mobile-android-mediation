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

/** Custom Truth [Subject] for [FakeInitializationCompleteCallback] assertions. */
class FakeInitializationCompleteCallbackSubject(
  failureMetadata: FailureMetadata,
  private val actual: FakeInitializationCompleteCallback?,
) : Subject(failureMetadata, actual) {

  /** Asserts that initialization succeeded. */
  fun hasSucceeded(): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationSucceeded").that(actual?.isInitializationSucceeded).isTrue()
    return this
  }

  /** Asserts that initialization has not succeeded. */
  fun hasNotSucceeded(): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationSucceeded").that(actual?.isInitializationSucceeded).isFalse()
    return this
  }

  /** Asserts that initialization failed with the given [expectedError] message. */
  fun hasFailedWith(expectedError: String): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationFailed").that(actual?.isInitializationFailed).isTrue()
    check("error").that(actual?.error).isEqualTo(expectedError)
    return this
  }

  /** Asserts that initialization failed with the given [expectedError]. */
  fun hasFailedWith(expectedError: AdError): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationFailed").that(actual?.isInitializationFailed).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).isEqualTo(expectedError)
    return this
  }

  /** Asserts that initialization failed with the given [errorCode] and [domain]. */
  fun hasFailedWith(errorCode: Int, domain: String): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationFailed").that(actual?.isInitializationFailed).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasCode(errorCode)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasDomain(domain)
    return this
  }

  /** Asserts that initialization failed with the given [errorCode], [message], and [domain]. */
  fun hasFailedWith(
    errorCode: Int,
    message: String,
    domain: String,
  ): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationFailed").that(actual?.isInitializationFailed).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasCode(errorCode)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasMessage(message)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasDomain(domain)
    return this
  }

  /** Asserts that initialization has failed. */
  fun hasFailed(): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationFailed").that(actual?.isInitializationFailed).isTrue()
    return this
  }

  /** Asserts that initialization has not failed. */
  fun hasNotFailed(): FakeInitializationCompleteCallbackSubject {
    isNotNull()
    check("isInitializationFailed").that(actual?.isInitializationFailed).isFalse()
    return this
  }

  /** Alias for [hasNotFailed]. */
  fun hasNoFailure(): FakeInitializationCompleteCallbackSubject = hasNotFailed()

  companion object {
    /** Factory for creating [FakeInitializationCompleteCallbackSubject] instances. */
    fun fakeInitializationCompleteCallbacks():
      Factory<FakeInitializationCompleteCallbackSubject, FakeInitializationCompleteCallback> =
      Factory(::FakeInitializationCompleteCallbackSubject)

    /** Entry point for [FakeInitializationCompleteCallback] assertions. */
    @JvmStatic
    fun assertThat(
      actual: FakeInitializationCompleteCallback?
    ): FakeInitializationCompleteCallbackSubject =
      assertAbout(fakeInitializationCompleteCallbacks()).that(actual)
  }
}

/** Top-level entry point for [FakeInitializationCompleteCallback] assertions. */
fun assertThat(
  actual: FakeInitializationCompleteCallback?
): FakeInitializationCompleteCallbackSubject =
  FakeInitializationCompleteCallbackSubject.assertThat(actual)
