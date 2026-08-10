package com.google.ads.mediation.verve

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import net.pubnative.lite.sdk.models.NativeAd
import net.pubnative.lite.sdk.request.HyBidNativeAdRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VerveNativeAdTest {
  // Subject of testing.
  private lateinit var verveNativeAd: VerveNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val testView = View(context)
  private val mockBitmap: Bitmap = mock()
  private val mockHyBidNativeAdRequest = mock<HyBidNativeAdRequest>()
  private val mockHyBidNativeAd =
    mock<NativeAd> {
      on { title } doReturn TEST_TITLE
      on { description } doReturn TEST_DESCRIPTION
      on { getContentInfo(eq(context)) } doReturn testView
      on { callToActionText } doReturn TEST_CALL_TO_ACTION
      on { rating } doReturn TEST_RATING
      on { iconBitmap } doReturn mockBitmap
      on { iconUrl } doReturn TEST_ICON_URL
      on { bannerBitmap } doReturn mockBitmap
    }
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)

  @Before
  fun setUp() {
    verveNativeAd =
      VerveNativeAd(context, nativeAdLoadCallback, TEST_BID_RESPONSE, mockHyBidNativeAdRequest)
  }

  @Test
  fun loadAd_invokesHyBidLoadAd() {
    verveNativeAd.loadAd()

    verify(mockHyBidNativeAdRequest).setPreLoadMediaAssets(eq(true))
    verify(mockHyBidNativeAdRequest).prepareAd(eq(TEST_BID_RESPONSE), eq(verveNativeAd))
  }

  @Test
  fun onRequestSuccess_invokesOnSuccess() {
    verveNativeAd.onRequestSuccess(mockHyBidNativeAd)

    assertThat(nativeAdLoadCallback).hasSucceededWith(verveNativeAd)
    assertThat(verveNativeAd.headline).isEqualTo(TEST_TITLE)
    assertThat(verveNativeAd.body).isEqualTo(TEST_DESCRIPTION)
    assertThat(verveNativeAd.adChoicesContent).isEqualTo(testView)
    assertThat(verveNativeAd.callToAction).isEqualTo(TEST_CALL_TO_ACTION)
    assertThat(verveNativeAd.starRating).isEqualTo(TEST_RATING.toDouble())
    assertThat(verveNativeAd.icon.drawable).isNotNull()
    assertThat(verveNativeAd.icon.uri.toString()).isEqualTo(TEST_ICON_URL)
    assertThat(verveNativeAd.overrideClickHandling).isTrue()
    assertThat(verveNativeAd.overrideImpressionRecording).isTrue()
  }

  @Test
  fun onRequestSuccess_withNullAd_invokesOnFailure() {
    val expectedAdError =
      AdError(ERROR_CODE_AD_LOAD_FAILED_TO_LOAD, "Could not load native ad", SDK_ERROR_DOMAIN)

    verveNativeAd.onRequestSuccess(null)

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailure() {
    val testError = Throwable("TestError")
    val expectedAdError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "Could not load native ad. Error: TestError",
        SDK_ERROR_DOMAIN,
      )

    verveNativeAd.onRequestFail(testError)

    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdImpression_invokesOnAdOpenedAndReportAdImpression() {
    verveNativeAd.onRequestSuccess(mockHyBidNativeAd)

    verveNativeAd.onAdImpression(mockHyBidNativeAd, view = null)

    assertThat(nativeAdCallback.isOpened).isTrue()
    assertThat(nativeAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClick_invokesReportAdClickedAndOnAdLeftApplication() {
    verveNativeAd.onRequestSuccess(mockHyBidNativeAd)

    verveNativeAd.onAdClick(mockHyBidNativeAd, view = null)

    assertThat(nativeAdCallback.isClicked).isTrue()
    assertThat(nativeAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun trackViews_invokesHyBidNativeAdStartTracking() {
    verveNativeAd.onRequestSuccess(mockHyBidNativeAd)

    verveNativeAd.trackViews(
      testView,
      /* clickableAssetViews= */ mapOf(),
      /* nonClickableAssetViews= */ mapOf(),
    )

    verify(mockHyBidNativeAd).startTracking(eq(testView), eq(verveNativeAd))
  }

  @Test
  fun unTrackViews_invokesStopTracking() {
    verveNativeAd.onRequestSuccess(mockHyBidNativeAd)

    verveNativeAd.untrackView(testView)

    verify(mockHyBidNativeAd).stopTracking()
  }

  @Test
  fun handleClick_invokesOnNativeClick() {
    verveNativeAd.onRequestSuccess(mockHyBidNativeAd)

    verveNativeAd.handleClick(testView)

    verify(mockHyBidNativeAd).onNativeClick(testView)
    assertThat(nativeAdCallback.isClicked).isTrue()
    assertThat(nativeAdCallback.isOpened).isTrue()
  }

  @Test
  fun trackViews_withClickableView_invokesNativeClickAndCallbacks() {
    verveNativeAd.onRequestSuccess(mockHyBidNativeAd)
    val clickableView = View(context)

    verveNativeAd.trackViews(
      testView,
      /* clickableAssetViews= */ mapOf("key" to clickableView),
      /* nonClickableAssetViews= */ mapOf(),
    )
    clickableView.performClick()

    verify(mockHyBidNativeAd).onNativeClick(clickableView)
    assertThat(nativeAdCallback.isClicked).isTrue()
    assertThat(nativeAdCallback.isOpened).isTrue()
  }

  private companion object {
    const val TEST_TITLE = "testTitle"
    const val TEST_DESCRIPTION = "testDescription"
    const val TEST_CALL_TO_ACTION = "testCallToAction"
    const val TEST_RATING = 4
    const val TEST_ICON_URL = "testIconUrl"
  }
}
