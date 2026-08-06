// Copyright 2024 Google LLC
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

package com.google.ads.mediation.fyber

import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.NativeAdUnitController
import com.fyber.inneractive.sdk.external.NativeAdVideoContentController

object FyberFactory {
  internal var delegate: Factory =
    object : Factory {
      override fun createRewardedAdSpot(): InneractiveAdSpot =
        InneractiveAdSpotManager.get().createSpot()

      override fun createInneractiveFullscreenUnitController():
        InneractiveFullscreenUnitController = InneractiveFullscreenUnitController()

      override fun createNativeAdUnitController(): NativeAdUnitController = NativeAdUnitController()

      override fun createNativeAdVideoContentController(): NativeAdVideoContentController =
        NativeAdVideoContentController()
    }

  @JvmStatic fun createRewardedAdSpot() = delegate.createRewardedAdSpot()

  @JvmStatic
  fun createInneractiveFullscreenUnitController() =
    delegate.createInneractiveFullscreenUnitController()

  @JvmStatic fun createNativeAdUnitController() = delegate.createNativeAdUnitController()

  @JvmStatic
  fun createNativeAdVideoContentController() = delegate.createNativeAdVideoContentController()
}

internal interface Factory {
  fun createRewardedAdSpot(): InneractiveAdSpot

  fun createInneractiveFullscreenUnitController(): InneractiveFullscreenUnitController

  fun createNativeAdUnitController(): NativeAdUnitController

  fun createNativeAdVideoContentController(): NativeAdVideoContentController
}
