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

package com.vungle.mediation;

import com.vungle.warren.Vungle;

/**
 * A public static class used to set Liftoff Monetize Consent Status.
 */
public class VungleConsent {

  /**
   * Update GDPR consent status and corresponding version number.
   */
  public static void updateConsentStatus(
      Vungle.Consent consentStatus, String consentMessageVersion) {
    Vungle.updateConsentStatus(consentStatus, consentMessageVersion);
  }

  public static Vungle.Consent getCurrentVungleConsent() {
    return Vungle.getConsentStatus();
  }

  public static String getCurrentVungleConsentMessageVersion() {
    return Vungle.getConsentMessageVersion();
  }
}
