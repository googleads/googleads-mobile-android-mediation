package com.google.ads.mediation.vungle

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_USER_ID
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_CANNOT_PLAY_AD
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.RewardedAd
import com.vungle.ads.VungleError
import com.vungle.ads.internal.protos.Sdk.SDKError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [VungleMediationAdapter]'s implementation of [MediationRewardedAd] and Liftoff's
 * [RewardedAdListener].
 */
@RunWith(AndroidJUnit4::class)
class VungleRewardedAdTest {

  /** Unit under test. */
  private lateinit var adapter: VungleMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
  private val vungleInitializer = mock<VungleInitializer>()
  private val vungleRewardedAd = mock<RewardedAd>()
  private val vungleFactory =
    mock<VungleFactory> {
      on { createRewardedAd(any(), any(), any()) } doReturn vungleRewardedAd
      on { createAdConfig() } doReturn mock()
    }
  private val rewardedAdConfiguration =
    createMediationRewardedAdConfiguration(
      context = context,
      serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID),
    )

  @Before
  fun setUp() {
    adapter = VungleMediationAdapter(vungleFactory)

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(vungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_callsLoadSuccess() {
    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    adapter.onAdLoaded(vungleRewardedAd)

    assertThat(rewardedAdLoadCallback).hasSucceededWith(adapter)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.API_REQUEST_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK rewarded ad load failed."
      }

    adapter.onAdFailedToLoad(vungleRewardedAd, liftoffError)

    val expectedError =
      AdError(
        liftoffError.code,
        liftoffError.errorMessage,
        VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN,
      )
    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun showAd_playsLiftoffAd() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)
    }

    adapter.showAd(context)

    verify(vungleRewardedAd).play(context)
  }

  @Test
  fun showAd_whenRewardedAdIsNull_invokesOnAdFailedToShow() {
    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)
    adapter.onAdLoaded(vungleRewardedAd)

    adapter.showAd(context)

    val expectedError =
      AdError(
        ERROR_CANNOT_PLAY_AD,
        "Failed to show waterfall rewarded ad from Liftoff Monetize.",
        ERROR_DOMAIN,
      )
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedError)
  }

  private fun renderAdAndMockLoadSuccess() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)
    }
    adapter.onAdLoaded(vungleRewardedAd)
  }

  @Test
  fun onAdStart_callsOnAdOpened() {
    renderAdAndMockLoadSuccess()

    adapter.onAdStart(vungleRewardedAd)

    assertThat(rewardedAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdEnd_callsOnAdClosed() {
    renderAdAndMockLoadSuccess()

    adapter.onAdEnd(vungleRewardedAd)

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    renderAdAndMockLoadSuccess()

    adapter.onAdClicked(vungleRewardedAd)

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdRewarded_callsOnVideoCompleteAndOnUserEarnedReward() {
    renderAdAndMockLoadSuccess()

    adapter.onAdRewarded(vungleRewardedAd)

    assertThat(rewardedAdCallback.isVideoCompleted).isTrue()
    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onAdLeftApplication_noInteractions() {
    renderAdAndMockLoadSuccess()

    adapter.onAdLeftApplication(vungleRewardedAd)

    assertThat(rewardedAdCallback.isOpened).isFalse()
    assertThat(rewardedAdCallback.isClosed).isFalse()
    assertThat(rewardedAdCallback.isClicked).isFalse()
    assertThat(rewardedAdCallback.isImpressionReported).isFalse()
  }

  @Test
  fun onAdFailedToPlay_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.AD_NOT_LOADED_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK rewarded ad play failed."
      }

    adapter.onAdFailedToPlay(vungleRewardedAd, liftoffError)

    val expectedError =
      AdError(
        liftoffError.code,
        liftoffError.errorMessage,
        VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN,
      )
    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedError)
  }

  @Test
  fun onAdImpression_callsOnVideoStartAndReportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapter.onAdImpression(vungleRewardedAd)

    assertThat(rewardedAdCallback.isVideoStarted).isTrue()
    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  private companion object {
    const val TEST_USER_ID = "testUserId"
  }
}
