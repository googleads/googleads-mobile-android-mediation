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

import com.google.android.gms.ads.RequestConfiguration
import com.my.target.common.MyTargetPrivacy

object MyTargetAdapterUtils {
  @JvmStatic
  val adapterVersion: String
    get() = BuildConfig.ADAPTER_VERSION

  @JvmStatic
  fun configureMyTargetPrivacy(requestConfiguration: RequestConfiguration) {
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
  }
}
