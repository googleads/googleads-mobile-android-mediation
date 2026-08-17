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

package com.google.ads.mediation.pubmatic

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/** Tests for [PubMaticBannerAd]. */
@RunWith(AndroidJUnit4::class)
class PubMaticBannerAdTests {

  // Subject of testing
  private lateinit var pubMaticBannerAd: PubMaticBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val bannerAdCallback = FakeMediationBannerAdCallback()

  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)

  private val pobBannerView = mock<POBBannerView>()

  private val pubMaticAdFactory =
    mock<PubMaticAdFactory> { on { createPOBBannerView(any()) } doReturn pobBannerView }

  private val mediationBannerAdConfiguration =
    MediationBannerAdConfiguration(
      context,
      "bid response",
      /*serverParameters = */ bundleOf(),
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      AdSize.BANNER,
      /*watermark=*/ "",
    )

  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    PubMaticBannerAd.newInstance(
        mediationBannerAdConfiguration,
        bannerAdLoadCallback,
        pubMaticAdFactory,
        isRTB = true,
        mediationUtils,
      )
      .onSuccess { pubMaticBannerAd = it }
  }

  @Test
  fun onAdReceived_invokesLoadSuccessCallback() {
    pubMaticBannerAd.onAdReceived(pobBannerView)

    assertThat(bannerAdLoadCallback).hasSucceededWith(pubMaticBannerAd)
  }

  @Test
  fun onAdFailed_invokesLoadFailureCallback() {
    val pobError = POBError(ERROR_PUBMATIC_AD_LOAD_FAILURE, "Ad load failed")

    pubMaticBannerAd.onAdFailed(pobBannerView, pobError)

    val expectedError = AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdImpression(pobBannerView)

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdClicked(pobBannerView)

    assertThat(bannerAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAppLeaving_reportsAdLeftApplication() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAppLeaving(pobBannerView)

    assertThat(bannerAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onAdOpened_reportsAdOpened() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdOpened(pobBannerView)

    assertThat(bannerAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdClosed_reportsAdClosed() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdClosed(pobBannerView)

    assertThat(bannerAdCallback.isClosed).isTrue()
  }

  @Test
  fun getView_returnsPubMaticBannerView() {
    assertThat(pubMaticBannerAd.view).isEqualTo(pobBannerView)
  }

  private companion object {
    const val ERROR_PUBMATIC_AD_LOAD_FAILURE = 1002
  }
}
