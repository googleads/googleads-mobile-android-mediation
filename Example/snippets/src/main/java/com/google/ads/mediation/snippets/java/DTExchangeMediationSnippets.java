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
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.google.ads.mediation.fyber.FyberMediationAdapter;
import com.google.android.gms.ads.AdRequest;

/**
 * Java code snippets for https://developers.google.com/admob/android/mediation/dt-exchange and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/dt-exchange
 */
public class DTExchangeMediationSnippets {

  // Placeholder values for a user's consent string and US privacy string.
  private static final String CONSENT_STRING = "TODO: Obtain GDPR CONSENT_STRING from your CMP";
  private static final String US_PRIVACY_STRING = "TODO: Obtain US_PRIVACY_STRING from your CMP";

  private void setGdprConsent() {
    // [START set_gdpr_consent]
    InneractiveAdManager.setGdprConsent(true);
    InneractiveAdManager.setGdprConsentString(CONSENT_STRING);
    // [END set_gdpr_consent]
  }

  private void setUSPrivacyString() {
    // [START set_us_privacy_string]
    InneractiveAdManager.setUSPrivacyString(US_PRIVACY_STRING);
    // [END set_us_privacy_string]
  }

  private void setNetworkSpecificParams() {
    // [START set_network_specific_params]
    Bundle extras = new Bundle();
    extras.putInt(InneractiveMediationDefs.KEY_AGE, 10);
    extras.putBoolean(FyberMediationAdapter.KEY_MUTE_VIDEO, false);

    AdRequest request =
        new AdRequest.Builder().addNetworkExtrasBundle(FyberMediationAdapter.class, extras).build();
    // [END set_network_specific_params]
  }
}
