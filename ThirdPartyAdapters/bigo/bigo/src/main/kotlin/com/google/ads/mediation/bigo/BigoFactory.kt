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

package com.google.ads.mediation.bigo

import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.InterstitialAd
import sg.bigo.ads.api.InterstitialAdLoader
import sg.bigo.ads.api.InterstitialAdRequest
import sg.bigo.ads.api.RewardVideoAd
import sg.bigo.ads.api.RewardVideoAdLoader
import sg.bigo.ads.api.RewardVideoAdRequest
import sg.bigo.ads.api.SplashAd
import sg.bigo.ads.api.SplashAdLoader
import sg.bigo.ads.api.SplashAdRequest

/**
 * Wrapper singleton to enable mocking of Bigo different ad formats for unit testing.
 *
 * **Note:** It is used as a layer between the Bigo Adapter's and the Bigo SDK. It is required to
 * use this class instead of calling the Bigo SDK methods directly.
 */
object BigoFactory {
  /** Delegate used on unit tests to help mock calls to create Bigo formats. */
  internal var delegate: SdkFactory =
    object : SdkFactory {
      override fun createInterstitialAdRequest(bidResponse: String, slotId: String) =
        InterstitialAdRequest.Builder().withBid(bidResponse).withSlotId(slotId).build()

      override fun createInterstitialAdLoader() =
        object : BigoInterstitialAdLoaderWrapper {
          private var instance: InterstitialAdLoader? = null

          override fun initializeAdLoader(loadListener: AdLoadListener<InterstitialAd>) {
            instance = InterstitialAdLoader.Builder().withAdLoadListener(loadListener).build()
          }

          override fun loadAd(adRequest: InterstitialAdRequest) {
            instance?.loadAd(adRequest)
          }
        }

      override fun createRewardVideoAdRequest(bidResponse: String, slotId: String) =
        RewardVideoAdRequest.Builder().withBid(bidResponse).withSlotId(slotId).build()

      override fun createRewardVideoAdLoader() =
        object : BigoRewardVideoAdLoaderWrapper {
          private var instance: RewardVideoAdLoader? = null

          override fun initializeAdLoader(loadListener: AdLoadListener<RewardVideoAd>) {
            instance = RewardVideoAdLoader.Builder().withAdLoadListener(loadListener).build()
          }

          override fun loadAd(adRequest: RewardVideoAdRequest) {
            instance?.loadAd(adRequest)
          }
        }

      override fun createSplashAdRequest(bidResponse: String, slotId: String) =
        SplashAdRequest.Builder().withBid(bidResponse).withSlotId(slotId).build()

      override fun createSplashAdLoader() =
        object : BigoSplashAdLoaderWrapper {
          private var instance: SplashAdLoader? = null

          override fun initializeAdLoader(loadListener: AdLoadListener<SplashAd>) {
            instance = SplashAdLoader.Builder().withAdLoadListener(loadListener).build()
          }

          override fun loadAd(adRequest: SplashAdRequest) {
            instance?.loadAd(adRequest)
          }
        }
    }
}

/** Declares the methods that will invoke the Bigo SDK */
interface SdkFactory {
  fun createInterstitialAdRequest(bidResponse: String, slotId: String): InterstitialAdRequest

  fun createRewardVideoAdRequest(bidResponse: String, slotId: String): RewardVideoAdRequest

  fun createSplashAdRequest(bidResponse: String, slotId: String): SplashAdRequest

  fun createInterstitialAdLoader(): BigoInterstitialAdLoaderWrapper

  fun createRewardVideoAdLoader(): BigoRewardVideoAdLoaderWrapper

  fun createSplashAdLoader(): BigoSplashAdLoaderWrapper
}

/**
 * Declares the methods that will invoke the [InterstitialAdLoader] methods
 *
 * This wrapper is needed to enable mocking of AdLoader operations and use it for unit testing.
 */
interface BigoInterstitialAdLoaderWrapper {
  fun initializeAdLoader(loadListener: AdLoadListener<InterstitialAd>)

  fun loadAd(adRequest: InterstitialAdRequest)
}

/**
 * Declares the methods that will invoke the [RewardVideoAdLoader] methods
 *
 * This wrapper is needed to enable mocking of AdLoader operations and use it for unit testing.
 */
interface BigoRewardVideoAdLoaderWrapper {
  fun initializeAdLoader(loadListener: AdLoadListener<RewardVideoAd>)

  fun loadAd(adRequest: RewardVideoAdRequest)
}

/**
 * Declares the methods that will invoke the [SplashAdLoader] methods
 *
 * This wrapper is needed to enable mocking of AdLoader operations and use it for unit testing.
 */
interface BigoSplashAdLoaderWrapper {
  fun initializeAdLoader(loadListener: AdLoadListener<SplashAd>)

  fun loadAd(adRequest: SplashAdRequest)
}
