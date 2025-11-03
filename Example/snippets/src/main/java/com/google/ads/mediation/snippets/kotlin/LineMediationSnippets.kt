/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.snippets.kotlin

import com.google.ads.mediation.line.LineExtras
import com.google.ads.mediation.line.LineMediationAdapter
import com.google.android.gms.ads.AdRequest

/**
 * Kotlin code snippets for https://developers.google.com/admob/android/mediation/line and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/line
 */
class LineMediationSnippets {

  private fun setTestMode() {
    // [START set_test_mode]
    LineMediationAdapter.setTestMode(true)
    // [END set_test_mode]
  }

  private fun setNetworkSpecificParams() {
    // [START set_network_specific_params]
    val lineExtras = LineExtras(enableAdSound = true)
    val extras = lineExtras.build()

    val request =
      AdRequest.Builder().addNetworkExtrasBundle(LineMediationAdapter::class.java, extras).build()
    // [END set_network_specific_params]
  }
}
