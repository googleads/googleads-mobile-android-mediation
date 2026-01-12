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
import android.util.Log
import com.google.ads.mediation.inmobi.InMobiAdapter
import com.google.ads.mediation.inmobi.InMobiConsent
import com.google.ads.mediation.inmobi.InMobiNetworkKeys
import com.google.ads.mediation.inmobi.InMobiNetworkValues
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.inmobi.sdk.InMobiSdk
import org.json.JSONException
import org.json.JSONObject

/**
 * Kotlin code snippets for https://developers.google.com/admob/android/mediation/inmobi and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/inmobi
 */
class InMobiMediationSnippets {

  private fun updateGDPRConsent() {
    // [START update_gdpr_consent]
    val consentObject = JSONObject()
    try {
      consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
      consentObject.put("gdpr", "1")
    } catch (exception: JSONException) {
      Log.e(TAG, "Error creating GDPR consent JSON.", exception)
    }

    InMobiConsent.updateGDPRConsent(consentObject)
    // [END update_gdpr_consent]
  }

  private fun setAdRequestWithParameters(): AdRequest {
    // [START set_ad_request_parameters]
    val extras = Bundle()
    extras.putString(InMobiNetworkKeys.AGE_GROUP, InMobiNetworkValues.BETWEEN_35_AND_44)
    extras.putString(InMobiNetworkKeys.AREA_CODE, AREA_CODE_VALUE)

    val request =
      AdRequest.Builder().addNetworkExtrasBundle(InMobiAdapter::class.java, extras).build()
    // [END set_ad_request_parameters]
    return request
  }

  private fun setAdManagerRequestWithParameters(): AdManagerAdRequest {
    // [START set_ad_manager_ad_request_parameters]
    val extras = Bundle()
    extras.putString(InMobiNetworkKeys.AGE_GROUP, InMobiNetworkValues.BETWEEN_35_AND_44)
    extras.putString(InMobiNetworkKeys.AREA_CODE, AREA_CODE_VALUE)

    val request =
      AdManagerAdRequest.Builder().addNetworkExtrasBundle(InMobiAdapter::class.java, extras).build()
    // [END set_ad_manager_ad_request_parameters]
    return request
  }

  private companion object {
    const val AREA_CODE_VALUE = "12345"
    const val TAG = "InMobiMediationSnippets"
  }
}
