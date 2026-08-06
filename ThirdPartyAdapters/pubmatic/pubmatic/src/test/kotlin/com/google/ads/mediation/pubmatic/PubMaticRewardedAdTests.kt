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
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_AD_NOT_READY
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.POBReward
import com.pubmatic.sdk.rewardedad.POBRewardedAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for PubMaticRewardedAd. */
@RunWith(AndroidJUnit4::class)
class PubMaticRewardedAdTests {

  // Subject of testing
  private lateinit var pubMaticRewardedAd: PubMaticRewardedAd

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val rewardedAdCallback = FakeMediationRewardedAdCallback()

  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )

  private val pobRewardedAd = mock<POBRewardedAd>()

  private val pubMaticAdFactory =
    mock<PubMaticAdFactory> { on { createPOBRewardedAd(any()) } doReturn pobRewardedAd }

  private val mediationRewardedAdConfiguration =
    MediationRewardedAdConfiguration(
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
    PubMaticRewardedAd.newInstance(
        mediationRewardedAdConfiguration,
        rewardedAdLoadCallback,
        pubMaticAdFactory,
        isRtb = true,
      )
      .onSuccess { pubMaticRewardedAd = it }
  }

  @Test
  fun onAdReceived_invokesLoadSuccessCallback() {
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    assertThat(rewardedAdLoadCallback).hasSucceededWith(pubMaticRewardedAd)
  }

  @Test
  fun onAdFailedToLoad_invokesLoadFailureCallback() {
    val pobError = POBError(ERROR_PUBMATIC_AD_LOAD_FAILURE, "Ad load failed")

    pubMaticRewardedAd.onAdFailedToLoad(pobRewardedAd, pobError)

    val expectedError = AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun showAd_ifAdIsReady_showsPubMaticAd() {
    whenever(pobRewardedAd.isReady).thenReturn(true)

    pubMaticRewardedAd.showAd(context)

    verify(pobRewardedAd).show()
  }

  @Test
  fun showAd_ifAdIsNotReady_invokesAdShowFailureCallback() {
    whenever(pobRewardedAd.isReady).thenReturn(false)
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.showAd(context)

    val expectedError = AdError(ERROR_AD_NOT_READY, "Ad not ready", ADAPTER_ERROR_DOMAIN)
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.error).isEqualTo(expectedError)
  }

  @Test
  fun onAdFailedToShow_invokesAdShowFailureCallback() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)
    val pobError = POBError(ERROR_PUBMATIC_AD_SHOW_FAILURE, "Ad show failed")

    pubMaticRewardedAd.onAdFailedToShow(pobRewardedAd, pobError)

    val expectedError = AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.error).isEqualTo(expectedError)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdImpression(pobRewardedAd)

    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdClicked(pobRewardedAd)

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdOpened_reportsAdOpened() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdOpened(pobRewardedAd)

    assertThat(rewardedAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdClosed_reportsAdClosed() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdClosed(pobRewardedAd)

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onReceiveReward_reportsUserEarnedReward() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onReceiveReward(pobRewardedAd, POBReward("USD", 1))

    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  private companion object {
    const val ERROR_PUBMATIC_AD_LOAD_FAILURE = 1002
    const val ERROR_PUBMATIC_AD_SHOW_FAILURE = 1003
  }
}
