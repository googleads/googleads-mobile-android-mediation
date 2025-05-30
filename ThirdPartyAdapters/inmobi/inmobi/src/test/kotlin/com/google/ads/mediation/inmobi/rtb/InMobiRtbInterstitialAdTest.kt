package com.google.ads.mediation.inmobi.rtb

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiAdapterUtils.KEY_PLACEMENT_ID
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiInterstitialWrapper = mock<InMobiInterstitialWrapper>()
  private val mediationInterstitialAdCallback = mock<MediationInterstitialAdCallback>()

  lateinit var rtbInterstitialAd: InMobiRtbInterstitialAd
  lateinit var adMetaInfo: AdMetaInfo

  @Before
  fun setUp() {
    adMetaInfo = AdMetaInfo("fake", null)
    whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationInterstitialAdCallback)

    rtbInterstitialAd =
      InMobiRtbInterstitialAd(
        interstitialAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory,
      )
  }

  @Test
  fun onShowAd_ifInterstitialAdIsReady_AdIsShown() {
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiInterstitialWrapper)
    whenever(interstitialAdConfiguration.bidResponse).thenReturn("BiddingToken")
    whenever(inMobiInterstitialWrapper.isReady).thenReturn(true)
    whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationInterstitialAdCallback)
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(KEY_PLACEMENT_ID to "67890")

    rtbInterstitialAd.loadAd()
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
    rtbInterstitialAd.loadAd()
    // mimic an ad load.
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    rtbInterstitialAd.showAd(context)

    val captor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdCallback).onAdFailedToShow(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_AD_NOT_READY)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
    verify(inMobiInterstitialWrapper, never()).show()
  }

  @Test
  fun onUserLeftApplication_invokesOnAdLeftApplicationCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onUserLeftApplication(inMobiInterstitialWrapper.inMobiInterstitial)

    verify(mediationInterstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdLoadSucceeded_invokesOnSuccessCallback() {
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    verify(mediationAdLoadCallback).onSuccess(ArgumentMatchers.any(rtbInterstitialAd::class.java))
  }

  @Test
  fun onAdLoadFailed_invokesOnFailureCallback() {
    var inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)

    rtbInterstitialAd.onAdLoadFailed(
      inMobiInterstitialWrapper.inMobiInterstitial,
      inMobiAdRequestStatus,
    )

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code)
      .isEqualTo(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus))
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.INMOBI_SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdDisplayed_invokesOnAdOpenedCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdDisplayed(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    verify(mediationInterstitialAdCallback).onAdOpened()
  }

  @Test
  fun onAdDisplayFailed_invokesOnAdFailedToShowCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    rtbInterstitialAd.onAdDisplayFailed(inMobiInterstitialWrapper.inMobiInterstitial)

    val captor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdCallback).onAdFailedToShow(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_AD_DISPLAY_FAILED)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun onAdDismissed_invokedOnAdClosedCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdDismissed(inMobiInterstitialWrapper.inMobiInterstitial)

    verify(mediationInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun onAdClicked_invokedOnAdClickedCallback() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdClicked(inMobiInterstitialWrapper.inMobiInterstitial, null)

    verify(mediationInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    // mimic an ad load
    rtbInterstitialAd.onAdLoadSucceeded(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)
    rtbInterstitialAd.onAdImpression(inMobiInterstitialWrapper.inMobiInterstitial)

    verify(mediationInterstitialAdCallback).reportAdImpression()
  }
}
