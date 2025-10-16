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

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MediationUtils
import sg.bigo.ads.api.AdSize

object BigoUtils {
  fun getGmaAdError(code: Int, message: String, domain: String) = AdError(code, message, domain)

  fun mapAdSizeToBigoBannerSize(
    context: Context,
    adSize: com.google.android.gms.ads.AdSize,
  ): AdSize {
    // List of banner ad sizes supported by BidMachine.
    val supportedSizes =
      listOf(
        com.google.android.gms.ads.AdSize.BANNER,
        com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE,
        com.google.android.gms.ads.AdSize.LARGE_BANNER,
        com.google.android.gms.ads.AdSize.LEADERBOARD,
      )
    // Find the supported size that is closest to the publisher-requested size.
    val closestSupportedSize = MediationUtils.findClosestSize(context, adSize, supportedSizes)
    return when (closestSupportedSize) {
      com.google.android.gms.ads.AdSize.BANNER -> AdSize.BANNER
      com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
      com.google.android.gms.ads.AdSize.LARGE_BANNER -> AdSize.LARGE_BANNER
      com.google.android.gms.ads.AdSize.LEADERBOARD -> AdSize.LEADERBOARD
      else -> AdSize.BANNER
    }
  }
}
