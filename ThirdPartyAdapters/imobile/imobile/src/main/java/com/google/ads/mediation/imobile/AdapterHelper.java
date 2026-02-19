// Copyright 2019 Google LLC
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

package com.google.ads.mediation.imobile;

import static com.google.ads.mediation.imobile.IMobileMediationAdapter.IMOBILE_SDK_ERROR_DOMAIN;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;

import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import jp.co.imobile.sdkads.android.FailNotificationReason;

/**
 * Helper of mediation adapter.
 */
public final class AdapterHelper {

  /**
   * Convert i-mobile fail reason to error code.
   *
   * @param reason i-mobile fail reason
   * @return error code
   */
  @NonNull
  public static AdError getAdError(FailNotificationReason reason) {
    // Error '99' to indicate that the error is new and has not been supported by the adapter yet.
    int code = 99;
    switch (reason) {
      case RESPONSE:
        code = 0;
        break;
      case PARAM:
        code = 1;
        break;
      case AUTHORITY:
        code = 2;
        break;
      case PERMISSION:
        code = 3;
        break;
      case NETWORK_NOT_READY:
        code = 4;
        break;
      case NETWORK:
        code = 5;
        break;
      case AD_NOT_READY:
        code = 6;
        break;
      case NOT_DELIVERY_AD:
        code = 7;
        break;
      case SHOW_TIMEOUT:
        code = 8;
        break;
      case UNKNOWN:
        code = 9;
        break;
    }
    return new AdError(code, "Failed to request ad from Imobile: " + reason,
        IMOBILE_SDK_ERROR_DOMAIN);
  }

  static String getAdapterVersion() {
    return BuildConfig.ADAPTER_VERSION;
  }

  static boolean getIsUserChild() {
    RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration();
    int tagForChildDirectedTreatment = requestConfiguration.getTagForChildDirectedTreatment();
    int tagForUnderAgeOfConsent = requestConfiguration.getTagForUnderAgeOfConsent();
    return tagForChildDirectedTreatment == TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        || tagForUnderAgeOfConsent == TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
  }
}