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

import com.unity3d.mediation.LevelPlay

/**
 * Kotlin code snippets for https://developers.google.com/admob/android/mediation/ironsource and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/ironsource
 */
class IronSourceMediationSnippets {

  private fun setUserConsent() {
    // [START set_user_consent]
    LevelPlay.setMetaData("do_not_sell", "true")
    // [END set_user_consent]
  }
}
