package com.google.ads.mediation.inmobi.rtb

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiBannerWrapper
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class InMobiRtbBannerAdTest {

  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiBannerWrapper = mock<InMobiBannerWrapper>()
  private val mediationBannerAdCallback = FakeMediationBannerAdCallback()
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(
      mediationBannerAdCallback
    )

  private lateinit var rtbBannerAd: InMobiRtbBannerAd
  private lateinit var adMetaInfo: AdMetaInfo

  @Before
  fun setUp() {
    adMetaInfo = AdMetaInfo("fake", null)
    rtbBannerAd = InMobiRtbBannerAd(mediationAdLoadCallback, inMobiInitializer, inMobiAdFactory)
  }

  @Test
  fun onUserLeftApplication_invokesOnAdLeftApplicationCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    rtbBannerAd.onUserLeftApplication(inMobiBannerWrapper.inMobiBanner)

    assertThat(mediationBannerAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onAdLoadSucceeded_invokesOnSuccessCallback() {
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    assertThat(mediationAdLoadCallback).hasSucceededWith(rtbBannerAd)
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

    rtbBannerAd.onAdLoadFailed(inMobiBannerWrapper.inMobiBanner, inMobiAdRequestStatus)

    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdDisplayed_invokesOnAdOpenedCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    rtbBannerAd.onAdDisplayed(inMobiBannerWrapper.inMobiBanner)

    assertThat(mediationBannerAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdDismissed_invokesOnAdClosedCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    rtbBannerAd.onAdDismissed(inMobiBannerWrapper.inMobiBanner)

    assertThat(mediationBannerAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClickedCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    rtbBannerAd.onAdClicked(inMobiBannerWrapper.inMobiBanner, null)

    assertThat(mediationBannerAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdImpression_invokesReportAdImpressionCallback() {
    // mimic an ad load first
    rtbBannerAd.onAdLoadSucceeded(inMobiBannerWrapper.inMobiBanner, adMetaInfo)

    rtbBannerAd.onAdImpression(inMobiBannerWrapper.inMobiBanner)

    assertThat(mediationBannerAdCallback.isImpressionReported).isTrue()
  }
}
