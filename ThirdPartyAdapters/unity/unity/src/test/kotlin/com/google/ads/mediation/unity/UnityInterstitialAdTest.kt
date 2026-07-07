package com.google.ads.mediation.unity

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.WATERMARK
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationLoadErrorCode
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationShowErrorCode
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.InterstitialAd
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
class UnityInterstitialAdTest {

  // Subject of tests
  private lateinit var unityInterstitialAd: UnityInterstitialAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val interstitialAdConfiguration: MediationInterstitialAdConfiguration = mock {
    on { watermark } doReturn TEST_WATERMARK
  }
  private val interstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val interstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
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

    doReturn(interstitialAdCallback)
      .whenever(interstitialAdLoadCallback)
      .onSuccess(unityInterstitialAd)
  }

  @Test
  fun onUnityAdsAdLoaded_invokesOnSuccess() {
    unityInterstitialAd.onAdLoaded(mock(), null)

    verify(interstitialAdLoadCallback).onSuccess(unityInterstitialAd)
  }

  @Test
  fun onUnityAdsFailedToLoad_invokesOnFailure() {
    val errorCaptor = argumentCaptor<AdError>()
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52100)
    whenever(unityAdsError.message).doReturn(TEST_ERROR_MESSAGE)
    val errorCode = getMediationLoadErrorCode(unityAdsError)

    unityInterstitialAd.onAdLoaded(null, unityAdsError)

    verify(interstitialAdLoadCallback).onFailure(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(TEST_ERROR_MESSAGE)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onUnityAdsShowStart_invokesOnAdOpened() {
    val interstitialAd = mock<InterstitialAd>()
    unityInterstitialAd.onAdLoaded(interstitialAd, null)

    unityInterstitialAd.onStarted(interstitialAd)

    verify(interstitialAdCallback).onAdOpened()
  }

  @Test
  fun onUnityAdsShowStart_withNullAdCallback_doesNotInvokeOnAdOpened() {
    val interstitialAd = mock<InterstitialAd>()

    unityInterstitialAd.onStarted(interstitialAd)

    verify(interstitialAdCallback, never()).onAdOpened()
  }

  @Test
  fun onUnityAdsShowClick_invokesReportAdClickedAndOnAdLeftApplication() {
    val interstitialAd = mock<InterstitialAd>()
    unityInterstitialAd.onAdLoaded(interstitialAd, null)

    unityInterstitialAd.onClicked(interstitialAd)

    verify(interstitialAdCallback).reportAdClicked()
    verify(interstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun onUnityAdsShowClick_withNullAdCallback_doesNotInvokeReportAdClickedOrOnAdLeftApplication() {
    unityInterstitialAd.onClicked(mock())

    verify(interstitialAdCallback, never()).reportAdClicked()
    verify(interstitialAdCallback, never()).onAdLeftApplication()
  }

  @Test
  fun onUnityAdsShowComplete_invokesOnAdClosed() {
    val interstitialAd = mock<InterstitialAd>()
    unityInterstitialAd.onAdLoaded(interstitialAd, null)

    unityInterstitialAd.onCompleted(interstitialAd, ShowFinishState.COMPLETED)

    verify(interstitialAdCallback).onAdClosed()
  }

  @Test
  fun onUnityAdsShowComplete_withNullAdCallback_doesNotInvokeOnAdClosed() {
    unityInterstitialAd.onCompleted(mock(), ShowFinishState.COMPLETED)

    verify(interstitialAdCallback, never()).onAdClosed()
  }

  @Test
  fun onUnityAdsShowFailure_invokesOnAdFailedToShow() {
    val errorCaptor = argumentCaptor<AdError>()
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.message).doReturn(TEST_ERROR_MESSAGE)
    whenever(unityAdsError.code).doReturn(52202)
    val errorCode = getMediationShowErrorCode(unityAdsError)
    unityInterstitialAd.onAdLoaded(mock(), null)

    unityInterstitialAd.onFailed(mock(), unityAdsError)

    verify(interstitialAdCallback).onAdFailedToShow(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(TEST_ERROR_MESSAGE)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onUnityAdsShowFailure_withNullAdCallback_doesNotInvokeOnAdFailedToShow() {
    unityInterstitialAd.onFailed(mock(), mock())

    verify(interstitialAdCallback, never()).onAdFailedToShow(any<AdError>())
  }

  @Test
  fun showAd_invokesUnityAdsShow() {
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[1] as InitializationListener).onInitializationComplete(null)
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any())
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(interstitialAdConfiguration.context) doReturn activity
    unityInterstitialAd.loadAd(interstitialAdConfiguration)

    val loadedAd = mock<InterstitialAd>()

    unityInterstitialAd.onAdLoaded(loadedAd, null)

    unityInterstitialAd.showAd(activity)

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
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(interstitialAdConfiguration.context) doReturn activity
    unityInterstitialAd.loadAd(interstitialAdConfiguration)
    val unityAdsLoadError = UnityAdsLoadError.NO_FILL

    unityInterstitialAd.onAdLoaded(
      null,
      mock<UnityAdsError>().apply {
        whenever(this.code).doReturn(0)
        whenever(this.message).doReturn(TEST_ERROR_MESSAGE)
      }
    )

    unityInterstitialAd.showAd(activity)

    verifyNoInteractions(interstitialAdCallback)
  }

  companion object {
    private const val TEST_PLACEMENT_ID = "test_placement_id"
    private const val TEST_GAME_ID = "test_game_id"
    private const val TEST_LOADED_PLACEMENT_ID = "test_loaded_placement_id"
    private const val TEST_ERROR_MESSAGE = "test_error_message"
    private const val TEST_WATERMARK = "test_watermark"
  }
}
