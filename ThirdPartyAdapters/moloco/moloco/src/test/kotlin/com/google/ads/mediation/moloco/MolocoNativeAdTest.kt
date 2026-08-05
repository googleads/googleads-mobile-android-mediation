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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.MEDIATION_PLATFORM_NAME
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.CreateNativeAdCallback
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.MolocoAdError.AdCreateError
import com.moloco.sdk.publisher.NativeAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MolocoNativeAdTest {
  // Subject of tests
  private lateinit var molocoNativeAd: MolocoNativeAd
  private lateinit var mediationAdConfiguration: MediationNativeAdConfiguration

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockNativeAd = mock<NativeAd>()
  private val mockSdkFactory = mock<SdkFactory>()
  private val mockMediationNativeAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val mockMediationAdCallback = mock<MediationNativeAdCallback>()

  @Before
  fun setUp() {
    MolocoSdkFactory.delegate = mockSdkFactory

    // Properly initialize molocoNativeAd
    mediationAdConfiguration = createMediationNativeAdConfiguration()
    MolocoNativeAd.newInstance(mediationAdConfiguration, mockMediationNativeAdLoadCallback)
      .onSuccess { molocoNativeAd = it }
    whenever(mockMediationNativeAdLoadCallback.onSuccess(molocoNativeAd)) doReturn
      mockMediationAdCallback
  }

  // region newInstance Tests
  @Test
  fun newInstance_withValidParameters_returnsSuccess() {
    val config = createMediationNativeAdConfiguration()

    val result = MolocoNativeAd.newInstance(config, mockMediationNativeAdLoadCallback)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun newInstance_withMissingAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf()
    val config = createMediationNativeAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = MolocoNativeAd.newInstance(config, mockMediationNativeAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    verify(mockMediationNativeAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT)
    assertThat(capturedError.message).isEqualTo(MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT)
    assertThat(capturedError.domain).isEqualTo(MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun newInstance_withEmptyAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val config = createMediationNativeAdConfiguration(serverParameters = serverParameters)
    val adErrorCaptor = argumentCaptor<AdError>()

    val result = MolocoNativeAd.newInstance(config, mockMediationNativeAdLoadCallback)

    assertThat(result.isFailure).isTrue()
    verify(mockMediationNativeAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT)
    assertThat(capturedError.message).isEqualTo(MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT)
    assertThat(capturedError.domain).isEqualTo(MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  // endregion

  // region loadAd Tests
  @Test
  fun loadAd_createsNativeAdWithCorrectParameters() {
    molocoNativeAd.loadAd()

    verify(mockSdkFactory)
      .createNativeAd(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        any(),
      )
  }

  @Test
  fun loadAd_whenAdCreationFailsWithMolocoError_invokesOnFailure() {
    val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()

    molocoNativeAd.loadAd()

    verify(mockSdkFactory)
      .createNativeAd(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createNativeAdCaptor.capture(),
      )
    val capturedCallback = createNativeAdCaptor.firstValue
    val molocoError = AdCreateError.SDK_INIT_FAILED
    capturedCallback.invoke(/* returnedAd= */ null, molocoError)

    val expectedAdError =
      AdError(
        AdCreateError.SDK_INIT_FAILED.errorCode,
        AdCreateError.SDK_INIT_FAILED.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenAdIsNullAndNoErrorReported_invokesOnFailure() {
    val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()

    molocoNativeAd.loadAd()

    verify(mockSdkFactory)
      .createNativeAd(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createNativeAdCaptor.capture(),
      )
    val capturedCallback = createNativeAdCaptor.firstValue
    capturedCallback.invoke(/* returnedAd= */ null, /* adCreateError= */ null)

    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
        MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )
    verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenAdCreated_loadsBidResponse() {
    loadNativeAd()

    verify(mockNativeAd).load(eq(TEST_BID_RESPONSE), eq(molocoNativeAd))
  }

  // endregion

  // region Asset Mapping & Callbacks Tests
  @Test
  fun onAdLoadSuccess_mapsAllAssetsAndInvokesOnSuccess() {
    val mockMediaView = View(context)
    val mockNativeAdAssets =
      mock<NativeAd.Assets> {
        on { sponsorText } doReturn "testAdvertiser"
        on { rating } doReturn 1.0f
        on { title } doReturn "testTitle"
        on { description } doReturn "testDescription"
        on { callToActionText } doReturn "testCallToAction"
        on { mediaView } doReturn mockMediaView
      }
    val mockMolocoNativeAd = mock<NativeAd> { on { assets } doReturn mockNativeAdAssets }
    molocoNativeAd.nativeAd = mockMolocoNativeAd

    molocoNativeAd.onAdLoadSuccess(mock())

    assertThat(molocoNativeAd.overrideClickHandling).isTrue()
    assertThat(molocoNativeAd.starRating).isEqualTo(1.0)
    assertThat(molocoNativeAd.advertiser).isEqualTo("testAdvertiser")
    assertThat(molocoNativeAd.store).isEqualTo("Google Play")
    assertThat(molocoNativeAd.headline).isEqualTo("testTitle")
    assertThat(molocoNativeAd.body).isEqualTo("testDescription")
    assertThat(molocoNativeAd.callToAction).isEqualTo("testCallToAction")
    assertThat(mockMediaView.tag).isEqualTo(MolocoNativeAd.MEDIA_VIEW_TAG)
    verify(mockMediationNativeAdLoadCallback).onSuccess(molocoNativeAd)
  }

  @Test
  fun onAdLoadSuccess_setsInteractionListener_generalClickHandled_invokesReportAdClicked() {
    val interactionListenerCaptor = argumentCaptor<NativeAd.InteractionListener>()
    val mockMolocoNativeAd = mock<NativeAd>()
    molocoNativeAd.nativeAd = mockMolocoNativeAd

    molocoNativeAd.onAdLoadSuccess(mock())

    verify(mockMolocoNativeAd).interactionListener = interactionListenerCaptor.capture()
    val capturedListener = interactionListenerCaptor.firstValue
    capturedListener.onGeneralClickHandled()
    verify(mockMediationAdCallback).reportAdClicked()
    capturedListener.onImpressionHandled()
  }

  @Test
  fun onAdLoadFailed_dueToSdkInit_invokesOnFailure() {
    val testError =
      MolocoAdError(
        "testNetwork",
        "testAdUnit",
        MolocoAdError.ErrorType.AD_LOAD_FAILED_SDK_NOT_INIT,
        "testDesc",
      )
    val expectedAdError =
      AdError(
        MolocoAdError.ErrorType.AD_LOAD_FAILED_SDK_NOT_INIT.errorCode,
        MolocoAdError.ErrorType.AD_LOAD_FAILED_SDK_NOT_INIT.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoNativeAd.onAdLoadFailed(testError)

    verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdLoadFailed_dueToAdLoadParsing_invokesOnFailure() {
    val testError =
      MolocoAdError(
        "testNetwork",
        "testAdUnit",
        MolocoAdError.ErrorType.AD_BID_PARSE_ERROR,
        "testDesc",
      )
    val expectedAdError =
      AdError(
        MolocoAdError.ErrorType.AD_BID_PARSE_ERROR.errorCode,
        MolocoAdError.ErrorType.AD_BID_PARSE_ERROR.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoNativeAd.onAdLoadFailed(testError)

    verify(mockMediationNativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Interactions and View Tracking Tests
  @Test
  fun handleClick_invokesHandleGeneralAdClick() {
    molocoNativeAd.nativeAd = mockNativeAd

    molocoNativeAd.handleClick(mock())

    verify(mockNativeAd).handleGeneralAdClick()
  }

  @Test
  fun recordImpression_invokesHandleImpression() {
    molocoNativeAd.nativeAd = mockNativeAd

    molocoNativeAd.recordImpression()

    verify(mockNativeAd).handleImpression()
  }

  @Test
  fun trackViews_registersClickListenersOnContainerAndClickableViews() {
    molocoNativeAd.nativeAd = mockNativeAd
    val viewContainer = View(context)
    val clickableView1 = View(context)
    val clickableView2 = View(context)
    val clickableAssets = mapOf("asset1" to clickableView1, "asset2" to clickableView2)
    val nonClickableAssets = mapOf("asset3" to View(context))

    molocoNativeAd.trackViews(viewContainer, clickableAssets, nonClickableAssets)
    viewContainer.callOnClick()
    clickableView1.callOnClick()
    clickableView2.callOnClick()

    verify(mockNativeAd, times(3)).handleGeneralAdClick()
  }

  @Test
  fun destroy_invokesNativeAdDestroyAndSetsNativeAdToNull() {
    molocoNativeAd.nativeAd = mockNativeAd

    molocoNativeAd.destroy()

    verify(mockNativeAd).destroy()
    assertThat(molocoNativeAd.nativeAd).isNull()
  }

  @Test
  fun molocoNativeMappedImage_properties_returnExpectedValues() {
    val mockDrawable = mock<Drawable>()
    val mappedImage = MolocoNativeAd.MolocoNativeMappedImage(mockDrawable)

    assertThat(mappedImage.drawable).isEqualTo(mockDrawable)
    assertThat(mappedImage.uri).isEqualTo(Uri.EMPTY)
    assertThat(mappedImage.scale).isEqualTo(1.0)
  }

  // endregion

  private fun loadNativeAd() {
    molocoNativeAd.loadAd()
    val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()
    verify(mockSdkFactory)
      .createNativeAd(
        argThat { name == MEDIATION_PLATFORM_NAME },
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createNativeAdCaptor.capture(),
      )
    val capturedCallback = createNativeAdCaptor.firstValue
    capturedCallback.invoke(mockNativeAd, /* adCreateError= */ null)
  }

  private fun createMediationNativeAdConfiguration(
    serverParameters: Bundle = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT),
    bidResponse: String = TEST_BID_RESPONSE,
    watermark: String = TEST_WATERMARK,
  ): MediationNativeAdConfiguration {
    return MediationNativeAdConfiguration(
      context,
      bidResponse,
      serverParameters,
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      watermark,
      /*p10=*/ null,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
