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

package com.google.ads.mediation.snippets.java;

import android.os.Bundle;
import com.google.ads.mediation.inmobi.InMobiAdapter;
import com.google.ads.mediation.inmobi.InMobiNetworkKeys;
import com.google.ads.mediation.inmobi.InMobiNetworkValues;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;

/**
 * Java code snippets for https://developers.google.com/admob/android/mediation/inmobi and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/inmobi
 */
public final class InMobiMediationSnippets {

  private static final String AREA_CODE_VALUE = "12345";

  private AdRequest setAdRequestWithParameters() {
    // [START set_ad_request_parameters]
    Bundle extras = new Bundle();
    extras.putString(InMobiNetworkKeys.AGE_GROUP, InMobiNetworkValues.BETWEEN_35_AND_44);
    extras.putString(InMobiNetworkKeys.AREA_CODE, AREA_CODE_VALUE);

    AdRequest request =
        new AdRequest.Builder("AD_UNIT_ID")
            .putAdSourceExtrasBundle(InMobiAdapter.class, extras)
            .build();
    // [END set_ad_request_parameters]
    return request;
  }

  private AdRequest setAdManagerRequestWithParameters() {
    // [START set_ad_manager_ad_request_parameters]
    Bundle extras = new Bundle();
    extras.putString(InMobiNetworkKeys.AGE_GROUP, InMobiNetworkValues.BETWEEN_35_AND_44);
    extras.putString(InMobiNetworkKeys.AREA_CODE, AREA_CODE_VALUE);

    AdRequest request =
        new AdRequest.Builder("AD_UNIT_ID")
            .putAdSourceExtrasBundle(InMobiAdapter.class, extras)
            .build();
    // [END set_ad_manager_ad_request_parameters]
    return request;
  }
}
