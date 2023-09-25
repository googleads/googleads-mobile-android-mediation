package com.google.ads.mediation.adaptertestkit

import com.google.android.gms.ads.AdError
import org.mockito.ArgumentMatcher

class AdErrorMatcher(val expected: AdError) : ArgumentMatcher<AdError> {

  override fun matches(actual: AdError): Boolean {
    return actual.code == expected.code &&
      actual.message == expected.message &&
      actual.domain == expected.domain
  }
}
