// Copyright 2025 Google LLC
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

package com.google.ads.mediation.bigo

import android.content.Context
import com.google.ads.mediation.common.AgeRestrictedTreatmentUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions
import sg.bigo.ads.api.AdConfig
import sg.bigo.ads.api.AdSize

object BigoUtils {
  fun getGmaAdError(code: Int, message: String, domain: String) = AdError(code, message, domain)

  fun mapAdSizeToBigoBannerSize(
    context: Context,
    adSize: com.google.android.gms.ads.AdSize,
    mediationUtils: MediationUtilsWrapper,
  ): AdSize {
    // List of banner ad sizes supported by Bigo.
    val supportedSizes =
      listOf(
        com.google.android.gms.ads.AdSize.BANNER,
        com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE,
        com.google.android.gms.ads.AdSize.LARGE_BANNER,
        com.google.android.gms.ads.AdSize.LEADERBOARD,
      )
    // Find the supported size that is closest to the publisher-requested size.
    val closestSupportedSize = mediationUtils.findClosestSize(context, adSize, supportedSizes)
    return when (closestSupportedSize) {
      com.google.android.gms.ads.AdSize.BANNER -> AdSize.BANNER
      com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
      com.google.android.gms.ads.AdSize.LARGE_BANNER -> AdSize.LARGE_BANNER
      com.google.android.gms.ads.AdSize.LEADERBOARD -> AdSize.LEADERBOARD
      else -> AdSize.BANNER
    }
  }

  /**
   * Maps the Google Mobile Ads SDK child-directed and under-age-of-consent settings to the Bigo SDK
   * COPPA consent option. Leaves the Bigo COPPA consent untouched if none of the settings are
   * specified.
   */
  fun configureBigoPrivacy(context: Context) {
    val requestConfiguration = MobileAds.getRequestConfiguration()
    val tagForChildDirected = requestConfiguration.tagForChildDirectedTreatment
    val tagForUnderAgeOfConsent = requestConfiguration.tagForUnderAgeOfConsent
    val isAgeRestrictedTreatmentChild =
      AgeRestrictedTreatmentUtils.runtimeGmaSdkSupportsChildAgeRestrictedTreatment() &&
        requestConfiguration.ageRestrictedTreatment == AgeRestrictedTreatment.CHILD
    if (
      tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE ||
        tagForUnderAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE ||
        isAgeRestrictedTreatmentChild
    ) {
      // A value of "true" indicates that the user is not a child under 13 years old, and a value of
      // "false" indicates that the user is a child under 13 years old.
      BigoAdSdk.setUserConsent(context, ConsentOptions.COPPA, false)
    } else if (
      tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE ||
        tagForUnderAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
    ) {
      BigoAdSdk.setUserConsent(context, ConsentOptions.COPPA, true)
    }
  }

  /** Initializes the Bigo SDK with [appId] and invokes [onInitialized] once it's initialized. */
  fun initializeBigoSdk(context: Context, appId: String, onInitialized: () -> Unit) {
    val adConfig = AdConfig.Builder().setAppId(appId).build()
    BigoAdSdk.initialize(context, adConfig) { onInitialized() }
  }
}
