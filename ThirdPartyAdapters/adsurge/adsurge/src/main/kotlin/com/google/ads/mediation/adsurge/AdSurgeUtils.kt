// Copyright 2026 Google LLC
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

package com.google.ads.mediation.adsurge

import com.adsurge.adn.ads.AdSurgeAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration

/** Utility functions and constants for the AdSurge mediation adapter. */
object AdSurgeUtils {

  const val KEY_APPLICATION_ID = "application_id"
  const val KEY_AD_UNIT_ID = "ad_unit_id"

  const val ERROR_INVALID_SERVER_PARAMETERS = 101
  const val ERROR_BANNER_SIZE_MISMATCH = 102
  const val ERROR_AD_NOT_READY = 103
  const val ERROR_AD_PRESENTATION_FAILED = 104
  const val ERROR_CONTEXT_NOT_ACTIVITY = 105

  /** Creates an [AdError] with the adapter error domain. */
  fun createAdapterError(code: Int, message: String): AdError =
    AdError(code, message, AdSurgeMediationAdapter.ADAPTER_ERROR_DOMAIN)

  /** Creates an [AdError] with the SDK error domain. */
  fun createSdkError(adError: AdSurgeAdError?): AdError {
    val code = adError?.errorCode ?: 0
    val message =
      if (adError?.errorMsg.isNullOrEmpty()) {
        "AdSurge SDK error."
      } else {
        adError?.errorMsg ?: "AdSurge SDK error."
      }
    return AdError(code, message, AdSurgeMediationAdapter.SDK_ERROR_DOMAIN)
  }

  /** Retrieves the first non-empty application ID from the given [mediationConfigurations]. */
  fun getAppId(mediationConfigurations: List<MediationConfiguration>): String? {
    for (config in mediationConfigurations) {
      val appId = config.serverParameters.getString(KEY_APPLICATION_ID)
      if (!appId.isNullOrEmpty()) {
        return appId
      }
    }
    return null
  }

  /**
   * Configures user privacy settings on [AdSurgeFactory] based on child-directed treatment and
   * under age of consent flags.
   */
  fun applyPrivacyConfiguration(
    childDirected: Int,
    underAgeOfConsent: Int,
    factory: AdSurgeFactory = AdSurgeFactory,
  ) {
    if (
      childDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE ||
        underAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
    ) {
      factory.setAgeRestrictedUser(true)
    } else if (
      childDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE ||
        underAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
    ) {
      factory.setAgeRestrictedUser(false)
    }
  }

  /** Configures user privacy settings on [AdSurgeFactory] using the given [configuration]. */
  fun applyPrivacyConfiguration(
    configuration: MediationAdConfiguration,
    factory: AdSurgeFactory = AdSurgeFactory,
  ) {
    applyPrivacyConfiguration(
      configuration.taggedForChildDirectedTreatment(),
      configuration.taggedForUnderAgeTreatment(),
      factory,
    )
  }
}
