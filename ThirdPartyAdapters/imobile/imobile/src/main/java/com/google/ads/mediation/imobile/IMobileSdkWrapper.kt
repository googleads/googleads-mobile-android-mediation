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

package com.google.ads.mediation.imobile

import android.app.Activity
import android.view.ViewGroup
import jp.co.imobile.sdkads.android.ImobileSdkAd
import jp.co.imobile.sdkads.android.ImobileSdkAdListener

/**
 * A wrapper to wrap static method calls to [ImobileSdkAd] so that the static methods calls can be
 * mocked/verified by unit tests.
 */
class IMobileSdkWrapper {

  fun registerSpotInline(
    activity: Activity,
    publisherId: String?,
    mediaId: String?,
    spotId: String?,
  ) {
    ImobileSdkAd.registerSpotInline(activity, publisherId, mediaId, spotId)
  }

  fun start(spotId: String?) {
    ImobileSdkAd.start(spotId)
  }

  fun setImobileSdkAdListener(spotId: String?, imobileSdkAdListener: ImobileSdkAdListener) {
    ImobileSdkAd.setImobileSdkAdListener(spotId, imobileSdkAdListener)
  }

  fun showAdForAdMobMediation(
    activity: Activity,
    spotId: String?,
    targetViewGroup: ViewGroup,
    adaptiveRatio: Float,
  ) {
    ImobileSdkAd.showAdForAdMobMediation(activity, spotId, targetViewGroup, adaptiveRatio)
  }
}
