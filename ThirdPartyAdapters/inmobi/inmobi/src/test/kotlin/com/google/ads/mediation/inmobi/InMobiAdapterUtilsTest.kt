package com.google.ads.mediation.inmobi

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.sdk.InMobiSdk
import java.util.ArrayList
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiAdapterUtilsTest {

  private val inMobiNativeWrapper = mock<InMobiNativeWrapper>()

  private lateinit var serverParameters: Bundle

  @Before
  fun setup() {
    serverParameters = Bundle()
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "67890")
    setupMobiNativeAdWrapper()
  }

  @Test
  fun getPlacementID_missingPlacementID_returnsZero() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    val placementID = InMobiAdapterUtils.getPlacementId(serverParameters)

    assertThat(placementID).isEqualTo(0L)
  }

  @Test
  fun getPlacementID_invalidPlacementID_returnsZero() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "inmobi")

    val placementID = InMobiAdapterUtils.getPlacementId(serverParameters)

    assertThat(placementID).isEqualTo(0L)
  }

  @Test
  fun getPlacementID_validPlacementID_returnsPlacementId() {
    val placementID = InMobiAdapterUtils.getPlacementId(serverParameters)

    assertThat(placementID).isEqualTo(67890L)
  }

  @Test
  fun setIsAgeRestricted_whenCOPPASet_setsAgeRestrictedTrueOnInMobiSDK() {
    val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
    setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
    )

    InMobiAdapterUtils.setIsAgeRestricted(inMobiSdkWrapper)

    verify(inMobiSdkWrapper).setIsAgeRestricted(true)
  }

  @Test
  fun setIsAgeRestricted_whenUnderAgeConsentSet_setsAgeRestrictedTrueOnInMobiSDK() {
    val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
    setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE,
    )

    InMobiAdapterUtils.setIsAgeRestricted(inMobiSdkWrapper)

    verify(inMobiSdkWrapper).setIsAgeRestricted(true)
  }

  @Test
  fun setIsAgeRestricted_whenCOPPANotSet_setsAgeRestrictedFalseOnInMobiSDK() {
    val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
    setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
    )

    InMobiAdapterUtils.setIsAgeRestricted(inMobiSdkWrapper)

    verify(inMobiSdkWrapper).setIsAgeRestricted(false)
  }

  @Test
  fun setIsAgeRestricted_whenUnderAgeConsentNotSet_setsAgeRestrictedFalseOnInMobiSDK() {
    val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
    setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE,
    )

    InMobiAdapterUtils.setIsAgeRestricted(inMobiSdkWrapper)

    verify(inMobiSdkWrapper).setIsAgeRestricted(false)
  }

  @Test
  fun setIsAgeRestricted_whenCOPPANotSpecified_setIsAgeRestrictedIsNeverInvoked() {
    val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
    setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      AgeRestrictedTreatment.UNSPECIFIED,
    )

    InMobiAdapterUtils.setIsAgeRestricted(inMobiSdkWrapper)

    verify(inMobiSdkWrapper, never()).setIsAgeRestricted(any())
  }

  @Test
  fun setIsAgeRestricted_whenAgeRestrictedTreatmentChild_setsAgeRestrictedTrueOnInMobiSDK() {
    val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
    setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      AgeRestrictedTreatment.CHILD,
    )

    InMobiAdapterUtils.setIsAgeRestricted(inMobiSdkWrapper)

    verify(inMobiSdkWrapper).setIsAgeRestricted(true)
  }

  @Test
  fun setIsAgeRestricted_whenAgeRestrictedTreatmentTeen_setIsAgeRestrictedIsNeverInvoked() {
    val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
    setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      AgeRestrictedTreatment.TEEN,
    )

    InMobiAdapterUtils.setIsAgeRestricted(inMobiSdkWrapper)

    verify(inMobiSdkWrapper, never()).setIsAgeRestricted(any())
  }

  @Test
  fun getAgeGroup_returnsCorrectAgeGroup() {
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.BELOW_18,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.BELOW_18,
    )
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.ABOVE_65,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.ABOVE_65,
    )
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.BETWEEN_18_AND_24,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.BETWEEN_18_AND_24,
    )
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.BETWEEN_25_AND_29,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.BETWEEN_25_AND_29,
    )
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.BETWEEN_30_AND_34,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.BETWEEN_30_AND_34,
    )
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.BETWEEN_35_AND_44,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.BETWEEN_35_AND_44,
    )
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.BETWEEN_45_AND_54,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.BETWEEN_45_AND_54,
    )
    invokeAndAssertGetAgeGroup(
      /* value= */ InMobiNetworkValues.BETWEEN_55_AND_65,
      /* expectedAgeGroup= */ InMobiSdk.AgeGroup.BETWEEN_55_AND_65,
    )
  }

  private fun invokeAndAssertGetAgeGroup(age: String, expectedAgeGroup: InMobiSdk.AgeGroup) {
    val ageGroup = InMobiAdapterUtils.getAgeGroup(age)

    assertThat(ageGroup).isEqualTo(expectedAgeGroup)
  }

  @Test
  fun getAgeGroup_invalidAge_returnsNull() {
    val ageGroup = InMobiAdapterUtils.getAgeGroup(/* value= */ "")

    assertThat(ageGroup).isNull()
  }

  @Test
  fun getEducation_returnsCorrectEducation() {
    invokeAndAssertGetEducation(
      /* value= */ InMobiNetworkValues.EDUCATION_COLLEGEORGRADUATE,
      /* expectedEducation= */ InMobiSdk.Education.COLLEGE_OR_GRADUATE,
    )
    invokeAndAssertGetEducation(
      /* value= */ InMobiNetworkValues.EDUCATION_HIGHSCHOOLORLESS,
      /* expectedEducation= */ InMobiSdk.Education.HIGH_SCHOOL_OR_LESS,
    )
    invokeAndAssertGetEducation(
      /* value= */ InMobiNetworkValues.EDUCATION_POSTGRADUATEORABOVE,
      /* expectedEducation= */ InMobiSdk.Education.POST_GRADUATE_OR_ABOVE,
    )
  }

  private fun invokeAndAssertGetEducation(value: String, expectedEducation: InMobiSdk.Education) {
    val education = InMobiAdapterUtils.getEducation(value)

    assertThat(education).isEqualTo(expectedEducation)
  }

  @Test
  fun getEducation_invalidEducation_returnsNull() {
    val education = InMobiAdapterUtils.getEducation(/* value= */ "")

    assertThat(education).isNull()
  }

  @Test
  fun getLogLevel_returnsCorrectLogLevel() {
    invokeAndAssertGetLogLevel(
      /* value= */ InMobiNetworkValues.LOGLEVEL_DEBUG,
      /* expectedLogLevel= */ InMobiSdk.LogLevel.DEBUG,
    )
    invokeAndAssertGetLogLevel(
      /* value= */ InMobiNetworkValues.LOGLEVEL_ERROR,
      InMobiSdk.LogLevel.ERROR,
    )
  }

  private fun invokeAndAssertGetLogLevel(value: String, expectedLogLevel: InMobiSdk.LogLevel) {
    val logLevel = InMobiAdapterUtils.getLogLevel(value)

    assertThat(logLevel).isEqualTo(expectedLogLevel)
  }

  @Test
  fun getLogLevel_invalidLogLevel_returnLogLevelNone() {
    val logLevel = InMobiAdapterUtils.getLogLevel(/* value= */ "random")

    assertThat(logLevel).isEqualTo(InMobiSdk.LogLevel.NONE)
  }

  @Test
  fun validateInMobiAdLoadParams_emptyAccountID_returnsAdError() {
    val adError =
      InMobiAdapterUtils.validateInMobiAdLoadParams(/* accountID= */ "", /* placementID= */ 12345L)

    assertThat(adError).isNotNull()
    assertThat(adError?.code).isEqualTo(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(adError?.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun validateInMobiAdLoadParams_NullAccountID_returnsAdError() {
    val adError =
      InMobiAdapterUtils.validateInMobiAdLoadParams(
        /* accountID= */ null,
        /* placementID= */ 12345L,
      )

    assertThat(adError).isNotNull()
    assertThat(adError?.code).isEqualTo(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(adError?.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun validateInMobiAdLoadParams_invalidPlacementID_returnsAdError() {
    val adError =
      InMobiAdapterUtils.validateInMobiAdLoadParams(
        /* accountID= */ "1234567890",
        /* placementID= */ -12345L,
      )

    assertThat(adError).isNotNull()
    assertThat(adError?.code).isEqualTo(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
    assertThat(adError?.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun validateInMobiAdLoadParams_validAccountIDAndPlacementID_returnsNull() {
    val adError =
      InMobiAdapterUtils.validateInMobiAdLoadParams(
        /* accountID= */ "1234567890",
        /* placementID= */ 12345L,
      )

    assertThat(adError).isNull()
  }

  @Test
  fun getMediationErrorCode_mapsAllStatusCodesCorrectly() {
    val statusMap =
      mapOf(
        InMobiAdRequestStatus.StatusCode.NO_ERROR to 0,
        InMobiAdRequestStatus.StatusCode.NETWORK_UNREACHABLE to 1,
        InMobiAdRequestStatus.StatusCode.NO_FILL to 2,
        InMobiAdRequestStatus.StatusCode.REQUEST_INVALID to 3,
        InMobiAdRequestStatus.StatusCode.REQUEST_PENDING to 4,
        InMobiAdRequestStatus.StatusCode.REQUEST_TIMED_OUT to 5,
        InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR to 6,
        InMobiAdRequestStatus.StatusCode.SERVER_ERROR to 7,
        InMobiAdRequestStatus.StatusCode.AD_ACTIVE to 8,
        InMobiAdRequestStatus.StatusCode.EARLY_REFRESH_REQUEST to 9,
        InMobiAdRequestStatus.StatusCode.AD_NO_LONGER_AVAILABLE to 10,
        InMobiAdRequestStatus.StatusCode.MISSING_REQUIRED_DEPENDENCIES to 11,
        InMobiAdRequestStatus.StatusCode.REPETITIVE_LOAD to 12,
        InMobiAdRequestStatus.StatusCode.GDPR_COMPLIANCE_ENFORCED to 13,
        InMobiAdRequestStatus.StatusCode.GET_SIGNALS_CALLED_WHILE_LOADING to 14,
        InMobiAdRequestStatus.StatusCode.LOAD_WITH_RESPONSE_CALLED_WHILE_LOADING to 15,
        InMobiAdRequestStatus.StatusCode.INVALID_RESPONSE_IN_LOAD to 16,
        InMobiAdRequestStatus.StatusCode.MONETIZATION_DISABLED to 17,
        InMobiAdRequestStatus.StatusCode.CALLED_FROM_WRONG_THREAD to 18,
        InMobiAdRequestStatus.StatusCode.CONFIGURATION_ERROR to 19,
        InMobiAdRequestStatus.StatusCode.LOW_MEMORY to 20,
      )

    for ((statusCode, expectedErrorCode) in statusMap) {
      val status = InMobiAdRequestStatus(statusCode)
      val errorCode = InMobiAdapterUtils.getMediationErrorCode(status)
      assertThat(errorCode).isEqualTo(expectedErrorCode)
    }
  }

  @Test
  fun findClosestBannerSize_passesPotentialSizesToMediationUtils() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val adSize = AdSize(320, 50)
    val mediationUtils = mock<MediationUtilsWrapper>()
    val expectedClosestSize = AdSize(320, 50)
    whenever(mediationUtils.findClosestSize(any(), any(), any())).thenReturn(expectedClosestSize)

    val closestSize = InMobiAdapterUtils.findClosestBannerSize(context, adSize, mediationUtils)

    val potentialsCaptor = argumentCaptor<ArrayList<AdSize>>()
    verify(mediationUtils).findClosestSize(eq(context), eq(adSize), potentialsCaptor.capture())
    assertThat(potentialsCaptor.firstValue)
      .containsExactly(AdSize(320, 50), AdSize(300, 250), AdSize(728, 90))
      .inOrder()
    assertThat(closestSize).isEqualTo(expectedClosestSize)
  }

  @Test
  fun configureGlobalTargeting_withAllTargetingParameters_setsTargetingOnInMobiSdk() {
    val extras =
      Bundle().apply {
        putString(InMobiNetworkKeys.AREA_CODE, "123")
        putString(InMobiNetworkKeys.AGE, "25")
        putString(InMobiNetworkKeys.POSTAL_CODE, "94043")
        putString(InMobiNetworkKeys.LANGUAGE, "en")
        putString(InMobiNetworkKeys.CITY, "Mountain View")
        putString(InMobiNetworkKeys.STATE, "CA")
        putString(InMobiNetworkKeys.COUNTRY, "USA")
        putString(InMobiNetworkKeys.AGE_GROUP, InMobiNetworkValues.BETWEEN_25_AND_29)
        putString(InMobiNetworkKeys.EDUCATION, InMobiNetworkValues.EDUCATION_COLLEGEORGRADUATE)
        putString(InMobiNetworkKeys.LOGLEVEL, InMobiNetworkValues.LOGLEVEL_DEBUG)
        putString(InMobiNetworkKeys.INTERESTS, "tech,gaming")
      }

    mockStatic(InMobiSdk::class.java).use { mockedInMobiSdk ->
      InMobiAdapterUtils.configureGlobalTargeting(extras)

      mockedInMobiSdk.verify { InMobiSdk.setAreaCode("123") }
      mockedInMobiSdk.verify { InMobiSdk.setAge(25) }
      mockedInMobiSdk.verify { InMobiSdk.setPostalCode("94043") }
      mockedInMobiSdk.verify { InMobiSdk.setLanguage("en") }
      mockedInMobiSdk.verify {
        InMobiSdk.setLocationWithCityStateCountry("Mountain View", "CA", "USA")
      }
      mockedInMobiSdk.verify { InMobiSdk.setAgeGroup(InMobiSdk.AgeGroup.BETWEEN_25_AND_29) }
      mockedInMobiSdk.verify { InMobiSdk.setEducation(InMobiSdk.Education.COLLEGE_OR_GRADUATE) }
      mockedInMobiSdk.verify { InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG) }
      mockedInMobiSdk.verify { InMobiSdk.setInterests("tech,gaming") }
    }
  }

  @Test
  fun configureGlobalTargeting_withNullExtras_doesNotInvokeInMobiSdkTargeting() {
    mockStatic(InMobiSdk::class.java).use { mockedInMobiSdk ->
      InMobiAdapterUtils.configureGlobalTargeting(null)

      mockedInMobiSdk.verifyNoInteractions()
    }
  }

  @Test
  fun configureGlobalTargeting_withInvalidAge_doesNotSetAgeOnInMobiSdk() {
    val extras = Bundle().apply { putString(InMobiNetworkKeys.AGE, "invalid_age") }

    mockStatic(InMobiSdk::class.java).use { mockedInMobiSdk ->
      InMobiAdapterUtils.configureGlobalTargeting(extras)

      mockedInMobiSdk.verify({ InMobiSdk.setAge(any()) }, never())
    }
  }

  private fun setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(
    coppa: Int,
    underAgeConsent: Int,
    ageRestrictedTreatment: AgeRestrictedTreatment = AgeRestrictedTreatment.UNSPECIFIED,
  ) {
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForChildDirectedTreatment(coppa)
        .setTagForUnderAgeOfConsent(underAgeConsent)
        .setAgeRestrictedTreatment(ageRestrictedTreatment)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
  }

  private fun setupMobiNativeAdWrapper(): Unit {
    whenever(inMobiNativeWrapper.adCtaText) doReturn ("SomeCtaText")
    whenever(inMobiNativeWrapper.adDescription) doReturn ("AdDescription")
    whenever(inMobiNativeWrapper.adIconUrl) doReturn ("http://www.example.com/docs/resource1.html")
    whenever(inMobiNativeWrapper.adTitle) doReturn ("adTitle")
  }
}
