package com.google.ads.mediation.unity

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
class UnityInterstitialAdTest {

  // Subject of tests
  private lateinit var unityInterstitialAd: UnityInterstitialAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val interstitialAdConfiguration: MediationInterstitialAdConfiguration = mock {
    on { watermark } doReturn TEST_WATERMARK
  }
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val interstitialAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val unityAdsLoader: UnityAdsLoader = mock()
  private val unityInitializer: UnityInitializer = spy(UnityInitializer.getInstance())

  @Before
  fun setUp() {
    unityInterstitialAd =
      UnityInterstitialAd(
        interstitialAdConfiguration,
        interstitialAdLoadCallback,
        unityInitializer,
        unityAdsLoader,
      )
  }

  @Test
  fun onUnityAdsAdLoaded_invokesOnSuccess() {
    unityInterstitialAd.onUnityAdsAdLoaded(PLACEMENT_ID)

    assertThat(interstitialAdLoadCallback).hasSucceededWith(unityInterstitialAd)
  }

  @Test
  fun onUnityAdsFailedToLoad_invokesOnFailure() {
    val unityAdsLoadError = UnityAdsLoadError.NO_FILL

    unityInterstitialAd.onUnityAdsFailedToLoad(PLACEMENT_ID, unityAdsLoadError, ERROR_MESSAGE)

    val errorCode = getMediationErrorCode(unityAdsLoadError)
    val expectedAdError = AdError(errorCode, ERROR_MESSAGE, SDK_ERROR_DOMAIN)
    assertThat(interstitialAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onUnityAdsShowStart_invokesOnAdOpenedAndReportsAdImpression() {
    unityInterstitialAd.onUnityAdsAdLoaded(PLACEMENT_ID)

    unityInterstitialAd.onUnityAdsShowStart(PLACEMENT_ID)

    assertThat(interstitialAdCallback.isOpened).isTrue()
    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onUnityAdsShowClick_invokesReportAdClickedAndOnAdLeftApplication() {
    unityInterstitialAd.onUnityAdsAdLoaded(PLACEMENT_ID)

    unityInterstitialAd.onUnityAdsShowClick(PLACEMENT_ID)

    assertThat(interstitialAdCallback.isClicked).isTrue()
    assertThat(interstitialAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onUnityAdsShowComplete_invokesOnAdClosed() {
    unityInterstitialAd.onUnityAdsAdLoaded(PLACEMENT_ID)

    unityInterstitialAd.onUnityAdsShowComplete(PLACEMENT_ID, UnityAdsShowCompletionState.COMPLETED)

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  @Test
  fun onUnityAdsShowFailure_invokesOnAdFailedToShow() {
    unityInterstitialAd.onUnityAdsAdLoaded(PLACEMENT_ID)

    unityInterstitialAd.onUnityAdsShowFailure(
      PLACEMENT_ID,
      UnityAdsShowError.INTERNAL_ERROR,
      ERROR_MESSAGE,
    )

    val expectedAdError =
      AdError(
        getMediationErrorCode(UnityAdsShowError.INTERNAL_ERROR),
        ERROR_MESSAGE,
        SDK_ERROR_DOMAIN,
      )
    assertThat(interstitialAdCallback.isFailedToShow).isTrue()
    assertThat(interstitialAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun showAd_withNonActivityContext_callsShowWithNullActivity() {
    val unityAdsShowOptions: UnityAdsShowOptions = mock()
    whenever(unityAdsLoader.createUnityAdsShowOptionsWithId(anyOrNull())) doReturn unityAdsShowOptions
    unityInterstitialAd.onUnityAdsAdLoaded(PLACEMENT_ID)

    unityInterstitialAd.showAd(ApplicationProvider.getApplicationContext())

    verify(unityAdsLoader).show(isNull(), any(), any(), any())
  }

  @Test
  fun showAd_invokesUnityAdsShow() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(interstitialAdConfiguration.context) doReturn activity
    val unityAdsLoadOptions: UnityAdsLoadOptions = mock()
    val unityAdsShowOptions: UnityAdsShowOptions = mock()
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn unityAdsLoadOptions
    whenever(unityAdsLoader.createUnityAdsShowOptionsWithId(any())) doReturn unityAdsShowOptions
    unityInterstitialAd.loadAd(interstitialAdConfiguration)
    unityInterstitialAd.onUnityAdsAdLoaded(TEST_LOADED_PLACEMENT_ID)

    unityInterstitialAd.showAd(activity)

    verify(unityAdsLoader).createUnityAdsShowOptionsWithId(notNull())
    verify(unityAdsShowOptions).set(UnityMediationAdapter.KEY_WATERMARK, TEST_WATERMARK)
    verify(unityAdsLoader).show(any(), eq(TEST_LOADED_PLACEMENT_ID), any(), any())
  }

  companion object {
    private const val PLACEMENT_ID = "test_placement_id"
    private const val TEST_LOADED_PLACEMENT_ID = "test_loaded_placement_id"
    private const val TEST_GAME_ID = "test_game_id"
    private const val TEST_WATERMARK = "test_watermark"
    private const val ERROR_MESSAGE = "test_error_message"
  }
}
