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

import com.google.ads.mediation.facebook.FacebookMediationAdapter
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Kotlin code snippets for https://developers.google.com/admob/android/mediation/meta and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/meta
 */
private class MetaMediationSnippets {

  private fun extractNativeAssets(nativeAd: NativeAd) {
    // [START extract_native_assets]
    val extras = nativeAd.getExtras()
    if (extras.containsKey(FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET)) {
      val socialContext = extras.getString(FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET)
      // ...
    }
    // [END extract_native_assets]
  }
}
