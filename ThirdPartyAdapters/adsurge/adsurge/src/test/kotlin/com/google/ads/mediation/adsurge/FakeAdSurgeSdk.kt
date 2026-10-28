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

package com.google.ads.mediation.adsurge

import android.app.Activity
import android.content.Context
import android.view.View
import com.adsurge.adn.ads.AdConfig
import com.adsurge.adn.ads.AdSurgeAd
import com.adsurge.adn.ads.AdSurgeAdError
import com.adsurge.adn.ads.AdSurgeAdFormat
import com.adsurge.adn.ads.AdSurgeAdSize
import com.adsurge.adn.ads.AdSurgeLossReason
import com.adsurge.adn.ads.AdSurgePlacementType
import com.adsurge.adn.ads.AdSurgePriceStrategyType
import com.adsurge.adn.ads.banner.BannerAdListener
import com.adsurge.adn.ads.interstitial.IInterstitialAd
import com.adsurge.adn.ads.interstitial.InterstitialAdListener
import com.adsurge.adn.ads.rewarded.IRewardedAd
import com.adsurge.adn.ads.rewarded.RewardItem as AdSurgeRewardItem
import com.adsurge.adn.ads.rewarded.RewardedAdListener
import com.adsurge.adn.managers.AdSurgeAdSdk
import com.adsurge.adn.managers.OnStartListener

/** Fake implementation of [SdkWrapper] for unit testing without mocks. */
class FakeSdkWrapper(
  @JvmField var sdkVersion: String = "2.0.1",
  var bannerAdWrapper: AdSurgeBannerAdWrapper? = null,
  var interstitialAd: FakeInterstitialAd = FakeInterstitialAd(),
  var rewardedAd: FakeRewardedAd = FakeRewardedAd(),
) : SdkWrapper {

  var initializedContext: Context? = null
  var initializedAppId: String? = null
  var initCallCount: Int = 0
  var initError: AdSurgeAdError? = null

  var bidderToken: String = "test_bid_token"
  var bidderTokenError: AdSurgeAdError? = null

  var ageRestrictedUser: Boolean? = null
  var lastCreatedAdUnitId: String? = null

  override fun getSdkVersion(): String = sdkVersion

  override fun init(context: Context, appId: String, listener: OnStartListener) {
    initCallCount++
    initializedContext = context
    initializedAppId = appId
    initError?.let { listener.onStartFailed(it) } ?: listener.onStartComplete()
  }

  override fun getBidderToken(callback: AdSurgeAdSdk.BidderTokenCallback) {
    bidderTokenError?.let { callback.onFailure(it) } ?: callback.onSuccess(bidderToken)
  }

  override fun setAgeRestrictedUser(isAgeRestricted: Boolean) {
    ageRestrictedUser = isAgeRestricted
  }

  override fun createAdView(context: Context, adUnitId: String): AdSurgeBannerAdWrapper {
    lastCreatedAdUnitId = adUnitId
    return bannerAdWrapper
      ?: FakeAdSurgeBannerAdWrapper(View(context)).also { bannerAdWrapper = it }
  }

  override fun createInterstitialAd(context: Context, adUnitId: String): IInterstitialAd {
    lastCreatedAdUnitId = adUnitId
    return interstitialAd
  }

  override fun createRewardedAd(context: Context, adUnitId: String): IRewardedAd {
    lastCreatedAdUnitId = adUnitId
    return rewardedAd
  }
}

/** Fake implementation of [AdSurgeBannerAdWrapper]. */
class FakeAdSurgeBannerAdWrapper(private val view: View) : AdSurgeBannerAdWrapper {
  @JvmField var listener: BannerAdListener? = null
  var loadedConfig: AdConfig? = null

  override fun getView(): View = view

  override fun setListener(listener: BannerAdListener) {
    this.listener = listener
  }

  override fun loadAd(adConfig: AdConfig) {
    this.loadedConfig = adConfig
  }
}

/** Fake implementation of [IInterstitialAd]. */
class FakeInterstitialAd : IInterstitialAd {
  var loadedConfig: AdConfig? = null
  var listener: InterstitialAdListener? = null
  var isAdValid: Boolean = false
  var shownActivity: Activity? = null

  override fun loadAd(adConfig: AdConfig, listener: InterstitialAdListener) {
    this.loadedConfig = adConfig
    this.listener = listener
  }

  override fun isValid(): Boolean = isAdValid

  override fun showAd(activity: Activity) {
    shownActivity = activity
  }

  override fun showAd() {}

  override fun showAd(extras: MutableMap<String, Any>?) {}

  override fun showAd(activity: Activity?, extras: MutableMap<String, Any>?) {}
}

/** Fake implementation of [IRewardedAd]. */
class FakeRewardedAd : IRewardedAd {
  var loadedConfig: AdConfig? = null
  var listener: RewardedAdListener? = null
  var isAdValid: Boolean = false
  var shownActivity: Activity? = null

  override fun loadAd(adConfig: AdConfig, listener: RewardedAdListener) {
    this.loadedConfig = adConfig
    this.listener = listener
  }

  override fun isValid(): Boolean = isAdValid

  override fun showAd(activity: Activity) {
    shownActivity = activity
  }

  override fun showAd() {}

  override fun showAd(extras: MutableMap<String, Any>?) {}

  override fun showAd(activity: Activity?, extras: MutableMap<String, Any>?) {}
}

/** Fake implementation of [AdSurgeAd]. */
class FakeAdSurgeAd : AdSurgeAd() {
  override fun getFormat(): AdSurgeAdFormat = AdSurgeAdFormat.BANNER

  override fun getSize(): AdSurgeAdSize = AdSurgeAdSize.BANNER

  override fun getAdUnitId(): String = "test_ad_unit_id"

  override fun getRevenue(): Double = 0.0

  override fun getCreativeId(): String = "test_creative_id"

  override fun sendWinNotification(
    price: Double,
    secondPrice: Double?,
    bidderName: String?,
    placementType: AdSurgePlacementType?,
    priceStrategyType: AdSurgePriceStrategyType?,
  ) {}

  override fun sendLossNotification(
    winPrice: Double,
    bidderName: String?,
    lossReason: AdSurgeLossReason?,
    winnerId: String?,
    placementType: AdSurgePlacementType?,
    priceStrategyType: AdSurgePriceStrategyType?,
  ) {}
}

/** Fake implementation of [AdSurgeRewardItem]. */
class FakeRewardItem(private val rewardAmount: Int = 10, private val rewardType: String = "coins") :
  AdSurgeRewardItem {
  override fun getAmount(): Int = rewardAmount

  override fun getType(): String = rewardType
}
