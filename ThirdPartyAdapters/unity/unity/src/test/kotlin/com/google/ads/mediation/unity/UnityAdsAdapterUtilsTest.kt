package com.google.ads.mediation.unity

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.ads.mediation.unity.UnityMediationAdapter.AD_TECHNOLOGY_PROVIDER_ID
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.banners.BannerErrorInfo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector

/** Unit tests for [UnityAdsAdapterUtils]. */
@RunWith(RobolectricTestParameterInjector::class)
class UnityAdsAdapterUtilsTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val sharedPreferences: SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)
  private var bannerErrorInfo: BannerErrorInfo = mock()
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    sharedPreferences.edit().clear().commit()
  }

  enum class BannerErrorTestCase(val error: BannerErrorCode, val expectedErrorCode: Int) {
    UNKNOWN(BannerErrorCode.UNKNOWN, 201),
    NATIVE_ERROR(BannerErrorCode.NATIVE_ERROR, 202),
    WEBVIEW_ERROR(BannerErrorCode.WEBVIEW_ERROR, 203),
    NO_FILL(BannerErrorCode.NO_FILL, 204),
  }

  enum class InitializationErrorTestCase(
    val error: UnityAdsInitializationError,
    val expectedErrorCode: Int,
  ) {
    INTERNAL_ERROR(UnityAdsInitializationError.INTERNAL_ERROR, 301),
    INVALID_ARGUMENT(UnityAdsInitializationError.INVALID_ARGUMENT, 302),
    AD_BLOCKER_DETECTED(UnityAdsInitializationError.AD_BLOCKER_DETECTED, 303),
  }

  enum class LoadErrorTestCase(val error: UnityAdsLoadError, val expectedErrorCode: Int) {
    INITIALIZE_FAILED(UnityAdsLoadError.INITIALIZE_FAILED, 401),
    INTERNAL_ERROR(UnityAdsLoadError.INTERNAL_ERROR, 402),
    INVALID_ARGUMENT(UnityAdsLoadError.INVALID_ARGUMENT, 403),
    NO_FILL(UnityAdsLoadError.NO_FILL, 404),
    TIMEOUT(UnityAdsLoadError.TIMEOUT, 405),
  }

  enum class ShowErrorTestCase(val error: UnityAdsShowError, val expectedErrorCode: Int) {
    NOT_INITIALIZED(UnityAdsShowError.NOT_INITIALIZED, 501),
    NOT_READY(UnityAdsShowError.NOT_READY, 502),
    VIDEO_PLAYER_ERROR(UnityAdsShowError.VIDEO_PLAYER_ERROR, 503),
    INVALID_ARGUMENT(UnityAdsShowError.INVALID_ARGUMENT, 504),
    NO_CONNECTION(UnityAdsShowError.NO_CONNECTION, 505),
    ALREADY_SHOWING(UnityAdsShowError.ALREADY_SHOWING, 506),
    INTERNAL_ERROR(UnityAdsShowError.INTERNAL_ERROR, 507),
    TIMEOUT(UnityAdsShowError.TIMEOUT, 508),
  }

  @Test
  fun getMediationErrorCode_withBannerErrorInfo_returnsCorrectValue(
    @TestParameter testCase: BannerErrorTestCase
  ) {
    bannerErrorInfo.errorCode = testCase.error

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(bannerErrorInfo)

    assertThat(errorCode).isEqualTo(testCase.expectedErrorCode)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsInitializationError_returnsCorrectValue(
    @TestParameter testCase: InitializationErrorTestCase
  ) {
    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(testCase.error)

    assertThat(errorCode).isEqualTo(testCase.expectedErrorCode)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsLoadError_returnsCorrectValue(
    @TestParameter testCase: LoadErrorTestCase
  ) {
    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(testCase.error)

    assertThat(errorCode).isEqualTo(testCase.expectedErrorCode)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorrectValue(
    @TestParameter testCase: ShowErrorTestCase
  ) {
    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(testCase.error)

    assertThat(errorCode).isEqualTo(testCase.expectedErrorCode)
  }

  @Test
  fun getUnityBannerSize_isRtbTrue_whenClosestSizeIsNull_returnsRequestedAdSize() {
    val adSize = AdSize(999, 999)
    whenever(mediationUtils.findClosestSize(eq(context), eq(adSize), any())) doReturn null

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ true, mediationUtils)

    assertThat(unityBannerAdSize).isNotNull()
    assertThat(unityBannerAdSize?.width).isEqualTo(adSize.width)
    assertThat(unityBannerAdSize?.height).isEqualTo(adSize.height)
  }

  @Test
  fun getUnityBannerSize_isRtbFalse_whenClosestSizeIsNull_returnsNull() {
    val adSize = AdSize(999, 999)
    whenever(mediationUtils.findClosestSize(eq(context), eq(adSize), any())) doReturn null

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false, mediationUtils)

    assertThat(unityBannerAdSize).isNull()
  }

  @Test
  fun getUnityBannerSize_returnsCorrectBannerSize() {
    val adSize = AdSize.BANNER
    whenever(mediationUtils.findClosestSize(eq(context), eq(adSize), any())) doReturn AdSize.BANNER

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false, mediationUtils)

    assertThat(unityBannerAdSize).isNotNull()
    assertThat(unityBannerAdSize?.width).isEqualTo(320)
    assertThat(unityBannerAdSize?.height).isEqualTo(50)
  }

  @Test
  fun getUnityBannerSize_returnsCorrectLeaderboardSize() {
    val adSize = AdSize.LEADERBOARD
    whenever(mediationUtils.findClosestSize(eq(context), eq(adSize), any())) doReturn
      AdSize.LEADERBOARD

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false, mediationUtils)

    assertThat(unityBannerAdSize).isNotNull()
    assertThat(unityBannerAdSize?.width).isEqualTo(728)
    assertThat(unityBannerAdSize?.height).isEqualTo(90)
  }

  @Test
  fun createAdError_returnsAdErrorWithCorrectValues() {
    val adError = UnityAdsAdapterUtils.createAdError(200, "Description")

    assertThat(adError.getCode()).isEqualTo(200)
    assertThat(adError.getMessage()).isEqualTo("Description")
    assertThat(adError.getDomain()).isEqualTo(UnityMediationAdapter.SDK_ERROR_DOMAIN)
  }

  // region setUnityAdsPrivacy() Tests
  private fun assertPrivacyPreferences(
    requestConfiguration: RequestConfiguration,
    expectedNonBehavioral: Boolean,
  ) {
    mockStatic(UnityAds::class.java).use { mockedUnityAds ->
      UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

      mockedUnityAds.verify { UnityAds.nonBehavioral = expectedNonBehavioral }
    }
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDTrueAndTFUATrue_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDTrueAndTFUAFalse_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDTrue_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDFalseAndTFUATrue_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  @Test
  fun setUnityAdsPrivacy_withTFUATrue_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDFalseAndTFUAFalse_setsNonBehavioralToFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = false)
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDFalseAndTFUAUnspecified_setsNonBehavioralToFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = false)
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDUnspecifiedAndTFUAFalse_setsNonBehavioralToFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = false)
  }

  @Test
  fun setUnityAdsPrivacy_withAgeRestrictedTreatmentChild_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  @Test
  fun setUnityAdsPrivacy_withAgeRestrictedTreatmentTeen_doesNotSetNonBehavioral() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN)
        .build()

    mockStatic(UnityAds::class.java).use { mockedUnityAds ->
      UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

      mockedUnityAds.verifyNoInteractions()
    }
  }

  @Test
  fun setUnityAdsPrivacy_withAllUnspecified_doesNotSetNonBehavioral() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()

    mockStatic(UnityAds::class.java).use { mockedUnityAds ->
      UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

      mockedUnityAds.verifyNoInteractions()
    }
  }

  @Test
  fun setUnityAdsPrivacy_withAgeRestrictedTreatmentChildOverridesLegacyAdult_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  @Test
  fun setUnityAdsPrivacy_withAgeRestrictedTreatmentTeenWithLegacyAdult_setsNonBehavioralToFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = false)
  }

  @Test
  fun setUnityAdsPrivacy_withLegacyChildOverridesAgeRestrictedTreatmentTeen_setsNonBehavioralToTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN)
        .build()

    assertPrivacyPreferences(requestConfiguration, expectedNonBehavioral = true)
  }

  // endregion

  // region hasACConsent() Tests
  @Test
  fun hasACConsent_withNegativeGDPRApplies_returnsUnknown() {
    sharedPreferences.edit().putInt("IABTCF_gdprApplies", -1).commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withZeroGDPRApplies_returnsUnknown() {
    sharedPreferences.edit().putInt("IABTCF_gdprApplies", 0).commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidGDPRApplies_returnsUnknown() {
    sharedPreferences.edit().putString("IABTCF_gdprApplies", "not_an_int").commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidAdditionalConsent_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putInt("IABTCF_AddtlConsent", 123)
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withEmptyConsent_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withUnknownSpecVersion_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "0~3234.1~dv.2.3")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withInvalidSpecVersion_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "a~3234.1~dv.2.3")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withNoConsentedVendor_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "1~")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withUnityIncludedInAdditionalConsent_returnsTrue() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "1~1.3234")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withUnityNotIncludedInAdditionalConsent_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "1~1.2")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionOneSpec_withUnexpectedParts_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "1~3234.1~dv.2.3")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withInvalidDisclosedFormat_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~3234.1~ax.2.3")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnexpectedParts_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~3234.1")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnityIncludedInAdditionalConsent_returnsTrue() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~1.3234~dv.2.3")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnityIncludedInAdditionalConsent_withNoneDisclosed_returnsTrue() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~1.3234~dv")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.TRUE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnityDisclosedInAdditionalConsent_returnsFalse() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~1.2~dv.3234.3")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.FALSE)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withUnityMissingInAdditionalConsent_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~1.2~dv.3.4")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withEmptyConsentedPartners_returnsUnknown() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~~dv.3.4")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.UNKNOWN)
  }

  @Test
  fun hasACConsent_withVersionTwoSpec_withEmptyConsentedPartners_withUnityDisclosed_returnsFalse() {
    sharedPreferences
      .edit()
      .putInt("IABTCF_gdprApplies", 1)
      .putString("IABTCF_AddtlConsent", "2~~dv.3234.3")
      .commit()

    val consentResult = UnityAdsAdapterUtils.hasACConsent(context, AD_TECHNOLOGY_PROVIDER_ID)

    assertThat(consentResult).isEqualTo(UnityAdsAdapterUtils.ConsentResult.FALSE)
  }

  // endregion
}
