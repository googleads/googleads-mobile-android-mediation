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
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_MY_TARGET_SDK
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_KEY
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_VALUE
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import com.my.target.ads.MyTargetView
import com.my.target.common.CustomParams
import com.my.target.common.models.IAdLoadingError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MyTargetBannerAdTest {
  // Subject of testing.
  private lateinit var myTargetBannerAd: MyTargetBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockBannerAdCallback: MediationBannerAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }
  private val mockMyTargetView: MyTargetView = mock()

  @Before
  fun setUp() {
    myTargetBannerAd = MyTargetBannerAd(mockAdLoadCallback)
  }

  @Test
  fun loadAd_withNoSpotId_invokesFailure() {
    val adConfiguration = createMediationBannerAdConfiguration(context = context)

    val expectedAdError =
      AdError(
        MyTargetMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Slot ID.",
        MyTargetMediationAdapter.ERROR_DOMAIN,
      )

    myTargetBannerAd.loadAd(adConfiguration)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_withInvalidAdSize_invokesFailure() {
    val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
    val badAdSize = AdSize(1, 2)
    val adConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        adSize = badAdSize,
        serverParameters = serverParameters,
      )

    val expectedAdError =
      AdError(
        MyTargetMediationAdapter.ERROR_BANNER_SIZE_MISMATCH,
        "Unsupported ad size: $badAdSize",
        MyTargetMediationAdapter.ERROR_DOMAIN,
      )

    myTargetBannerAd.loadAd(adConfiguration)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_withValidParameters_invokesLoadAd() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationBannerAdConfiguration(
          context = context,
          adSize = AdSize.BANNER,
          serverParameters = serverParameters,
        )
      val mockCustomParams: CustomParams = mock()
      whenever(MyTargetSdkWrapper.createBannerAd(eq(context))) doReturn mockMyTargetView
      whenever(mockMyTargetView.customParams) doReturn mockCustomParams

      myTargetBannerAd.loadAd(adConfiguration)

      verify(mockMyTargetView).setRefreshAd(false)
      verify(mockMyTargetView).customParams
      verify(mockCustomParams).setCustomParam(eq(PARAM_MEDIATION_KEY), eq(PARAM_MEDIATION_VALUE))
      verify(mockMyTargetView).listener = myTargetBannerAd
      verify(mockMyTargetView).load()
    }
  }

  @Test
  fun getView_returnsMyTargetView() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationBannerAdConfiguration(
          context = context,
          adSize = AdSize.BANNER,
          serverParameters = serverParameters,
        )
      val mockCustomParams: CustomParams = mock()
      whenever(MyTargetSdkWrapper.createBannerAd(eq(context))) doReturn mockMyTargetView
      whenever(mockMyTargetView.customParams) doReturn mockCustomParams

      myTargetBannerAd.loadAd(adConfiguration)

      assertThat(myTargetBannerAd.view).isEqualTo(mockMyTargetView)
    }
  }

  @Test
  fun onLoad_invokesSuccess() {
    myTargetBannerAd.onLoad(mockMyTargetView)

    verify(mockAdLoadCallback).onSuccess(myTargetBannerAd)
  }

  @Test
  fun onNoAd_invokesFailure() {
    val mockAdLoadError: IAdLoadingError = mock()
    whenever(mockAdLoadError.message) doReturn TEST_MYTARGET_ERROR_MESSAGE

    myTargetBannerAd.onNoAd(mockAdLoadError, mockMyTargetView)

    val expectedAdError =
      AdError(ERROR_MY_TARGET_SDK, TEST_MYTARGET_ERROR_MESSAGE, MY_TARGET_SDK_ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onShow_invokesNoCallback() {
    myTargetBannerAd.onLoad(mockMyTargetView)
    myTargetBannerAd.onShow(mockMyTargetView)

    verify(mockBannerAdCallback).reportAdImpression()
  }

  @Test
  fun onClick_invokesClickOpenedAndLeftApplication() {
    myTargetBannerAd.onLoad(mockMyTargetView)
    myTargetBannerAd.onClick(mockMyTargetView)

    verify(mockBannerAdCallback).reportAdClicked()
    verify(mockBannerAdCallback).onAdOpened()
    verify(mockBannerAdCallback).onAdLeftApplication()
  }

  private companion object {
    const val TEST_SLOT_ID = "1234"
    const val TEST_MYTARGET_ERROR_MESSAGE = "TEST_ERROR_MESSAGE"
  }
}
