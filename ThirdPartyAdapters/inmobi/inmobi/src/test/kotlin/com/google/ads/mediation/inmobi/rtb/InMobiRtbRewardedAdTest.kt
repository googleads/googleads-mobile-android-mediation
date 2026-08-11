package com.google.ads.mediation.inmobi.rtb

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiAdapterUtils.KEY_PLACEMENT_ID
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiRtbRewardedAdTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rewardedAdConfiguration =
    mock<MediationRewardedAdConfiguration>() { on { context } doReturn context }
  private val mediationRewardedAdCallback = FakeMediationRewardedAdCallback()
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      mediationRewardedAdCallback
    )
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiRewardedWrapper = mock<InMobiInterstitialWrapper>()

  private lateinit var rtbRewardedAd: InMobiRtbRewardedAd
  private lateinit var adMetaInfo: AdMetaInfo

  @Before
  fun setUp() {
    adMetaInfo = AdMetaInfo("fake", null)
    rtbRewardedAd = InMobiRtbRewardedAd(mediationAdLoadCallback, inMobiInitializer, inMobiAdFactory)
  }

  @Test
  fun onShowAd_ifRewardedAdNotReady_invokesOnAdFailedToShow() {
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiRewardedWrapper)
    whenever(inMobiRewardedWrapper.isReady).thenReturn(false)
    whenever(rewardedAdConfiguration.bidResponse).thenReturn("BiddingToken")
    whenever(rewardedAdConfiguration.serverParameters) doReturn
      bundleOf(KEY_PLACEMENT_ID to "67890")

    // invoke the create rewardedAd method to get an instance of InMobiRewardedWrapper
    rtbRewardedAd.loadAd(rewardedAdConfiguration)
    // mimic an ad load
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    rtbRewardedAd.showAd(context)

    val expectedAdError =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_AD_NOT_READY,
        "InMobi rewarded ad is not yet ready to be shown.",
      )
    assertThat(mediationRewardedAdCallback.isFailedToShow).isTrue()
    assertThat(mediationRewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onShowAd_ifRewardedAdIsReady_AdIsShown() {
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiRewardedWrapper)
    whenever(inMobiRewardedWrapper.isReady).thenReturn(true)
    whenever(rewardedAdConfiguration.bidResponse).thenReturn("BiddingToken")
    whenever(rewardedAdConfiguration.serverParameters) doReturn
      bundleOf(KEY_PLACEMENT_ID to "67890")

    // invoke the create rewardedAd method to get an instance of InMobiRewardedWrapper
    rtbRewardedAd.loadAd(rewardedAdConfiguration)
    rtbRewardedAd.showAd(context)

    verify(inMobiRewardedWrapper).show()
  }

  @Test
  fun onAdDisplayed_invokesOnAdOpenedAndOnVideoStartCallback() {
    // mimic an ad load
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    rtbRewardedAd.onAdDisplayed(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)

    assertThat(mediationRewardedAdCallback.isOpened).isTrue()
    assertThat(mediationRewardedAdCallback.isVideoStarted).isTrue()
  }

  @Test
  fun onAdDisplayFailed_invokesOnAdFailedToShowCallback() {
    // mimic an ad load
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    rtbRewardedAd.onAdDisplayFailed(inMobiRewardedWrapper.inMobiInterstitial)

    val expectedAdError =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_AD_DISPLAY_FAILED,
        "InMobi rewarded ad failed to show.",
      )
    assertThat(mediationRewardedAdCallback.isFailedToShow).isTrue()
    assertThat(mediationRewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onAdDismissed_invokesOnAdClosedCallback() {
    // mimic an ad load
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    rtbRewardedAd.onAdDismissed(inMobiRewardedWrapper.inMobiInterstitial)

    assertThat(mediationRewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onRewardsUnlocked_invokesOnUserEarnedReward() {
    val expectedRewardType = "SecondReward"
    val expectedReward = "2"
    val rewards = mapOf<Any, Any>("firstReward" to "", expectedRewardType to expectedReward)

    // mimic an ad load
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    rtbRewardedAd.onRewardsUnlocked(inMobiRewardedWrapper.inMobiInterstitial, rewards)

    assertThat(mediationRewardedAdCallback.isVideoCompleted).isTrue()
    assertThat(mediationRewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    // mimic an ad load
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    rtbRewardedAd.onAdClicked(inMobiRewardedWrapper.inMobiInterstitial, null)

    assertThat(mediationRewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    // mimic an ad load
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    rtbRewardedAd.onAdImpression(inMobiRewardedWrapper.inMobiInterstitial)

    assertThat(mediationRewardedAdCallback.isImpressionReported).isTrue()
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

    rtbRewardedAd.onAdLoadFailed(inMobiRewardedWrapper.inMobiInterstitial, inMobiAdRequestStatus)

    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onAdLoadSucceeded_invokesOnSuccessCallback() {
    rtbRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)

    assertThat(mediationAdLoadCallback).hasSucceededWith(rtbRewardedAd)
  }
}
