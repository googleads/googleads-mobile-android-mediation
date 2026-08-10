package com.google.ads.mediation.unity

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode
import com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY
import com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_NON_ACTIVITY
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
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
  private val rewardedAdConfiguration: MediationRewardedAdConfiguration = mock {
    on { watermark } doReturn TEST_WATERMARK
  }
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
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
  }

  @Test
  fun onUnityAdsAdLoaded_invokesOnSuccess() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    assertThat(rewardedAdLoadCallback).hasSucceededWith(unityRewardedAd)
  }

  @Test
  fun onUnityAdsFailedToLoad_invokesOnFailure() {
    val unityAdsLoadError = UnityAdsLoadError.NO_FILL
    val errorCode = getMediationErrorCode(unityAdsLoadError)
    val expectedAdError = AdError(errorCode, TEST_ERROR_MESSAGE, SDK_ERROR_DOMAIN)

    unityRewardedAd.unityLoadListener.onUnityAdsFailedToLoad(
      TEST_PLACEMENT_ID,
      unityAdsLoadError,
      TEST_ERROR_MESSAGE,
    )

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onUnityAdsShowStart_invokesOnAdOpenedReportAdImpressionAndOnVideoStart() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowStart(TEST_PLACEMENT_ID)

    assertThat(rewardedAdCallback.isOpened).isTrue()
    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
    assertThat(rewardedAdCallback.isVideoStarted).isTrue()
  }

  @Test
  fun onUnityAdsShowStart_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    unityRewardedAd.unityShowListener.onUnityAdsShowStart(TEST_PLACEMENT_ID)

    assertThat(rewardedAdCallback.isOpened).isFalse()
    assertThat(rewardedAdCallback.isImpressionReported).isFalse()
    assertThat(rewardedAdCallback.isVideoStarted).isFalse()
  }

  @Test
  fun onUnityAdsShowClick_invokesReportAdClicked() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowClick(TEST_PLACEMENT_ID)

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onUnityAdsShowClick_withNullAdCallback_doesNotInvokeReportAdClicked() {
    unityRewardedAd.unityShowListener.onUnityAdsShowClick(TEST_PLACEMENT_ID)

    assertThat(rewardedAdCallback.isClicked).isFalse()
  }

  @Test
  fun onUnityAdsShowComplete_withStateCompleted_invokesOnVideoCompleteOnUserEarnedRewardAndOnAdClosed() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowComplete(
      TEST_PLACEMENT_ID,
      UnityAds.UnityAdsShowCompletionState.COMPLETED,
    )

    assertThat(rewardedAdCallback.isVideoCompleted).isTrue()
    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onUnityAdsShowComplete_withStateNotCompleted_invokesOnlyOnAdClosed() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowComplete(
      TEST_PLACEMENT_ID,
      UnityAds.UnityAdsShowCompletionState.SKIPPED,
    )

    assertThat(rewardedAdCallback.isVideoCompleted).isFalse()
    assertThat(rewardedAdCallback.isUserEarnedReward).isFalse()
    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onUnityAdsShowComplete_withNullAdCallback_doesNotInvokeAnyCallbackMethod() {
    unityRewardedAd.unityShowListener.onUnityAdsShowComplete(
      TEST_PLACEMENT_ID,
      UnityAds.UnityAdsShowCompletionState.COMPLETED,
    )

    assertThat(rewardedAdCallback.isVideoCompleted).isFalse()
    assertThat(rewardedAdCallback.isUserEarnedReward).isFalse()
    assertThat(rewardedAdCallback.isClosed).isFalse()
  }

  @Test
  fun onUnityAdsShowFailure_invokesOnAdFailedToShow() {
    val unityAdsShowError = UnityAdsShowError.INTERNAL_ERROR
    val errorCode = getMediationErrorCode(unityAdsShowError)
    val expectedAdError = AdError(errorCode, TEST_ERROR_MESSAGE, SDK_ERROR_DOMAIN)
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)

    unityRewardedAd.unityShowListener.onUnityAdsShowFailure(
      TEST_PLACEMENT_ID,
      unityAdsShowError,
      TEST_ERROR_MESSAGE,
    )

    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onUnityAdsShowFailure_withNullAdCallback_doesNotInvokeOnAdFailedToShow() {
    unityRewardedAd.unityShowListener.onUnityAdsShowFailure(
      TEST_PLACEMENT_ID,
      UnityAdsShowError.INTERNAL_ERROR,
      TEST_ERROR_MESSAGE,
    )

    assertThat(rewardedAdCallback.isFailedToShow).isFalse()
  }

  @Test
  fun showAd_withNonActivityContext_invokesOnAdFailedToShow() {
    unityRewardedAd.unityLoadListener.onUnityAdsAdLoaded(TEST_PLACEMENT_ID)
    val expectedAdError =
      AdError(ERROR_CONTEXT_NOT_ACTIVITY, ERROR_MSG_NON_ACTIVITY, ADAPTER_ERROR_DOMAIN)

    unityRewardedAd.showAd(ApplicationProvider.getApplicationContext())

    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
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
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    val unityAdsShowOptions: UnityAdsShowOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    whenever(unityAdsLoader.createUnityAdsShowOptionsWithId(any())) doReturn unityAdsShowOptions
    unityRewardedAd.loadAd(rewardedAdConfiguration)
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
    unityRewardedAd.loadAd(rewardedAdConfiguration)
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
