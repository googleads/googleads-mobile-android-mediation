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
import android.util.Log
import com.google.android.gms.ads.AdSize
import com.vungle.ads.BidTokenCallback
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleMediationLogger

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

      override fun getBiddingToken(context: Context, callback: BidTokenCallback) =
        VungleAds.getBiddingToken(context, callback)

      override fun getSdkVersion(): String = VungleAds.getSdkVersion()

      override fun init(
        context: Context,
        appId: String,
        initializationListener: InitializationListener,
      ): Unit = VungleAds.init(context, appId, initializationListener)

      override fun isInitialized(): Boolean = VungleAds.isInitialized()
    }

  @JvmStatic
  fun logCustomSizeForBannerPlacement(
    bannerAdView: VungleBannerView,
    adapterAdFormat: String,
    placementId: String,
    adSize: AdSize,
  ) {
    if (!VungleAds.isInline(placementId)
      && !(adSize.width == AdSize.BANNER.width && adSize.height == AdSize.BANNER.height)
      && !(adSize.width == AdSize.MEDIUM_RECTANGLE.width && adSize.height == AdSize.MEDIUM_RECTANGLE.height)
      && !(adSize.width == AdSize.LEADERBOARD.width && adSize.height == AdSize.LEADERBOARD.height)) {
      bannerAdView.adapterAdFormat = adapterAdFormat
      val customSizeMismatchMessage = "CustomBannerSizeMismatch:w-${adSize.width}|h-${adSize.height}"
      VungleMediationLogger.logError(bannerAdView, customSizeMismatchMessage)
      Log.e(VungleMediationAdapter.TAG, "Please use a Liftoff inline placement ID in order to use custom size banner: placementId=$placementId adSize=$adSize")
    }
  }
}

/** Declares the methods that will invoke the Liftoff Monetize SDK */
interface SdkWrapper {
  fun getBiddingToken(context: Context, callback: BidTokenCallback)

  fun getSdkVersion(): String

  fun init(context: Context, appId: String, initializationListener: InitializationListener)

  fun isInitialized(): Boolean
}
