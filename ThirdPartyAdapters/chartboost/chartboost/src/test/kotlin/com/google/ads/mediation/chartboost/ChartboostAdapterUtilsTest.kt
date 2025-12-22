// Copyright 2025 Google LLC
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

package com.google.ads.mediation.chartboost

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.chartboost.ChartboostConstants.AD_TECHNOLOGY_PROVIDER_ID
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [ChartboostAdapterUtils]. */
@RunWith(AndroidJUnit4::class)
class ChartboostAdapterUtilsTest {

  private val context: Context = mock()
  private var sharedPreferences: SharedPreferences = mock()

  @Before
  fun setUp() {
    whenever(context.getSharedPreferences(any(), any())).thenReturn(sharedPreferences)
  }

  // region hasACConsent() Tests
  @Test
  fun hasACConsent_withNegativeGDPRApplies_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(-1)

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withZeroGDPRApplies_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(0)

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidGDPRApplies_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any()))
      .thenThrow(ClassCastException::class.java)

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidAdditionalConsent_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenThrow(ClassCastException::class.java)

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withUnknownSpecVersion_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenReturn("0~2898.1~dv.2.3")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidSpecVersion_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenReturn("a~2898.1~dv.2.3")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withNoConsentedVendor_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any())).thenReturn("1~")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withChartboostIncludedInAdditionalConsent_returnsTrue() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any())).thenReturn("1~1.2898")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withChartboostNotIncludedInAdditionalConsent_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any())).thenReturn("1~1.2")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withUnexpectedParts_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenReturn("1~2898.1~dv.2.3")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withInvalidDisclosedFormat_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenReturn("2~2898.1~ax.2.3")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnexpectedParts_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any())).thenReturn("2~2898.1")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withChartboostIncludedInAdditionalConsent_returnsTrue() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenReturn("2~1.2898~dv.2.3")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withChartboostDisclosedInAdditionalConsent_returnsFalse() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenReturn("2~1.2~dv.2898.3")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.FALSE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withChartboostMissingInAdditionalConsent_returnsUnknown() {
    whenever(sharedPreferences.getInt(eq("IABTCF_gdprApplies"), any())).thenReturn(1)
    whenever(sharedPreferences.getString(eq("IABTCF_AddtlConsent"), any()))
      .thenReturn("2~1.2~dv.3.4")

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostConstants.ConsentResult.UNKNOWN)
  }
  // endregion

}
