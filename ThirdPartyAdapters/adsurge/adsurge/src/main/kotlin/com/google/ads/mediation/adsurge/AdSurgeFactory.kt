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

import android.content.Context
import android.view.View
import androidx.annotation.VisibleForTesting
import com.adsurge.adn.ads.AdConfig
import com.adsurge.adn.ads.AdSurgePrivacyConfiguration
import com.adsurge.adn.ads.banner.AdView
import com.adsurge.adn.ads.banner.BannerAdListener
import com.adsurge.adn.ads.interstitial.IInterstitialAd
import com.adsurge.adn.ads.interstitial.InterstitialAd
import com.adsurge.adn.ads.rewarded.IRewardedAd
import com.adsurge.adn.ads.rewarded.RewardedAd
import com.adsurge.adn.managers.AdSurgeAdSdk
import com.adsurge.adn.managers.AdSurgeAdSdkInitConfig
import com.adsurge.adn.managers.OnStartListener

/** Wrapper interface for [AdView] operations to enable Robolectric unit testing. */
interface AdSurgeBannerAdWrapper {
  fun getView(): View

  fun setListener(listener: BannerAdListener)

  fun loadAd(adConfig: AdConfig)
}

/** Default implementation of [AdSurgeBannerAdWrapper] that delegates to [AdView]. */
class DefaultAdSurgeBannerAdWrapper(private val adView: AdView) : AdSurgeBannerAdWrapper {
  override fun getView(): View = adView

  override fun setListener(listener: BannerAdListener) = adView.setListener(listener)

  override fun loadAd(adConfig: AdConfig) = adView.loadAd(adConfig)
}

/** Factory and SDK wrapper for AdSurge SDK to allow unit test mocking. */
object AdSurgeFactory {

  private val defaultDelegate: SdkWrapper =
    object : SdkWrapper {
      override fun getSdkVersion(): String = AdSurgeAdSdk.getSdkVersion()

      override fun init(context: Context, appId: String, listener: OnStartListener) {
        val config = AdSurgeAdSdkInitConfig.Builder().setContext(context).setAppId(appId).build()
        AdSurgeAdSdk.getInstance().init(config, listener)
      }

      override fun getBidderToken(callback: AdSurgeAdSdk.BidderTokenCallback) {
        AdSurgeAdSdk.getInstance().getBidderToken(callback)
      }

      override fun setAgeRestrictedUser(isAgeRestricted: Boolean) {
        AdSurgePrivacyConfiguration.setAgeRestrictedUser(isAgeRestricted)
      }

      override fun createAdView(context: Context, adUnitId: String): AdSurgeBannerAdWrapper =
        DefaultAdSurgeBannerAdWrapper(AdView(context, adUnitId))

      override fun createInterstitialAd(context: Context, adUnitId: String): IInterstitialAd =
        InterstitialAd(context, adUnitId)

      override fun createRewardedAd(context: Context, adUnitId: String): IRewardedAd =
        RewardedAd(context, adUnitId)
    }

  @VisibleForTesting internal var delegate: SdkWrapper = defaultDelegate

  @VisibleForTesting
  internal fun reset() {
    delegate = defaultDelegate
  }

  fun getSdkVersion(): String = delegate.getSdkVersion()

  fun init(context: Context, appId: String, listener: OnStartListener) {
    delegate.init(context, appId, listener)
  }

  fun getBidderToken(callback: AdSurgeAdSdk.BidderTokenCallback) {
    delegate.getBidderToken(callback)
  }

  fun setAgeRestrictedUser(isAgeRestricted: Boolean) {
    delegate.setAgeRestrictedUser(isAgeRestricted)
  }

  fun createAdView(context: Context, adUnitId: String): AdSurgeBannerAdWrapper =
    delegate.createAdView(context, adUnitId)

  fun createInterstitialAd(context: Context, adUnitId: String): IInterstitialAd =
    delegate.createInterstitialAd(context, adUnitId)

  fun createRewardedAd(context: Context, adUnitId: String): IRewardedAd =
    delegate.createRewardedAd(context, adUnitId)
}

/** Interface for wrapping AdSurge SDK invocations. */
interface SdkWrapper {
  fun getSdkVersion(): String

  fun init(context: Context, appId: String, listener: OnStartListener)

  fun getBidderToken(callback: AdSurgeAdSdk.BidderTokenCallback)

  fun setAgeRestrictedUser(isAgeRestricted: Boolean)

  fun createAdView(context: Context, adUnitId: String): AdSurgeBannerAdWrapper

  fun createInterstitialAd(context: Context, adUnitId: String): IInterstitialAd

  fun createRewardedAd(context: Context, adUnitId: String): IRewardedAd
}
