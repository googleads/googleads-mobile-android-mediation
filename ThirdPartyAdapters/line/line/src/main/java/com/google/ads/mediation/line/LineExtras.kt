// Copyright 2023 Google LLC
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

package com.google.ads.mediation.line

import android.os.Bundle
import androidx.core.os.bundleOf

/** Creates a mediation extras [Bundle] for the Line adapter. */
data class LineExtras(
  val enableAdSound: Boolean = false,
) {

  /**
   * Generates the [Bundle] instance to pair with [LineMediationAdapter] to add as a Network Extra
   * Bundle to a [AdRequest]
   */
  fun build(): Bundle {
    return bundleOf(
      KEY_ENABLE_AD_SOUND to enableAdSound,
    )
  }

  companion object {
    const val KEY_ENABLE_AD_SOUND = "enableAdSound"
  }
}
