package com.google.ads.mediation.ironsource

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.truth.os.BundleSubject.assertThat
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.ironsourceads.interstitial.InterstitialAd
import com.unity3d.ironsourceads.interstitial.InterstitialAdLoader
import com.unity3d.ironsourceads.interstitial.InterstitialAdRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val mockInterstitialAd: InterstitialAd = mock()
  private val mockInterstitialAdLoader = mockStatic(InterstitialAdLoader::class.java)

  @Before
  fun setUp() {
    ironSourceRtbInterstitialAd = IronSourceRtbInterstitialAd(interstitialAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockInterstitialAdLoader.close()
  }

  @Test
  fun testLoadRtbAd_Success() {
    // when
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // then
    assertThat(interstitialAdLoadCallback).hasSucceededWith(ironSourceRtbInterstitialAd)
  }

  @Test
  fun testOnInterstitialAdLoaded() {
    // when
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // then
    assertThat(interstitialAdLoadCallback).hasSucceededWith(ironSourceRtbInterstitialAd)
  }

  @Test
  fun onInterstitialAdLoadFailed_verifyOnFailureCallback() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)
    val ironSourceError = IronSourceError(123, "An error occurred")

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdLoadFailed(ironSourceError)

    // then
    val expectedAdError = AdError(123, "An error occurred", "com.ironsource.mediationsdk")
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
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
    val expectedAdError =
      AdError(
        102,
        "IronSource requires an Activity context to load ads.",
        "com.google.ads.mediation.ironsource",
      )
    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onInterstitialAdShowFailed_verifyOnAdFailedToShow() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)
    val ironSourceError = IronSourceError(123, "An error occurred")

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdFailedToShow(mockInterstitialAd, ironSourceError)

    // then
    val expectedAdError = AdError(123, "An error occurred", "com.ironsource.mediationsdk")
    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onInterstitialAdShowFailed_withoutInterstitialAdCallbackInstance_verifyOnAdFailedToShow() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)
    val errorRes = "An error occurred"
    val errorCode = 123
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdFailedToShow(mockInterstitialAd, ironSourceError)

    // then
    assertThat(interstitialAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun onInterstitialAdOpened_verifyOnInterstitialAdOpenedCallbacks() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdShown(mockInterstitialAd)

    // then
    assertThat(interstitialAdCallback.isOpened).isTrue()
    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onInterstitialAdClosed_verifyOnAdClosedCallback() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdDismissed(mockInterstitialAd)

    // then
    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onInterstitialAdClicked_verifyReportAdClickedCallback() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdClicked(mockInterstitialAd)

    // then
    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdEvents_withoutInterstitialAd_verifyNoCallbacks() {
    // given
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdShown(mockInterstitialAd)
    ironSourceRtbInterstitialAd.onInterstitialAdDismissed(mockInterstitialAd)
    ironSourceRtbInterstitialAd.onInterstitialAdClicked(mockInterstitialAd)

    // then
    assertThat(interstitialAdCallback.isOpened).isFalse()
    assertThat(interstitialAdCallback.isImpressionReported).isFalse()
    assertThat(interstitialAdCallback.isClosed).isFalse()
    assertThat(interstitialAdCallback.isClicked).isFalse()
  }

  @Test
  fun showAd_whenAdNotLoaded_neverShowsAd() {
    // given
    val activity = Robolectric.buildActivity(Activity::class.java).get()

    // when
    ironSourceRtbInterstitialAd.showAd(activity)

    // then
    verify(mockInterstitialAd, never()).show(any())
  }

  @Test
  fun showAd_nonActivityContext_neverShowsAd() {
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
    assertThat(interstitialAdCallback.isOpened).isTrue()
    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun testOnInterstitialAdDismissed_CallbackSet() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdDismissed(mockInterstitialAd)

    // then
    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun testOnInterstitialAdClicked_CallbackSet() {
    // given
    ironSourceRtbInterstitialAd.onInterstitialAdLoaded(mockInterstitialAd)

    // when
    ironSourceRtbInterstitialAd.onInterstitialAdClicked(mockInterstitialAd)

    // then
    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun testLoadRtbAd_VerifyWatermarkInRequest() {
    // given
    val requestCaptor = argumentCaptor<InterstitialAdRequest>()

    // when
    ironSourceRtbInterstitialAd.loadRtbAd(mockInterstitialAdConfig)

    // then
    mockInterstitialAdLoader.verify { InterstitialAdLoader.loadAd(requestCaptor.capture(), any()) }
    val capturedRequest = requestCaptor.firstValue
    assertThat(capturedRequest.extraParams).string("google_watermark").isEqualTo(TEST_WATERMARK)
  }
}
