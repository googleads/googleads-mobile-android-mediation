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
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.maio.MaioMediationAdapter.getAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_MEDIA_ID
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_ZONE_ID
import jp.maio.sdk.android.v2.interstitial.IInterstitialLoadCallback
import jp.maio.sdk.android.v2.interstitial.Interstitial
import jp.maio.sdk.android.v2.request.MaioRequest
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

/** Tests for [MaioInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class MaioInterstitialAdTest {

  private lateinit var maioInterstitialAd: MaioInterstitialAd
  private lateinit var mockInterstitialStatic: MockedStatic<Interstitial>

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mockInterstitial = mock<Interstitial>()
  private val mockInterstitialAdCallback = mock<MediationInterstitialAdCallback>()
  private val mockAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>> {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }

  @Before
  fun setUp() {
    mockInterstitialStatic = mockStatic(Interstitial::class.java)
    mockInterstitialStatic
      .`when`<Interstitial> { MaioTestHelper.loadInterstitialAd(any(), any(), any()) }
      .thenReturn(mockInterstitial)

    maioInterstitialAd = MaioInterstitialAd(mockAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockInterstitialStatic.close()
  }

  // region loadAd Tests

  @Test
  fun loadAd_missingMediaId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_ZONE_ID to TEST_ZONE_ID)
    val config =
      createMediationInterstitialAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioInterstitialAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyMediaId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to "", KEY_ZONE_ID to TEST_ZONE_ID)
    val config =
      createMediationInterstitialAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioInterstitialAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_missingZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID)
    val config =
      createMediationInterstitialAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioInterstitialAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID, KEY_ZONE_ID to "")
    val config =
      createMediationInterstitialAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
      )

    maioInterstitialAd.loadAd(config)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_validServerParameters_invokesInterstitialLoadAd() {
    val config = createDefaultInterstitialAdConfiguration(isTesting = true)

    maioInterstitialAd.loadAd(config)

    val requestCaptor = argumentCaptor<MaioRequest>()
    mockInterstitialStatic.verify {
      MaioTestHelper.loadInterstitialAd(requestCaptor.capture(), eq(activity), any())
    }
    assertThat(requestCaptor.firstValue.zoneId).isEqualTo(TEST_ZONE_ID)
    assertThat(requestCaptor.firstValue.testMode).isTrue()
  }

  // endregion

  // region IInterstitialLoadCallback Tests

  @Test
  fun loaded_invokesOnSuccess() {
    val config = createDefaultInterstitialAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IInterstitialLoadCallback>()

    maioInterstitialAd.loadAd(config)

    mockInterstitialStatic.verify {
      MaioTestHelper.loadInterstitialAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.loaded(mockInterstitial)

    verify(mockAdLoadCallback).onSuccess(maioInterstitialAd)
  }

  @Test
  fun failed_invokesOnFailure() {
    val config = createDefaultInterstitialAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IInterstitialLoadCallback>()

    maioInterstitialAd.loadAd(config)

    mockInterstitialStatic.verify {
      MaioTestHelper.loadInterstitialAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.failed(mockInterstitial, TEST_ERROR_CODE)

    val expectedAdError = getAdError(TEST_ERROR_CODE)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Presentation and IInterstitialShowCallback Tests

  @Test
  fun showAd_invokesInterstitialShow() {
    val config = createDefaultInterstitialAdConfiguration()
    maioInterstitialAd.loadAd(config)

    maioInterstitialAd.showAd(activity)

    verify(mockInterstitial).show(eq(activity), eq(maioInterstitialAd))
  }

  @Test
  fun opened_invokesOnAdOpened() {
    loadAdAndTriggerLoaded()

    maioInterstitialAd.opened(mockInterstitial)

    verify(mockInterstitialAdCallback).onAdOpened()
  }

  @Test
  fun opened_withNullAdCallback_doesNotCrash() {
    maioInterstitialAd.opened(mockInterstitial)
  }

  @Test
  fun closed_invokesOnAdClosed() {
    loadAdAndTriggerLoaded()

    maioInterstitialAd.closed(mockInterstitial)

    verify(mockInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun closed_withNullAdCallback_doesNotCrash() {
    maioInterstitialAd.closed(mockInterstitial)
  }

  @Test
  fun clicked_invokesReportAdClickedAndOnAdLeftApplication() {
    loadAdAndTriggerLoaded()

    maioInterstitialAd.clicked(mockInterstitial)

    verify(mockInterstitialAdCallback).reportAdClicked()
    verify(mockInterstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun clicked_withNullAdCallback_doesNotCrash() {
    maioInterstitialAd.clicked(mockInterstitial)
  }

  @Test
  fun failed_duringPresentation_invokesOnAdOpenedAndOnAdClosed() {
    loadAdAndTriggerLoaded()

    maioInterstitialAd.failed(mockInterstitial, TEST_ERROR_CODE)

    verify(mockInterstitialAdCallback).onAdOpened()
    verify(mockInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun failed_withNullAdCallback_doesNotCrash() {
    maioInterstitialAd.failed(mockInterstitial, TEST_ERROR_CODE)
  }

  // endregion

  private fun loadAdAndTriggerLoaded() {
    val config = createDefaultInterstitialAdConfiguration()
    val loadCallbackCaptor = argumentCaptor<IInterstitialLoadCallback>()
    maioInterstitialAd.loadAd(config)
    mockInterstitialStatic.verify {
      MaioTestHelper.loadInterstitialAd(any(), eq(activity), loadCallbackCaptor.capture())
    }
    loadCallbackCaptor.firstValue.loaded(mockInterstitial)
  }

  private fun createDefaultInterstitialAdConfiguration(
    isTesting: Boolean = true
  ): MediationInterstitialAdConfiguration {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID, KEY_ZONE_ID to TEST_ZONE_ID)
    return createMediationInterstitialAdConfiguration(
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
