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

import android.view.ViewGroup
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

      override fun onDestroy() {
        instance?.onDestroy()
      }
    }
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

  fun onDestroy()
}
