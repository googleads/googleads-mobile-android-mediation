package com.google.ads.mediation.moloco

import android.content.Context
import android.net.Uri
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
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.CreateNativeAdCallback
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.Moloco.createNativeAd
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MolocoNativeAdTest {
    // Subject of tests
    private lateinit var adapterRtbNativeAd: MolocoNativeAd
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
        MolocoNativeAd.newInstance(mediationAdConfiguration, mockMediationAdLoadCallback)
            .onSuccess { adapterRtbNativeAd = it }
        whenever(mockMediationAdLoadCallback.onSuccess(adapterRtbNativeAd)) doReturn
                mockMediationAdCallback

        // Mock the assets of mockNativeAd
        val mockAssets = mock<NativeAd.Assets> {
            on { title } doReturn AD_TITLE
            on { description } doReturn AD_BODY_TEXT
            on { callToActionText } doReturn AD_CALL_TO_ACTION_TEXT
            on { rating } doReturn AD_STAR_RATING.toFloat()
            on { sponsorText } doReturn AD_SPONSORED_TEXT
            on { iconUri } doReturn Uri.parse(APP_ICON_URL)
        }
        whenever(mockNativeAd.assets) doReturn mockAssets
    }

    @Test
    fun onAdLoadFailed_dueToSdkInit_invokesOnFailure() {
        // This test validates createAd failure path

        // Mock the Moloco createNativeAd behavior to immediately return an error in the callback
        mockStatic(Moloco::class.java).use { mockedMoloco ->
            val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()
            val createError = MolocoAdError.AdCreateError.SDK_INIT_FAILED
            whenever(createNativeAd(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createNativeAdCaptor.capture())).thenAnswer {
                createNativeAdCaptor.firstValue.invoke(null, createError)
            }

            adapterRtbNativeAd.loadAd()

            // SDK create ad failed because SDK init had failed
            val expectedAdError = AdError(
                MolocoAdError.AdCreateError.SDK_INIT_FAILED.errorCode,
                MolocoAdError.AdCreateError.SDK_INIT_FAILED.description,
                MolocoMediationAdapter.SDK_ERROR_DOMAIN
            )

            verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
        }
    }

    @Test
    fun onAdLoadFailed_dueToAdLoadParsing_invokesOnFailure() {
        // This test validates loadAd failure path.

        val testError =
            MolocoAdError("testNetwork", "testAdUnit", MolocoAdError.ErrorType.AD_BID_PARSE_ERROR, "testDesc")
        val expectedAdError =
            AdError(
                MolocoAdError.ErrorType.AD_BID_PARSE_ERROR.errorCode,
                MolocoAdError.ErrorType.AD_BID_PARSE_ERROR.description,
                MolocoMediationAdapter.SDK_ERROR_DOMAIN,
            )

        // Mock the Moloco createNativeAd behavior to immediately return
        // a mock ad in the callback
        mockStatic(Moloco::class.java).use { mockedMoloco ->
            val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()
            whenever(createNativeAd(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createNativeAdCaptor.capture())).thenAnswer {
                createNativeAdCaptor.firstValue.invoke(mockNativeAd, /* error= */ null)
            }

            // Mock the internal MolocoAd load behavior to invoke a load success callback
            val loadCallbackCaptor = argumentCaptor<AdLoad.Listener>()
            whenever(mockNativeAd.load(eq(TEST_BID_RESPONSE), loadCallbackCaptor.capture())).thenAnswer {
                loadCallbackCaptor.firstValue.onAdLoadFailed(testError)
            }

            adapterRtbNativeAd.loadAd()
            verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
        }
    }

    @Test
    fun onAdLoadSuccess_invokesOnSuccess() {
        // Mock the Moloco createNativeAd behavior to immediately return
        // a mock ad in the callback
        mockStatic(Moloco::class.java).use { mockedMoloco ->
            val createNativeAdCaptor = argumentCaptor<CreateNativeAdCallback>()
            whenever(createNativeAd(eq(TEST_AD_UNIT), eq(TEST_WATERMARK), createNativeAdCaptor.capture())).thenAnswer {
                createNativeAdCaptor.firstValue.invoke(mockNativeAd, /* error= */ null)
            }

            // Mock the internal MolocoAd load behavior to invoke a load success callback
            val loadCallbackCaptor = argumentCaptor<AdLoad.Listener>()
            whenever(mockNativeAd.load(eq(TEST_BID_RESPONSE), loadCallbackCaptor.capture())).thenAnswer {
                loadCallbackCaptor.firstValue.onAdLoadSuccess(mock())
            }

            adapterRtbNativeAd.loadAd()

            assertThat(adapterRtbNativeAd.nativeAd?.assets?.title).isEqualTo(AD_TITLE)
            assertThat(adapterRtbNativeAd.nativeAd?.assets?.description).isEqualTo(AD_BODY_TEXT)
            assertThat(adapterRtbNativeAd.nativeAd?.assets?.callToActionText).isEqualTo(AD_CALL_TO_ACTION_TEXT)
            assertThat(adapterRtbNativeAd.nativeAd?.assets?.rating).isEqualTo(AD_STAR_RATING)
            assertThat(adapterRtbNativeAd.nativeAd?.assets?.sponsorText).isEqualTo(AD_SPONSORED_TEXT)
            val adIconUri = adapterRtbNativeAd.nativeAd?.assets?.iconUri
            assertThat(adIconUri.toString()).isEqualTo(APP_ICON_URL)
            assertThat(adapterRtbNativeAd.overrideImpressionRecording).isFalse()
            assertThat(adapterRtbNativeAd.overrideClickHandling).isTrue()

            verify(mockMediationAdLoadCallback).onSuccess(adapterRtbNativeAd)
        }
    }

    @Test
    fun handleClick_invokesReportAdClicked() {
        adapterRtbNativeAd.nativeAd = mockNativeAd

        adapterRtbNativeAd.handleClick(mock())

        verify(mockNativeAd).handleGeneralAdClick()
    }

    @Test
    fun destroy_invokesOnAdClosed() {
        adapterRtbNativeAd.nativeAd = mockNativeAd

        adapterRtbNativeAd.destroy()

        verify(mockNativeAd).destroy()
        assert(adapterRtbNativeAd.nativeAd == null) {
            "Expected nativeAd to be null after calling destroy"
        }
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
            /*p10=*/ null
        )
    }

    private companion object {
        const val TEST_AD_UNIT = "testAdUnit"
        const val AD_TITLE = "Ad title"
        const val AD_BODY_TEXT = "Ad body text"
        const val AD_CALL_TO_ACTION_TEXT = "Ad call to action text"
        const val AD_STAR_RATING = 4.5f
        const val AD_SPONSORED_TEXT = "Ad sponsored text"
        const val APP_ICON_URL = "file://moloco/app/icon"
    }
}
