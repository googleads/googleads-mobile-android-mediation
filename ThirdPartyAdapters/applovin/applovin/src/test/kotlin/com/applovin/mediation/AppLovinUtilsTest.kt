// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.applovin.mediation

import com.google.ads.mediation.applovin.AppLovinMediationAdapter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Unit tests for [AppLovinUtils]. */
@RunWith(JUnit4::class)
class AppLovinUtilsTest {

  @Test
  fun anyErrorSendTo_getAdError_returnsAppLovinSdkErrorDomain() {
    var adError = AppLovinUtils.getAdError(0)
    assertThat(adError.domain).isEqualTo(AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN)
    adError = AppLovinUtils.getAdError(1)
    assertThat(adError.domain).isEqualTo(AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN)
    adError = AppLovinUtils.getAdError(2)
    assertThat(adError.domain).isEqualTo(AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN)
  }
}
