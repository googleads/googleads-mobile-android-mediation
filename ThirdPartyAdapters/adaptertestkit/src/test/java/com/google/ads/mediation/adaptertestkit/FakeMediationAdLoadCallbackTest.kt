// Copyright 2024 Google LLC
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

package com.google.ads.mediation.adaptertestkit

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeMediationAdLoadCallbackTest {

  @Test
  fun initialState_allValuesDefault() {
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()

    assertThat(callback.isSuccessInvoked).isFalse()
    assertThat(callback.isSuccess).isFalse()
    assertThat(callback.successInvokeCount).isEqualTo(0)
    assertThat(callback.isFailureInvoked).isFalse()
    assertThat(callback.isFailure).isFalse()
    assertThat(callback.failureInvokeCount).isEqualTo(0)
    assertThat(callback.loadedAd).isNull()
    assertThat(callback.adError).isNull()
    assertThat(callback.error).isNull()
    assertThat(callback.customCallback).isNull()
    assertThat(callback.adCallback).isNull()
  }

  @Test
  fun onSuccess_withCustomCallback_recordsSuccessAndReturnsCallback() {
    val bannerAdCallback = FakeMediationBannerAdCallback()
    val loadCallback =
      FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
    val bannerAd =
      object : MediationBannerAd {
        override fun getView() = android.view.View(null)
      }

    val resultCallback = loadCallback.onSuccess(bannerAd)

    assertThat(loadCallback.isSuccessInvoked).isTrue()
    assertThat(loadCallback.isSuccess).isTrue()
    assertThat(loadCallback.successInvokeCount).isEqualTo(1)
    assertThat(loadCallback.loadedAd).isSameInstanceAs(bannerAd)
    assertThat(resultCallback).isSameInstanceAs(bannerAdCallback)
  }

  @Test
  fun onSuccess_withoutCustomCallback_throwsIllegalStateException() {
    val loadCallback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    val bannerAd =
      object : MediationBannerAd {
        override fun getView() = android.view.View(null)
      }

    val exception =
      assertThrows(IllegalStateException::class.java) { loadCallback.onSuccess(bannerAd) }

    assertThat(exception).hasMessageThat().contains("customCallback was not provided")
  }

  @Test
  fun onFailure_recordsFailureAndAdError() {
    val loadCallback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    val error = AdError(201, "Banner load failure", "com.google.ads.mediation")

    loadCallback.onFailure(error)

    assertThat(loadCallback.isFailureInvoked).isTrue()
    assertThat(loadCallback.isFailure).isTrue()
    assertThat(loadCallback.failureInvokeCount).isEqualTo(1)
    assertThat(loadCallback.adError).isEqualTo(error)
    assertThat(loadCallback.error).isEqualTo(error)
  }
}
