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

import com.vungle.ads.VunglePrivacySettings;

/**
 * A public static class used to set Vungle Consent Status.
 */
public class VungleConsent {

  public static void setGDPRStatus(
      Boolean optedIn, String consentMessageVersion) {
    VunglePrivacySettings.setGDPRStatus(optedIn, consentMessageVersion);
  }

  public static String getGDPRStatus() {
    return VunglePrivacySettings.getGDPRStatus();
  }

  public static String getGDPRMessageVersion() {
    return VunglePrivacySettings.getGDPRMessageVersion();
  }

  public static String getCcpaStatus() {
    return VunglePrivacySettings.getCCPAStatus();
  }

  public static void setCCPAStatus(Boolean optedIn) {
    VunglePrivacySettings.setCCPAStatus(optedIn);
  }

  public static void publishAndroidId(Boolean publishAndroidId) {
    VunglePrivacySettings.setPublishAndroidId(publishAndroidId);
  }

}
