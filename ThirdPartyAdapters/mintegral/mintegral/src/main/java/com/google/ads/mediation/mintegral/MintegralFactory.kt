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

package com.google.ads.mediation.mintegral

import android.content.Context
import android.view.ViewGroup
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler
import com.mbridge.msdk.newinterstitial.out.MBNewInterstitialHandler
import com.mbridge.msdk.newinterstitial.out.NewInterstitialWithCodeListener
import com.mbridge.msdk.out.MBBannerView
import com.mbridge.msdk.out.MBSplashHandler
import com.mbridge.msdk.out.MBSplashLoadWithCodeListener
import com.mbridge.msdk.out.MBSplashShowListener
import org.json.JSONObject

/**
 * Wrapper singleton to enable mocking of Mintegral's different ad formats for unit testing.
 *
 * **Note:** It is used as a layer between the Mintegral Adapter's and the Mintegral SDK. It is
 * required to use this class instead of calling the Mintegral SDK methods directly.
 */
object MintegralFactory {

  @JvmStatic
  fun createSplashAdWrapper() =
    object : MintegralSplashAdWrapper {
      private var instance: MBSplashHandler? = null

      override fun createAd(placementId: String, adUnitId: String) {
        instance = MBSplashHandler(placementId, adUnitId, true, 5)
      }

      override fun setSplashLoadListener(listener: MBSplashLoadWithCodeListener) {
        instance?.setSplashLoadListener(listener)
      }

      override fun setSplashShowListener(listener: MBSplashShowListener) {
        instance?.setSplashShowListener(listener)
      }

      override fun setExtraInfo(jsonObject: JSONObject) {
        instance?.setExtraInfo(jsonObject)
      }

      override fun preLoad() {
        instance?.preLoad()
      }

      override fun preLoadByToken(token: String) {
        instance?.preLoadByToken(token)
      }

      override fun show(group: ViewGroup) {
        instance?.show(group)
      }

      override fun show(group: ViewGroup, bidToken: String) {
        instance?.show(group, bidToken)
      }

      override fun onDestroy() {
        instance?.onDestroy()
      }
    }

  @JvmStatic
  fun createInterstitialHandler() =
    object : MintegralNewInterstitialAdWrapper {
      private var instance: MBNewInterstitialHandler? = null

      override fun createAd(context: Context, placementId: String, adUnitId: String) {
        instance = MBNewInterstitialHandler(context, placementId, adUnitId)
      }

      override fun setInterstitialVideoListener(listener: NewInterstitialWithCodeListener) {
        instance?.setInterstitialVideoListener(listener)
      }

      override fun load() {
        instance?.load()
      }

      override fun playVideoMute(muteConstant: Int) {
        instance?.playVideoMute(muteConstant)
      }

      override fun show() {
        instance?.show()
      }
    }

  @JvmStatic
  fun createBidInterstitialHandler() =
    object : MintegralBidNewInterstitialAdWrapper {
      private var instance: MBBidNewInterstitialHandler? = null

      override fun createAd(context: Context, placementId: String, adUnitId: String) {
        instance = MBBidNewInterstitialHandler(context, placementId, adUnitId)
      }

      override fun setExtraInfo(jsonObject: JSONObject) {
        instance?.setExtraInfo(jsonObject)
      }

      override fun setInterstitialVideoListener(listener: NewInterstitialWithCodeListener) {
        instance?.setInterstitialVideoListener(listener)
      }

      override fun loadFromBid(bidToken: String) {
        instance?.loadFromBid(bidToken)
      }

      override fun playVideoMute(muteConstant: Int) {
        instance?.playVideoMute(muteConstant)
      }

      override fun showFromBid() {
        instance?.showFromBid()
      }
    }

  @JvmStatic fun createMBBannerView(context: Context) = MBBannerView(context)
}

/** Declares the methods that will invoke the [MBSplashHandler] methods */
interface MintegralSplashAdWrapper {
  fun createAd(placementId: String, adUnitId: String)

  fun setSplashLoadListener(listener: MBSplashLoadWithCodeListener)

  fun setSplashShowListener(listener: MBSplashShowListener)

  fun setExtraInfo(jsonObject: JSONObject)

  fun preLoad()

  fun preLoadByToken(token: String)

  fun show(group: ViewGroup)

  fun show(group: ViewGroup, bidToken: String)

  fun onDestroy()
}

interface MintegralNewInterstitialAdWrapper {
  fun createAd(context: Context, placementId: String, adUnitId: String)

  fun setInterstitialVideoListener(listener: NewInterstitialWithCodeListener)

  fun load()

  fun playVideoMute(muteConstant: Int)

  fun show()
}

interface MintegralBidNewInterstitialAdWrapper {
  fun createAd(context: Context, placementId: String, adUnitId: String)

  fun setExtraInfo(jsonObject: JSONObject)

  fun setInterstitialVideoListener(listener: NewInterstitialWithCodeListener)

  fun loadFromBid(bidToken: String)

  fun playVideoMute(muteConstant: Int)

  fun showFromBid()
}
