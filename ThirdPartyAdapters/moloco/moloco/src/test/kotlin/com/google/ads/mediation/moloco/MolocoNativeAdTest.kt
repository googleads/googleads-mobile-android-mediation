package com.google.ads.mediation.moloco

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.NativeAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
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
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val mockMediationAdCallback = mock<MediationNativeAdCallback>()

  @Before
  fun setUp() {
    // Properly initialize molocoNativeAd
    mediationAdConfiguration = createMediationNativeAdConfiguration()
    MolocoNativeAd.newInstance(mediationAdConfiguration, mockMediationAdLoadCallback).onSuccess {
      molocoNativeAd = it
    }
    whenever(mockMediationAdLoadCallback.onSuccess(molocoNativeAd)) doReturn mockMediationAdCallback
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
  fun onAdLoadSuccess_invokesOnSuccess() {
    val mockNativeAdAssets =
      mock<NativeAd.Assets> {
        on { sponsorText } doReturn "testAdvertiser"
        on { rating } doReturn 1.0f
        on { title } doReturn "testTitle"
        on { description } doReturn "testDescription"
        on { callToActionText } doReturn "testCallToAction"
        on { iconUri } doReturn Uri.EMPTY
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
    assertThat(molocoNativeAd.icon).isNotNull()
    verify(mockMediationAdLoadCallback).onSuccess(molocoNativeAd)
  }

  @Test
  fun handleClick_invokesReportAdClicked() {
    molocoNativeAd.nativeAd = mockNativeAd

    molocoNativeAd.handleClick(mock())

    verify(mockNativeAd).handleGeneralAdClick()
  }

  @Test
  fun destroy_invokesOnAdClosed() {
    molocoNativeAd.nativeAd = mockNativeAd

    molocoNativeAd.destroy()

    verify(mockNativeAd).destroy()
    assert(molocoNativeAd.nativeAd == null) { "Expected nativeAd to be null after calling destroy" }
  }

  @Test
  fun trackViews_invokesRegisterViews() {
    molocoNativeAd.nativeAd = mockNativeAd
    val viewContainer = View(context)
    val clickableView = View(context)
    val clickableAssets = mutableMapOf(Pair("testView", clickableView))

    molocoNativeAd.trackViews(viewContainer, clickableAssets, /* nonClickableAssetViews= */ mock())
    viewContainer.callOnClick()
    clickableView.callOnClick()

    verify(mockNativeAd, times(2)).handleGeneralAdClick()
  }

  private fun createMediationNativeAdConfiguration(): MediationNativeAdConfiguration {
    val serverParameters = bundleOf(MolocoMediationAdapter.KEY_AD_UNIT_ID to TEST_AD_UNIT)
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
      /*p10=*/ null,
    )
  }

  private companion object {
    const val TEST_AD_UNIT = "testAdUnit"
  }
}
