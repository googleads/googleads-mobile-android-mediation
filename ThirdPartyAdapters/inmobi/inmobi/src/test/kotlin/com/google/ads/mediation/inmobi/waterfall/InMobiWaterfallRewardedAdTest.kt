package com.google.ads.mediation.inmobi.waterfall

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiWaterfallRewardedAdTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rewardedAdConfiguration =
    mock<MediationRewardedAdConfiguration>() { on { context } doReturn context }
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiRewardedWrapper = mock<InMobiInterstitialWrapper>()
  private val mediationRewardedAdCallback = mock<MediationRewardedAdCallback>()

  lateinit var waterfallRewardedAd: InMobiWaterfallRewardedAd
  lateinit var adMetaInfo: AdMetaInfo

  @Before
  fun setUp() {
    adMetaInfo = AdMetaInfo("fake", null)
    whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationRewardedAdCallback)

    waterfallRewardedAd =
      InMobiWaterfallRewardedAd(
        rewardedAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory,
      )
  }

  @Test
  fun onShowAd_ifRewardedAdNotReady_invokesRewardedAdCallback() {
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiRewardedWrapper)
    whenever(inMobiRewardedWrapper.isReady).thenReturn(false)

    val placementId = 67890L
    // invoke the create rewardedAd method to get an instance of InMobiRewardedWrapper
    waterfallRewardedAd.createAndLoadRewardAd(context, placementId, mediationAdLoadCallback)
    // mimic an ad load
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    waterfallRewardedAd.showAd(context)

    val captor = argumentCaptor<AdError>()
    verify(mediationRewardedAdCallback).onAdFailedToShow(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_AD_NOT_READY)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun onShowAd_ifRewardedAdIsReady_AdIsShown() {
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiRewardedWrapper)
    whenever(inMobiRewardedWrapper.isReady).thenReturn(true)

    val placementId = 67890L
    // invoke the create rewardedAd method to get an instance of InMobiRewardedWrapper
    waterfallRewardedAd.createAndLoadRewardAd(context, placementId, mediationAdLoadCallback)
    waterfallRewardedAd.showAd(context)

    verify(inMobiRewardedWrapper).show()
  }

  @Test
  fun onAdDisplayed_invokesOnAdOpenedAndOnVideoStartCallback() {
    // mimic an ad load
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    waterfallRewardedAd.onAdDisplayed(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)

    verify(mediationRewardedAdCallback).onAdOpened()
    verify(mediationRewardedAdCallback).onVideoStart()
  }

  @Test
  fun onAdDisplayFailed_invokesOnAdFailedToShowCallback() {
    // mimic an ad load
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    waterfallRewardedAd.onAdDisplayFailed(inMobiRewardedWrapper.inMobiInterstitial)

    val captor = argumentCaptor<AdError>()
    verify(mediationRewardedAdCallback).onAdFailedToShow(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_AD_DISPLAY_FAILED)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun onAdDismissed_invokesOnAdClosedCallback() {
    // mimic an ad load
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    waterfallRewardedAd.onAdDismissed(inMobiRewardedWrapper.inMobiInterstitial)

    verify(mediationRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onRewardsUnlocked_invokesOnUserEarnedReward() {
    val expectedRewardType = "SecondReward"
    val expectedReward = "2"
    var rewards = mapOf<Any, Any>("firstReward" to "", expectedRewardType to expectedReward)

    // mimic an ad load
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    waterfallRewardedAd.onRewardsUnlocked(inMobiRewardedWrapper.inMobiInterstitial, rewards)

    verify(mediationRewardedAdCallback).onVideoComplete()
    verify(mediationRewardedAdCallback).onUserEarnedReward()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    // mimic an ad load
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    waterfallRewardedAd.onAdClicked(inMobiRewardedWrapper.inMobiInterstitial, null)

    verify(mediationRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    // mimic an ad load
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)
    waterfallRewardedAd.onAdImpression(inMobiRewardedWrapper.inMobiInterstitial)

    verify(mediationRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onAdLoadFailed_invokesOnFailureCallback() {
    var inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)

    waterfallRewardedAd.onAdLoadFailed(
      inMobiRewardedWrapper.inMobiInterstitial,
      inMobiAdRequestStatus,
    )

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code)
      .isEqualTo(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus))
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.INMOBI_SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdLoadSucceeded_invokesOnSuccessCallback() {
    waterfallRewardedAd.onAdLoadSucceeded(inMobiRewardedWrapper.inMobiInterstitial, adMetaInfo)

    verify(mediationAdLoadCallback).onSuccess(ArgumentMatchers.any(waterfallRewardedAd::class.java))
  }
}
