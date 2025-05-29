package com.google.ads.mediation.unity

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode
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
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.notNull
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class UnityRewardedAdTest {

  // Subject of tests
  private lateinit var unityRewardedAd: UnityRewardedAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val rewardedAdConfiguration: MediationRewardedAdConfiguration = mock()
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
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    verify(rewardedAdLoadCallback).onSuccess(unityRewardedAd)
  }

  @Test
  fun onUnityAdsFailedToLoad_invokesOnFailure() {
    val errorCaptor = argumentCaptor<AdError>()
    val unityAdsLoadError = UnityAdsLoadError.NO_FILL
    val errorCode = getMediationErrorCode(unityAdsLoadError)

    unityRewardedAd.unityLoadListener.onUnityAdsFailedToLoad(
      TEST_PLACEMENT_ID,
      unityAdsLoadError,
      TEST_ERROR_MESSAGE,
    )

    verify(rewardedAdLoadCallback).onFailure(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(TEST_ERROR_MESSAGE)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onUnityAdsShowStart_invokesOnAdOpenedReportAdImpressionAndOnVideoStart() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowStart(TEST_PLACEMENT_ID)

    verify(rewardedAdCallback).onAdOpened()
    verify(rewardedAdCallback).reportAdImpression()
    verify(rewardedAdCallback).onVideoStart()
  }

  @Test
  fun onUnityAdsShowStart_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    unityRewardedAd.unityShowListener.onUnityAdsShowStart(TEST_PLACEMENT_ID)

    verify(rewardedAdCallback, never()).onAdOpened()
    verify(rewardedAdCallback, never()).reportAdImpression()
    verify(rewardedAdCallback, never()).onVideoStart()
  }

  @Test
  fun onUnityAdsShowClick_invokesReportAdClicked() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowClick(TEST_PLACEMENT_ID)

    verify(rewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onUnityAdsShowClick_withNullAdCallback_doesNotInvokeReportAdClicked() {
    unityRewardedAd.unityShowListener.onUnityAdsShowClick(TEST_PLACEMENT_ID)

    verify(rewardedAdCallback, never()).reportAdClicked()
  }

  @Test
  fun onUnityAdsShowComplete_withStateCompleted_invokesOnVideoCompleteOnUserEarnedRewardAndOnAdClosed() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowComplete(
      TEST_PLACEMENT_ID,
      UnityAds.UnityAdsShowCompletionState.COMPLETED,
    )

    verify(rewardedAdCallback).onVideoComplete()
    verify(rewardedAdCallback).onUserEarnedReward()
    verify(rewardedAdCallback).onAdClosed()
  }

  @Test
  fun onUnityAdsShowComplete_withStateNotCompleted_invokesOnlyOnAdClosed() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowComplete(
      TEST_PLACEMENT_ID,
      UnityAds.UnityAdsShowCompletionState.SKIPPED,
    )

    verify(rewardedAdCallback, never()).onVideoComplete()
    verify(rewardedAdCallback, never()).onUserEarnedReward()
    verify(rewardedAdCallback).onAdClosed()
  }

  @Test
  fun onUnityAdsShowComplete_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    unityRewardedAd.unityShowListener.onUnityAdsShowComplete(
      TEST_PLACEMENT_ID,
      UnityAds.UnityAdsShowCompletionState.COMPLETED,
    )

    verify(rewardedAdCallback, never()).onVideoComplete()
    verify(rewardedAdCallback, never()).onUserEarnedReward()
    verify(rewardedAdCallback, never()).onAdClosed()
  }

  @Test
  fun onUnityAdsShowFailure_invokesOnAdFailedToShow() {
    val errorCaptor = argumentCaptor<AdError>()
    val unityAdsShowError = UnityAdsShowError.INTERNAL_ERROR
    val errorCode = getMediationErrorCode(unityAdsShowError)
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowFailure(
      TEST_PLACEMENT_ID,
      unityAdsShowError,
      TEST_ERROR_MESSAGE,
    )

    verify(rewardedAdCallback).onAdFailedToShow(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(TEST_ERROR_MESSAGE)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onUnityAdsShowFailure_withNullAdCallback_doesNotInvokeOnAdFailedToShow() {
    unityRewardedAd.unityShowListener.onUnityAdsShowFailure(
      TEST_PLACEMENT_ID,
      UnityAdsShowError.INTERNAL_ERROR,
      TEST_ERROR_MESSAGE,
    )

    verify(rewardedAdCallback, never()).onAdFailedToShow(any<AdError>())
  }

  @Test
  fun showAd_withNonActivityContext_invokesOnAdFailedToShow() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)
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
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    whenever(rewardedAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(rewardedAdConfiguration.context) doReturn activity
    whenever(rewardedAdConfiguration.watermark) doReturn TEST_WATERMARK
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    val unityAdsShowOptions: UnityAdsShowOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    whenever(unityAdsLoader.createUnityAdsShowOptionsWithId(any())) doReturn unityAdsShowOptions
    unityRewardedAd.loadAd()
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_LOADED_PLACEMENT_ID)

    unityRewardedAd.showAd(activity)

    verify(unityAdsLoader).createUnityAdsShowOptionsWithId(notNull())
    verify(unityAdsShowOptions).set(UnityMediationAdapter.KEY_WATERMARK, TEST_WATERMARK)
    verify(unityAdsLoader).show(any(), eq(TEST_LOADED_PLACEMENT_ID), any(), any())
  }

  @Test
  fun showAd_withNullPlacementId_invokesUnityAdsShowWithNullId() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    whenever(rewardedAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(rewardedAdConfiguration.context) doReturn activity
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    val unityAdsShowOptions: UnityAdsShowOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    whenever(unityAdsLoader.createUnityAdsShowOptionsWithId(any())) doReturn unityAdsShowOptions
    unityRewardedAd.loadAd()
    val unityAdsLoadError = UnityAdsLoadError.NO_FILL
    unityRewardedAd.unityLoadListener.onUnityAdsFailedToLoad(
      null,
      unityAdsLoadError,
      TEST_ERROR_MESSAGE,
    )

    unityRewardedAd.showAd(activity)

    verify(unityAdsLoader).createUnityAdsShowOptionsWithId(notNull())
    verify(unityAdsLoader).show(any(), isNull(), any(), any())
  }

  companion object {
    private const val TEST_PLACEMENT_ID = "test_placement_id"
    private const val TEST_GAME_ID = "test_game_id"
    private const val TEST_LOADED_PLACEMENT_ID = "test_loaded_placement_id"
    private const val TEST_ERROR_MESSAGE = "test_error_message"
    private const val TEST_WATERMARK = "test_watermark"
  }
}
