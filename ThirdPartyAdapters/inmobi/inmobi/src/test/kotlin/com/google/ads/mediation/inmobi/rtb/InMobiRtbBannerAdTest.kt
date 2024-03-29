package com.google.ads.mediation.inmobi.rtb

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiBannerWrapper
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiRtbBannerAdTest {

  private val bannerAdConfiguration = mock<MediationBannerAdConfiguration>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiBannerWrapper = mock<InMobiBannerWrapper>()
  private val mediationBannerAdCallback = mock<MediationBannerAdCallback>()
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()

  lateinit var rtbBannerAd: InMobiRtbBannerAd
  lateinit var adMetaInfo: AdMetaInfo

  @Before
  fun setUp() {

    adMetaInfo = AdMetaInfo("fake", null)
    whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationBannerAdCallback)

    rtbBannerAd =
      InMobiRtbBannerAd(
        bannerAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory
      )
  }

  @Test
  fun onUserLeftApplication_invokesOnAdLeftApplicationCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    rtbBannerAd.onUserLeftApplication(inMobiBannerWrapper.inMobiBanner)

    verify(mediationBannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdLoadSucceeded_invokesOnSuccessCallback() {
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    verify(mediationAdLoadCallback).onSuccess(rtbBannerAd)
  }

  @Test
  fun onAdLoadFailed_invokesOnFailureCallback() {
    val inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)

    rtbBannerAd.onAdLoadFailed(inMobiBannerWrapper.inMobiBanner, inMobiAdRequestStatus)

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code)
      .isEqualTo(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus))
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.INMOBI_SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdDisplayed_invokesOnAdOpenedCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)
    rtbBannerAd.onAdDisplayed(inMobiBannerWrapper.inMobiBanner)

    verify(mediationBannerAdCallback).onAdOpened()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosedCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)
    rtbBannerAd.onAdDismissed(inMobiBannerWrapper.inMobiBanner)

    verify(mediationBannerAdCallback).onAdClosed()
  }

  @Test
  fun onAdClicked_invokesReportAdClickedCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)
    rtbBannerAd.onAdClicked(inMobiBannerWrapper.inMobiBanner, null)

    verify(mediationBannerAdCallback).reportAdClicked()
  }

  @Test
  fun onAdImpression_invokesReportAdImpressionCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)
    rtbBannerAd.onAdImpression(inMobiBannerWrapper.inMobiBanner)

    verify(mediationBannerAdCallback).reportAdImpression()
  }
}
