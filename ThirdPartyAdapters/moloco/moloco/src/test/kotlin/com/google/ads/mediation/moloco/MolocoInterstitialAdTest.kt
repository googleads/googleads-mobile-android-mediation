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

package com.google.ads.mediation.moloco

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.moloco.sdk.publisher.CreateInterstitialAdCallback
import com.moloco.sdk.publisher.InterstitialAd
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MolocoInterstitialAdTest {
  // Subject of tests
  private lateinit var molocoInterstitialAd: MolocoInterstitialAd
  private lateinit var mediationAdConfiguration: MediationInterstitialAdConfiguration

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInterstitialAd = mock<InterstitialAd>()
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mockMediationAdCallback = mock<MediationInterstitialAdCallback>()

  @Before
  fun setUp() {
    // Properly initialize molocoInterstitialAd
    mediationAdConfiguration = createMediationInterstitialAdConfiguration()
    MolocoInterstitialAd.newInstance(mediationAdConfiguration, mockMediationAdLoadCallback)
      .onSuccess { molocoInterstitialAd = it }
    whenever(mockMediationAdLoadCallback.onSuccess(molocoInterstitialAd)) doReturn
      mockMediationAdCallback
  }

  @Test
  fun showAd_invokesMolocoShow() {
    loadInterstitialAd()

    molocoInterstitialAd.showAd(context)

    verify(mockInterstitialAd).show(molocoInterstitialAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val testError =
      MolocoAdError("testNetwork", "testAdUnit", MolocoAdError.ErrorType.UNKNOWN, "testDesc")
    val expectedAdError =
      AdError(
        MolocoAdError.ErrorType.UNKNOWN.errorCode,
        MolocoAdError.ErrorType.UNKNOWN.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoInterstitialAd.onAdLoadFailed(testError)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdLoadSuccess_invokesOnSuccess() {
    molocoInterstitialAd.onAdLoadSuccess(mock())

    verify(mockMediationAdLoadCallback).onSuccess(molocoInterstitialAd)
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    molocoInterstitialAd.onAdLoadSuccess(mock())

    molocoInterstitialAd.onAdClicked(mock())

    verify(mockMediationAdCallback).reportAdClicked()
  }

  @Test
  fun onAdHidden_invokesOnAdClosed() {
    molocoInterstitialAd.onAdLoadSuccess(mock())

    molocoInterstitialAd.onAdHidden(mock())

    verify(mockMediationAdCallback).onAdClosed()
  }

  @Test
  fun onAdShowFailed_invokesOnAdFailedToShow() {
    molocoInterstitialAd.onAdLoadSuccess(mock())
    val testError =
      MolocoAdError("testNetwork", "testAdUnit", MolocoAdError.ErrorType.UNKNOWN, "testDesc")
    val expectedAdError =
      AdError(
        MolocoAdError.ErrorType.UNKNOWN.errorCode,
        MolocoAdError.ErrorType.UNKNOWN.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoInterstitialAd.onAdShowFailed(testError)

    verify(mockMediationAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdShowSuccess_invokesOnAdOpenedAndReportAdImpression() {
    molocoInterstitialAd.onAdLoadSuccess(mock())

    molocoInterstitialAd.onAdShowSuccess(mock())

    verify(mockMediationAdCallback).onAdOpened()
    verify(mockMediationAdCallback).reportAdImpression()
  }

  private fun loadInterstitialAd() {
    mockStatic(Moloco::class.java).use { mockedMoloco ->
      molocoInterstitialAd.loadAd()
      val createInterstitialCaptor = argumentCaptor<CreateInterstitialAdCallback>()
      mockedMoloco.verify {
        Moloco.createInterstitial(
          eq(TEST_AD_UNIT),
          eq(TEST_WATERMARK),
          createInterstitialCaptor.capture(),
        )
      }
      val capturedCallback = createInterstitialCaptor.firstValue
      capturedCallback.invoke(mockInterstitialAd, /* error= */ null)
    }
  }

  private fun createMediationInterstitialAdConfiguration(): MediationInterstitialAdConfiguration {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
    return MediationInterstitialAdConfiguration(
      context,
      TEST_BID_RESPONSE,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
