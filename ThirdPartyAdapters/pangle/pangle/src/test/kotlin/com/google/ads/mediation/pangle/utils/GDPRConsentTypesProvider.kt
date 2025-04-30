package com.google.ads.mediation.pangle.utils

import com.bytedance.sdk.openadsdk.api.PAGConstant
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

class GDPRConsentTypesProvider : TestParameterValuesProvider() {
  override fun provideValues(context: Context): List<Int> =
    listOf(
      PAGConstant.PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_DEFAULT,
      PAGConstant.PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_NO_CONSENT,
      PAGConstant.PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_CONSENT,
    )
}
