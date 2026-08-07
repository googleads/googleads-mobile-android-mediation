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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.moloco.MolocoMediationAdapter.Companion.MEDIATION_PLATFORM_NAME
import com.google.ads.mediation.moloco.MolocoNativeAd.Companion.MEDIA_VIEW_TAG
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.CreateNativeAdCallback
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.NativeAd
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
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

  private lateinit var molocoNativeAd: MolocoNativeAd
  private lateinit var mediationAdConfiguration: MediationNativeAdConfiguration
  private lateinit var mockMoloco: MockedStatic<Moloco>

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockNativeAd = mock<NativeAd>()
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val mockMediationAdCallback = mock<MediationNativeAdCallback>()

  @Before
  fun setUp() {
    mockMoloco = mockStatic(Moloco::class.java)
    mediationAdConfiguration = createMediationNativeAdConfiguration()
    MolocoNativeAd.newInstance(mediationAdConfiguration, mockMediationAdLoadCallback).onSuccess {
      molocoNativeAd = it
    }
    whenever(mockMediationAdLoadCallback.onSuccess(molocoNativeAd)) doReturn mockMediationAdCallback
  }

  @After
  fun tearDown() {
    mockMoloco.close()
  }

  // region newInstance Tests
  @Test
  fun newInstance_emptyAdUnitId_invokesOnFailureAndReturnsFailure() {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to "")
    val configuration = createMediationNativeAdConfiguration(serverParameters = serverParameters)
    val callback = mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>>()
    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
        MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )

    val result = MolocoNativeAd.newInstance(configuration, callback)

    assertThat(result.isFailure).isTrue()
    verify(callback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun newInstance_validConfiguration_returnsSuccess() {
    val configuration = createMediationNativeAdConfiguration()
    val callback = mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>>()

    val result = MolocoNativeAd.newInstance(configuration, callback)

    assertThat(result.isSuccess).isTrue()
  }

  // endregion

  // region loadAd Tests
  @Test
  fun loadAd_createsNativeAdAndLoadsAd() {
    val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()
    val mediationInfoCaptor = argumentCaptor<MediationInfo>()

    molocoNativeAd.loadAd()

    mockMoloco.verify {
      Moloco.createNativeAd(
        mediationInfoCaptor.capture(),
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createNativeAdCaptor.capture(),
      )
    }
    assertThat(mediationInfoCaptor.firstValue.name).isEqualTo(MEDIATION_PLATFORM_NAME)

    val capturedCallback = createNativeAdCaptor.firstValue
    capturedCallback.invoke(mockNativeAd, /* adCreateError= */ null)

    verify(mockNativeAd).load(eq(TEST_BID_RESPONSE), eq(molocoNativeAd))
    assertThat(molocoNativeAd.nativeAd).isEqualTo(mockNativeAd)
  }

  @Test
  fun loadAd_whenAdCreateErrorIsReported_invokesOnFailure() {
    val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()
    val adCreateError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
    val expectedAdError =
      AdError(
        MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
        MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )

    molocoNativeAd.loadAd()

    mockMoloco.verify {
      Moloco.createNativeAd(
        any(),
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createNativeAdCaptor.capture(),
      )
    }
    val capturedCallback = createNativeAdCaptor.firstValue
    capturedCallback.invoke(/* returnedAd= */ null, adCreateError)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadAd_whenNativeAdIsNullAndNoAdCreateErrorIsReported_invokesOnFailure() {
    val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()
    val expectedAdError =
      AdError(
        MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
        MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
        MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
      )

    molocoNativeAd.loadAd()

    mockMoloco.verify {
      Moloco.createNativeAd(
        any(),
        eq(TEST_AD_UNIT),
        eq(TEST_WATERMARK),
        createNativeAdCaptor.capture(),
      )
    }
    val capturedCallback = createNativeAdCaptor.firstValue
    capturedCallback.invoke(/* returnedAd= */ null, /* adCreateError= */ null)

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region Callback and Asset Mapping Tests
  @Test
  fun onAdLoadSuccess_mapsAllAssetsAndRegistersInteractionListener() {
    val mediaView = View(context)
    val mockNativeAdAssets =
      mock<NativeAd.Assets> {
        on { sponsorText } doReturn "testAdvertiser"
        on { rating } doReturn 4.5f
        on { title } doReturn "testTitle"
        on { description } doReturn "testDescription"
        on { callToActionText } doReturn "testCallToAction"
        on { iconUri } doReturn Uri.EMPTY
        on { this.mediaView } doReturn mediaView
      }
    val interactionListenerCaptor = argumentCaptor<NativeAd.InteractionListener>()
    val mockMolocoNativeAd = mock<NativeAd> { on { assets } doReturn mockNativeAdAssets }
    molocoNativeAd.nativeAd = mockMolocoNativeAd

    molocoNativeAd.onAdLoadSuccess(mock())

    assertThat(molocoNativeAd.overrideClickHandling).isTrue()
    assertThat(molocoNativeAd.starRating).isEqualTo(4.5)
    assertThat(molocoNativeAd.advertiser).isEqualTo("testAdvertiser")
    assertThat(molocoNativeAd.store).isEqualTo("Google Play")
    assertThat(molocoNativeAd.headline).isEqualTo("testTitle")
    assertThat(molocoNativeAd.body).isEqualTo("testDescription")
    assertThat(molocoNativeAd.callToAction).isEqualTo("testCallToAction")
    assertThat(molocoNativeAd.icon).isNotNull()
    assertThat(mediaView.tag).isEqualTo(MEDIA_VIEW_TAG)
    verify(mockMediationAdLoadCallback).onSuccess(molocoNativeAd)

    verify(mockMolocoNativeAd).interactionListener = interactionListenerCaptor.capture()
    val capturedListener = interactionListenerCaptor.firstValue

    capturedListener.onGeneralClickHandled()
    verify(mockMediationAdCallback).reportAdClicked()

    capturedListener.onImpressionHandled()
  }

  @Test
  fun onAdLoadSuccess_withNullAssets_mapsFieldsGracefully() {
    val mockNativeAdAssets =
      mock<NativeAd.Assets> {
        on { sponsorText } doReturn null
        on { rating } doReturn null
        on { title } doReturn null
        on { description } doReturn null
        on { callToActionText } doReturn null
        on { iconUri } doReturn null
        on { mediaView } doReturn null
      }
    val mockMolocoNativeAd = mock<NativeAd> { on { assets } doReturn mockNativeAdAssets }
    molocoNativeAd.nativeAd = mockMolocoNativeAd

    molocoNativeAd.onAdLoadSuccess(mock())

    assertThat(molocoNativeAd.overrideClickHandling).isTrue()
    assertThat(molocoNativeAd.starRating).isNull()
    assertThat(molocoNativeAd.advertiser).isNull()
    assertThat(molocoNativeAd.store).isEqualTo("Google Play")
    assertThat(molocoNativeAd.headline).isNull()
    assertThat(molocoNativeAd.body).isNull()
    assertThat(molocoNativeAd.callToAction).isNull()
    assertThat(molocoNativeAd.icon).isNull()
    verify(mockMediationAdLoadCallback).onSuccess(molocoNativeAd)
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

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
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

    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun handleClick_invokesReportAdClicked() {
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
  fun trackViews_invokesRegisterViews() {
    molocoNativeAd.nativeAd = mockNativeAd
    val viewContainer = View(context)
    val clickableView = View(context)
    val clickableAssets = mapOf("testView" to clickableView)

    molocoNativeAd.trackViews(viewContainer, clickableAssets, /* nonclickableAssetViews= */ mapOf())
    viewContainer.callOnClick()
    clickableView.callOnClick()

    verify(mockNativeAd, times(2)).handleGeneralAdClick()
  }

  @Test
  fun destroy_destroysNativeAdAndNullsReference() {
    molocoNativeAd.nativeAd = mockNativeAd

    molocoNativeAd.destroy()

    verify(mockNativeAd).destroy()
    assertThat(molocoNativeAd.nativeAd).isNull()
  }

  @Test
  fun molocoNativeMappedImage_returnsCorrectProperties() {
    val drawable: Drawable = ColorDrawable(Color.RED)
    val uri = Uri.parse("https://google.com/icon.png")
    val scale = 2.0

    val image = MolocoNativeAd.MolocoNativeMappedImage(drawable, uri, scale)

    assertThat(image.drawable).isEqualTo(drawable)
    assertThat(image.uri).isEqualTo(uri)
    assertThat(image.scale).isEqualTo(scale)
  }

  // endregion

  // region com.moloco.sdk.publisher.NativeAd.InteractionListener implementation tests
  @Test
  fun onImpressionHandled_reportsAdImpression() {
    molocoNativeAd.onAdLoadSuccess(mock())

    molocoNativeAd.onImpressionHandled()

    verify(mockMediationAdCallback).reportAdImpression()
  }

  @Test
  fun onGeneralClickHandled_reportsAdClicked() {
    molocoNativeAd.onAdLoadSuccess(mock())

    molocoNativeAd.onGeneralClickHandled()

    verify(mockMediationAdCallback).reportAdClicked()
  }

  // endregion

  private fun createMediationNativeAdConfiguration(
    serverParameters: android.os.Bundle =
      bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
  ): MediationNativeAdConfiguration {
    return MediationNativeAdConfiguration(
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
      null,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
