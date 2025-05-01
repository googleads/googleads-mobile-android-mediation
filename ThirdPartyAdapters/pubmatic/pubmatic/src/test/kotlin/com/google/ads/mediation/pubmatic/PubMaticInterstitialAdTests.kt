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
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_AD_NOT_READY
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for PubMaticInterstitialAd. */
@RunWith(AndroidJUnit4::class)
class PubMaticInterstitialAdTests {

  // Subject of testing
  private lateinit var pubMaticInterstitialAd: PubMaticInterstitialAd

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val mediationInterstitialAdCallback = mock<MediationInterstitialAdCallback>()

  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>> {
      on { onSuccess(any()) } doReturn mediationInterstitialAdCallback
    }

  private val pobInterstitial = mock<POBInterstitial>()

  private val adErrorCaptor = argumentCaptor<AdError>()

  private val pubMaticAdFactory =
    mock<PubMaticAdFactory> { on { createPOBInterstitial(any()) } doReturn pobInterstitial }

  private val mediationInterstitialAdConfiguration =
    MediationInterstitialAdConfiguration(
      context,
      "bid response",
      /*serverParameters = */ bundleOf(),
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      /*watermark=*/ "",
    )

  @Before
  fun setUp() {
    PubMaticInterstitialAd.newInstance(
        mediationInterstitialAdConfiguration,
        mediationAdLoadCallback,
        pubMaticAdFactory,
      )
      .onSuccess { pubMaticInterstitialAd = it }
  }

  @Test
  fun onAdReceived_invokesLoadSuccessCallback() {
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)

    verify(mediationAdLoadCallback).onSuccess(pubMaticInterstitialAd)
  }

  @Test
  fun onAdFailedToLoad_invokesLoadFailureCallback() {
    val pobError = POBError(ERROR_PUBMATIC_AD_LOAD_FAILURE, "Ad load failed")

    pubMaticInterstitialAd.onAdFailedToLoad(pobInterstitial, pobError)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_PUBMATIC_AD_LOAD_FAILURE)
    assertThat(adError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun showAd_ifAdIsReady_showsPubMaticAd() {
    whenever(pobInterstitial.isReady).thenReturn(true)

    pubMaticInterstitialAd.showAd(context)

    verify(pobInterstitial).show()
  }

  @Test
  fun showAd_ifAdIsNotReady_invokesAdShowFailureCallback() {
    whenever(pobInterstitial.isReady).thenReturn(false)
    // Call onAdReceived() to set pubMaticInterstitialAd.mediationInterstitialAdCallback
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)

    pubMaticInterstitialAd.showAd(context)

    verify(mediationInterstitialAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_AD_NOT_READY)
    assertThat(adError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun onAdFailedToShow_invokesAdShowFailureCallback() {
    // Call onAdReceived() to set pubMaticInterstitialAd.mediationInterstitialAdCallback
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)
    val pobError = POBError(ERROR_PUBMATIC_AD_SHOW_FAILURE, "Ad show failed")

    pubMaticInterstitialAd.onAdFailedToShow(pobInterstitial, pobError)

    verify(mediationInterstitialAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_PUBMATIC_AD_SHOW_FAILURE)
    assertThat(adError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    // Call onAdReceived() to set pubMaticInterstitialAd.mediationInterstitialAdCallback
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)

    pubMaticInterstitialAd.onAdImpression(pobInterstitial)

    verify(mediationInterstitialAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    // Call onAdReceived() to set pubMaticInterstitialAd.mediationInterstitialAdCallback
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)

    pubMaticInterstitialAd.onAdClicked(pobInterstitial)

    verify(mediationInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAppLeaving_reportsAdLeftApplication() {
    // Call onAdReceived() to set pubMaticInterstitialAd.mediationInterstitialAdCallback
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)

    pubMaticInterstitialAd.onAppLeaving(pobInterstitial)

    verify(mediationInterstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdOpened_reportsAdOpened() {
    // Call onAdReceived() to set pubMaticInterstitialAd.mediationInterstitialAdCallback
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)

    pubMaticInterstitialAd.onAdOpened(pobInterstitial)

    verify(mediationInterstitialAdCallback).onAdOpened()
  }

  @Test
  fun onAdClosed_reportsAdClosed() {
    // Call onAdReceived() to set pubMaticInterstitialAd.mediationInterstitialAdCallback
    pubMaticInterstitialAd.onAdReceived(pobInterstitial)

    pubMaticInterstitialAd.onAdClosed(pobInterstitial)

    verify(mediationInterstitialAdCallback).onAdClosed()
  }

  private companion object {
    const val ERROR_PUBMATIC_AD_LOAD_FAILURE = 1002
    const val ERROR_PUBMATIC_AD_SHOW_FAILURE = 1003
  }
}
