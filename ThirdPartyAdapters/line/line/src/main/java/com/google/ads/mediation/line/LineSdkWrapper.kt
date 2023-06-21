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

package com.google.ads.mediation.line

import androidx.annotation.VisibleForTesting

/** Delegate used on unit tests to help mock calls to the third party SDK. */
@VisibleForTesting var lineSdkDelegate: SdkWrapper? = null

// TODO(b/286926646): Add Line Sdk classes to Adapter Documentation
/**
 * Wrapper singleton to enable mocking of [LineSDK] for unit testing.
 *
 * Note: It is used as a layer between the Line Adapter's and the Line SDK. It is required to use
 * this class instead of calling the Line SDK methods directly. More background:
 * http://yaqs/6706506443522048
 */
object LineSdkWrapper : SdkWrapper {
  // TODO (b/286457445): Add Version from thrid party SDK.
  override fun getSdkVersion() = lineSdkDelegate?.getSdkVersion() ?: "4.3.2.1"
}

/** Declares the methods that will invoke the third party SDK */
interface SdkWrapper {
  fun getSdkVersion(): String
}
