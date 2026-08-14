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
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.RewardVideoAd
import sg.bigo.ads.api.RewardVideoAdRequest

@RunWith(AndroidJUnit4::class)
class BigoRewardedAdTest {
  // Subject of testing
  private lateinit var bigoRewardedAd: BigoRewardedAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val adLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
  private val mockRewardVideoAdRequest = mock<RewardVideoAdRequest>()
  private val mockRewardVideoAdLoader = mock<BigoRewardVideoAdLoaderWrapper>()
  private val mockBigoFactory =
    mock<SdkFactory> {
      on {
        createRewardVideoAdRequest(eq(TEST_BID_RESPONSE), eq(TEST_SLOT_ID), eq(TEST_WATERMARK))
      } doReturn mockRewardVideoAdRequest
      on { createRewardVideoAdLoader() } doReturn mockRewardVideoAdLoader
    }

  @Before
  fun setUp() {
    val serverParams = bundleOf(SLOT_ID_KEY to TEST_SLOT_ID)
    val adConfiguration =
      createMediationRewardedAdConfiguration(
        context = context,
        bidResponse = TEST_BID_RESPONSE,
        serverParameters = serverParams,
        watermark = TEST_WATERMARK,
      )
    BigoFactory.delegate = mockBigoFactory
    BigoRewardedAd.newInstance(adConfiguration, adLoadCallback).onSuccess { bigoRewardedAd = it }
  }

  @Test
  fun loadAd_invokesWrapperInitializeAdLoaderAndLoadAd() {
    bigoRewardedAd.loadAd(TEST_VERSION_STRING)

    inOrder(mockRewardVideoAdLoader) {
      verify(mockRewardVideoAdLoader).initializeAdLoader(bigoRewardedAd, TEST_VERSION_STRING)
      verify(mockRewardVideoAdLoader).loadAd(mockRewardVideoAdRequest)
    }
  }

  @Test
  fun onAdLoaded_setsListenerAndInvokesOnSuccess() {
    val mockRewardVideoAd = mock<RewardVideoAd>()

    bigoRewardedAd.onAdLoaded(mockRewardVideoAd)

    verify(mockRewardVideoAd).setAdInteractionListener(bigoRewardedAd)
    assertThat(adLoadCallback).hasSucceededWith(bigoRewardedAd)
  }

  @Test
  fun onError_invokesOnFailure() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)

    bigoRewardedAd.onError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    assertThat(adLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun showAd_invokesShow() {
    val mockRewardVideoAd = mock<RewardVideoAd>()
    bigoRewardedAd.onAdLoaded(mockRewardVideoAd)

    bigoRewardedAd.showAd(context)

    verify(mockRewardVideoAd).show()
  }

  @Test
  fun onAdRewarded_invokesOnUserEarnedReward() {
    bigoRewardedAd.onAdLoaded(mock())

    bigoRewardedAd.onAdRewarded()

    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onAdError_invokesOnAdFailedToShow() {
    val expectedAdError = BigoUtils.getGmaAdError(TEST_ERROR_CODE, TEST_ERROR_MSG, SDK_ERROR_DOMAIN)
    bigoRewardedAd.onAdLoaded(mock())

    bigoRewardedAd.onAdError(AdError(TEST_ERROR_CODE, TEST_ERROR_MSG))

    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    bigoRewardedAd.onAdLoaded(mock())

    bigoRewardedAd.onAdImpression()

    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    bigoRewardedAd.onAdLoaded(mock())

    bigoRewardedAd.onAdClicked()

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdOpened_invokesOnAdOpened() {
    bigoRewardedAd.onAdLoaded(mock())

    bigoRewardedAd.onAdOpened()

    assertThat(rewardedAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdClosed_invokesOnAdClosed() {
    bigoRewardedAd.onAdLoaded(mock())

    bigoRewardedAd.onAdClosed()

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  private companion object {
    const val TEST_SLOT_ID = "testSlotId"
    const val TEST_ERROR_CODE = 123
    const val TEST_ERROR_MSG = "testError"
    const val TEST_VERSION_STRING = "testVersionString"
  }
}
