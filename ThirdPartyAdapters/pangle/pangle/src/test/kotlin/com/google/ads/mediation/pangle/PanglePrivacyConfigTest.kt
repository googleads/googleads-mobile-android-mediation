package com.google.ads.mediation.pangle

import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_CHILD
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_NON_CHILD
import com.google.ads.mediation.pangle.utils.ChildDirectedTypesProvider
import com.google.ads.mediation.pangle.utils.GmaChildDirectedTagsProvider
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector

/** Unit tests class for [PanglePrivacyConfig]. */
@RunWith(RobolectricTestParameterInjector::class)
class PanglePrivacyConfigTest {

  private lateinit var panglePrivacyConfig: PanglePrivacyConfig

  private val pangleSdkWrapper: PangleSdkWrapper = mock()

  @Before
  fun setUp() {
    panglePrivacyConfig = PanglePrivacyConfig(pangleSdkWrapper)
    panglePrivacyConfig.setCoppa(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
  }

  /** This method tests that the getCoppa method returns the value of the private coppa field */
  @Test
  fun getCoppa_returnsTheDefaultValue() {
    // Given the starting value for Coppa (UNSPECIFIED)
    // Calling getCoppa returns the Default PAGChildDirectedType value.
    assertThat(PanglePrivacyConfig.getCoppa())
      .isEqualTo(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT)
  }

  /**
   * This tests that calling setCoppa with the RequestConfiguration values return the equivalent
   * value of the [PAGChildDirectedType] (PAG_CHILD_DIRECTED_TYPE_CHILD)
   */
  @Test
  fun setCoppa_correctlyUpdatesCoppaToTrue() {
    // Given the starting Default value.
    // When we call setCoppa with True...
    panglePrivacyConfig.setCoppa(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)

    // ... we assert that the value for coppa is PAG_CHILD_DIRECTED_TYPE_CHILD.
    assertThat(PanglePrivacyConfig.getCoppa())
      .isEqualTo(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_CHILD)
  }

  /**
   * This tests that calling setCoppa with the RequestConfiguration values return the equivalent
   * value of the [PAGChildDirectedType] (PAG_CHILD_DIRECTED_TYPE_NON_CHILD)
   */
  @Test
  fun setCoppa_correctlyUpdatesCoppaToFalse() {
    // Given the starting Default value.
    // When we call setCoppa with False...
    panglePrivacyConfig.setCoppa(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)

    // ... we assert that the value for coppa is PAG_CHILD_DIRECTED_TYPE_NON_CHILD.
    assertThat(PanglePrivacyConfig.getCoppa())
      .isEqualTo(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_NON_CHILD)
  }

  /**
   * This tests that calling setCoppa with the RequestConfiguration values return the equivalent
   * value of the [PAGChildDirectedType] (PAG_CHILD_DIRECTED_TYPE_DEFAULT)
   */
  @Test
  fun setCoppa_correctlyUpdatesCoppaToDefault() {
    // Given the starting Default value.
    // When we call setCoppa with False and then to Unspecified...
    panglePrivacyConfig.setCoppa(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
    panglePrivacyConfig.setCoppa(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)

    // ... we assert that the value for coppa is PAG_CHILD_DIRECTED_TYPE_DEFAULT.
    assertThat(PanglePrivacyConfig.getCoppa())
      .isEqualTo(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT)
  }

  @Test
  fun setCoppaAsUnspecified_setsChildDirectedTypeAsDefaultOnPangleSdk(
  ) {
    // Mock that the Pangle SDK is initialized.
    whenever(pangleSdkWrapper.isInitSuccess()).thenReturn(true)

    panglePrivacyConfig.setCoppa(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)

    verify(pangleSdkWrapper).setChildDirected(PAG_CHILD_DIRECTED_TYPE_DEFAULT)
  }

  @Test
  fun setCoppaAsFalse_setsChildDirectedTypeAsNonChildOnPangleSdk(
  ) {
    // Mock that the Pangle SDK is initialized.
    whenever(pangleSdkWrapper.isInitSuccess()).thenReturn(true)

    panglePrivacyConfig.setCoppa(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)

    verify(pangleSdkWrapper).setChildDirected(PAG_CHILD_DIRECTED_TYPE_NON_CHILD)
  }

  @Test
  fun setCoppaAsTrue_setsChildDirectedTypeAsChildOnPangleSdk(
  ) {
    // Mock that the Pangle SDK is initialized.
    whenever(pangleSdkWrapper.isInitSuccess()).thenReturn(true)

    panglePrivacyConfig.setCoppa(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)

    verify(pangleSdkWrapper).setChildDirected(PAG_CHILD_DIRECTED_TYPE_CHILD)
  }

  @Test
  fun setCoppa_ifPangleSdkIsNotInitialized_doesNotSetChildDirectedTypeOnPangleSdk(
    @TestParameter(valuesProvider = GmaChildDirectedTagsProvider::class) gmaChildDirectedTag: Int
  ) {
    // Mock that the Pangle SDK isn't initialized.
    whenever(pangleSdkWrapper.isInitSuccess()).thenReturn(false)

    panglePrivacyConfig.setCoppa(gmaChildDirectedTag)

    verify(pangleSdkWrapper, never()).setChildDirected(any())
  }
}
