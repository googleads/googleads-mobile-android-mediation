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
 * Custom Truth [Subject] for [FakeMediationAdLoadCallback] assertions.
 *
 * Provides concise and ergonomic assertions on ad load completion states and error matching.
 */
class FakeMediationAdLoadCallbackSubject<MediationAdT : Any, MediationAdCallbackT : Any>(
  failureMetadata: FailureMetadata,
  private val actual: FakeMediationAdLoadCallback<MediationAdT, MediationAdCallbackT>?,
) : Subject(failureMetadata, actual) {

  /** Asserts that the callback recorded a failure with the given [expectedError]. */
  fun hasFailedWith(
    expectedError: AdError
  ): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).isEqualTo(expectedError)
    return this
  }

  /** Asserts that the callback recorded a failure with the given [errorCode] and [domain]. */
  fun hasFailedWith(
    errorCode: Int,
    domain: String,
  ): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasCode(errorCode)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasDomain(domain)
    return this
  }

  /**
   * Asserts that the callback recorded a failure with the given [errorCode], [message], and
   * [domain].
   */
  fun hasFailedWith(
    errorCode: Int,
    message: String,
    domain: String,
  ): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasCode(errorCode)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasMessage(message)
    check("adError").about(AdErrorSubject.adErrors()).that(actual?.adError).hasDomain(domain)
    return this
  }

  /** Asserts that [FakeMediationAdLoadCallback.isFailure] is true. */
  fun hasFailed(): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isTrue()
    return this
  }

  /** Asserts that [FakeMediationAdLoadCallback.isFailure] is false. */
  fun hasNotFailed(): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isFailure").that(actual?.isFailure).isFalse()
    return this
  }

  /** Alias for [hasNotFailed]. */
  fun hasNoFailure(): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> =
    hasNotFailed()

  /** Asserts that [FakeMediationAdLoadCallback.isSuccess] is true. */
  fun hasSucceeded(): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isSuccess").that(actual?.isSuccess).isTrue()
    return this
  }

  /** Asserts that [FakeMediationAdLoadCallback.isSuccess] is false. */
  fun hasNotSucceeded(): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isSuccess").that(actual?.isSuccess).isFalse()
    return this
  }

  /** Asserts that [FakeMediationAdLoadCallback.isSuccess] is true and loaded ad is [expectedAd]. */
  fun hasSucceededWith(
    expectedAd: MediationAdT
  ): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("isSuccess").that(actual?.isSuccess).isTrue()
    check("loadedAd").that(actual?.loadedAd).isEqualTo(expectedAd)
    return this
  }

  /** Asserts that the loaded ad is [expectedAd]. */
  fun hasLoadedAd(
    expectedAd: MediationAdT
  ): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> {
    isNotNull()
    check("loadedAd").that(actual?.loadedAd).isEqualTo(expectedAd)
    return this
  }

  companion object {
    /** Factory for creating [FakeMediationAdLoadCallbackSubject] instances. */
    fun <MediationAdT : Any, MediationAdCallbackT : Any> fakeMediationAdLoadCallbacks():
      Factory<
        FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT>,
        FakeMediationAdLoadCallback<MediationAdT, MediationAdCallbackT>,
      > = Factory { metadata, actual -> FakeMediationAdLoadCallbackSubject(metadata, actual) }

    /** Entry point for [FakeMediationAdLoadCallback] assertions. */
    @JvmStatic
    fun <MediationAdT : Any, MediationAdCallbackT : Any> assertThat(
      actual: FakeMediationAdLoadCallback<MediationAdT, MediationAdCallbackT>?
    ): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> =
      assertAbout(fakeMediationAdLoadCallbacks<MediationAdT, MediationAdCallbackT>()).that(actual)
  }
}

/** Top-level entry point for [FakeMediationAdLoadCallback] assertions. */
fun <MediationAdT : Any, MediationAdCallbackT : Any> assertThat(
  actual: FakeMediationAdLoadCallback<MediationAdT, MediationAdCallbackT>?
): FakeMediationAdLoadCallbackSubject<MediationAdT, MediationAdCallbackT> =
  FakeMediationAdLoadCallbackSubject.assertThat(actual)
