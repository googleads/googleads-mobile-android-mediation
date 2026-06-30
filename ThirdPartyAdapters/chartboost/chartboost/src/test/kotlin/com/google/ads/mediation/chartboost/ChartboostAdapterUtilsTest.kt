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

package com.google.ads.mediation.chartboost

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.chartboost.ChartboostConstants.AD_TECHNOLOGY_PROVIDER_ID
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/** Tests for [ChartboostAdapterUtils]. */
@RunWith(AndroidJUnit4::class)
class ChartboostAdapterUtilsTest {

  private var sharedPreferences = mock<SharedPreferences>()
  private val context =
    mock<Context> { on { getSharedPreferences(any(), any()) } doReturn sharedPreferences }

  // region hasACConsent() Tests
  @Test
  fun hasACConsent_withNegativeGDPRApplies_returnsUnknown() {
    sharedPreferences.stub { on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn -1 }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withZeroGDPRApplies_returnsUnknown() {
    sharedPreferences.stub { on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 0 }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidGDPRApplies_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doThrow ClassCastException::class
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidAdditionalConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doThrow ClassCastException::class
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withEmptyConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn ""
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withUnknownSpecVersion_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "0~2898.1~dv.2.3"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidSpecVersion_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "a~2898.1~dv.2.3"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withNoConsentedVendor_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withChartboostIncludedInAdditionalConsent_returnsTrue() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~1.2898"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withChartboostNotIncludedInAdditionalConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~1.2"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withUnexpectedParts_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~2898.1~dv.2.3"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withInvalidDisclosedFormat_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~2898.1~ax.2.3"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnexpectedParts_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~2898.1"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withChartboostIncludedInAdditionalConsent_returnsTrue() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.2898~dv.2.3"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withChartboostIncludedInAdditionalConsent_withNoneDisclosed_returnsTrue() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.2898~dv"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withChartboostDisclosedInAdditionalConsent_returnsFalse() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.2~dv.2898.3"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.FALSE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withChartboostMissingInAdditionalConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.2~dv.3.4"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withEmptyAdditionalConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~~dv.3.4"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withEmptyAdditionalConsent_withChartboostDisclosed_returnsFalse() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~~dv.2898.3"
    }

    val consentResult = ChartboostAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(ChartboostAdapterUtils.ConsentResult.FALSE)
  }
  // endregion
}
