// Copyright 2026 Google LLC
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

package com.google.ads.mediation.mintegral

import androidx.core.os.bundleOf
import com.google.ads.mediation.mintegral.FlagValueGetter.Companion.KEY_FLIP_MULTIPLE_AD_LOADS_BEHAVIOR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [FlagValueGetter]. */
@RunWith(RobolectricTestRunner::class)
class FlagValueGetterTest {

  private val flagValueGetter = FlagValueGetter()

  @Before
  fun setUp() {
    FlagValueGetter.flipMultipleAdLoadsBehavior = false
  }

  @Test
  fun processMultipleAdLoadsServerParam_ifParamIsTrue_setsFieldTrue() {
    val serverParams = bundleOf(KEY_FLIP_MULTIPLE_AD_LOADS_BEHAVIOR to "true")

    flagValueGetter.processMultipleAdLoadsServerParam(serverParams)

    assertThat(FlagValueGetter.flipMultipleAdLoadsBehavior).isTrue()
  }

  @Test
  fun processMultipleAdLoadsServerParam_ifParamIsNotTrue_doesNotSetFieldTrue() {
    val serverParams = bundleOf(KEY_FLIP_MULTIPLE_AD_LOADS_BEHAVIOR to "false")

    flagValueGetter.processMultipleAdLoadsServerParam(serverParams)

    assertThat(FlagValueGetter.flipMultipleAdLoadsBehavior).isFalse()
  }

  @Test
  fun processMultipleAdLoadsServerParam_ifParamIsAbsent_doesNotSetFieldTrue() {
    val serverParams = bundleOf()

    flagValueGetter.processMultipleAdLoadsServerParam(serverParams)

    assertThat(FlagValueGetter.flipMultipleAdLoadsBehavior).isFalse()
  }
}
