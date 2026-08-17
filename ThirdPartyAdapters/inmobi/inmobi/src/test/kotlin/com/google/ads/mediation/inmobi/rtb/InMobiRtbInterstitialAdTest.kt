package com.google.ads.mediation.inmobi.rtb

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiAdapterUtils.KEY_PLACEMENT_ID
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiRtbInterstitialAdTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val interstitialAdConfiguration =
    mock<MediationInterstitialAdConfiguration>() { on { context } doReturn context }
  private val mediationInterstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      mediationInterstitialAdCallback
    )
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiInterstitialWrapper = mock<InMobiInterstitialWrapper>()

  private lateinit var rtbInterstitialAd: InMobiRtbInterstitialAd
  private lateinit var adMetaInfo: AdMetaInfo

  @Before
  fun setUp() {
    adMetaInfo = AdMetaInfo("fake", null)
    rtbInterstitialAd =
      InMobiRtbInterstitialAd(mediationAdLoadCallback, inMobiInitializer, inMobiAdFactory)
  }

  @Test
  fun onShowAd_ifInterstitialAdIsReady_AdIsShown() {
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiInterstitialWrapper)
    whenever(interstitialAdConfiguration.bidResponse).thenReturn("BiddingToken")
    whenever(inMobiInterstitialWrapper.isReady).thenReturn(true)
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(KEY_PLACEMENT_ID to "67890")

    rtbInterstitialAd.loadAd(interstitialAdConfiguration)
    rtbInterstitialAd.showAd(context)

    verify(inMobiInterstitialWrapper).show()
  }

  @Test
  fun onShowAd_ifInterstitialAdNotReady_invokesOnAdFailedToShowCallback() {
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiInterstitialWrapper)
    whenever(interstitialAdConfiguration.bidResponse).thenReturn("BiddingToken")
    whenever(inMobiInterstitialWrapper.isReady).thenReturn(false)
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(KEY_PLACEMENT_ID to "67890")
    rtbInterstitialAd.loadAd(interstitialAdConfiguration)
    // mimic an ad load.
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    rtbInterstitialAd.showAd(context)

    val expectedAdError =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_AD_NOT_READY,
        "InMobi interstitial ad is not yet ready to be shown.",
      )
    assertThat(mediationInterstitialAdCallback.isFailedToShow).isTrue()
    assertThat(mediationInterstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
    verify(inMobiInterstitialWrapper, never()).show()
  }

  @Test
  fun onUserLeftApplication_invokesOnAdLeftApplicationCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onUserLeftApplication(inMobiInterstitialWrapper.inMobiInterstitial)

    assertThat(mediationInterstitialAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onAdLoadSucceeded_invokesOnSuccessCallback() {
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    assertThat(mediationAdLoadCallback).hasSucceededWith(rtbInterstitialAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailureCallback() {
    val inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)
    val expectedAdError =
      InMobiConstants.createSdkError(
        InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
        inMobiAdRequestStatus.message.orEmpty(),
      )

    rtbInterstitialAd.onAdLoadFailed(
      inMobiInterstitialWrapper.inMobiInterstitial,
      inMobiAdRequestStatus,
    )

    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdDisplayed_invokesOnAdOpenedCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdDisplayed(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    assertThat(mediationInterstitialAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdDisplayFailed_invokesOnAdFailedToShowCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    rtbInterstitialAd.onAdDisplayFailed(inMobiInterstitialWrapper.inMobiInterstitial)

    val expectedAdError =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_AD_DISPLAY_FAILED,
        "InMobi SDK failed to display an interstitial ad.",
      )
    assertThat(mediationInterstitialAdCallback.isFailedToShow).isTrue()
    assertThat(mediationInterstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onAdDismissed_invokedOnAdClosedCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdDismissed(inMobiInterstitialWrapper.inMobiInterstitial)

    assertThat(mediationInterstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdClicked_invokedOnAdClickedCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdClicked(inMobiInterstitialWrapper.inMobiInterstitial, null)

    assertThat(mediationInterstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdImpression(inMobiInterstitialWrapper.inMobiInterstitial)

    assertThat(mediationInterstitialAdCallback.isImpressionReported).isTrue()
  }
}
