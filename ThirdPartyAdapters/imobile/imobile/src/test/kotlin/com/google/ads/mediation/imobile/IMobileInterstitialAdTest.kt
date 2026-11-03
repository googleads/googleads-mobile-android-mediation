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

package com.google.ads.mediation.imobile

import android.app.Activity
import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.imobile.Constants.KEY_MEDIA_ID
import com.google.ads.mediation.imobile.Constants.KEY_PUBLISHER_ID
import com.google.ads.mediation.imobile.Constants.KEY_SPOT_ID
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.FailNotificationReason
import jp.co.imobile.sdkads.android.ImobileSdkAdListener
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for the APIs implemented by [IMobileInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class IMobileInterstitialAdTest {

  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()

  private val adLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )

  private val iMobileSdkWrapper: IMobileSdkWrapper = mock()

  private val iMobileInterstitialAd = IMobileInterstitialAd(adLoadCallback, iMobileSdkWrapper)

  @Test
  fun loadAd_ifContextIsNotActivity_invokesOnFailure() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val adConfig = createMediationInterstitialAdConfiguration(context = context)

    iMobileInterstitialAd.loadAd(adConfig)

    assertThat(adLoadCallback).hasFailedWith(ERROR_REQUIRES_ACTIVITY_CONTEXT, ERROR_DOMAIN)
    verify(iMobileSdkWrapper, never()).registerSpotFullScreen(any(), any(), any(), any())
  }

  @Test
  fun loadAd_whenAdNotReady_registersSpotWithSelfAsListenerAndStarts() {
    val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
    val adConfig =
      createMediationInterstitialAdConfiguration(
        context = activity,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to PUBLISHER_ID,
            KEY_MEDIA_ID to MEDIA_ID,
            KEY_SPOT_ID to SPOT_ID,
          ),
      )

    iMobileInterstitialAd.loadAd(adConfig)

    verify(iMobileSdkWrapper).registerSpotFullScreen(activity, PUBLISHER_ID, MEDIA_ID, SPOT_ID)
    val listenerCaptor = argumentCaptor<ImobileSdkAdListener>()
    verify(iMobileSdkWrapper).setImobileSdkAdListener(eq(SPOT_ID), listenerCaptor.capture())
    assertThat(listenerCaptor.firstValue).isSameInstanceAs(iMobileInterstitialAd)
    verify(iMobileSdkWrapper).start(SPOT_ID)

    listenerCaptor.firstValue.onAdReadyCompleted()
    assertThat(adLoadCallback).hasSucceededWith(iMobileInterstitialAd)
  }

  @Test
  fun loadAd_whenAdAlreadyReady_registersSelfAsListenerAndCallsOnSuccess() {
    val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
    val adConfig =
      createMediationInterstitialAdConfiguration(
        context = activity,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to PUBLISHER_ID,
            KEY_MEDIA_ID to MEDIA_ID,
            KEY_SPOT_ID to SPOT_ID,
          ),
      )
    whenever(iMobileSdkWrapper.isShowAd(SPOT_ID)) doReturn true

    iMobileInterstitialAd.loadAd(adConfig)

    verify(iMobileSdkWrapper).registerSpotFullScreen(activity, PUBLISHER_ID, MEDIA_ID, SPOT_ID)
    verify(iMobileSdkWrapper).setImobileSdkAdListener(SPOT_ID, iMobileInterstitialAd)
    verify(iMobileSdkWrapper, never()).start(any())
    assertThat(adLoadCallback).hasSucceededWith(iMobileInterstitialAd)

    iMobileInterstitialAd.onAdShowCompleted()
    assertThat(interstitialAdCallback.isOpened).isTrue()
  }

  // region ImobileSdkAdListener implementation tests
  @Test
  fun onAdReadyCompleted_callsLoadSuccessCallback() {
    iMobileInterstitialAd.onAdReadyCompleted()

    assertThat(adLoadCallback).hasSucceededWith(iMobileInterstitialAd)
  }

  @Test
  fun onAdShowCompleted_callsOnAdOpened() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdShowCompleted()

    assertThat(interstitialAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdCliclkCompleted_reportsAdClickedAndAdOpenedAndAdLeftApplication() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdCliclkCompleted()

    assertThat(interstitialAdCallback.isClicked).isTrue()
    assertThat(interstitialAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onAdCloseCompleted_callsOnAdClosed() {
    // Call onAdReadyCompleted to set iMobileInterstitialAd.InterstitialAdCallback
    iMobileInterstitialAd.onAdReadyCompleted()

    iMobileInterstitialAd.onAdCloseCompleted()

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onFailed_callsLoadFailureCallback() {
    val expectedError = AdapterHelper.getAdError(FailNotificationReason.RESPONSE)

    iMobileInterstitialAd.onFailed(FailNotificationReason.RESPONSE)

    assertThat(adLoadCallback).hasFailedWith(expectedError)
  }

  // endregion

  private companion object {
    const val PUBLISHER_ID = "a_publisher_id"
    const val MEDIA_ID = "a_media_id"
    const val SPOT_ID = "a_spot_id"
  }
}
