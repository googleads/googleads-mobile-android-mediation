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

import android.app.Activity
import android.content.Context
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdInterstitial
import com.five_corp.ad.FiveAdVideoReward

/**
 * Wrapper singleton to enable mocking of [FiveAd] different ad formats for unit testing.
 *
 * **Note:** It is used as a layer between the Line Adapter's and the FiveAd SDK. It is required to
 * use this class instead of calling the FiveAd SDK methods directly.
 */
object LineSdkFactory {
  /** Delegate used on unit tests to help mock calls to create [FiveAd] formats. */
  internal var delegate: SdkFactory =
    object : SdkFactory {
      override fun createFiveAdConfig(appId: String): FiveAdConfig = FiveAdConfig(appId)

      override fun createFiveAdCustomLayout(
        context: Context,
        slotId: String,
        width: Int,
      ): FiveAdCustomLayout = FiveAdCustomLayout(context, slotId, width)

      override fun createFiveAdInterstitial(
        activity: Activity,
        slotId: String,
      ): FiveAdInterstitial = FiveAdInterstitial(activity, slotId)

      override fun createFiveVideoRewarded(activity: Activity, slotId: String): FiveAdVideoReward =
        FiveAdVideoReward(activity, slotId)
    }
}

/** Declares the methods that will invoke the [FiveAd] SDK */
interface SdkFactory {
  fun createFiveAdConfig(appId: String): FiveAdConfig

  fun createFiveAdCustomLayout(context: Context, slotId: String, width: Int): FiveAdCustomLayout

  fun createFiveAdInterstitial(activity: Activity, slotId: String): FiveAdInterstitial

  fun createFiveVideoRewarded(activity: Activity, slotId: String): FiveAdVideoReward
}
