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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.imobile.IMobileMediationAdapter.IMOBILE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.FailNotificationReason
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for the APIs implemented by [IMobileBannerAd]. */
@RunWith(AndroidJUnit4::class)
class IMobileBannerAdTest {

  private val adLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()

  private val iMobileBannerAd = IMobileBannerAd(adLoadCallback)

  private val bannerAdCallback = mock<MediationBannerAdCallback>()

  @Before
  fun setUp() {
    whenever(adLoadCallback.onSuccess(iMobileBannerAd)) doReturn bannerAdCallback
  }

  // region ImobileSdkAdListener implementation tests
  @Test
  fun onAdReadyCompleted_callsLoadSuccessCallback() {
    iMobileBannerAd.onAdReadyCompleted()

    verify(adLoadCallback).onSuccess(iMobileBannerAd)
  }

  @Test
  fun onAdCliclkCompleted_reportsAdClickedAndAdOpenedAndAdLeftApplication() {
    // Call onAdReadyCompleted to set iMobileBannerAd.bannerAdCallback
    iMobileBannerAd.onAdReadyCompleted()

    iMobileBannerAd.onAdCliclkCompleted()

    verify(bannerAdCallback).reportAdClicked()
    verify(bannerAdCallback).onAdOpened()
    verify(bannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun onDismissAdScreen_callsOnAdClosed() {
    // Call onAdReadyCompleted to set iMobileBannerAd.bannerAdCallback
    iMobileBannerAd.onAdReadyCompleted()

    iMobileBannerAd.onDismissAdScreen()

    verify(bannerAdCallback).onAdClosed()
  }

  @Test
  fun onFailed_callsLoadFailureCallback() {
    iMobileBannerAd.onFailed(FailNotificationReason.RESPONSE)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(adLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
  }
  // endregion
}
