// Copyright 2023 Google LLC
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

package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_ERROR_MESSAGE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.MINTEGRAL_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallAppOpenAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class MintegralWaterfallAppOpenAdTest {
  // Subject under testing
  private lateinit var mintegralAppOpenAd: MintegralWaterfallAppOpenAd

  private val activity = Robolectric.buildActivity(Activity::class.java).get()
  private val serverParameters =
    bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
  private val mockSplashAdWrapper: MintegralSplashAdWrapper = mock()
  private val mockAdCallback: MediationAppOpenAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockAdCallback
    }

  @Before
  fun setUp() {
    mintegralAppOpenAd = MintegralWaterfallAppOpenAd(mockAdLoadCallback)
  }

  @Test
  fun onLoadFailedWithCode_invokesOnFailureWithGivenAdError() {
    mintegralAppOpenAd.onLoadFailedWithCode(
      /*mBridgeIds=*/ null,
      /*code=*/ 2,
      TEST_ERROR_MESSAGE,
      /*reqType=*/ 3,
    )

    val expectedError = AdError(2, TEST_ERROR_MESSAGE, MINTEGRAL_SDK_ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun isSupportZoomOut_throwsNoException() {
    mintegralAppOpenAd.isSupportZoomOut(/* mBridgeIds= */ null, /* isSupported= */ true)
  }

  @Test
  fun onLoadSuccessed_invokesOnSuccess() {
    mintegralAppOpenAd.onLoadSuccessed(/* mBridgeIds= */ null, /* type= */ 1)

    verify(mockAdLoadCallback).onSuccess(mintegralAppOpenAd)
  }

  @Test
  fun onShowSuccessed_invokesOnAdOpenedAndReportAdImpression() {
    mintegralAppOpenAd.onLoadSuccessed(/* mBridgeIds= */ null, /* type= */ 1)

    mintegralAppOpenAd.onShowSuccessed(/* mBridgeIds= */ null)

    verify(mockAdCallback).onAdOpened()
    verify(mockAdCallback).reportAdImpression()
  }

  @Test
  fun onShowFailed_invokesonAdFailedToShow() {
    mintegralAppOpenAd.onLoadSuccessed(/* mBridgeIds= */ null, /* type= */ 1)

    mintegralAppOpenAd.onShowFailed(/* mBridgeIds= */ null, /* msg= */ TEST_ERROR_MESSAGE)

    val expectedError =
      AdError(
        MintegralConstants.ERROR_MINTEGRAL_SDK,
        TEST_ERROR_MESSAGE,
        MINTEGRAL_SDK_ERROR_DOMAIN,
      )
    verify(mockAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun showAd_invokesSplashAdShowWithLayout() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn mockSplashAdWrapper
      mintegralAppOpenAd.loadAd(
        createMediationAppOpenAdConfiguration(
          context = activity,
          serverParameters = serverParameters,
        )
      )

      mintegralAppOpenAd.showAd(activity)

      verify(mockSplashAdWrapper).show(any())
    }
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    mintegralAppOpenAd.onLoadSuccessed(/* mBridgeIds= */ null, /* type= */ 1)

    mintegralAppOpenAd.onAdClicked(/* mBridgeIds= */ null)

    verify(mockAdCallback).reportAdClicked()
  }

  @Test
  fun onDismiss_invokesOnAdClosedAndOnDestroy() {
    mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn mockSplashAdWrapper
      mintegralAppOpenAd.loadAd(
        createMediationAppOpenAdConfiguration(
          context = activity,
          serverParameters = serverParameters,
        )
      )
      mintegralAppOpenAd.onLoadSuccessed(/* mBridgeIds= */ null, /* type= */ 1)

      mintegralAppOpenAd.onDismiss(/* mBridgeIds= */ null, /* type= */ 1)

      verify(mockAdCallback).onAdClosed()
      verify(mockSplashAdWrapper).onDestroy()
    }
  }

  @Test
  fun onAdTick_throwsNoException() {
    mintegralAppOpenAd.onAdTick(/* mBridgeIds= */ null, /* millisUntilFinished= */ 1)
  }

  @Test
  fun onZoomOutPlayStart_throwsNoException() {
    mintegralAppOpenAd.onZoomOutPlayStart(/* mBridgeIds= */ null)
  }

  @Test
  fun onZoomOutPlayFinish_throwsNoException() {
    mintegralAppOpenAd.onZoomOutPlayFinish(/* mBridgeIds= */ null)
  }
}
