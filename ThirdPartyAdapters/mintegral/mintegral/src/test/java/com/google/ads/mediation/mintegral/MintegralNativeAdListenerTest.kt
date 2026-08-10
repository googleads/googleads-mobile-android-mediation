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

package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_ERROR_MESSAGE
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_CODE_NO_FILL
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.MINTEGRAL_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mintegral.mediation.MintegralNativeAd
import com.google.ads.mediation.mintegral.mediation.MintegralNativeAdListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.common.truth.Truth.assertThat
import com.mbridge.msdk.out.Campaign
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

/** Tests for [MintegralNativeAdListener]. */
@RunWith(AndroidJUnit4::class)
class MintegralNativeAdListenerTest {

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockNativeAdCallback: MediationNativeAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockNativeAdCallback
    }
  private val adConfiguration = createMediationNativeAdConfiguration(context = context)

  private lateinit var mintegralNativeAd: TestMintegralNativeAd
  private lateinit var listener: MintegralNativeAdListener

  @Before
  fun setUp() {
    mintegralNativeAd = TestMintegralNativeAd(adConfiguration, mockAdLoadCallback)
    listener = MintegralNativeAdListener(mintegralNativeAd, context, mockAdLoadCallback)
  }

  @Test
  fun onAdLoaded_withNullCampaignList_invokesOnFailure() {
    listener.onAdLoaded(null, 0)

    val expectedError =
      AdError(ERROR_CODE_NO_FILL, "Mintegral SDK failed to return a native ad.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun onAdLoaded_withEmptyCampaignList_invokesOnFailure() {
    listener.onAdLoaded(emptyList(), 0)

    val expectedError =
      AdError(ERROR_CODE_NO_FILL, "Mintegral SDK failed to return a native ad.", ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun onAdLoaded_withValidCampaignList_mapsNativeAdAndInvokesOnSuccess() {
    val mockCampaign = mock<Campaign> { on { appName } doReturn "Test App Name" }

    listener.onAdLoaded(listOf(mockCampaign), 0)

    verify(mockAdLoadCallback).onSuccess(mintegralNativeAd)
    assertThat(mintegralNativeAd.headline).isEqualTo("Test App Name")
  }

  @Test
  fun onAdLoadErrorWithCode_invokesOnFailureWithGivenAdError() {
    listener.onAdLoadErrorWithCode(123, TEST_ERROR_MESSAGE)

    val expectedError = AdError(123, TEST_ERROR_MESSAGE, MINTEGRAL_SDK_ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun onAdClick_withNativeCallback_invokesReportAdClickedAndOnAdLeftApplication() {
    val mockCampaign = mock<Campaign>()
    listener.onAdLoaded(listOf(mockCampaign), 0)

    listener.onAdClick(mockCampaign)

    verify(mockNativeAdCallback).reportAdClicked()
    verify(mockNativeAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdClick_withoutNativeCallback_throwsNoException() {
    listener.onAdClick(mock())
  }

  @Test
  fun onLoggingImpression_withNativeCallback_invokesReportAdImpression() {
    val mockCampaign = mock<Campaign>()
    listener.onAdLoaded(listOf(mockCampaign), 0)

    listener.onLoggingImpression(0)

    verify(mockNativeAdCallback).reportAdImpression()
  }

  @Test
  fun onLoggingImpression_withoutNativeCallback_throwsNoException() {
    listener.onLoggingImpression(0)
  }

  @Test
  fun onAdFramesLoaded_throwsNoException() {
    listener.onAdFramesLoaded(listOf())
  }

  private class TestMintegralNativeAd(
    adConfiguration: MediationNativeAdConfiguration,
    adLoadCallback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  ) : MintegralNativeAd(adConfiguration, adLoadCallback) {
    override fun loadAd(adConfiguration: MediationNativeAdConfiguration?) {}
  }
}
