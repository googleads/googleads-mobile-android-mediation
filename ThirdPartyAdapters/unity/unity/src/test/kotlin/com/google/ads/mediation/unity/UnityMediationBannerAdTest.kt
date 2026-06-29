package com.google.ads.mediation.unity

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationLoadErrorCode
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.unity3d.ads.BannerAd
import com.unity3d.ads.UnityAdsError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class UnityMediationBannerAdTest {
  // Subject of tests
  private lateinit var unityMediationBannerAd: UnityMediationBannerAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val bannerAdConfiguration: MediationBannerAdConfiguration = mock()
  private val bannerAdCallback: MediationBannerAdCallback = mock()
  private val bannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val adSize: AdSize = AdSize.BANNER
  private val unityAdsLoader: UnityAdsLoader = mock()
  private val unityInitializer: UnityInitializer = spy(UnityInitializer.getInstance())
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    unityMediationBannerAd =
      UnityMediationBannerAd(
        bannerAdLoadCallback,
        unityInitializer,
        unityAdsLoader,
      )
    doReturn(bannerAdCallback).whenever(bannerAdLoadCallback).onSuccess(unityMediationBannerAd)
  }

  @Test
  fun getView_returnsBannerView() {
    val bannerAd = mock<BannerAd>()
    val mockView = mock<android.view.View>()
    whenever(bannerAd.view) doReturn mockView

    unityMediationBannerAd.onAdLoaded(bannerAd, null)

    val actualBannerView = unityMediationBannerAd.getView()

    assertThat(actualBannerView).isEqualTo(mockView)
  }

  @Test
  fun onBannerLoaded_invokesOnSuccess() {
    unityMediationBannerAd.onAdLoaded(mock(), null)

    verify(bannerAdLoadCallback).onSuccess(unityMediationBannerAd)
  }

  @Test
  fun onBannerClick_invokesReportAdClickedAndOnAdOpened() {
    val bannerAd = mock<BannerAd>()
    unityMediationBannerAd.onAdLoaded(bannerAd, null)

    unityMediationBannerAd.onClicked(bannerAd)

    verify(bannerAdCallback).reportAdClicked()
    verify(bannerAdCallback).onAdOpened()
  }

  @Test
  fun onBannerFailedToLoad_invokesOnFailure() {
    val errorCaptor = argumentCaptor<AdError>()
    val unityAdsError = mock<UnityAdsError>()
    whenever(unityAdsError.code).doReturn(52100)
    whenever(unityAdsError.message).doReturn(TEST_ERROR_MESSAGE)
    val errorCode = getMediationLoadErrorCode(unityAdsError)

    unityMediationBannerAd.onAdLoaded(null, unityAdsError)

    verify(bannerAdLoadCallback).onFailure(errorCaptor.capture())
    val capturedError = errorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(TEST_ERROR_MESSAGE)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onBannerClick_withNullAdCallback_doesNotInvokeReportAdClickedOrOnAdOpened() {
    unityMediationBannerAd.onClicked(mock())

    verify(bannerAdCallback, never()).reportAdClicked()
    verify(bannerAdCallback, never()).onAdOpened()
  }

  @Test
  fun onImpression_invokesReportAdImpression() {
    val bannerAd = mock<BannerAd>()
    unityMediationBannerAd.onAdLoaded(bannerAd, null)

    unityMediationBannerAd.onImpression(bannerAd)

    verify(bannerAdCallback).reportAdImpression()
  }

  @Test
  fun onImpression_withNullAdCallback_doesNotInvokeReportAdImpression() {
    unityMediationBannerAd.onImpression(mock())

    verify(bannerAdCallback, never()).reportAdImpression()
  }

  companion object {
    private const val TEST_PLACEMENT_ID = "test_placement_id"
    private const val TEST_GAME_ID = "test_game_id"
    private const val TEST_ERROR_MESSAGE = "test_error_message"
  }
}
