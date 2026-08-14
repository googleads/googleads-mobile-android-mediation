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

package com.google.ads.mediation.maio

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.common.truth.Truth.assertThat
import jp.maio.sdk.android.mediation.admob.adapter.BuildConfig
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [MaioUtils]. */
@RunWith(AndroidJUnit4::class)
class MaioUtilsTest {

  @Test
  fun getVersionInfo_returnsAdapterVersion() {
    assertThat(MaioUtils.getVersionInfo()).isEqualTo(BuildConfig.ADAPTER_VERSION)
  }

  @Test
  fun getIsUserChild_tagForChildDirectedTreatmentTrue_returnsTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    assertThat(MaioUtils.getIsUserChild()).isTrue()
  }

  @Test
  fun getIsUserChild_tagForUnderAgeOfConsentTrue_returnsTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    assertThat(MaioUtils.getIsUserChild()).isTrue()
  }

  @Test
  fun getIsUserChild_ageRestrictedTreatmentChild_returnsTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    assertThat(MaioUtils.getIsUserChild()).isTrue()
  }

  @Test
  fun getIsUserChild_allUnspecified_returnsFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    assertThat(MaioUtils.getIsUserChild()).isFalse()
  }

  @Test
  fun getIsUserChild_allFalseAndTeen_returnsFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    assertThat(MaioUtils.getIsUserChild()).isFalse()
  }
}
