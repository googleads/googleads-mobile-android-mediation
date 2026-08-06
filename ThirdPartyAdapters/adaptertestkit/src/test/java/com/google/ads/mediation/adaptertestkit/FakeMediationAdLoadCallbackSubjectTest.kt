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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeMediationAdLoadCallbackSubjectTest {

  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val expectedError = AdError(101, "Error message", "com.google.ads.mediation")
  private val dummyAd =
    object : MediationBannerAd {
      override fun getView() = android.view.View(null)
    }

  @Test
  fun hasFailedWith_matchingAdError_succeeds() {
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    val actualError = AdError(101, "Error message", "com.google.ads.mediation")
    callback.onFailure(actualError)

    assertThat(callback).hasFailedWith(expectedError)
    assertThat(callback).hasFailed()
    assertThat(callback).hasNotSucceeded()
  }

  @Test
  fun hasFailedWith_matchingCodeAndDomain_succeeds() {
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    callback.onFailure(expectedError)

    assertThat(callback).hasFailedWith(101, "com.google.ads.mediation")
  }

  @Test
  fun hasFailedWith_matchingCodeMessageAndDomain_succeeds() {
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    callback.onFailure(expectedError)

    assertThat(callback).hasFailedWith(101, "Error message", "com.google.ads.mediation")
  }

  @Test
  fun hasFailedWith_mismatchingError_fails() {
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    val differentError = AdError(102, "Different message", "com.google.ads.mediation")
    callback.onFailure(differentError)

    assertThrows(AssertionError::class.java) { assertThat(callback).hasFailedWith(expectedError) }
    assertThrows(AssertionError::class.java) {
      assertThat(callback).hasFailedWith(101, "com.google.ads.mediation")
    }
    assertThrows(AssertionError::class.java) {
      assertThat(callback).hasFailedWith(101, "Error message", "com.google.ads.mediation")
    }
  }

  @Test
  fun hasFailedWith_whenSuccessInvoked_fails() {
    val callback =
      FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
    callback.onSuccess(dummyAd)

    assertThrows(AssertionError::class.java) { assertThat(callback).hasFailedWith(expectedError) }
    assertThrows(AssertionError::class.java) { assertThat(callback).hasFailed() }
  }

  @Test
  fun hasSucceeded_and_hasLoadedAd_succeeds() {
    val callback =
      FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
    callback.onSuccess(dummyAd)

    assertThat(callback).hasSucceeded()
    assertThat(callback).hasNotFailed()
    assertThat(callback).hasNoFailure()
    assertThat(callback).hasLoadedAd(dummyAd)
    assertThat(callback).hasSucceededWith(dummyAd)
  }

  @Test
  fun hasSucceeded_whenNotInvoked_fails() {
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()

    assertThrows(AssertionError::class.java) { assertThat(callback).hasSucceeded() }
  }

  @Test
  fun hasSucceededWith_differentAd_fails() {
    val callback =
      FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
    callback.onSuccess(dummyAd)

    val otherAd =
      object : MediationBannerAd {
        override fun getView() = android.view.View(null)
      }

    assertThrows(AssertionError::class.java) { assertThat(callback).hasSucceededWith(otherAd) }
  }

  @Test
  fun hasNotFailed_whenFailed_fails() {
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    callback.onFailure(expectedError)

    assertThrows(AssertionError::class.java) { assertThat(callback).hasNotFailed() }
    assertThrows(AssertionError::class.java) { assertThat(callback).hasNoFailure() }
  }
}
