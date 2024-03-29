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

import android.content.Context
import com.five_corp.ad.FiveAd
import com.five_corp.ad.FiveAdConfig

/**
 * Wrapper singleton to enable mocking of [FiveAd] (Line) for unit testing.
 *
 * **Note:** It is used as a layer between the Line Adapter's and the FiveAd SDK. It is required to
 * use this class instead of calling the FiveAd SDK methods directly.
 */
object LineSdkWrapper {
  /** Delegate used on unit tests to help mock calls to the third party SDK. */
  internal var delegate =
    object : SdkWrapper {

      override fun getSdkVersion(): String = FiveAd.getSdkSemanticVersion()

      override fun isInitialized(): Boolean = FiveAd.isInitialized()

      override fun initialize(context: Context, configuration: FiveAdConfig) =
        FiveAd.initialize(context, configuration)
    }
}

/** Declares the methods that will invoke the FiveAd SDK */
interface SdkWrapper {
  fun getSdkVersion(): String

  fun isInitialized(): Boolean

  fun initialize(context: Context, configuration: FiveAdConfig)
}
