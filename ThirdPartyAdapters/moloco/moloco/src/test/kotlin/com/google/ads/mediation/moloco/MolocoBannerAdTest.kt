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
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.MEDIATION_PLATFORM_NAME
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.BannerAdSize
import com.moloco.sdk.publisher.CreateBannerCallback
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.MolocoAdError.AdCreateError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MolocoBannerAdTest {
  // Subject of tests
  private lateinit var molocoBannerAd: MolocoBannerAd
  private lateinit var mediationAdConfiguration: MediationBannerAdConfiguration

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockBannerAd = mock<Banner>()
  private val mockSdkFactory = mock<SdkFactory>()
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val mockMediationAdCallback = mock<MediationBannerAdCallback>()

  @Before
  fun setUp() {
    MolocoSdkFactory.delegate = mockSdkFactory

    // Properly initialize molocoBannerAd
    mediationAdConfiguration = createMediationBannerAdConfiguration()
    MolocoBannerAd.newInstance(mediationAdConfiguration, mockMediationAdLoadCallback).onSuccess {
      molocoBannerAd = it
    }
    whenever(mockMediationAdLoadCallback.onSuccess(molocoBannerAd)) doReturn mockMediationAdCallback
  }

  // region newInstance Tests
  @Test
  fun newInstance_withValidParameters_returnsSuccess() {
    val config = createMediationBannerAdConfiguration()

    val result = MolocoBannerAd.newInstance(config, mockMediationAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun newInstance_withMissingAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf()
    val config = createMediationBannerAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = MolocoBannerAd.newInstance(config, mockMediationAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    verify(mockMediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT)
    assertThat(capturedError.message).isEqualTo(MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT)
    assertThat(capturedError.domain).isEqualTo(MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun newInstance_withEmptyAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val config = createMediationBannerAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = MolocoBannerAd.newInstance(config, mockMediationAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    verify(mockMediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT)
    assertThat(capturedError.message).isEqualTo(MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT)
    assertThat(capturedError.domain).isEqualTo(MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  // endregion

  // region loadAd Tests
  @Test
  fun loadAd_createsBannerWithCorrectParameters() {
    molocoBannerAd.loadAd(context)

    verify(mockSdkFactory)
      .createBanner(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        any(),
      )
  }

  @Test
  fun loadAd_whenAdCreationFailsWithMolocoError_invokesOnFailure() {
    val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

    molocoBannerAd.loadAd(context)

    verify(mockSdkFactory)
      .createBanner(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        createBannerCaptor.capture(),
      )
    val capturedCallback = createBannerCaptor.firstValue
    val molocoError = AdCreateError.SDK_INIT_FAILED
    capturedCallback.invoke(/* banner= */ null, molocoError)

    val expectedAdError =
      AdError(
        AdCreateError.SDK_INIT_FAILED.errorCode,
        AdCreateError.SDK_INIT_FAILED.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenAdIsNullAndNoErrorReported_invokesOnFailure() {
    val createBannerCaptor = argumentCaptor<CreateBannerCallback>()

    molocoBannerAd.loadAd(context)

    verify(mockSdkFactory)
      .createBanner(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        createBannerCaptor.capture(),
      )
    val capturedCallback = createBannerCaptor.firstValue
    capturedCallback.invoke(/* banner= */ null, /* molocoError= */ null)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
        MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenAdCreated_setsShowListenerAndLoadsBidResponse() {
    loadBannerAd()

    verify(mockBannerAd).adShowListener = molocoBannerAd
    verify(mockBannerAd).load(eq(TEST_BID_RESPONSE), eq(molocoBannerAd))
  }

  @Test
  fun getView_returnsBannerAd() {
    loadBannerAd()

    val view = molocoBannerAd.view

    assertThat(view).isEqualTo(mockBannerAd)
  }

  // endregion

  // region resolveBannerAdSize Tests
  @Test
  fun resolveBannerAdSize_forBanner_returnsStandard() {
    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, AdSize.BANNER)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.Standard)
  }

  @Test
  fun resolveBannerAdSize_forMediumRectangle_returnsMREC() {
    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, AdSize.MEDIUM_RECTANGLE)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.MREC)
  }

  @Test
  fun resolveBannerAdSize_forLeaderboard_returnsTablet() {
    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, AdSize.LEADERBOARD)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.Tablet)
  }

  @Test
  fun resolveBannerAdSize_forInlineAdaptive_returnsInlineAdaptive() {
    val inlineSize = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, 320)

    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, inlineSize)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.InlineAdaptive(availableWidth = 320))
  }

  @Test
  fun resolveBannerAdSize_forCurrentOrientationAnchoredAdaptive_returnsAnchoredAdaptive() {
    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 320)

    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, adaptiveSize)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.AnchoredAdaptive(availableWidth = 320))
  }

  @Test
  fun resolveBannerAdSize_forPortraitAnchoredAdaptive_returnsAnchoredAdaptive() {
    val adaptiveSize = AdSize.getPortraitAnchoredAdaptiveBannerAdSize(context, 320)

    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, adaptiveSize)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.AnchoredAdaptive(availableWidth = 320))
  }

  @Test
  fun resolveBannerAdSize_forLandscapeAnchoredAdaptive_returnsAnchoredAdaptive() {
    val adaptiveSize = AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(context, 320)

    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, adaptiveSize)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.AnchoredAdaptive(availableWidth = 320))
  }

  @Test
  fun resolveBannerAdSize_forCustomSize_returnsInlineAdaptiveFallback() {
    val customSize = AdSize(123, 456)

    val resolvedSize = MolocoBannerAd.resolveBannerAdSize(context, customSize)

    assertThat(resolvedSize).isEqualTo(BannerAdSize.InlineAdaptive(availableWidth = 123))
  }

  // endregion

  // region Callback Tests
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

    molocoBannerAd.onAdLoadFailed(testError)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdLoadSuccess_invokesOnSuccess() {
    molocoBannerAd.onAdLoadSuccess(mock())

    verify(mockMediationAdLoadCallback).onSuccess(molocoBannerAd)
  }

  @Test
  fun onAdClicked_invokesReportAdClickedAndOnAdLeftApplication() {
    molocoBannerAd.onAdLoadSuccess(mock())

    molocoBannerAd.onAdClicked(mock())

    verify(mockMediationAdCallback).reportAdClicked()
    verify(mockMediationAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdHidden_invokesOnAdClosed() {
    molocoBannerAd.onAdLoadSuccess(mock())

    molocoBannerAd.onAdHidden(mock())

    verify(mockMediationAdCallback).onAdClosed()
  }

  @Test
  fun onAdShowFailed_invokesOnFailure() {
    molocoBannerAd.onAdLoadSuccess(mock())
    val testError =
      MolocoAdError("testNetwork", "testAdUnit", MolocoAdError.ErrorType.UNKNOWN, "testDesc")
    val expectedAdError =
      AdError(
        MolocoAdError.ErrorType.UNKNOWN.errorCode,
        MolocoAdError.ErrorType.UNKNOWN.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoBannerAd.onAdShowFailed(testError)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdShowSuccess_invokesOnAdOpenedAndReportAdImpression() {
    molocoBannerAd.onAdLoadSuccess(mock())

    molocoBannerAd.onAdShowSuccess(mock())

    verify(mockMediationAdCallback).onAdOpened()
    verify(mockMediationAdCallback).reportAdImpression()
  }

  // endregion

  private fun loadBannerAd() {
    molocoBannerAd.loadAd(context)
    val createBannerCaptor = argumentCaptor<CreateBannerCallback>()
    verify(mockSdkFactory)
      .createBanner(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        createBannerCaptor.capture(),
      )
    val capturedCallback = createBannerCaptor.firstValue
    capturedCallback.invoke(mockBannerAd, /* molocoError= */ null)
  }

  private fun createMediationBannerAdConfiguration(
    serverParameters: Bundle = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT),
    adSize: AdSize = AdSize.BANNER,
    bidResponse: String = TEST_BID_RESPONSE,
    watermark: String = TEST_WATERMARK,
  ): MediationBannerAdConfiguration {
    return MediationBannerAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      adSize,
      watermark,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
