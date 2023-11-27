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

package com.google.ads.mediation.vungle

import android.content.Context
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleAds

/**
 * Wrapper singleton to enable mocking of [Liftoff Monetize] for unit testing.
 *
 * **Note:** It is used as a layer between the Liftoff Monetize Adapter and the Liftoff Monetize
 * SDK. It is required to use this class instead of calling the Liftoff Monetize SDK methods
 * directly.
 */
object VungleSdkWrapper {
  /** Delegate used on unit tests to help mock calls to the third party SDK. */
  @kotlin.jvm.JvmField
  var delegate =
    object : SdkWrapper {

      override fun getBiddingToken(context: Context): String? = VungleAds.getBiddingToken(context)

      override fun getSdkVersion(): String = VungleAds.getSdkVersion()

      override fun init(
        context: Context,
        appId: String,
        initializationListener: InitializationListener
      ): Unit = VungleAds.init(context, appId, initializationListener)

      override fun isInitialized(): Boolean = VungleAds.isInitialized()
    }
}

/** Declares the methods that will invoke the Liftoff Monetize SDK */
interface SdkWrapper {
  fun getBiddingToken(context: Context): String?

  fun getSdkVersion(): String

  fun init(context: Context, appId: String, initializationListener: InitializationListener)

  fun isInitialized(): Boolean
}
