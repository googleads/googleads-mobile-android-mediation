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
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.MEDIATION_PLATFORM_NAME
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.BannerAdSize
import com.moloco.sdk.publisher.CreateBannerCallback
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import kotlin.test.assertIs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MolocoBannerAdTest {

  private lateinit var molocoBannerAd: MolocoBannerAd
  private lateinit var mediationAdConfiguration: MediationBannerAdConfiguration
  private lateinit var mockMoloco: MockedStatic<Moloco>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val adSize = AdSize.BANNER
  private val mockBannerAd = mock<Banner>()
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)

  @Before
  fun setUp() {
    mockMoloco = mockStatic(Moloco::class.java)
    mediationAdConfiguration = createMediationBannerAdConfiguration()
    MolocoBannerAd.newInstance(mediationAdConfiguration, bannerAdLoadCallback).onSuccess {
      molocoBannerAd = it
    }
  }

  @After
  fun tearDown() {
    mockMoloco.close()
  }

  // region newInstance Tests
  @Test
  fun newInstance_emptyAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val configuration = createMediationBannerAdConfiguration(serverParameters = serverParameters)
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()
    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )

    val result = MolocoBannerAd.newInstance(configuration, callback)

    assertThat(result.isFailure).isTrue()
    assertThat(callback).hasFailedWith(expectedAdError)
  }

  @Test
  fun newInstance_validConfiguration_returnsSuccess() {
    val configuration = createMediationBannerAdConfiguration()
    val callback = FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>()

    val result = MolocoBannerAd.newInstance(configuration, callback)

    assertThat(result.isSuccess).isTrue()
  }

  // endregion

  // region loadAd Tests
  @Test
  fun loadAd_createsBannerAndLoadsAd() {
    val createBannerCaptor = argumentCaptor<CreateBannerCallback>()
    val mediationInfoCaptor = argumentCaptor<MediationInfo>()

    molocoBannerAd.loadAd(context)

    mockMoloco.verify {
      Moloco.createMolocoBanner(
        mediationInfoCaptor.capture(),
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        createBannerCaptor.capture(),
      )
    }
    assertThat(mediationInfoCaptor.firstValue.name).isEqualTo(MEDIATION_PLATFORM_NAME)

    val capturedCallback = createBannerCaptor.firstValue
    capturedCallback.invoke(mockBannerAd, /* molocoError= */ null)

    verify(mockBannerAd).adShowListener = molocoBannerAd
    verify(mockBannerAd).load(eq(TEST_BID_RESPONSE), eq(molocoBannerAd))
  }

  @Test
  fun loadAd_whenMolocoErrorIsReported_invokesOnFailure() {
    val createBannerCaptor = argumentCaptor<CreateBannerCallback>()
    val molocoError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
    val expectedAdError =
      AdError(
        MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
        MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoBannerAd.loadAd(context)

    mockMoloco.verify {
      Moloco.createMolocoBanner(
        any(),
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        createBannerCaptor.capture(),
      )
    }
    val capturedCallback = createBannerCaptor.firstValue
    capturedCallback.invoke(/* banner= */ null, molocoError)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun loadAd_whenBannerIsNullAndNoMolocoErrorIsReported_invokesOnFailure() {
    val createBannerCaptor = argumentCaptor<CreateBannerCallback>()
    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
        MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )

    molocoBannerAd.loadAd(context)

    mockMoloco.verify {
      Moloco.createMolocoBanner(
        any(),
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        createBannerCaptor.capture(),
      )
    }
    val capturedCallback = createBannerCaptor.firstValue
    capturedCallback.invoke(/* banner= */ null, /* molocoError= */ null)

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  // endregion

  // region getView Tests
  @Test
  fun getView_returnsMolocoAd() {
    val createBannerCaptor = argumentCaptor<CreateBannerCallback>()
    molocoBannerAd.loadAd(context)
    mockMoloco.verify {
      Moloco.createMolocoBanner(
        any(),
        eq(TEST_AD_UNIT),
        eq(BannerAdSize.Standard),
        eq(TEST_WATERMARK),
        createBannerCaptor.capture(),
      )
    }
    createBannerCaptor.firstValue.invoke(mockBannerAd, /* molocoError= */ null)

    assertThat(molocoBannerAd.view).isEqualTo(mockBannerAd)
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

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdLoadSuccess_invokesOnSuccess() {
    molocoBannerAd.onAdLoadSuccess(mock())

    assertThat(bannerAdLoadCallback).hasSucceededWith(molocoBannerAd)
  }

  @Test
  fun onAdClicked_reportsAdClickedAndInvokesOnAdOpenedAndOnAdLeftApplication() {
    molocoBannerAd.onAdLoadSuccess(mock())

    molocoBannerAd.onAdClicked(mock())

    assertThat(bannerAdCallback.isClicked).isTrue()
    assertThat(bannerAdCallback.isOpened).isTrue()
    assertThat(bannerAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onAdHidden_invokesOnAdClosed() {
    molocoBannerAd.onAdLoadSuccess(mock())

    molocoBannerAd.onAdHidden(mock())

    assertThat(bannerAdCallback.isClosed).isTrue()
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

    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdShowSuccess_reportsAdImpression() {
    molocoBannerAd.onAdLoadSuccess(mock())

    molocoBannerAd.onAdShowSuccess(mock())

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  // endregion

  // region resolveBannerAdSize Tests
  @Test
  fun resolveBannerAdSize_heightZero_returnsInlineAdaptive() {
    val size = AdSize(320, 0)

    val resolved = MolocoBannerAd.resolveBannerAdSize(context, size)

    val inline = assertIs<BannerAdSize.InlineAdaptive>(resolved)
    assertThat(inline.availableWidth).isEqualTo(320)
  }

  @Test
  fun resolveBannerAdSize_standardBanner_returnsStandard() {
    val resolved = MolocoBannerAd.resolveBannerAdSize(context, AdSize.BANNER)

    assertIs<BannerAdSize.Standard>(resolved)
  }

  @Test
  fun resolveBannerAdSize_mediumRectangle_returnsMREC() {
    val resolved = MolocoBannerAd.resolveBannerAdSize(context, AdSize.MEDIUM_RECTANGLE)

    assertIs<BannerAdSize.MREC>(resolved)
  }

  @Test
  fun resolveBannerAdSize_leaderboard_returnsTablet() {
    val resolved = MolocoBannerAd.resolveBannerAdSize(context, AdSize.LEADERBOARD)

    assertIs<BannerAdSize.Tablet>(resolved)
  }

  @Test
  fun resolveBannerAdSize_currentOrientationAnchoredAdaptive_returnsAnchoredAdaptive() {
    val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 320)

    val resolved = MolocoBannerAd.resolveBannerAdSize(context, size)

    val anchored = assertIs<BannerAdSize.AnchoredAdaptive>(resolved)
    assertThat(anchored.availableWidth).isEqualTo(320)
  }

  @Test
  fun resolveBannerAdSize_portraitAnchoredAdaptive_returnsAnchoredAdaptive() {
    val size = AdSize.getPortraitAnchoredAdaptiveBannerAdSize(context, 320)

    val resolved = MolocoBannerAd.resolveBannerAdSize(context, size)

    val anchored = assertIs<BannerAdSize.AnchoredAdaptive>(resolved)
    assertThat(anchored.availableWidth).isEqualTo(320)
  }

  @Test
  fun resolveBannerAdSize_landscapeAnchoredAdaptive_returnsAnchoredAdaptive() {
    val size = AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(context, 320)

    val resolved = MolocoBannerAd.resolveBannerAdSize(context, size)

    val anchored = assertIs<BannerAdSize.AnchoredAdaptive>(resolved)
    assertThat(anchored.availableWidth).isEqualTo(320)
  }

  @Test
  fun resolveBannerAdSize_customNonStandardSize_returnsInlineAdaptive() {
    val size = AdSize(123, 456)

    val resolved = MolocoBannerAd.resolveBannerAdSize(context, size)

    val inline = assertIs<BannerAdSize.InlineAdaptive>(resolved)
    assertThat(inline.availableWidth).isEqualTo(123)
  }

  // endregion

  private fun createMediationBannerAdConfiguration(
    serverParameters: android.os.Bundle =
      bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
  ): MediationBannerAdConfiguration {
    return MediationBannerAdConfiguration(
      context,
      TEST_BID_RESPONSE,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      adSize,
      TEST_WATERMARK,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
