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

package com.google.ads.mediation.vungle.renderers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.VungleAdSize
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for the utility function getVungleBannerAdSizeFromGoogleAdSize() */
@RunWith(AndroidJUnit4::class)
class VungleBannerSizeAdapterTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun getVungleBannerAdSize_forGoogleSize300By50_returnsLiftoffSizeBannerShort() {
    val liftoffBannerSize =
      VungleBannerAd.getVungleBannerAdSizeFromGoogleAdSize(AdSize(300, 50), PLACEMENT_ID)

    assertThat(liftoffBannerSize).isEqualTo(VungleAdSize.BANNER_SHORT)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeRegularBanner_returnsLiftoffSizeRegularBanner() {
    val liftoffBannerSize =
      VungleBannerAd.getVungleBannerAdSizeFromGoogleAdSize(AdSize.BANNER, PLACEMENT_ID)

    assertThat(liftoffBannerSize).isEqualTo(VungleAdSize.BANNER)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeLeaderboard_returnsLiftoffSizeLeaderboard() {
    val liftoffBannerSize =
      VungleBannerAd.getVungleBannerAdSizeFromGoogleAdSize(AdSize.LEADERBOARD, PLACEMENT_ID)

    assertThat(liftoffBannerSize).isEqualTo(VungleAdSize.BANNER_LEADERBOARD)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeMediumRectangle_returnsLiftoffSizeMediumRectangle() {
    val liftoffBannerSize =
      VungleBannerAd.getVungleBannerAdSizeFromGoogleAdSize(AdSize.MEDIUM_RECTANGLE, PLACEMENT_ID)

    assertThat(liftoffBannerSize).isEqualTo(VungleAdSize.MREC)
  }

  @Test
  fun getVungleBannerAdSize_forNonStandardGoogleBannerSize_returnsCustomSize() {
    val liftoffBannerSize =
      VungleBannerAd.getVungleBannerAdSizeFromGoogleAdSize(AdSize.WIDE_SKYSCRAPER, PLACEMENT_ID)

    assertThat(liftoffBannerSize).isNotNull()
    assertThat(liftoffBannerSize.width).isEqualTo(AdSize.WIDE_SKYSCRAPER.width)
    assertThat(liftoffBannerSize.height).isEqualTo(AdSize.WIDE_SKYSCRAPER.height)
  }

  private companion object {
    const val PLACEMENT_ID = "testPlacementId"
  }
}
