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

import com.applovin.sdk.AppLovinPrivacySettings

/**
 * Kotlin code snippets for https://developers.google.com/admob/android/mediation/applovin and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/applovin
 */
class AppLovinMediationSnippets {

  private fun setUserConsent() {
    // [START set_user_consent]
    AppLovinPrivacySettings.setHasUserConsent(true)
    // [END set_user_consent]
  }

  private fun setDoNotSell() {
    // [START set_do_not_sell]
    AppLovinPrivacySettings.setDoNotSell(true)
    // [END set_do_not_sell]
  }
}
