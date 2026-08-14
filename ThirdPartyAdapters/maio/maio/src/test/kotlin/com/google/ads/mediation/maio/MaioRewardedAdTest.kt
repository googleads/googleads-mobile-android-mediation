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

package com.google.ads.mediation.maio

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.maio.MaioMediationAdapter.getAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_MEDIA_ID
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_ZONE_ID
import jp.maio.sdk.android.v2.request.MaioRequest
import jp.maio.sdk.android.v2.rewarddata.RewardData
import jp.maio.sdk.android.v2.rewarded.IRewardedLoadCallback
import jp.maio.sdk.android.v2.rewarded.IRewardedShowCallback
import jp.maio.sdk.android.v2.rewarded.Rewarded
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

/** Tests for [MaioRewardedAd]. */
@RunWith(AndroidJUnit4::class)
class MaioRewardedAdTest {

  private lateinit var maioRewardedAd: MaioRewardedAd
  private lateinit var mockRewardedStatic: MockedStatic<Rewarded>

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mockRewarded = mock<Rewarded>()
  private val mockRewardedAdCallback = mock<MediationRewardedAdCallback>()
  private val mockAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>> {
      on { onSuccess(any()) } doReturn mockRewardedAdCallback
    }

  @Before
  fun setUp() {
    mockRewardedStatic = mockStatic(Rewarded::class.java)
    mockRewardedStatic
      .`when`<Rewarded> { MaioTestHelper.loadRewardedAd(any(), any(), any()) }
      .thenReturn(mockRewarded)

    maioRewardedAd = MaioRewardedAd(mockAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockRewardedStatic.close()
  }

  // region loadAd Tests

  @Test
  fun loadAd_missingMediaId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_ZONE_ID to TEST_ZONE_ID)
    val config =
      createMediationRewardedAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioRewardedAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyMediaId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to "", KEY_ZONE_ID to TEST_ZONE_ID)
    val config =
      createMediationRewardedAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioRewardedAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_missingZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID)
    val config =
      createMediationRewardedAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioRewardedAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID, KEY_ZONE_ID to "")
    val config =
      createMediationRewardedAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioRewardedAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_validServerParameters_invokesRewardedLoadAd() {
    val config = createDefaultRewardedAdConfiguration(isTesting = true)

    maioRewardedAd.loadAd(config)

    val requestCaptor = argumentCaptor<MaioRequest>()
    mockRewardedStatic.verify {
      MaioTestHelper.loadRewardedAd(requestCaptor.capture(), eq(activity), any())
    }
    assertThat(requestCaptor.firstValue.zoneId).isEqualTo(TEST_ZONE_ID)
    assertThat(requestCaptor.firstValue.testMode).isTrue()
  }

  // endregion

  // region IRewardedLoadCallback Tests

  @Test
  fun loaded_invokesOnSuccess() {
    val config = createDefaultRewardedAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IRewardedLoadCallback>()

    maioRewardedAd.loadAd(config)

    mockRewardedStatic.verify {
      MaioTestHelper.loadRewardedAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.loaded(mockRewarded)

    verify(mockAdLoadCallback).onSuccess(maioRewardedAd)
  }

  @Test
  fun loaded_withNullAdLoadCallback_doesNotCrash() {
    val adWithoutCallback = MaioRewardedAd(null)
    val config = createDefaultRewardedAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IRewardedLoadCallback>()

    adWithoutCallback.loadAd(config)

    mockRewardedStatic.verify {
      MaioTestHelper.loadRewardedAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.loaded(mockRewarded)
  }

  @Test
  fun failed_invokesOnFailure() {
    val config = createDefaultRewardedAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IRewardedLoadCallback>()

    maioRewardedAd.loadAd(config)

    mockRewardedStatic.verify {
      MaioTestHelper.loadRewardedAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.failed(mockRewarded, TEST_ERROR_CODE)

    val expectedAdError = getAdError(TEST_ERROR_CODE)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun failed_withNullAdLoadCallback_doesNotCrash() {
    val adWithoutCallback = MaioRewardedAd(null)
    val config = createDefaultRewardedAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IRewardedLoadCallback>()

    adWithoutCallback.loadAd(config)

    mockRewardedStatic.verify {
      MaioTestHelper.loadRewardedAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.failed(mockRewarded, TEST_ERROR_CODE)
  }

  // endregion

  // region Presentation and IRewardedShowCallback Tests

  @Test
  fun showAd_invokesRewardedShow() {
    val config = createDefaultRewardedAdConfiguration()
    maioRewardedAd.loadAd(config)

    maioRewardedAd.showAd(activity)

    verify(mockRewarded).show(eq(activity), any())
  }

  @Test
  fun opened_invokesOnAdOpenedAndOnVideoStartAndReportAdImpression() {
    val showCallback = loadAdAndShow()

    showCallback.opened(mockRewarded)

    verify(mockRewardedAdCallback).onAdOpened()
    verify(mockRewardedAdCallback).onVideoStart()
    verify(mockRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun opened_withNullAdCallback_doesNotCrash() {
    val showCallback = showWithoutLoaded()

    showCallback.opened(mockRewarded)
  }

  @Test
  fun closed_invokesOnAdClosedAndOnVideoComplete() {
    val showCallback = loadAdAndShow()

    showCallback.closed(mockRewarded)

    verify(mockRewardedAdCallback).onAdClosed()
    verify(mockRewardedAdCallback).onVideoComplete()
  }

  @Test
  fun closed_withNullAdCallback_doesNotCrash() {
    val showCallback = showWithoutLoaded()

    showCallback.closed(mockRewarded)
  }

  @Test
  fun clicked_invokesReportAdClicked() {
    val showCallback = loadAdAndShow()

    showCallback.clicked(mockRewarded)

    verify(mockRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun clicked_withNullAdCallback_doesNotCrash() {
    val showCallback = showWithoutLoaded()

    showCallback.clicked(mockRewarded)
  }

  @Test
  fun rewarded_invokesOnUserEarnedReward() {
    val showCallback = loadAdAndShow()
    val mockRewardData = mock<RewardData>()

    showCallback.rewarded(mockRewarded, mockRewardData)

    verify(mockRewardedAdCallback).onUserEarnedReward()
  }

  @Test
  fun rewarded_withNullAdCallback_doesNotCrash() {
    val showCallback = showWithoutLoaded()
    val mockRewardData = mock<RewardData>()

    showCallback.rewarded(mockRewarded, mockRewardData)
  }

  @Test
  fun failed_duringPresentation_invokesOnAdFailedToShow() {
    val showCallback = loadAdAndShow()

    showCallback.failed(mockRewarded, TEST_ERROR_CODE)

    val expectedAdError = getAdError(TEST_ERROR_CODE)
    verify(mockRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun failed_duringPresentationWithNullAdCallback_doesNotCrash() {
    val showCallback = showWithoutLoaded()

    showCallback.failed(mockRewarded, TEST_ERROR_CODE)
  }

  // endregion

  private fun loadAdAndShow(): IRewardedShowCallback {
    val config = createDefaultRewardedAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IRewardedLoadCallback>()
    maioRewardedAd.loadAd(config)
    mockRewardedStatic.verify {
      MaioTestHelper.loadRewardedAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.loaded(mockRewarded)

    val showCallbackCaptor = argumentCaptor<IRewardedShowCallback>()
    maioRewardedAd.showAd(activity)
    verify(mockRewarded).show(eq(activity), showCallbackCaptor.capture())
    return showCallbackCaptor.firstValue
  }

  private fun showWithoutLoaded(): IRewardedShowCallback {
    val config = createDefaultRewardedAdConfiguration()
    maioRewardedAd.loadAd(config)

    val showCallbackCaptor = argumentCaptor<IRewardedShowCallback>()
    maioRewardedAd.showAd(activity)
    verify(mockRewarded).show(eq(activity), showCallbackCaptor.capture())
    return showCallbackCaptor.firstValue
  }

  private fun createDefaultRewardedAdConfiguration(
    isTesting: Boolean = true
  ): MediationRewardedAdConfiguration {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID, KEY_ZONE_ID to TEST_ZONE_ID)
    return createMediationRewardedAdConfiguration(
      context = activity,
      serverParameters = serverParameters,
      isTesting = isTesting,
    )
  }

  private companion object {
    const val TEST_MEDIA_ID = "testMediaId"
    const val TEST_ZONE_ID = "testZoneId"
    const val TEST_ERROR_CODE = 10700 // noFill
  }
}
