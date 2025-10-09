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
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs
import com.google.ads.mediation.fyber.FyberMediationAdapter
import com.google.android.gms.ads.AdRequest

/**
 * Java code snippets for https://developers.google.com/admob/android/mediation/dt-exchange and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/dt-exchange
 */
class DTExchangeMediationSnippets {

  private fun setGdprConsent() {
    // [START set_gdpr_consent]
    InneractiveAdManager.setGdprConsent(true)
    InneractiveAdManager.setGdprConsentString(CONSENT_STRING)
    // [END set_gdpr_consent]
  }

  private fun setUSPrivacyString() {
    // [START set_us_privacy_string]
    InneractiveAdManager.setUSPrivacyString(US_PRIVACY_STRING)
    // [END set_us_privacy_string]
  }

  private fun setNetworkSpecificParams() {
    // [START set_network_specific_params]
    val extras = Bundle()
    extras.putInt(InneractiveMediationDefs.KEY_AGE, 10)
    extras.putBoolean(FyberMediationAdapter.KEY_MUTE_VIDEO, false)

    val request =
      AdRequest.Builder().addNetworkExtrasBundle(FyberMediationAdapter::class.java, extras).build()
    // [END set_network_specific_params]
  }

  private companion object {
    // Placeholder values for a user's consent string and US privacy string.
    const val CONSENT_STRING = "consent_string"
    const val US_PRIVACY_STRING = "us_privacy_string"
  }
}
