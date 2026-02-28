// Copyright 2023 Google LLC
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

package com.google.ads.mediation.mytarget

import android.content.Context
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.AD_TECHNOLOGY_PROVIDER_ID
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ConsentResult
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.TAG
import com.google.android.gms.ads.RequestConfiguration
import com.my.target.common.MyTargetPrivacy

object MyTargetAdapterUtils {
  @JvmStatic
  val adapterVersion: String
    get() = BuildConfig.ADAPTER_VERSION

  @JvmStatic
  fun configureMyTargetPrivacy(context: Context, requestConfiguration: RequestConfiguration) {
    val isChildDirected = requestConfiguration.tagForChildDirectedTreatment
    val isUnderAgeOfConsent = requestConfiguration.tagForUnderAgeOfConsent

    if (
      isChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE ||
        isUnderAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
    ) {
      MyTargetPrivacy.setUserAgeRestricted(true)
    } else if (
      isChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE ||
        isUnderAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
    ) {
      MyTargetPrivacy.setUserAgeRestricted(false)
    }

    val consentResult = hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)
    if (consentResult == ConsentResult.TRUE) {
      MyTargetPrivacy.setUserConsent(true)
    } else if (consentResult == ConsentResult.FALSE) {
      MyTargetPrivacy.setUserConsent(false)
    }
  }

  /**
   * Checks whether the user provided consent to a Google Ad Tech Provider (ATP) in Google’s
   * Additional Consent technical specification. For more details, see
   * [Google’s Additional Consent technical specification](https://support.google.com/admob/answer/9681920).
   *
   * Returns [ConsentResult.UNKNOWN] if GDPR does not apply or if positive or negative consent was
   * not explicitly detected.
   *
   * @param context [Context] object of your application
   * @param vendorId a Google Ad Tech Provider (ATP) ID from
   *   https://storage.googleapis.com/tcfac/additional-consent-providers.csv
   * @return A [ConsentResult] indicating consent for the given ATP.
   */
  @JvmStatic
  fun hasACConsent(context: Context, vendorId: Int): ConsentResult {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)

    var gdprApplies = -1
    try {
      gdprApplies = sharedPref.getInt("IABTCF_gdprApplies", -1)
    } catch (exception: ClassCastException) {
      Log.w(
        TAG,
        "Could not parse IABTCF_gdprApplies as an integer. Did your CMP write it correctly?",
        exception,
      )
    }

    if (gdprApplies != 1) {
      return ConsentResult.UNKNOWN
    }

    var additionalConsentString = ""
    try {
      additionalConsentString = sharedPref.getString("IABTCF_AddtlConsent", "")!!
    } catch (exception: ClassCastException) {
      Log.w(
        TAG,
        "Could not parse IABTCF_AddtlConsent as a string. Did your CMP write it correctly?",
        exception,
      )
    }

    if (TextUtils.isEmpty(additionalConsentString)) {
      return ConsentResult.UNKNOWN
    }

    val vendorIdString = vendorId.toString()
    val additionalConsentParts = additionalConsentString.split("~").toTypedArray()

    val version: Int
    try {
      version = additionalConsentParts[0].toInt()
    } catch (exception: Exception) {
      Log.w(
        TAG,
        "Could not parse the IABTCF_AddtlConsent spec version. Did your CMP write it correctly?",
        exception,
      )
      return ConsentResult.UNKNOWN
    }

    if (version == 1) {
      // Spec version 1
      Log.w(
        TAG,
        ("The IABTCF_AddtlConsent string uses version 1 of Google’s Additional Consent spec." +
          " Version 1 does not report vendors to whom the user denied consent. To detect" +
          " vendors that the user denied consent, upgrade to a CMP that supports version 2 of" +
          " Google's Additional Consent technical specification."),
      )

      if (additionalConsentParts.size == 1) {
        // The AC string had no consented vendor.
        return ConsentResult.UNKNOWN
      } else if (additionalConsentParts.size == 2) {
        val consentedIds: Array<String?> = additionalConsentParts[1].split(".").toTypedArray()
        if (consentedIds.contains(vendorIdString)) {
          return ConsentResult.TRUE
        }
      } else {
        Log.w(
          TAG,
          "Could not parse the IABTCF_AddtlConsent string: \"${additionalConsentString}\"." +
            " String had more parts than expected. Did your CMP write IABTCF_AddtlConsent correctly?",
        )
        return ConsentResult.UNKNOWN
      }

      return ConsentResult.UNKNOWN
    } else if (version >= 2) {
      // Spec version 2
      if (additionalConsentParts.size < 3) {
        Log.w(
          TAG,
          "Could not parse the IABTCF_AddtlConsent string: \"${additionalConsentString}\"." +
            " String had less parts than expected. Did your CMP write IABTCF_AddtlConsent correctly?",
        )
        return ConsentResult.UNKNOWN
      }

      val disclosedIds: Array<String?> = additionalConsentParts[2].split(".").toTypedArray()
      if (disclosedIds[0] != "dv") {
        Log.w(
          TAG,
          "Could not parse the IABTCF_AddtlConsent string: \"${additionalConsentString}\"." +
            " Expected disclosed vendors part to have the string \"dv.\". Did your CMP write" +
            " IABTCF_AddtlConsent correctly?",
        )
        return ConsentResult.UNKNOWN
      }

      val consentedIds: Array<String?> = additionalConsentParts[1].split(".").toTypedArray()
      if (consentedIds.contains(vendorIdString)) {
        return ConsentResult.TRUE
      }

      if (disclosedIds.contains(vendorIdString)) {
        return ConsentResult.FALSE
      }

      return ConsentResult.UNKNOWN
    } else {
      // Unknown spec version
      Log.w(
        TAG,
        "Could not parse the IABTCF_AddtlConsent string: \"${additionalConsentString}\"." +
          " Spec version was unexpected. Did your CMP write IABTCF_AddtlConsent correctly?",
      )
      return ConsentResult.UNKNOWN
    }
  }
}
