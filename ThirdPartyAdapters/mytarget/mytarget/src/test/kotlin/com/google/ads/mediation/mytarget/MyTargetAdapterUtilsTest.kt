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

package com.google.ads.mediation.mytarget

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.AD_TECHNOLOGY_PROVIDER_ID
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/** Class containing unit tests for MyTargetAdapterUtils.java */
@RunWith(AndroidJUnit4::class)
class MyTargetAdapterUtilsTest {

  private var sharedPreferences = mock<SharedPreferences>()
  private val context =
    mock<Context> { on { getSharedPreferences(any(), any()) } doReturn sharedPreferences }

  // region hasACConsent() Tests
  @Test
  fun hasACConsent_withNegativeGDPRApplies_returnsUnknown() {
    sharedPreferences.stub { on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn -1 }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withZeroGDPRApplies_returnsUnknown() {
    sharedPreferences.stub { on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 0 }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidGDPRApplies_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doThrow ClassCastException::class
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidAdditionalConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doThrow ClassCastException::class
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withUnknownSpecVersion_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "0~1067.1~dv.2.3"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidSpecVersion_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "a~1067.1~dv.2.3"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withNoConsentedVendor_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withMyTargetIncludedInAdditionalConsent_returnsTrue() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~1.1067"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withMyTargetNotIncludedInAdditionalConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~1.2"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withUnexpectedParts_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "1~1067.1~dv.2.3"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withInvalidDisclosedFormat_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1067.1~ax.2.3"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnexpectedParts_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1067.1"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withMyTargetIncludedInAdditionalConsent_returnsTrue() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.1067~dv.2.3"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withMyTargetIncludedInAdditionalConsent_withNoneDisclosed_returnsTrue() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.1067~dv"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withMyTargetDisclosedInAdditionalConsent_returnsFalse() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.2~dv.1067.3"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.FALSE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withMyTargetMissingInAdditionalConsent_returnsUnknown() {
    sharedPreferences.stub {
      on { getInt(eq("IABTCF_gdprApplies"), any()) } doReturn 1
      on { getString(eq("IABTCF_AddtlConsent"), any()) } doReturn "2~1.2~dv.3.4"
    }

    val consentResult = MyTargetAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(MyTargetMediationAdapter.ConsentResult.UNKNOWN)
  }

  // endregion

}
