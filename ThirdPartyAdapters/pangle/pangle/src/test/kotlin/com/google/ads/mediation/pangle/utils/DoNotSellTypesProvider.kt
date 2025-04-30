package com.google.ads.mediation.pangle.utils

import com.bytedance.sdk.openadsdk.api.PAGConstant
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

class DoNotSellTypesProvider : TestParameterValuesProvider() {
  override fun provideValues(context: Context): List<Int> =
    listOf(
      PAGConstant.PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_DEFAULT,
      PAGConstant.PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_SELL,
      PAGConstant.PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_NOT_SELL,
    )
}
