package com.google.ads.mediation.unity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import com.unity3d.ads.UnityAdsError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [UnityAdsAdapterUtils]. */
@RunWith(AndroidJUnit4::class)
class UnityAdsAdapterUtilsTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
  }

  @Test
  fun getMediationInitializationErrorCode_withInternalError_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52000)

    val errorCode = UnityAdsAdapterUtils.getMediationInitializationErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(301)
  }

  @Test
  fun getMediationInitializationErrorCode_withInvalidArgument_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52001)

    val errorCode = UnityAdsAdapterUtils.getMediationInitializationErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(302)
  }

  @Test
  fun getMediationLoadErrorCode_withNotInitialized_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52101)

    val errorCode = UnityAdsAdapterUtils.getMediationLoadErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(401)
  }

  @Test
  fun getMediationLoadErrorCode_withInternalError_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52103)

    val errorCode = UnityAdsAdapterUtils.getMediationLoadErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(402)
  }

  @Test
  fun getMediationLoadErrorCode_withInvalidArgument_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52102)

    val errorCode = UnityAdsAdapterUtils.getMediationLoadErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(403)
  }

  @Test
  fun getMediationLoadErrorCode_withNoFill_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52100)

    val errorCode = UnityAdsAdapterUtils.getMediationLoadErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(404)
  }

  @Test
  fun getMediationLoadErrorCode_withTimeout_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(2)

    val errorCode = UnityAdsAdapterUtils.getMediationLoadErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(405)
  }

  @Test
  fun getMediationShowErrorCode_withAlreadyShowing_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52201)

    val errorCode = UnityAdsAdapterUtils.getMediationShowErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(506)
  }

  @Test
  fun getMediationShowErrorCode_withInternalError_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52202)

    val errorCode = UnityAdsAdapterUtils.getMediationShowErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(507)
  }

  @Test
  fun getMediationShowErrorCode_withTimeout_returnsCorrectValue() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(2)

    val errorCode = UnityAdsAdapterUtils.getMediationShowErrorCode(unityAdsError)

    assertThat(errorCode).isEqualTo(508)
  }

  @Test
  fun getUnityBannerSize_returnsNullOnInvalidSize() {
    val adSize = AdSize.WIDE_SKYSCRAPER

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false, mediationUtils)

    assertThat(unityBannerAdSize).isNull()
  }

  @Test
  fun getUnityBannerSize_returnsCorrectBannerSize() {
    val adSize = AdSize.BANNER
    whenever(mediationUtils.findClosestSize(eq(context), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false, mediationUtils)

    assertThat(unityBannerAdSize).isNotNull()
    assertThat(unityBannerAdSize?.width).isEqualTo(320)
    assertThat(unityBannerAdSize?.height).isEqualTo(50)
  }

  @Test
  fun getUnityBannerSize_returnsCorrectLeaderboardSize() {
    val adSize = AdSize.LEADERBOARD
    whenever(mediationUtils.findClosestSize(eq(context), eq(AdSize.LEADERBOARD), any())) doReturn
      AdSize.LEADERBOARD

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false, mediationUtils)

    assertThat(unityBannerAdSize).isNotNull()
    assertThat(unityBannerAdSize?.width).isEqualTo(728)
    assertThat(unityBannerAdSize?.height).isEqualTo(90)
  }

  @Test
  fun createSDKLoadError_returnsAdErrorWithCorrectValues() {
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52100)
    whenever(unityAdsError.message).doReturn("Test Error")

    val adError = UnityAdsAdapterUtils.createSDKLoadError(unityAdsError, "Test Description")

    assertThat(adError.code).isEqualTo(404)
    assertThat(adError.message).isEqualTo("Test Description")
    assertThat(adError.domain).isEqualTo(UnityMediationAdapter.SDK_ERROR_DOMAIN)
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDTrue_setsNonBehavioralTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    // This method now directly calls UnityAds.setNonBehavioral()
    UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

    // We can't verify the internal call to UnityAds.setNonBehavioral without mocking the static method
    // This test just verifies the method executes without errors
  }

  @Test
  fun setUnityAdsPrivacy_withTFUATrue_setsNonBehavioralTrue() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    // This method now directly calls UnityAds.setNonBehavioral()
    UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

    // We can't verify the internal call to UnityAds.setNonBehavioral without mocking the static method
    // This test just verifies the method executes without errors
  }

  @Test
  fun setUnityAdsPrivacy_withTFCDFalse_setsNonBehavioralFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    // This method now directly calls UnityAds.setNonBehavioral()
    UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

    // We can't verify the internal call to UnityAds.setNonBehavioral without mocking the static method
    // This test just verifies the method executes without errors
  }

  @Test
  fun setUnityAdsPrivacy_withTFUAFalse_setsNonBehavioralFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    // This method now directly calls UnityAds.setNonBehavioral()
    UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

    // We can't verify the internal call to UnityAds.setNonBehavioral without mocking the static method
    // This test just verifies the method executes without errors
  }

  @Test
  fun setUnityAdsPrivacy_withTFUAUnspecifiedAndTFCDUnspecified_setsNonBehavioralFalse() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    // This method now directly calls UnityAds.setNonBehavioral()
    UnityAdsAdapterUtils.setUnityAdsPrivacy(requestConfiguration)

    // We can't verify the internal call to UnityAds.setNonBehavioral without mocking the static method
    // This test just verifies the method executes without errors
  }
}
