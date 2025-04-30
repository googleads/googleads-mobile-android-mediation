package com.google.ads.mediation.pangle.utils

import com.google.android.gms.ads.RequestConfiguration
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

class GmaChildDirectedTagsProvider : TestParameterValuesProvider() {
  override fun provideValues(context: Context): List<Int> =
    listOf(
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE,
    )
}
