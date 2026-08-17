package com.google.ads.mediation.ironsource

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.ironsourceads.rewarded.RewardedAd
import com.unity3d.ironsourceads.rewarded.RewardedAdLoader
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IronSourceRtbRewardedAdTest {
  private lateinit var ironSourceRtbRewardedAd: IronSourceRtbRewardedAd

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val bundle = Bundle().apply { putString("instanceId", "mockInstanceId") }

  private val mockRewardedAdConfig: MediationRewardedAdConfiguration = mock {
    on { context } doReturn context
    on { serverParameters } doReturn bundle
    on { bidResponse } doReturn TEST_BID_RESPONSE
    on { watermark } doReturn TEST_WATERMARK
  }
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
  private val mockRewardedAd: RewardedAd = mock()
  private val mockRewardedAdLoader = mockStatic(RewardedAdLoader::class.java)

  @Before
  fun setUp() {
    ironSourceRtbRewardedAd = IronSourceRtbRewardedAd(rewardedAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockRewardedAdLoader.close()
  }

  @Test
  fun onLoadRtbAd_verifyOnSuccessCallback() {
    // When
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // Then
    assertThat(rewardedAdLoadCallback).hasSucceededWith(ironSourceRtbRewardedAd)
  }

  @Test
  fun onRewardedVideoAdLoadSuccess_verifyOnSuccessCallback() {
    // When
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // Then
    assertThat(rewardedAdLoadCallback).hasSucceededWith(ironSourceRtbRewardedAd)
  }

  @Test
  fun onRewardedAdLoadFailed_verifyOnFailureCallback() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    val errorRes = "An error occurred"
    val errorCode = 123
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // When
    ironSourceRtbRewardedAd.onRewardedAdLoadFailed(ironSourceError)

    // Then
    val expectedAdError = AdError(errorCode, errorRes, "com.ironsource.mediationsdk")
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun showAd_verifyShowAdInvoked() {
    // Given
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)
    val activity = Robolectric.buildActivity(Activity::class.java).get()

    // When
    ironSourceRtbRewardedAd.showAd(activity)

    // Then
    verify(mockRewardedAd).show(activity)
  }

  @Test
  fun onRewardedAdShowFailed_verifyOnAdFailedToShow() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    val errorRes = "An error occurred"
    val errorCode = 123
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // When
    ironSourceRtbRewardedAd.onRewardedAdFailedToShow(mockRewardedAd, ironSourceError)

    // Then
    val expectedAdError = AdError(errorCode, errorRes, IRONSOURCE_SDK_ERROR_DOMAIN)
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun showAd_invalidContext_expectObFailureCallbackWithError() {
    // given
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // when
    ironSourceRtbRewardedAd.showAd(context)

    // then
    val expectedAdError =
      AdError(
        102,
        "IronSource requires an Activity context to load ads.",
        "com.google.ads.mediation.ironsource",
      )
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onRewardedAdShowFailed_withoutRewardedAdCallbackInstance_verifyOnAdFailedToShow() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    val errorRes = "An error occurred"
    val errorCode = 123
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // When
    ironSourceRtbRewardedAd.onRewardedAdFailedToShow(mockRewardedAd, ironSourceError)

    // Then
    assertThat(rewardedAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun onRewardedAdOpened_withRewardedAd_verifyOnRewardedAdOpenedCallbacks() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onRewardedAdShown(mockRewardedAd)

    // Then
    assertThat(rewardedAdCallback.isOpened).isTrue()
    assertThat(rewardedAdCallback.isVideoStarted).isTrue()
    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onRewardedAdClosed_withRewardedAd_verifyOnAdClosedCallback() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onRewardedAdDismissed(mockRewardedAd)

    // Then
    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onRewardedVideoAdRewarded_withRewardedAd_verifyOnRewardedCallbacks() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onUserEarnedReward(mockRewardedAd)

    // Then
    assertThat(rewardedAdCallback.isVideoCompleted).isTrue()
    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onRewardedAdClicked_withRewardedAd_verifyReportAdClickedCallback() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd(mockRewardedAdConfig)
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onRewardedAdClicked(mockRewardedAd)

    // Then
    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdEvents_withoutRewardedAd_verifyNoCallbacks() {
    // When
    ironSourceRtbRewardedAd.onRewardedAdShown(mockRewardedAd)
    ironSourceRtbRewardedAd.onRewardedAdDismissed(mockRewardedAd)
    ironSourceRtbRewardedAd.onRewardedAdClicked(mockRewardedAd)

    // Then
    assertThat(rewardedAdCallback.isOpened).isFalse()
    assertThat(rewardedAdCallback.isVideoStarted).isFalse()
    assertThat(rewardedAdCallback.isImpressionReported).isFalse()
    assertThat(rewardedAdCallback.isClosed).isFalse()
    assertThat(rewardedAdCallback.isClicked).isFalse()
  }
}
