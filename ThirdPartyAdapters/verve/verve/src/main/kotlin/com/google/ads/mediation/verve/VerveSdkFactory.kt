// Copyright 2025 Google LLC
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

package com.google.ads.mediation.verve

import android.content.Context
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd

/**
 * Wrapper singleton to enable mocking of HyBid different ad formats for unit testing.
 *
 * **Note:** It is used as a layer between the Verve Adapter's and the HyBid SDK. This class is
 * required instead of calling the HyBid SDK methods directly.
 */
object VerveSdkFactory {
  /** Delegate used on unit tests to help mock calls to create [FiveAd] formats. */
  internal var delegate: SdkFactory =
    object : SdkFactory {
      override fun createHyBidInterstitialAd(
        context: Context,
        listener: HyBidInterstitialAd.Listener,
      ): HyBidInterstitialAd =
        // Using this constructor since older ones require an Activity, which shouldn't be used
        // when loading an Interstitial. During SDK bidding, zoneId should be irrelevant.
        HyBidInterstitialAd(context, /* zoneId= */ "", listener)
    }
}

/** Declares the methods that will invoke the HyBid SDK */
interface SdkFactory {
  fun createHyBidInterstitialAd(
    context: Context,
    listener: HyBidInterstitialAd.Listener,
  ): HyBidInterstitialAd
}
