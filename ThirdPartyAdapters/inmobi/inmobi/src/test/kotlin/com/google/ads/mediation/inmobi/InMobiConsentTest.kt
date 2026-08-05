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

package com.google.ads.mediation.inmobi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InMobiConsentTest {

  @Test
  fun updateGDPRConsent_updatesConsentObj() {
    val consentJson =
      JSONObject().apply {
        put("gdpr_consent_available", true)
        put("gdpr", "1")
      }

    InMobiConsent.updateGDPRConsent(consentJson)

    assertThat(InMobiConsent.getConsentObj()).isEqualTo(consentJson)
  }

  @Test
  fun getConsentObj_defaultReturnsEmptyJsonObject() {
    InMobiConsent.updateGDPRConsent(JSONObject())

    assertThat(InMobiConsent.getConsentObj().length()).isEqualTo(0)
  }
}
