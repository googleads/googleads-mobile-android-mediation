package com.google.ads.mediation.vungle.rtb

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.InitializationListener
import com.vungle.ads.RewardedAd
import com.vungle.ads.VungleError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Tests for [VungleRtbRewardedAd]. */
@RunWith(AndroidJUnit4::class)
class VungleRtbRewardedAdTest {

  /** Unit under test. */
  private lateinit var adapterRtbRewardedAd: VungleRtbRewardedAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val rewardedAdCallback = mock<MediationRewardedAdCallback>()
  private val rewardedAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>> {
      on { onSuccess(any()) } doReturn rewardedAdCallback
    }
  private val mockVungleInitializer = mock<VungleInitializer>()
  private val vungleRewardedAd = mock<RewardedAd>()
  private val vungleFactory =
    mock<VungleFactory> {
      on { createRewardedAd(any(), any(), any()) } doReturn vungleRewardedAd
      on { createAdConfig() } doReturn mock()
    }

  @Before
  fun setUp() {
    adapterRtbRewardedAd =
      VungleRtbRewardedAd(
        createMediationRewardedAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE)
        ),
        rewardedAdLoadCallback,
        vungleFactory
      )

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as InitializationListener).onSuccess()
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_callsLoadSuccess() {
    adapterRtbRewardedAd.onAdLoaded(vungleRewardedAd)

    verify(rewardedAdLoadCallback).onSuccess(adapterRtbRewardedAd)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn VungleError.AD_FAILED_TO_DOWNLOAD
        on { errorMessage } doReturn "Liftoff Monetize SDK rewarded ad load failed."
      }

    adapterRtbRewardedAd.onAdFailedToLoad(vungleRewardedAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(rewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun showAd_playsLiftoffAd() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterRtbRewardedAd.render()
    }

    adapterRtbRewardedAd.showAd(context)

    verify(vungleRewardedAd).play(context)
  }

  private fun renderAdAndMockLoadSuccess() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterRtbRewardedAd.render()
    }
    adapterRtbRewardedAd.onAdLoaded(vungleRewardedAd)
  }

  @Test
  fun onAdStart_callsOnAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterRtbRewardedAd.onAdStart(vungleRewardedAd)

    verify(rewardedAdCallback).onAdOpened()
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdEnd_callsOnAdClosed() {
    renderAdAndMockLoadSuccess()

    adapterRtbRewardedAd.onAdEnd(vungleRewardedAd)

    verify(rewardedAdCallback).onAdClosed()
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    renderAdAndMockLoadSuccess()

    adapterRtbRewardedAd.onAdClicked(vungleRewardedAd)

    verify(rewardedAdCallback).reportAdClicked()
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdRewarded_callsOnVideoCompleteAndOnUserEarnedReward() {
    renderAdAndMockLoadSuccess()

    adapterRtbRewardedAd.onAdRewarded(vungleRewardedAd)

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

    adapterRtbRewardedAd.onAdLeftApplication(vungleRewardedAd)

    verifyNoInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdFailedToPlay_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn VungleError.AD_UNABLE_TO_PLAY
        on { errorMessage } doReturn "Liftoff Monetize SDK rewarded ad play failed."
      }

    adapterRtbRewardedAd.onAdFailedToPlay(vungleRewardedAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(rewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
    verifyNoMoreInteractions(rewardedAdCallback)
  }

  @Test
  fun onAdImpression_callsOnVideoStartAndReportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterRtbRewardedAd.onAdImpression(vungleRewardedAd)

    verify(rewardedAdCallback).onVideoStart()
    verify(rewardedAdCallback).reportAdImpression()
    verifyNoMoreInteractions(rewardedAdCallback)
  }
}
