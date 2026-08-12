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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_EMPTY_NATIVE_ADS_LIST
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.FailNotificationReason
import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

/** Tests for [NativeAdDataListener]. */
@RunWith(AndroidJUnit4::class)
class NativeAdDataListenerTest {

  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val adLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val nativeAdDataListener = NativeAdDataListener(adLoadCallback, activity)

  @Test
  fun onNativeAdDataReciveCompleted_ifAdDataListIsNull_invokesLoadFailure() {
    nativeAdDataListener.onNativeAdDataReciveCompleted(null)

    assertThat(adLoadCallback)
      .hasFailedWith(ERROR_EMPTY_NATIVE_ADS_LIST, IMobileMediationAdapter.ERROR_DOMAIN)
  }

  @Test
  fun onNativeAdDataReciveCompleted_ifAdDataListIsEmpty_invokesLoadFailure() {
    nativeAdDataListener.onNativeAdDataReciveCompleted(mutableListOf())

    assertThat(adLoadCallback)
      .hasFailedWith(ERROR_EMPTY_NATIVE_ADS_LIST, IMobileMediationAdapter.ERROR_DOMAIN)
  }

  @Test
  fun onNativeAdDataReciveCompleted_ifAdDataListIsNotEmpty_getsAdImage() {
    val adData: ImobileSdkAdsNativeAdData = mock()

    nativeAdDataListener.onNativeAdDataReciveCompleted(mutableListOf(adData))

    verify(adData).getAdImage(eq(activity), any())
  }

  @Test
  fun onFailed_invokesLoadFailure() {
    val expectedError = AdapterHelper.getAdError(FailNotificationReason.RESPONSE)

    nativeAdDataListener.onFailed(FailNotificationReason.RESPONSE)

    assertThat(adLoadCallback).hasFailedWith(expectedError)
  }
}
