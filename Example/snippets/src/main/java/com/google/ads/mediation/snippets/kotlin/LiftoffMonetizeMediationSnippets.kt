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

import android.os.Bundle
import com.google.ads.mediation.vungle.VungleConstants
import com.google.android.gms.ads.AdRequest
import com.vungle.ads.VunglePrivacySettings
import com.vungle.mediation.VungleAdapter
import com.vungle.mediation.VungleInterstitialAdapter

/**
 * Kotlin code snippets for https://developers.google.com/admob/android/mediation/liftoff-monetize
 * and https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/liftoff-monetize
 */
class LiftoffMonetizeMediationSnippets {

  private fun setCCPAStatus() {
    // [START set_ccpa_status]
    VunglePrivacySettings.setCCPAStatus(true)
    // [END set_ccpa_status]
  }

  private fun setNetworkSpecificParams() {
    // [START set_network_specific_params]
    val extras = Bundle()
    extras.putString(VungleConstants.KEY_USER_ID, "myUserID")
    extras.putInt(VungleConstants.KEY_ORIENTATION, 1)

    val request =
      AdRequest.Builder()
        .addNetworkExtrasBundle(VungleAdapter::class.java, extras) // Rewarded.
        .addNetworkExtrasBundle(VungleInterstitialAdapter::class.java, extras) // Interstitial.
        .build()
    // [END set_network_specific_params]
  }
}
