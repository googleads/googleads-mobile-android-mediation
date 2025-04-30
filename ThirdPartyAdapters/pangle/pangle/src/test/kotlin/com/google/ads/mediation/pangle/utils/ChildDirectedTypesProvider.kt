package com.google.ads.mediation.pangle.utils

import com.bytedance.sdk.openadsdk.api.PAGConstant
import com.google.android.gms.ads.RequestConfiguration
import com.google.testing.junit.testparameterinjector.TestParameters
import com.google.testing.junit.testparameterinjector.TestParametersValuesProvider

/**
 * Provides the list of all possible (gmaChildDirectedTag, pangleChildDirectedType) pairs.
 *
 * gmaChildDirectedTag is child-directed tag defined by the GMA SDK.
 *
 * pangleChildDirectedType is child-directed type defined by the Pangle SDK.
 */
class ChildDirectedTypesProvider : TestParametersValuesProvider() {
  override fun provideValues(context: Context): List<TestParameters.TestParametersValues> =
    listOf(
      TestParameters.TestParametersValues.builder()
        .name("Child directed type is default")
        .addParameter(
          "gmaChildDirectedTag",
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        )
        .addParameter(
          "pangleChildDirectedType",
          PAGConstant.PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT,
        )
        .build(),
      TestParameters.TestParametersValues.builder()
        .name("Child directed type is non-child")
        .addParameter(
          "gmaChildDirectedTag",
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
        )
        .addParameter(
          "pangleChildDirectedType",
          PAGConstant.PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_NON_CHILD,
        )
        .build(),
      TestParameters.TestParametersValues.builder()
        .name("Child directed type is child")
        .addParameter(
          "gmaChildDirectedTag",
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE,
        )
        .addParameter(
          "pangleChildDirectedType",
          PAGConstant.PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_CHILD,
        )
        .build(),
    )
}
