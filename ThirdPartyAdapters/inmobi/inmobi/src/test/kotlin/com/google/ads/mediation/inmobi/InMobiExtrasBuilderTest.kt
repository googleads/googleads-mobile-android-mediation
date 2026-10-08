package com.google.ads.mediation.inmobi

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class InMobiExtrasBuilderTest(
  private val mediationExtras: Map<String, String>?,
  private val protocol: String,
) {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    val requestConfig =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfig)
  }

  @Test
  fun buildInMobiExtras_returnsInMobiExtrasWithCorrectProtocol() {
    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.THIRD_PARTY_KEY]).isEqualTo(protocol)
  }

  @Test
  fun buildInMobiExtras_returnsInMobiExtrasWithCorrectMobileAdsVersion() {
    val mobileAdsVersion = MobileAds.getVersion().toString()

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.THIRD_PARTY_VERSION])
      .isEqualTo(mobileAdsVersion)
  }

  // region COPPA Tests

  @Test
  fun buildInMobiExtras_whenCoppaTrue_returnsInMobiExtrasWithCoppaTrue() {
    // when coppa value is set...
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    // ...should set coppa as '1' in the inMobiExtras Map
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isEqualTo("1")
  }

  @Test
  fun buildInMobiExtras_whenCoppaFalse_returnsInMobiExtrasWithCoppaFalse() {
    // when coppa value is not set...
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    // ...should set coppa as '0' in the inMobiExtras Map
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isEqualTo("0")
  }

  @Test
  fun buildInMobiExtras_whenUnderagedTrue_returnsInMobiExtrasWithCoppaTrue() {
    // when coppa value is set...
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    // ...should set coppa as '1' in the inMobiExtras Map
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isEqualTo("1")
  }

  @Test
  fun buildInMobiExtras_whenUnderagedFalse_returnsInMobiExtrasWithCoppaFalse() {
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isEqualTo("0")
  }

  @Test
  fun buildInMobiExtras_whenAgeRestrictedTreatmentChild_returnsInMobiExtrasWithCoppaTrue() {
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isEqualTo("1")
  }

  @Test
  fun buildInMobiExtras_whenAgeRestrictedTreatmentTeen_returnsInMobiExtrasWithCoppaUnknown() {
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isNull()
  }

  @Test
  fun buildInMobiExtras_whenCoppaNotSet_returnsInMobiExtrasWithCoppaUnknown() {
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isNull()
  }

  @Test
  fun buildInMobiExtras_whenUnderageNotSet_returnsInMobiExtrasWithCoppaUnknown() {
    val requestConfiguration =
      MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    assertThat(inMobiExtras.parameterMap[InMobiAdapterUtils.COPPA]).isNull()
  }

  // endregion

  @Test
  fun buildInMobiExtras_returnsInMobiExtrasMapPopulatedWithMediationExtras() {
    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.parameterMap).isNotEmpty()
    verifyInMobiExtrasParameterMap(mediationExtras, inMobiExtras)
  }

  @Test
  fun buildInMobiExtras_alwaysReturnAnEmptyKeywordsString() {
    val inMobiExtras = InMobiExtrasBuilder.build(context, mapToBundle(mediationExtras), protocol)

    assertThat(inMobiExtras).isNotNull()
    assertThat(inMobiExtras.keywords).isEmpty()
  }

  private fun mapToBundle(map: Map<String, String>?) =
    Bundle().apply { map?.entries?.forEach { this.putString(it.key, it.value) } }

  private fun verifyInMobiExtrasParameterMap(
    mediationExtras: Map<String, String>?,
    inMobiExtras: InMobiExtras,
  ) {
    if (!mediationExtras.isNullOrEmpty()) {
      assertThat(inMobiExtras.parameterMap).containsAtLeastEntriesIn(mediationExtras)
    }
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "mediationExtras={0}, protocol={1}")
    fun params() =
      listOf(
        arrayOf(
          mapOf(InMobiNetworkKeys.AGE to "25", InMobiNetworkKeys.AREA_CODE to "12345"),
          InMobiAdapterUtils.PROTOCOL_RTB,
        ),
        arrayOf(
          mapOf(InMobiNetworkKeys.CITY to "MTV", InMobiNetworkKeys.LANGUAGE to "Japanese"),
          InMobiAdapterUtils.PROTOCOL_WATERFALL,
        ),
        arrayOf(null, InMobiAdapterUtils.PROTOCOL_WATERFALL),
        arrayOf(null, InMobiAdapterUtils.PROTOCOL_RTB),
      )
  }
}
