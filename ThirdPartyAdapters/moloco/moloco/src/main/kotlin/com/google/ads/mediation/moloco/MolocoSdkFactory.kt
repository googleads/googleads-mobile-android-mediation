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

package com.google.ads.mediation.moloco

import android.content.Context
import com.moloco.sdk.publisher.BannerAdSize
import com.moloco.sdk.publisher.CreateBannerCallback
import com.moloco.sdk.publisher.CreateInterstitialAdCallback
import com.moloco.sdk.publisher.CreateNativeAdCallback
import com.moloco.sdk.publisher.CreateRewardedInterstitialAdCallback
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoBidTokenListener

/**
 * Factory singleton to enable mocking of Moloco ad creation and bid token calls for unit testing.
 *
 * **Note:** It is used as a layer between the Moloco Adapter and the Moloco SDK. It is required to
 * use this class instead of calling the Moloco SDK ad creation methods directly.
 */
object MolocoSdkFactory {
  /** Delegate used in unit tests to help mock calls to create Moloco ad formats and bid tokens. */
  internal var delegate: SdkFactory =
    object : SdkFactory {
      override fun getBidToken(
        mediationInfo: MediationInfo,
        context: Context,
        listener: MolocoBidTokenListener,
      ) {
        Moloco.getBidToken(mediationInfo, context, listener)
      }

      override fun createBanner(
        mediationInfo: MediationInfo,
        adUnitId: String,
        size: BannerAdSize,
        watermark: String,
        callback: CreateBannerCallback,
      ) {
        Moloco.createMolocoBanner(
          mediationInfo = mediationInfo,
          adUnitId = adUnitId,
          size = size,
          watermarkString = watermark,
          callback = callback,
        )
      }

      override fun createInterstitial(
        mediationInfo: MediationInfo,
        adUnitId: String,
        watermark: String,
        callback: CreateInterstitialAdCallback,
      ) {
        Moloco.createInterstitial(
          mediationInfo = mediationInfo,
          adUnitId = adUnitId,
          watermarkString = watermark,
          callback = callback,
        )
      }

      override fun createRewarded(
        mediationInfo: MediationInfo,
        adUnitId: String,
        watermark: String,
        callback: CreateRewardedInterstitialAdCallback,
      ) {
        Moloco.createRewardedInterstitial(
          mediationInfo = mediationInfo,
          adUnitId = adUnitId,
          watermarkString = watermark,
          callback = callback,
        )
      }

      override fun createNativeAd(
        mediationInfo: MediationInfo,
        adUnitId: String,
        watermark: String,
        callback: CreateNativeAdCallback,
      ) {
        Moloco.createNativeAd(
          mediationInfo = mediationInfo,
          adUnitId = adUnitId,
          watermarkString = watermark,
          callback = callback,
        )
      }
    }
}

/** Declares the methods that will invoke the Moloco SDK factory creation methods. */
interface SdkFactory {
  fun getBidToken(mediationInfo: MediationInfo, context: Context, listener: MolocoBidTokenListener)

  fun createBanner(
    mediationInfo: MediationInfo,
    adUnitId: String,
    size: BannerAdSize,
    watermark: String,
    callback: CreateBannerCallback,
  )

  fun createInterstitial(
    mediationInfo: MediationInfo,
    adUnitId: String,
    watermark: String,
    callback: CreateInterstitialAdCallback,
  )

  fun createRewarded(
    mediationInfo: MediationInfo,
    adUnitId: String,
    watermark: String,
    callback: CreateRewardedInterstitialAdCallback,
  )

  fun createNativeAd(
    mediationInfo: MediationInfo,
    adUnitId: String,
    watermark: String,
    callback: CreateNativeAdCallback,
  )
}
