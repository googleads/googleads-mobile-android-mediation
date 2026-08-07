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
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.maio.MaioMediationAdapter.getAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_MEDIA_ID
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager.KEY_ZONE_ID
import jp.maio.sdk.android.v2.banner.MaioBannerListener
import jp.maio.sdk.android.v2.banner.MaioBannerView
import kotlin.test.assertIs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [MaioBannerAd]. */
@RunWith(AndroidJUnit4::class)
class MaioBannerAdTest {

  private lateinit var maioBannerAd: MaioBannerAd
  private lateinit var mockBannerViewConstruction: MockedConstruction<MaioBannerView>

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mediationUtils: MediationUtilsWrapper = mock()
  private val mockBannerAdCallback = mock<MediationBannerAdCallback>()
  private val mockAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>> {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }

  @Before
  fun setUp() {
    mockBannerViewConstruction = mockConstruction(MaioBannerView::class.java)
    whenever(mediationUtils.findClosestSize(any(), any(), any())) doReturn AdSize.BANNER

    maioBannerAd = MaioBannerAd(mockAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockBannerViewConstruction.close()
  }

  // region loadAd Tests

  @Test
  fun loadAd_missingMediaId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_ZONE_ID to TEST_ZONE_ID)
    val config =
      createMediationBannerAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    maioBannerAd.loadAd(config, mediationUtils)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyMediaId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to "", KEY_ZONE_ID to TEST_ZONE_ID)
    val config =
      createMediationBannerAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    maioBannerAd.loadAd(config, mediationUtils)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_missingZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID)
    val config =
      createMediationBannerAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    maioBannerAd.loadAd(config, mediationUtils)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_emptyZoneId_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID, KEY_ZONE_ID to "")
    val config =
      createMediationBannerAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
        adSize = AdSize.BANNER,
      )

    maioBannerAd.loadAd(config, mediationUtils)

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_unsupportedAdSize_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID, KEY_ZONE_ID to TEST_ZONE_ID)
    val config =
      createMediationBannerAdConfiguration(
        context = activity,
        serverParameters = serverParameters,
        adSize = AdSize.WIDE_SKYSCRAPER,
      )
    whenever(
      mediationUtils.findClosestSize(eq(activity), eq(AdSize.WIDE_SKYSCRAPER), any())
    ) doReturn null

    maioBannerAd.loadAd(config, mediationUtils)

    val expectedAdError =
      AdError(
        ERROR_INVALID_SERVER_PARAMETERS,
        "The requested ad size is not supported by maio SDK.",
        ERROR_DOMAIN,
      )
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_validParameters_createsBannerViewAndLoadsAd() {
    val config = createDefaultBannerAdConfiguration(isTesting = true)

    maioBannerAd.loadAd(config, mediationUtils)

    assertThat(mockBannerViewConstruction.constructed()).hasSize(1)
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    verify(mockBannerView).listener = any()
    MaioTestHelper.loadBannerView(verify(mockBannerView), true)
  }

  // endregion

  // region MaioBannerListener Tests

  @Test
  fun loaded_invokesOnSuccess() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()

    listener.loaded(mockBannerView)

    verify(mockAdLoadCallback).onSuccess(maioBannerAd)
  }

  @Test
  fun loaded_withNullAdLoadCallback_doesNotCrash() {
    val adWithoutCallback = MaioBannerAd(null)
    val config = createDefaultBannerAdConfiguration()
    adWithoutCallback.loadAd(config, mediationUtils)
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    val listenerCaptor = argumentCaptor<MaioBannerListener>()
    verify(mockBannerView).listener = listenerCaptor.capture()

    listenerCaptor.firstValue.loaded(mockBannerView)
  }

  @Test
  fun failedToLoad_invokesOnFailure() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()

    listener.failedToLoad(mockBannerView, TEST_ERROR_CODE)

    val expectedAdError = getAdError(TEST_ERROR_CODE)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun failedToLoad_withNullAdLoadCallback_doesNotCrash() {
    val adWithoutCallback = MaioBannerAd(null)
    val config = createDefaultBannerAdConfiguration()
    adWithoutCallback.loadAd(config, mediationUtils)
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    val listenerCaptor = argumentCaptor<MaioBannerListener>()
    verify(mockBannerView).listener = listenerCaptor.capture()

    listenerCaptor.firstValue.failedToLoad(mockBannerView, TEST_ERROR_CODE)
  }

  @Test
  fun impression_invokesReportAdImpression() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    listener.loaded(mockBannerView)

    listener.impression(mockBannerView)

    verify(mockBannerAdCallback).reportAdImpression()
  }

  @Test
  fun impression_withNullAdCallback_doesNotCrash() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()

    listener.impression(mockBannerView)
  }

  @Test
  fun clicked_invokesReportAdClicked() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    listener.loaded(mockBannerView)

    listener.clicked(mockBannerView)

    verify(mockBannerAdCallback).reportAdClicked()
  }

  @Test
  fun clicked_withNullAdCallback_doesNotCrash() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()

    listener.clicked(mockBannerView)
  }

  @Test
  fun leftApplication_invokesOnAdLeftApplication() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    listener.loaded(mockBannerView)

    listener.leftApplication(mockBannerView)

    verify(mockBannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun leftApplication_withNullAdCallback_doesNotCrash() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()

    listener.leftApplication(mockBannerView)
  }

  @Test
  fun failedToShow_invokesOnFailure() {
    val listener = loadAdAndGetListener()
    val mockBannerView = mockBannerViewConstruction.constructed().first()

    listener.failedToShow(mockBannerView, TEST_ERROR_CODE)

    val expectedAdError = getAdError(TEST_ERROR_CODE)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun failedToShow_withNullAdLoadCallback_doesNotCrash() {
    val adWithoutCallback = MaioBannerAd(null)
    val config = createDefaultBannerAdConfiguration()
    adWithoutCallback.loadAd(config, mediationUtils)
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    val listenerCaptor = argumentCaptor<MaioBannerListener>()
    verify(mockBannerView).listener = listenerCaptor.capture()

    listenerCaptor.firstValue.failedToShow(mockBannerView, TEST_ERROR_CODE)
  }

  // endregion

  // region getView Tests

  @Test
  fun getView_afterLoadAd_returnsBannerView() {
    val config = createDefaultBannerAdConfiguration()
    maioBannerAd.loadAd(config, mediationUtils)

    val view = maioBannerAd.view

    assertIs<MaioBannerView>(view)
    assertThat(view).isEqualTo(mockBannerViewConstruction.constructed().first())
  }

  // endregion

  private fun loadAdAndGetListener(): MaioBannerListener {
    val config = createDefaultBannerAdConfiguration()
    maioBannerAd.loadAd(config, mediationUtils)
    val mockBannerView = mockBannerViewConstruction.constructed().first()
    val listenerCaptor = argumentCaptor<MaioBannerListener>()
    verify(mockBannerView).listener = listenerCaptor.capture()
    return listenerCaptor.firstValue
  }

  private fun createDefaultBannerAdConfiguration(
    isTesting: Boolean = true,
    adSize: AdSize = AdSize.BANNER,
  ): MediationBannerAdConfiguration {
    val serverParameters = bundleOf(KEY_MEDIA_ID to TEST_MEDIA_ID, KEY_ZONE_ID to TEST_ZONE_ID)
    return createMediationBannerAdConfiguration(
      context = activity,
      serverParameters = serverParameters,
      adSize = adSize,
      isTesting = isTesting,
    )
  }

  private companion object {
    const val TEST_MEDIA_ID = "testMediaId"
    const val TEST_ZONE_ID = "testZoneId"
    const val TEST_ERROR_CODE = 10700 // noFill
  }
}
