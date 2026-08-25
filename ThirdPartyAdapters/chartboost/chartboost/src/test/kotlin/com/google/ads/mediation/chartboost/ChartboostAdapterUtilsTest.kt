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
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.ads.Banner
import com.chartboost.sdk.privacy.model.COPPA
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_AD_LOCATION
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_APP_ID
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.KEY_APP_SIGNATURE
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.LOCATION_DEFAULT
import com.google.ads.mediation.chartboost.ChartboostConstants.AD_TECHNOLOGY_PROVIDER_ID
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MediationUtils
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/** Tests for [ChartboostAdapterUtils]. */
@RunWith(AndroidJUnit4::class)
class ChartboostAdapterUtilsTest {

  private val sharedPreferences = mock<SharedPreferences>()
  private val context =
    mock<Context> { on { getSharedPreferences(any(), any()) } doReturn sharedPreferences }
  private val appContext = ApplicationProvider.getApplicationContext<Context>()

  // region createChartboostParams() Tests
  @Test
  fun createChartboostParams_validBundle_returnsPopulatedChartboostParams() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to TEST_LOCATION,
      )

    val params = ChartboostAdapterUtils.createChartboostParams(serverParameters)

    assertThat(params.appId).isEqualTo(TEST_APP_ID)
    assertThat(params.appSignature).isEqualTo(TEST_APP_SIGNATURE)
    assertThat(params.location).isEqualTo(TEST_LOCATION)
  }

  @Test
  fun createChartboostParams_emptyLocation_defaultsToLocationDefault() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to TEST_APP_ID,
        KEY_APP_SIGNATURE to TEST_APP_SIGNATURE,
        KEY_AD_LOCATION to "",
      )

    val params = ChartboostAdapterUtils.createChartboostParams(serverParameters)

    assertThat(params.location).isEqualTo(LOCATION_DEFAULT)
  }

  @Test
  fun createChartboostParams_withWhitespace_trimsValues() {
    val serverParameters =
      bundleOf(
        KEY_APP_ID to "  $TEST_APP_ID  ",
        KEY_APP_SIGNATURE to "  $TEST_APP_SIGNATURE  ",
        KEY_AD_LOCATION to "  $TEST_LOCATION  ",
      )

    val params = ChartboostAdapterUtils.createChartboostParams(serverParameters)

    assertThat(params.appId).isEqualTo(TEST_APP_ID)
    assertThat(params.appSignature).isEqualTo(TEST_APP_SIGNATURE)
    assertThat(params.location).isEqualTo(TEST_LOCATION)
  }

  // endregion

  // region isValidChartboostParams() Tests
  @Test
  fun isValidChartboostParams_nullParams_returnsFalse() {
    val isValid = ChartboostAdapterUtils.isValidChartboostParams(null)

    assertThat(isValid).isFalse()
  }

  @Test
  fun isValidChartboostParams_emptyAppId_returnsFalse() {
    val params =
      ChartboostParams().apply {
        appId = ""
        appSignature = TEST_APP_SIGNATURE
      }

    val isValid = ChartboostAdapterUtils.isValidChartboostParams(params)

    assertThat(isValid).isFalse()
  }

  @Test
  fun isValidChartboostParams_emptyAppSignature_returnsFalse() {
    val params =
      ChartboostParams().apply {
        appId = TEST_APP_ID
        appSignature = ""
      }

    val isValid = ChartboostAdapterUtils.isValidChartboostParams(params)

    assertThat(isValid).isFalse()
  }

  @Test
  fun isValidChartboostParams_validAppIdAndSignature_returnsTrue() {
    val params =
      ChartboostParams().apply {
        appId = TEST_APP_ID
        appSignature = TEST_APP_SIGNATURE
      }

    val isValid = ChartboostAdapterUtils.isValidChartboostParams(params)

    assertThat(isValid).isTrue()
  }

  // endregion

  // region findClosestBannerSize() Tests
  @Test
  fun findClosestBannerSize_standardSize_returnsStandardBannerSize() {
    mockStatic(MediationUtils::class.java).use { mockMediationUtils ->
      mockMediationUtils
        .`when`<AdSize> { MediationUtils.findClosestSize(eq(appContext), eq(AdSize.BANNER), any()) }
        .thenAnswer { (it.arguments[2] as List<AdSize>)[0] }

      val bannerSize = ChartboostAdapterUtils.findClosestBannerSize(appContext, AdSize.BANNER)

      assertThat(bannerSize).isEqualTo(Banner.BannerSize.STANDARD)
    }
  }

  @Test
  fun findClosestBannerSize_mediumSize_returnsMediumBannerSize() {
    val mediumRectangle = AdSize.MEDIUM_RECTANGLE
    mockStatic(MediationUtils::class.java).use { mockMediationUtils ->
      mockMediationUtils
        .`when`<AdSize> {
          MediationUtils.findClosestSize(eq(appContext), eq(mediumRectangle), any())
        }
        .thenAnswer { (it.arguments[2] as List<AdSize>)[1] }

      val bannerSize = ChartboostAdapterUtils.findClosestBannerSize(appContext, mediumRectangle)

      assertThat(bannerSize).isEqualTo(Banner.BannerSize.MEDIUM)
    }
  }

  @Test
  fun findClosestBannerSize_leaderboardSize_returnsLeaderboardBannerSize() {
    val leaderboard = AdSize.LEADERBOARD
    mockStatic(MediationUtils::class.java).use { mockMediationUtils ->
      mockMediationUtils
        .`when`<AdSize> { MediationUtils.findClosestSize(eq(appContext), eq(leaderboard), any()) }
        .thenAnswer { (it.arguments[2] as List<AdSize>)[2] }

      val bannerSize = ChartboostAdapterUtils.findClosestBannerSize(appContext, leaderboard)

      assertThat(bannerSize).isEqualTo(Banner.BannerSize.LEADERBOARD)
    }
  }

  @Test
  fun findClosestBannerSize_halfPageSize_returnsHalfPageBannerSize() {
    val halfPage = AdSize(300, 600)
    mockStatic(MediationUtils::class.java).use { mockMediationUtils ->
      mockMediationUtils
        .`when`<AdSize> { MediationUtils.findClosestSize(eq(appContext), eq(halfPage), any()) }
        .thenAnswer { (it.arguments[2] as List<AdSize>)[3] }

      val bannerSize = ChartboostAdapterUtils.findClosestBannerSize(appContext, halfPage)

      assertThat(bannerSize).isEqualTo(Banner.BannerSize.HALFPAGE)
    }
  }

  @Test
  fun findClosestBannerSize_unsupportedSize_returnsNull() {
    val unsupportedSize = AdSize(123, 456)
    mockStatic(MediationUtils::class.java).use { mockMediationUtils ->
      mockMediationUtils
        .`when`<AdSize> {
          MediationUtils.findClosestSize(eq(appContext), eq(unsupportedSize), any())
        }
        .thenReturn(null)

      val bannerSize = ChartboostAdapterUtils.findClosestBannerSize(appContext, unsupportedSize)

      assertThat(bannerSize).isNull()
    }
  }

  // endregion

  // region getChartboostMediation() Tests
  @Test
  fun getChartboostMediation_returnsMediationInstanceWithCorrectVersion() {
    mockStatic(Chartboost::class.java).use { mockChartboost ->
      mockChartboost.`when`<String> { Chartboost.getSDKVersion() }.thenReturn(TEST_SDK_VERSION)

      val mediation = ChartboostAdapterUtils.getChartboostMediation()

      assertIs<Mediation>(mediation)
      assertThat(mediation.mediationType).isEqualTo("AdMob")
      assertThat(mediation.libraryVersion).isEqualTo(TEST_SDK_VERSION)
      assertThat(mediation.adapterVersion).isEqualTo(BuildConfig.ADAPTER_VERSION)
    }
  }

  // endregion

  // region updateCoppaStatus() Tests
  @Test
  fun updateCoppaStatus_withChildDirectedTrue_addsCoppaTrue() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    val coppaCaptor = argumentCaptor<COPPA>()

    mockStatic(Chartboost::class.java).use { mockChartboost ->
      ChartboostAdapterUtils.updateCoppaStatus(appContext, requestConfig)

      mockChartboost.verify { Chartboost.addDataUseConsent(eq(appContext), coppaCaptor.capture()) }
      assertThat(coppaCaptor.firstValue.consent).isTrue()
    }
  }

  @Test
  fun updateCoppaStatus_withUnderAgeConsentTrue_addsCoppaTrue() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    val coppaCaptor = argumentCaptor<COPPA>()

    mockStatic(Chartboost::class.java).use { mockChartboost ->
      ChartboostAdapterUtils.updateCoppaStatus(appContext, requestConfig)

      mockChartboost.verify { Chartboost.addDataUseConsent(eq(appContext), coppaCaptor.capture()) }
      assertThat(coppaCaptor.firstValue.consent).isTrue()
    }
  }

  @Test
  fun updateCoppaStatus_withChildDirectedFalse_addsCoppaFalse() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .build()
    val coppaCaptor = argumentCaptor<COPPA>()

    mockStatic(Chartboost::class.java).use { mockChartboost ->
      ChartboostAdapterUtils.updateCoppaStatus(appContext, requestConfig)

      mockChartboost.verify { Chartboost.addDataUseConsent(eq(appContext), coppaCaptor.capture()) }
      assertThat(coppaCaptor.firstValue.consent).isFalse()
    }
  }

  @Test
  fun updateCoppaStatus_withUnderAgeConsentFalse_addsCoppaFalse() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    val coppaCaptor = argumentCaptor<COPPA>()

    mockStatic(Chartboost::class.java).use { mockChartboost ->
      ChartboostAdapterUtils.updateCoppaStatus(appContext, requestConfig)

      mockChartboost.verify { Chartboost.addDataUseConsent(eq(appContext), coppaCaptor.capture()) }
      assertThat(coppaCaptor.firstValue.consent).isFalse()
    }
  }

  // endregion

  // region getAdapterVersion() Tests
  @Test
  fun getAdapterVersion_returnsBuildConfigAdapterVersion() {
    val version = ChartboostAdapterUtils.getAdapterVersion()

    assertThat(version).isEqualTo(BuildConfig.ADAPTER_VERSION)
  }

  // endregion

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

  private companion object {
    const val TEST_APP_ID = "test_app_id"
    const val TEST_APP_SIGNATURE = "test_app_signature"
    const val TEST_LOCATION = "test_location"
    const val TEST_SDK_VERSION = "9.13.0"
  }
}
