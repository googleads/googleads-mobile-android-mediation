package com.google.ads.mediation.unity

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.WATERMARK
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationLoadErrorCode
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationShowErrorCode
import com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_NON_ACTIVITY
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.ShowConfiguration
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAdsError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class UnityRewardedAdTest {

  // Subject of tests
  private lateinit var unityRewardedAd: UnityRewardedAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val rewardedAdConfiguration: MediationRewardedAdConfiguration = mock {
    on { watermark } doReturn TEST_WATERMARK
  }
  private val rewardedAdCallback: MediationRewardedAdCallback = mock()
  private val rewardedAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock()
  private val unityAdsLoader: UnityAdsLoader = mock()
  private val unityInitializer: UnityInitializer = spy(UnityInitializer.getInstance())

  @Before
  fun setUp() {
    unityRewardedAd =
      UnityRewardedAd(
        rewardedAdConfiguration,
        rewardedAdLoadCallback,
        unityInitializer,
        unityAdsLoader,
      )

    doReturn(rewardedAdCallback).whenever(rewardedAdLoadCallback).onSuccess(unityRewardedAd)
  }

  @Test
  fun onUnityAdsAdLoaded_invokesOnSuccess() {
    unityRewardedAd.loadListener.onAdLoaded(mock(), null)

    verify(rewardedAdLoadCallback).onSuccess(unityRewardedAd)
  }

  @Test
  fun onUnityAdsFailedToLoad_invokesOnFailure() {
    val errorCaptor = argumentCaptor<AdError>()
    val unityAdsLoadError = mock<UnityAdsError>()
    whenever(unityAdsLoadError.code).doReturn(52100)
    whenever(unityAdsLoadError.message).doReturn(TEST_ERROR_MESSAGE)
    val errorCode = getMediationLoadErrorCode(unityAdsLoadError)

    unityRewardedAd.loadListener.onAdLoaded(null, unityAdsLoadError)

    verify(rewardedAdLoadCallback).onFailure(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(TEST_ERROR_MESSAGE)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onUnityAdsShowStart_invokesOnAdOpenedReportAdImpressionAndOnVideoStart() {
    val rewardedAd = mock<RewardedAd>()
    unityRewardedAd.loadListener.onAdLoaded(rewardedAd, null)

    unityRewardedAd.unityShowListener.onStarted(rewardedAd)

    verify(rewardedAdCallback).onAdOpened()
    verify(rewardedAdCallback).reportAdImpression()
    verify(rewardedAdCallback).onVideoStart()
  }

  @Test
  fun onUnityAdsShowStart_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    val rewardedAd = mock<RewardedAd>()

    unityRewardedAd.unityShowListener.onStarted(rewardedAd)

    verify(rewardedAdCallback, never()).onAdOpened()
    verify(rewardedAdCallback, never()).reportAdImpression()
    verify(rewardedAdCallback, never()).onVideoStart()
  }

  @Test
  fun onUnityAdsShowClick_invokesReportAdClicked() {
    val rewardedAd = mock<RewardedAd>()
    unityRewardedAd.loadListener.onAdLoaded(rewardedAd, null)

    unityRewardedAd.unityShowListener.onClicked(rewardedAd)

    verify(rewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onUnityAdsShowClick_withNullAdCallback_doesNotInvokeReportAdClicked() {
    unityRewardedAd.unityShowListener.onClicked(mock())

    verify(rewardedAdCallback, never()).reportAdClicked()
  }

  @Test
  fun onUnityAdsShowComplete_withStateCompleted_invokesOnVideoCompleteAndOnAdClosed() {
    val rewardedAd = mock<RewardedAd>()
    unityRewardedAd.loadListener.onAdLoaded(rewardedAd, null)

    unityRewardedAd.unityShowListener.onCompleted(
      rewardedAd,
      ShowFinishState.COMPLETED,
    )

    verify(rewardedAdCallback).onVideoComplete()
    verify(rewardedAdCallback).onAdClosed()

    // this is triggered by a new callback. Should not be triggered here
    verify(rewardedAdCallback, never()).onUserEarnedReward()
  }

  @Test
  fun onUnityAdsShowComplete_withStateNotCompleted_invokesOnlyOnAdClosed() {
    unityRewardedAd.loadListener.onAdLoaded(mock(), null)

    unityRewardedAd.unityShowListener.onCompleted(
      mock(),
      ShowFinishState.SKIPPED,
    )

    verify(rewardedAdCallback, never()).onVideoComplete()
    verify(rewardedAdCallback, never()).onUserEarnedReward()
    verify(rewardedAdCallback).onAdClosed()
  }

  @Test
  fun onUnityAdsShowComplete_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    unityRewardedAd.unityShowListener.onCompleted(
      mock(),
      ShowFinishState.COMPLETED,
    )

    verify(rewardedAdCallback, never()).onVideoComplete()
    verify(rewardedAdCallback, never()).onAdClosed()
  }

  @Test
  fun onUnityAdsShowFailure_invokesOnAdFailedToShow() {
    val errorCaptor = argumentCaptor<AdError>()
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.message).doReturn(TEST_ERROR_MESSAGE)
    whenever(unityAdsError.code).doReturn(52202)
    val errorCode = getMediationShowErrorCode(unityAdsError)
    unityRewardedAd.loadListener.onAdLoaded(mock(), null)

    unityRewardedAd.unityShowListener.onFailed(mock(), unityAdsError)

    verify(rewardedAdCallback).onAdFailedToShow(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(TEST_ERROR_MESSAGE)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onUnityAdsShowFailure_withNullAdCallback_doesNotInvokeOnAdFailedToShow() {
    unityRewardedAd.unityShowListener.onFailed(mock(), mock())

    verify(rewardedAdCallback, never()).onAdFailedToShow(any<AdError>())
  }

  @Test
  fun showAd_withNonActivityContext_invokesOnAdFailedToShow() {
    unityRewardedAd.loadListener.onAdLoaded(mock(), null)
    val errorCaptor = argumentCaptor<AdError>()

    unityRewardedAd.showAd(ApplicationProvider.getApplicationContext())

    verify(rewardedAdCallback).onAdFailedToShow(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(ERROR_CONTEXT_NOT_ACTIVITY)
    assertThat(capturedError.message).isEqualTo(ERROR_MSG_NON_ACTIVITY)
    assertThat(capturedError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun showAd_invokesUnityAdsShow() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[1] as InitializationListener).onInitializationComplete(null)
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any())
    whenever(rewardedAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(rewardedAdConfiguration.context) doReturn activity
    unityRewardedAd.loadAd(rewardedAdConfiguration)

    val loadedAd = mock<RewardedAd>()

    unityRewardedAd.loadListener.onAdLoaded(loadedAd, null)

    unityRewardedAd.showAd(activity)

    val configCaptor = argumentCaptor<ShowConfiguration>()
    verify(loadedAd).show(eq(activity), configCaptor.capture(), any())
    assertThat(configCaptor.firstValue.extras[WATERMARK]).isEqualTo(TEST_WATERMARK)
  }

  @Test
  fun showAd_withNullPlacementId_invokesUnityAdsShowWithNullId() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[1] as InitializationListener).onInitializationComplete(null)
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any())
    whenever(rewardedAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(rewardedAdConfiguration.context) doReturn activity
    unityRewardedAd.loadAd(rewardedAdConfiguration)
    val unityAdsLoadError = UnityAdsLoadError.NO_FILL

    unityRewardedAd.loadListener.onAdLoaded(
      null,
      mock<UnityAdsError>().apply {
        whenever(this.code).doReturn(0)
        whenever(this.message).doReturn(TEST_ERROR_MESSAGE)
      }
    )

    unityRewardedAd.showAd(activity)

    verifyNoInteractions(rewardedAdCallback)
  }

  companion object {
    private const val TEST_PLACEMENT_ID = "test_placement_id"
    private const val TEST_GAME_ID = "test_game_id"
    private const val TEST_LOADED_PLACEMENT_ID = "test_loaded_placement_id"
    private const val TEST_ERROR_MESSAGE = "test_error_message"
    private const val TEST_WATERMARK = "test_watermark"
  }
}
