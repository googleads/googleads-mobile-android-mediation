// Copyright 2023 Google LLC
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
