package com.google.ads.mediation.pangle.utils

import com.google.android.gms.ads.AdError
import org.mockito.ArgumentMatcher

class AdErrorMatcher(val expected: AdError) : ArgumentMatcher<AdError> {

  override fun matches(actual: AdError): Boolean {
    return actual.getCode() == expected.getCode()
  }
}
