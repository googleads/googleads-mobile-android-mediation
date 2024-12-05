import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.ironsource.IronSourceRtbInterstitialAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.ironsourceads.interstitial.InterstitialAd
import com.unity3d.ironsourceads.interstitial.InterstitialAdLoader
import com.unity3d.ironsourceads.interstitial.InterstitialAdRequest
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IronSourceRtbInterstitialAdTest {

  private lateinit var ironSourceRtbInterstitialAd: IronSourceRtbInterstitialAd

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val bundle = Bundle().apply { putString("instanceId", "mockInstanceId") }
  private val mockInterstitialAdConfig: MediationInterstitialAdConfiguration = mock {
    on { context } doReturn context
    on { serverParameters } doReturn bundle
    on { bidResponse } doReturn TEST_BID_RESPONSE
    on { watermark } doReturn TEST_WATERMARK
  }
  private val mockMediationInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockMediationInterstitialAdCallback
    }
  private val mockInterstitialAd: InterstitialAd = mock()
  private val mockInterstitialAdLoader = mockStatic(InterstitialAdLoader::class.java)

  @Before
  fun setUp() {
    ironSourceRtbInterstitialAd =
      IronSourceRtbInterstitialAd(mockInterstitialAdConfig, mockMediationAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockInterstitialAdLoader.close()
  }

  @Test
  fun testLoadRtbAd_Success() {
    // given

    // when
    ironSourceRtbInterstitialAd.loadRtbAd()
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // then
    verify(mockMediationAdLoadCallback).onSuccess(ironSourceRtbInterstitialAd)
    verifyNoMoreInteractions(mockMediationAdLoadCallback)
  }

  @Test
  fun testOnInterstitialAdLoaded() {
    // given

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // then
    verify(mockMediationAdLoadCallback).onSuccess(ironSourceRtbInterstitialAd)
    verifyNoMoreInteractions(mockMediationAdLoadCallback)
  }

  @Test
  fun onInterstitialAdLoadFailed_verifyOnFailureCallback() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd()
    val ironSourceError = IronSourceError(123, "An error occurred")

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdLoadFailed(ironSourceError)

    // then
    val expectedAdError = AdError(123, "An error occurred", "com.ironsource.mediationsdk")
    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun showAd_verifyShowAdInvoked() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)
    val activity = Robolectric.buildActivity(Activity::class.java).get()

    // when
    ironSourceRtbInterstitialAd.showAd(activity)

    // then
    verify(mockInterstitialAd).show(activity)
  }

  @Test
  fun showAd_invalidContext_expectObFailureCallbackWithError() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.showAd(context)

    // then
    val captor = argumentCaptor<AdError>()
    verify(mockMediationInterstitialAdCallback).onAdFailedToShow(captor.capture())
    val capturedError = captor.firstValue
    assertEquals(102, capturedError.code)
    assertEquals("IronSource requires an Activity context to load ads.", capturedError.message)
    assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
  }

  @Test
  fun onInterstitialAdShowFailed_verifyOnAdFailedToShow() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd()
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)
    val ironSourceError = IronSourceError(123, "An error occurred")

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdFailedToShow(mockInterstitialAd, ironSourceError)

    // then
    val expectedAdError = AdError(123, "An error occurred", "com.ironsource.mediationsdk")
    verify(mockMediationInterstitialAdCallback)
      .onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onInterstitialAdShowFailed_withoutInterstitialAdCallbackInstance_verifyOnAdFailedToShow() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd()
    val errorRes = "An error occurred"
    val errorCode = 123
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // When
    doReturn(null).whenever(mockMediationAdLoadCallback).onSuccess(any())
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)
    ironSourceRtbInterstitialAd.onInterstitialAdFailedToShow(mockInterstitialAd, ironSourceError)

    // Then
    verifyNoInteractions(mockMediationInterstitialAdCallback)
  }

  @Test
  fun onInterstitialAdOpened_verifyOnInterstitialAdOpenedCallbacks() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd()
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdShown(mockInterstitialAd)

    // then
    verify(mockMediationInterstitialAdCallback).onAdOpened()
    verify(mockMediationInterstitialAdCallback).reportAdImpression()
  }

  @Test
  fun onInterstitialAdClosed_verifyOnAdClosedCallback() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd()
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdDismissed(mockInterstitialAd)

    // then
    verify(mockMediationInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun onInterstitialAdClicked_verifyReportAdClickedCallback() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd()
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdClicked(mockInterstitialAd)

    // then
    verify(mockMediationInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAdEvents_withoutInterstitialAd_verifyNoCallbacks() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd()

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdShown(mockInterstitialAd)
    ironSourceRtbInterstitialAd.onInterstitialAdDismissed(mockInterstitialAd)
    ironSourceRtbInterstitialAd.onInterstitialAdClicked(mockInterstitialAd)

    // then
    verifyNoMoreInteractions(mockMediationInterstitialAdCallback)
  }

  @Test
  fun testShowAd_NullAd() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.showAd(context)

    // then
    verify(mockInterstitialAd, never()).show(any())
  }

  @Test
  fun testShowAd_NonActivityContext() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.showAd(context)

    // then
    verify(mockInterstitialAd, never()).show(any())
  }

  @Test
  fun testOnInterstitialAdShown_CallbackSet() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdShown(mockInterstitialAd)

    // then
    verify(mockMediationInterstitialAdCallback).onAdOpened()
    verify(mockMediationInterstitialAdCallback).reportAdImpression()
  }

  @Test
  fun testOnInterstitialAdDismissed_CallbackSet() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdDismissed(mockInterstitialAd)

    // then
    verify(mockMediationInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun testOnInterstitialAdClicked_CallbackSet() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdClicked(mockInterstitialAd)

    // then
    verify(mockMediationInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun testLoadRtbAd_VerifyWatermarkInRequest() {
    // given
    val requestCaptor = argumentCaptor<InterstitialAdRequest>()

    // when
    ironSourceRtbInterstitialAd.loadRtbAd()

    // then
    mockInterstitialAdLoader.verify { InterstitialAdLoader.loadAd(requestCaptor.capture(), any()) }
    val capturedRequest = requestCaptor.firstValue
    val actualWatermark = capturedRequest.extraParams?.getString("google_watermark")
    assertEquals(TEST_WATERMARK, actualWatermark)
  }
}
