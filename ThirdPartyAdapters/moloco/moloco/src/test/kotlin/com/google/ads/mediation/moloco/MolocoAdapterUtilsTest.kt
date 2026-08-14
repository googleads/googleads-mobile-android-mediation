// Copyright 2024 Google LLC
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

package com.google.ads.mediation.moloco

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.privacy.MolocoPrivacy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.argumentCaptor

@RunWith(AndroidJUnit4::class)
class MolocoAdapterUtilsTest {

  private lateinit var mockMolocoPrivacy: MockedStatic<MolocoPrivacy>

  @Before
  fun setUp() {
    mockMolocoPrivacy = mockStatic(MolocoPrivacy::class.java)
  }

  @After
  fun tearDown() {
    mockMolocoPrivacy.close()
    MolocoPrivacy.privacySettings = MolocoPrivacy.PrivacySettings()
  }

  @Test
  fun adapterVersion_returnsAdapterVersion() {
    assertThat(MolocoAdapterUtils.adapterVersion).isEqualTo(BuildConfig.ADAPTER_VERSION)
  }

  @Test
  fun setMolocoIsAgeRestricted_true_setsPrivacySettingsWithAgeRestrictedTrue() {
    val existingPrivacySettings =
      MolocoPrivacy.PrivacySettings(
        isAgeRestrictedUser = false,
        isDoNotSell = true,
        isUserConsent = true,
      )
    MolocoPrivacy.privacySettings = existingPrivacySettings
    val privacySettingsCaptor = argumentCaptor<MolocoPrivacy.PrivacySettings>()

    MolocoAdapterUtils.setMolocoIsAgeRestricted(true)

    mockMolocoPrivacy.verify { MolocoPrivacy.setPrivacy(privacySettingsCaptor.capture()) }
    val capturedSettings = privacySettingsCaptor.firstValue
    assertThat(capturedSettings.isAgeRestrictedUser).isTrue()
    assertThat(capturedSettings.isDoNotSell).isTrue()
    assertThat(capturedSettings.isUserConsent).isTrue()
  }

  @Test
  fun setMolocoIsAgeRestricted_false_setsPrivacySettingsWithAgeRestrictedFalse() {
    val existingPrivacySettings =
      MolocoPrivacy.PrivacySettings(
        isAgeRestrictedUser = true,
        isDoNotSell = false,
        isUserConsent = false,
      )
    MolocoPrivacy.privacySettings = existingPrivacySettings
    val privacySettingsCaptor = argumentCaptor<MolocoPrivacy.PrivacySettings>()

    MolocoAdapterUtils.setMolocoIsAgeRestricted(false)

    mockMolocoPrivacy.verify { MolocoPrivacy.setPrivacy(privacySettingsCaptor.capture()) }
    val capturedSettings = privacySettingsCaptor.firstValue
    assertThat(capturedSettings.isAgeRestrictedUser).isFalse()
    assertThat(capturedSettings.isDoNotSell).isFalse()
    assertThat(capturedSettings.isUserConsent).isFalse()
  }
}
