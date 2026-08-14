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

package com.google.ads.mediation.mytarget

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_MY_TARGET_SDK
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_KEY
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_VALUE
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import com.my.target.common.CustomParams
import com.my.target.common.models.IAdLoadingError
import com.my.target.nativeads.NativeAd
import com.my.target.nativeads.banners.NativePromoBanner
import kotlin.use
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MyTargetNativeAdTest {
  // Subject of testing.
  private lateinit var myTargetNativeAd: MyTargetNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val adLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val mockMyTargetNativeAd: NativeAd = mock()

  @Before
  fun setUp() {
    myTargetNativeAd = MyTargetNativeAd(adLoadCallback)
  }

  @Test
  fun loadAd_withNoSpotId_invokesFailure() {
    val adConfiguration = createMediationNativeAdConfiguration(context = context)

    val expectedAdError =
      AdError(
        MyTargetMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Slot ID.",
        MyTargetMediationAdapter.ERROR_DOMAIN,
      )

    myTargetNativeAd.loadAd(adConfiguration)
    assertThat(adLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadAd_withValidParameters_invokesLoadAd() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
      whenever(MyTargetSdkWrapper.createNativeAd(eq(TEST_SLOT_ID.toInt()), eq(context))) doReturn
        mockMyTargetNativeAd

      val mockCustomParams: CustomParams = mock()
      whenever(mockMyTargetNativeAd.customParams) doReturn mockCustomParams

      myTargetNativeAd.loadAd(adConfiguration)

      verify(mockMyTargetNativeAd).customParams
      verify(mockCustomParams).setCustomParam(eq(PARAM_MEDIATION_KEY), eq(PARAM_MEDIATION_VALUE))
      verify(mockMyTargetNativeAd).listener = myTargetNativeAd
      verify(mockMyTargetNativeAd).load()
    }
  }

  @Test
  fun onLoad_withMismatchingNativeAd_invokesFailure() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
      whenever(MyTargetSdkWrapper.createNativeAd(eq(TEST_SLOT_ID.toInt()), eq(context))) doReturn
        mockMyTargetNativeAd

      val mockCustomParams: CustomParams = mock()
      whenever(mockMyTargetNativeAd.customParams) doReturn mockCustomParams

      myTargetNativeAd.loadAd(adConfiguration)
      myTargetNativeAd.onLoad(mock(), mock())

      val expectedAdError =
        AdError(
          MyTargetMediationAdapter.ERROR_INVALID_NATIVE_AD_LOADED,
          "Loaded native ad object does not match the requested ad object.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      assertThat(adLoadCallback).hasFailedWith(expectedAdError)
    }
  }

  @Test
  fun onLoad_withNoBannerAssets_invokesFailure() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
      whenever(MyTargetSdkWrapper.createNativeAd(eq(TEST_SLOT_ID.toInt()), eq(context))) doReturn
        mockMyTargetNativeAd

      val mockCustomParams: CustomParams = mock()
      whenever(mockMyTargetNativeAd.customParams) doReturn mockCustomParams

      myTargetNativeAd.loadAd(adConfiguration)
      myTargetNativeAd.onLoad(mock(), mockMyTargetNativeAd)

      val expectedAdError =
        AdError(
          MyTargetMediationAdapter.ERROR_MISSING_REQUIRED_NATIVE_ASSET,
          "Native ad is missing one of the following required assets: image or icon.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      assertThat(adLoadCallback).hasFailedWith(expectedAdError)
    }
  }

  @Test
  fun onLoad_withMissingBannerIconAsset_invokesFailure() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
      whenever(MyTargetSdkWrapper.createNativeAd(eq(TEST_SLOT_ID.toInt()), eq(context))) doReturn
        mockMyTargetNativeAd

      val mockCustomParams: CustomParams = mock()
      whenever(mockMyTargetNativeAd.customParams) doReturn mockCustomParams

      myTargetNativeAd.loadAd(adConfiguration)
      val mockNativePromoBanner: NativePromoBanner = mock()
      whenever(mockNativePromoBanner.image) doReturn mock()
      myTargetNativeAd.onLoad(mockNativePromoBanner, mockMyTargetNativeAd)

      val expectedAdError =
        AdError(
          MyTargetMediationAdapter.ERROR_MISSING_REQUIRED_NATIVE_ASSET,
          "Native ad is missing one of the following required assets: image or icon.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      assertThat(adLoadCallback).hasFailedWith(expectedAdError)
    }
  }

  @Test
  fun onLoad_withMissingBannerImageAsset_invokesFailure() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
      whenever(MyTargetSdkWrapper.createNativeAd(eq(TEST_SLOT_ID.toInt()), eq(context))) doReturn
        mockMyTargetNativeAd

      val mockCustomParams: CustomParams = mock()
      whenever(mockMyTargetNativeAd.customParams) doReturn mockCustomParams

      myTargetNativeAd.loadAd(adConfiguration)
      val mockNativePromoBanner: NativePromoBanner = mock()
      whenever(mockNativePromoBanner.icon) doReturn mock()
      myTargetNativeAd.onLoad(mockNativePromoBanner, mockMyTargetNativeAd)

      val expectedAdError =
        AdError(
          MyTargetMediationAdapter.ERROR_MISSING_REQUIRED_NATIVE_ASSET,
          "Native ad is missing one of the following required assets: image or icon.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      assertThat(adLoadCallback).hasFailedWith(expectedAdError)
    }
  }

  @Test
  fun onLoad_withValidAssets_invokesSuccess() {
    loadNativeAdSuccessfully()

    assertThat(adLoadCallback).hasSucceededWith(myTargetNativeAd)
  }

  @Test
  fun onNoAd_invokesFailure() {
    val mockAdLoadError: IAdLoadingError = mock()
    whenever(mockAdLoadError.message) doReturn TEST_MYTARGET_ERROR_MESSAGE

    myTargetNativeAd.onNoAd(mockAdLoadError, mockMyTargetNativeAd)

    val expectedAdError =
      AdError(ERROR_MY_TARGET_SDK, TEST_MYTARGET_ERROR_MESSAGE, MY_TARGET_SDK_ERROR_DOMAIN)
    assertThat(adLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onShow_invokesReportImpression() {
    loadNativeAdSuccessfully()
    myTargetNativeAd.onShow(mockMyTargetNativeAd)

    assertThat(nativeAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onClick_invokesAdClickedOpenedAndLeftApplication() {
    loadNativeAdSuccessfully()
    myTargetNativeAd.onClick(mock(), mockMyTargetNativeAd)

    assertThat(nativeAdCallback.isClicked).isTrue()
    assertThat(nativeAdCallback.isOpened).isTrue()
    assertThat(nativeAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onClick_withDeprecatedCallback_invokesAdClickedOpenedAndLeftApplication() {
    loadNativeAdSuccessfully()
    myTargetNativeAd.onClick(mockMyTargetNativeAd)

    assertThat(nativeAdCallback.isClicked).isTrue()
    assertThat(nativeAdCallback.isOpened).isTrue()
    assertThat(nativeAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onVideoPlay_invokesOnVideoPlay() {
    loadNativeAdSuccessfully()
    myTargetNativeAd.onVideoPlay(mockMyTargetNativeAd)

    assertThat(nativeAdCallback.isVideoPlaying).isTrue()
  }

  @Test
  fun onVideoPause_invokesOnVideoPause() {
    loadNativeAdSuccessfully()
    myTargetNativeAd.onVideoPause(mockMyTargetNativeAd)

    assertThat(nativeAdCallback.isVideoPaused).isTrue()
  }

  @Test
  fun onVideoComplete_invokesOnVideoComplete() {
    loadNativeAdSuccessfully()
    myTargetNativeAd.onVideoComplete(mockMyTargetNativeAd)

    assertThat(nativeAdCallback.isVideoCompleted).isTrue()
  }

  // region Utility methods
  fun loadNativeAdSuccessfully() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationNativeAdConfiguration(context = context, serverParameters = serverParameters)
      whenever(MyTargetSdkWrapper.createNativeAd(eq(TEST_SLOT_ID.toInt()), eq(context))) doReturn
        mockMyTargetNativeAd

      val mockCustomParams: CustomParams = mock()
      whenever(mockMyTargetNativeAd.customParams) doReturn mockCustomParams

      myTargetNativeAd.loadAd(adConfiguration)

      val mockNativePromoBanner: NativePromoBanner = mock()
      whenever(mockNativePromoBanner.icon) doReturn mock()
      whenever(mockNativePromoBanner.image) doReturn mock()
      myTargetNativeAd.onLoad(mockNativePromoBanner, mockMyTargetNativeAd)
    }
  }

  // endregion

  private companion object {
    const val TEST_SLOT_ID = "1234"
    const val TEST_MYTARGET_ERROR_MESSAGE = "TEST_ERROR_MESSAGE"
  }
}
