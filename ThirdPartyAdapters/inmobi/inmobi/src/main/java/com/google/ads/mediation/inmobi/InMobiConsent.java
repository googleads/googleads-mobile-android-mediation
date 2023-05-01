// Copyright 2018 Google LLC
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

package com.google.ads.mediation.inmobi;

import com.inmobi.sdk.InMobiSdk;
import org.json.JSONObject;

public class InMobiConsent {

  private static JSONObject consentObj = new JSONObject();

  /**
   * Call InMobiConsent.updateGDPRConsent() to update GDPR consent for the user on each request
   * basis.
   */
  public static void updateGDPRConsent(JSONObject consentObj) {
    if (InMobiSdk.isSDKInitialized()) {
      InMobiSdk.updateGDPRConsent(consentObj);
    }
    InMobiConsent.consentObj = consentObj;
  }

  static JSONObject getConsentObj() {
    return consentObj;
  }
}
