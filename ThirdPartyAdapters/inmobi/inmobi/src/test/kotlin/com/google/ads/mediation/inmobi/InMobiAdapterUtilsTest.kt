package com.google.ads.mediation.inmobi

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import com.inmobi.sdk.InMobiSdk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
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

  private fun setCOPPAAndUnderAgeOnMobileAdsRequestConfiguration(coppa: Int, underAgeConsent: Int) {
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForChildDirectedTreatment(coppa)
        .setTagForUnderAgeOfConsent(underAgeConsent)
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
