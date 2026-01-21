/*
 * Copyright 2026 Google LLC
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

import net.pubnative.lite.sdk.HyBid

/**
 * Java code snippets for https://developers.google.com/admob/android/mediation/verve and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/verve
 */
private class VerveMediationSnippets {

  private fun setIABUSPrivacyString() {
    // [START set_iab_us_privacy_string]
    HyBid.getUserDataManager().setIABUSPrivacyString(US_PRIVACY_STRING)
    // [END set_iab_us_privacy_string]
  }

  private companion object {
    // Placeholder value for a user's US privacy string.
    const val US_PRIVACY_STRING = "TODO: Obtain US_PRIVACY_STRING from your CMP"
  }
}
