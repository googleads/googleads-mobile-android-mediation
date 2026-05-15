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
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_AD_FAILED_TO_SHOW
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_MSG_AD_FAILED_TO_SHOW
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_MY_TARGET_SDK
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_KEY
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_VALUE
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.my.target.ads.InterstitialAd
import com.my.target.common.CustomParams
import com.my.target.common.models.IAdLoadingError
import kotlin.use
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MyTargetInterstitialAdTest {
  // Subject of testing.
  private lateinit var myTargetInterstitialAd: MyTargetInterstitialAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }
  private val mockMyTargetInterstitialAd: InterstitialAd = mock()

  @Before
  fun setUp() {
    myTargetInterstitialAd = MyTargetInterstitialAd(mockAdLoadCallback)
  }

  @Test
  fun loadAd_withNoSpotId_invokesFailure() {
    val adConfiguration = createMediationInterstitialAdConfiguration(context = context)

    val expectedAdError =
      AdError(
        MyTargetMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
        "Missing or invalid Slot ID.",
        ERROR_DOMAIN,
      )

    myTargetInterstitialAd.loadAd(adConfiguration)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_withValidParameters_invokesLoadAd() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )
      val mockCustomParams: CustomParams = mock()
      whenever(
        MyTargetSdkWrapper.createInterstitialAd(eq(TEST_SLOT_ID.toInt()), eq(context))
      ) doReturn mockMyTargetInterstitialAd
      whenever(mockMyTargetInterstitialAd.customParams) doReturn mockCustomParams

      myTargetInterstitialAd.loadAd(adConfiguration)

      verify(mockMyTargetInterstitialAd).customParams
      verify(mockCustomParams).setCustomParam(eq(PARAM_MEDIATION_KEY), eq(PARAM_MEDIATION_VALUE))
      verify(mockMyTargetInterstitialAd).listener = myTargetInterstitialAd
      verify(mockMyTargetInterstitialAd).load()
    }
  }

  @Test
  fun showAd_invokesShowAd() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val serverParameters = bundleOf(MyTargetTools.KEY_SLOT_ID to TEST_SLOT_ID)
      val adConfiguration =
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters = serverParameters,
        )
      val mockCustomParams: CustomParams = mock()
      whenever(
        MyTargetSdkWrapper.createInterstitialAd(eq(TEST_SLOT_ID.toInt()), eq(context))
      ) doReturn mockMyTargetInterstitialAd
      whenever(mockMyTargetInterstitialAd.customParams) doReturn mockCustomParams

      myTargetInterstitialAd.loadAd(adConfiguration)
      myTargetInterstitialAd.showAd(context)

      verify(mockMyTargetInterstitialAd).show()
    }
  }

  @Test
  fun onLoad_invokesOnSuccess() {
    myTargetInterstitialAd.onLoad(mockMyTargetInterstitialAd)

    verify(mockAdLoadCallback).onSuccess(myTargetInterstitialAd)
  }

  @Test
  fun onNoAd_invokesFailure() {
    val mockAdLoadError: IAdLoadingError = mock()
    whenever(mockAdLoadError.message) doReturn TEST_MYTARGET_ERROR_MESSAGE

    myTargetInterstitialAd.onNoAd(mockAdLoadError, mockMyTargetInterstitialAd)

    val expectedAdError =
      AdError(ERROR_MY_TARGET_SDK, TEST_MYTARGET_ERROR_MESSAGE, MY_TARGET_SDK_ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onDisplay_invokesOpenedAndAdImpression() {
    myTargetInterstitialAd.onLoad(mockMyTargetInterstitialAd)
    myTargetInterstitialAd.onDisplay(mockMyTargetInterstitialAd)

    verify(mockInterstitialAdCallback).onAdOpened()
    verify(mockInterstitialAdCallback).reportAdImpression()
  }

  @Test
  fun onFailedToShow_invokesOnFailedToShow() {
    myTargetInterstitialAd.onLoad(mockMyTargetInterstitialAd)
    myTargetInterstitialAd.onFailedToShow(mockMyTargetInterstitialAd)

    val expectedAdError =
      AdError(ERROR_AD_FAILED_TO_SHOW, ERROR_MSG_AD_FAILED_TO_SHOW, ERROR_DOMAIN)
    verify(mockInterstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onClick_invokesClickAndLeftApplication() {
    myTargetInterstitialAd.onLoad(mockMyTargetInterstitialAd)
    myTargetInterstitialAd.onClick(mockMyTargetInterstitialAd)

    verify(mockInterstitialAdCallback).reportAdClicked()
    verify(mockInterstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun onDismiss_invokesAdClosed() {
    myTargetInterstitialAd.onLoad(mockMyTargetInterstitialAd)
    myTargetInterstitialAd.onDismiss(mockMyTargetInterstitialAd)

    verify(mockInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun onVideoCompleted_invokesNoCallbacks() {
    myTargetInterstitialAd.onLoad(mockMyTargetInterstitialAd)
    myTargetInterstitialAd.onVideoCompleted(mockMyTargetInterstitialAd)

    verifyNoInteractions(mockInterstitialAdCallback)
  }

  private companion object {
    const val TEST_SLOT_ID = "1234"
    const val TEST_MYTARGET_ERROR_MESSAGE = "TEST_ERROR_MESSAGE"
  }
}
