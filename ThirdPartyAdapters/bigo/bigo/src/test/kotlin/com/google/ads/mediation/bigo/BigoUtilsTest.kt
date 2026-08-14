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

package com.google.ads.mediation.bigo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BigoUtilsTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockMediationUtils = mock<MediationUtilsWrapper>()

  @Test
  fun mapAdSizeToBigoBannerSize_banner_returnsBigoBanner() {
    whenever(mockMediationUtils.findClosestSize(eq(context), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER

    val bigoAdSize = BigoUtils.mapAdSizeToBigoBannerSize(context, AdSize.BANNER, mockMediationUtils)

    assertThat(bigoAdSize).isEqualTo(sg.bigo.ads.api.AdSize.BANNER)
  }

  @Test
  fun mapAdSizeToBigoBannerSize_mediumRectangle_returnsBigoMediumRectangle() {
    whenever(
      mockMediationUtils.findClosestSize(eq(context), eq(AdSize.MEDIUM_RECTANGLE), any())
    ) doReturn AdSize.MEDIUM_RECTANGLE

    val bigoAdSize =
      BigoUtils.mapAdSizeToBigoBannerSize(context, AdSize.MEDIUM_RECTANGLE, mockMediationUtils)

    assertThat(bigoAdSize).isEqualTo(sg.bigo.ads.api.AdSize.MEDIUM_RECTANGLE)
  }

  @Test
  fun mapAdSizeToBigoBannerSize_largeBanner_returnsBigoLargeBanner() {
    whenever(
      mockMediationUtils.findClosestSize(eq(context), eq(AdSize.LARGE_BANNER), any())
    ) doReturn AdSize.LARGE_BANNER

    val bigoAdSize =
      BigoUtils.mapAdSizeToBigoBannerSize(context, AdSize.LARGE_BANNER, mockMediationUtils)

    assertThat(bigoAdSize).isEqualTo(sg.bigo.ads.api.AdSize.LARGE_BANNER)
  }

  @Test
  fun mapAdSizeToBigoBannerSize_leaderboard_returnsBigoLeaderboard() {
    whenever(
      mockMediationUtils.findClosestSize(eq(context), eq(AdSize.LEADERBOARD), any())
    ) doReturn AdSize.LEADERBOARD

    val bigoAdSize =
      BigoUtils.mapAdSizeToBigoBannerSize(context, AdSize.LEADERBOARD, mockMediationUtils)

    assertThat(bigoAdSize).isEqualTo(sg.bigo.ads.api.AdSize.LEADERBOARD)
  }

  @Test
  fun mapAdSizeToBigoBannerSize_unsupportedSize_returnsDefaultBanner() {
    whenever(
      mockMediationUtils.findClosestSize(eq(context), eq(AdSize.WIDE_SKYSCRAPER), any())
    ) doReturn null

    val bigoAdSize =
      BigoUtils.mapAdSizeToBigoBannerSize(context, AdSize.WIDE_SKYSCRAPER, mockMediationUtils)

    assertThat(bigoAdSize).isEqualTo(sg.bigo.ads.api.AdSize.BANNER)
  }
}
