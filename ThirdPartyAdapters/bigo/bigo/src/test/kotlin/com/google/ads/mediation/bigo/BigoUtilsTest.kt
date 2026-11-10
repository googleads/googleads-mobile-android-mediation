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
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions

@RunWith(AndroidJUnit4::class)
class BigoUtilsTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockMediationUtils = mock<MediationUtilsWrapper>()
  private lateinit var mockBigoSdk: MockedStatic<BigoAdSdk>

  @Before
  fun setUp() {
    mockBigoSdk = mockStatic(BigoAdSdk::class.java)
    MobileAds.setRequestConfiguration(RequestConfiguration.Builder().build())
  }

  @After
  fun tearDown() {
    mockBigoSdk.close()
  }

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

  // region configureBigoPrivacy tests
  @Test
  fun configureBigoPrivacy_tagForChildTrue_setsBigoCoppaToFalse() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )

    BigoUtils.configureBigoPrivacy(context)

    mockBigoSdk.verify {
      BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(false))
    }
  }

  @Test
  fun configureBigoPrivacy_tagForUnderAgeTrue_setsBigoCoppaToFalse() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    )

    BigoUtils.configureBigoPrivacy(context)

    mockBigoSdk.verify {
      BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(false))
    }
  }

  @Test
  fun configureBigoPrivacy_tagForChildFalse_setsBigoCoppaToTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
          RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        )
        .build()
    )

    BigoUtils.configureBigoPrivacy(context)

    mockBigoSdk.verify { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(true)) }
  }

  @Test
  fun configureBigoPrivacy_tagForUnderAgeFalse_setsBigoCoppaToTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    )

    BigoUtils.configureBigoPrivacy(context)

    mockBigoSdk.verify { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), eq(true)) }
  }

  @Test
  fun configureBigoPrivacy_unspecified_doesNotModifyBigoCoppa() {
    BigoUtils.configureBigoPrivacy(context)

    mockBigoSdk.verify(
      { BigoAdSdk.setUserConsent(eq(context), eq(ConsentOptions.COPPA), any()) },
      never(),
    )
  }

  // endregion
}
