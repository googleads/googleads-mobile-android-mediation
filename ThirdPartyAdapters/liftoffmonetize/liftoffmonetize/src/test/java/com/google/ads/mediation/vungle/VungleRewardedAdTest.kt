package com.google.ads.mediation.vungle

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_USER_ID
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.RewardedAd
import com.vungle.ads.VungleError
import com.vungle.ads.internal.protos.Sdk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
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
  private val rewardedAdCallback = mock<MediationRewardedAdCallback>()
  private val rewardedAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>> {
      on { onSuccess(any()) } doReturn rewardedAdCallback
    }
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
      mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE, KEY_USER_ID to TEST_USER_ID)
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

    verify(rewardedAdLoadCallback).onSuccess(adapter)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn Sdk.SDKError.Reason.API_REQUEST_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK rewarded ad load failed."
      }

    adapter.onAdFailedToLoad(vungleRewardedAd, liftoffError)

    val expectedError =
      AdError(
        liftoffError.code,
        liftoffError.errorMessage,
        VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
      )
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
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

    verify(rewardedAdCallback).onAdOpened()
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdEnd_callsOnAdClosed() {
    renderAdAndMockLoadSuccess()

    adapter.onAdEnd(vungleRewardedAd)

    verify(rewardedAdCallback).onAdClosed()
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    renderAdAndMockLoadSuccess()

    adapter.onAdClicked(vungleRewardedAd)

    verify(rewardedAdCallback).reportAdClicked()
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdRewarded_callsOnVideoCompleteAndOnUserEarnedReward() {
    renderAdAndMockLoadSuccess()

    adapter.onAdRewarded(vungleRewardedAd)

    verify(rewardedAdCallback).onVideoComplete()
    verify(rewardedAdCallback)
      .onUserEarnedReward(
        argThat { rewardItem -> rewardItem.type == "vungle" && rewardItem.amount == 1 }
      )
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdLeftApplication_noInteractions() {
    renderAdAndMockLoadSuccess()

    adapter.onAdLeftApplication(vungleRewardedAd)

    verifyNoInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdFailedToPlay_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn Sdk.SDKError.Reason.AD_NOT_LOADED_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK rewarded ad play failed."
      }

    adapter.onAdFailedToPlay(vungleRewardedAd, liftoffError)

    val expectedError =
      AdError(
        liftoffError.code,
        liftoffError.errorMessage,
        VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
      )
    verify(rewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdImpression_callsOnVideoStartAndReportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapter.onAdImpression(vungleRewardedAd)

    verify(rewardedAdCallback).onVideoStart()
    verify(rewardedAdCallback).reportAdImpression()
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  private companion object {
    const val TEST_USER_ID = "testUserId"
  }
}
