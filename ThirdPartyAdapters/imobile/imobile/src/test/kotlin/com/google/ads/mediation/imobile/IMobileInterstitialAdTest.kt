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
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.FailNotificationReason
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for the APIs implemented by [IMobileInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class IMobileInterstitialAdTest {

  private val adLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()

  private val iMobileSdkWrapper: IMobileSdkWrapper = mock()

  private val iMobileInterstitialAd = IMobileInterstitialAd(adLoadCallback, iMobileSdkWrapper)

  private val interstitialAdCallback = mock<MediationInterstitialAdCallback>()

  @Before
  fun setUp() {
    whenever(adLoadCallback.onSuccess(iMobileInterstitialAd)) doReturn interstitialAdCallback
  }

  // region ImobileSdkAdListener implementation tests
  @Test
  fun onAdReadyCompleted_callsLoadSuccessCallback() {
    iMobileInterstitialAd.onAdReadyCompleted()

    verify(adLoadCallback).onSuccess(iMobileInterstitialAd)
  }

  @Test
  fun onAdShowCompleted_callsOnAdOpened() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdShowCompleted()

    verify(interstitialAdCallback).onAdOpened()
  }

  @Test
  fun onAdCliclkCompleted_reportsAdClickedAndAdOpenedAndAdLeftApplication() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdCliclkCompleted()

    verify(interstitialAdCallback).reportAdClicked()
    verify(interstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdCloseCompleted_callsOnAdClosed() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdCloseCompleted()

    verify(interstitialAdCallback).onAdClosed()
  }

  @Test
  fun onFailed_callsLoadFailureCallback() {
    iMobileInterstitialAd.onFailed(FailNotificationReason.RESPONSE)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(adLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
  }
  // endregion
}
