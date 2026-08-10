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
import com.google.common.truth.Fact.fact
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StringSubject
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

/**
 * Custom Truth [Subject] for [AdError] assertions.
 *
 * Provides concise and ergonomic assertions comparing [AdError] objects by their properties
 * (`code`, `domain`, `message`, and recursive `cause`).
 */
open class AdErrorSubject(failureMetadata: FailureMetadata, private val actual: AdError?) :
  Subject(failureMetadata, actual) {

  override fun actualCustomStringRepresentation(): String {
    return formatAdError(actual)
  }

  /**
   * Asserts that the [actual] [AdError] is equal to [expected] by comparing code, domain, message,
   * and recursive cause.
   */
  override fun isEqualTo(expected: Any?) {
    val current = actual
    if (current == null && expected == null) {
      return
    }
    if (current == null) {
      val expectedStr = if (expected is AdError) formatAdError(expected) else expected.toString()
      failWithActual(fact("expected", expectedStr))
      return
    }
    if (expected == null) {
      failWithActual(simpleFact("expected null but was: ${formatAdError(current)}"))
      return
    }
    if (expected !is AdError) {
      failWithActual(fact("expected instance of AdError but was", expected.javaClass.name))
      return
    }

    check("code").that(current.code).isEqualTo(expected.code)
    check("domain").that(current.domain).isEqualTo(expected.domain)
    check("message").that(current.message).isEqualTo(expected.message)
    if (expected.cause != null) {
      check("cause").about(adErrors()).that(current.cause).isEqualTo(expected.cause)
    } else {
      check("cause").about(adErrors()).that(current.cause).isNull()
    }
  }

  /** Asserts that the [actual] [AdError] has the expected [errorCode]. */
  fun hasCode(errorCode: Int): AdErrorSubject {
    isNotNull()
    check("code").that(actual?.code).isEqualTo(errorCode)
    return this
  }

  /** Asserts that the [actual] [AdError] has the expected [domain]. */
  fun hasDomain(domain: String): AdErrorSubject {
    isNotNull()
    check("domain").that(actual?.domain).isEqualTo(domain)
    return this
  }

  /** Asserts that the [actual] [AdError] has the expected [message]. */
  fun hasMessage(message: String): AdErrorSubject {
    isNotNull()
    check("message").that(actual?.message).isEqualTo(message)
    return this
  }

  /** Returns a [StringSubject] on the [actual] [AdError]'s message for detailed assertions. */
  fun hasMessageThat(): StringSubject {
    isNotNull()
    return check("message").that(actual?.message)
  }

  /** Asserts that the [actual] [AdError] has the expected [cause]. */
  fun hasCause(cause: AdError?): AdErrorSubject {
    isNotNull()
    if (cause != null) {
      check("cause").about(adErrors()).that(actual?.cause).isEqualTo(cause)
    } else {
      check("cause").about(adErrors()).that(actual?.cause).isNull()
    }
    return this
  }

  /** Returns an [AdErrorSubject] on the cause of the [AdError] for chained cause assertions. */
  fun hasCauseThat(): AdErrorSubject {
    isNotNull()
    return check("cause").about(adErrors()).that(actual?.cause)
  }

  /** Asserts that the [actual] [AdError] has no cause (i.e. cause is null). */
  fun hasNoCause(): AdErrorSubject {
    isNotNull()
    check("cause").about(adErrors()).that(actual?.cause).isNull()
    return this
  }

  companion object {
    /** Factory for creating [AdErrorSubject] instances. */
    fun adErrors(): Factory<AdErrorSubject, AdError> = Factory(::AdErrorSubject)

    /** Entry point for [AdError] assertions. */
    @JvmStatic
    fun assertThat(actual: AdError?): AdErrorSubject = assertAbout(adErrors()).that(actual)

    private fun formatAdError(error: AdError?): String {
      if (error == null) return "null"
      val causeStr = if (error.cause != null) formatAdError(error.cause) else "null"
      return "AdError(code=${error.code}, domain=\"${error.domain}\", message=\"${error.message}\", cause=$causeStr)"
    }
  }
}

/** Top-level entry point for [AdError] assertions. */
fun assertThat(actual: AdError?): AdErrorSubject = AdErrorSubject.assertThat(actual)
