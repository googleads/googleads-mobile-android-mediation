package com.google.ads.mediation.unity

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class UnityMediationBannerAdTest {
  // Subject of tests
  private lateinit var unityMediationBannerAd: UnityMediationBannerAd
  private lateinit var bannerView: BannerView

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val bannerAdConfiguration: MediationBannerAdConfiguration = mock()
  private val bannerAdCallback: MediationBannerAdCallback = mock()
  private val bannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val adSize: AdSize = AdSize.BANNER
  private val unityBannerViewFactory: UnityBannerViewFactory = mock()
  private val unityBannerViewWrapper: UnityBannerViewWrapper = mock()
  private val unityAdsLoader: UnityAdsLoader = mock()
  private val unityInitializer: UnityInitializer = spy(UnityInitializer.getInstance())

  @Before
  fun setUp() {
    val unityBannerSize: UnityBannerSize? =
      UnityAdsAdapterUtils.getUnityBannerSize(activity, adSize, /* isRtb= */ false)
    bannerView = BannerView(activity, TEST_PLACEMENT_ID, unityBannerSize)
    unityMediationBannerAd =
      UnityMediationBannerAd(
        bannerAdConfiguration,
        bannerAdLoadCallback,
        unityInitializer,
        unityBannerViewFactory,
        unityAdsLoader,
      )
    doReturn(bannerAdCallback).whenever(bannerAdLoadCallback).onSuccess(unityMediationBannerAd)
    doReturn(unityBannerViewWrapper)
      .whenever(unityBannerViewFactory)
      .createBannerView(any(), any(), any())
  }

  @Test
  fun getView_returnsBannerView() {
    whenever(unityAdsLoader.createUnityAdsLoadOptionsWithId(any())) doReturn mock()
    whenever(unityBannerViewWrapper.bannerView) doReturn bannerView
    doAnswer { invocation ->
        val args = invocation.arguments
        (args[2] as IUnityAdsInitializationListener).onInitializationComplete()
      }
      .whenever(unityInitializer)
      .initializeUnityAds(any(), any(), any())
    whenever(bannerAdConfiguration.serverParameters) doReturn
      bundleOf(
        UnityMediationAdapter.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID,
        UnityMediationAdapter.KEY_GAME_ID to TEST_GAME_ID,
      )
    whenever(bannerAdConfiguration.context) doReturn activity
    whenever(bannerAdConfiguration.adSize) doReturn adSize
    unityMediationBannerAd.loadAd()

    val actualBannerView = unityMediationBannerAd.getView()

    assertThat(actualBannerView).isEqualTo(bannerView)
  }

  @Test
  fun onBannerLoaded_invokesOnSuccess() {
    unityMediationBannerAd.onBannerLoaded(bannerView)

    verify(bannerAdLoadCallback).onSuccess(unityMediationBannerAd)
  }

  @Test
  fun onBannerClick_invokesReportAdClicked() {
    // Simulate a successful Banner load to instantiate bannerAdCallback
    unityMediationBannerAd.onBannerLoaded(bannerView)

    unityMediationBannerAd.onBannerClick(bannerView)

    verify(bannerAdCallback).reportAdClicked()
    verify(bannerAdCallback).onAdOpened()
  }

  @Test
  fun onBannerFailedToLoad_invokesOnFailure() {
    val bannerErrorInfo = BannerErrorInfo(ERROR_MESSAGE, BannerErrorCode.NO_FILL)

    unityMediationBannerAd.onBannerFailedToLoad(bannerView, bannerErrorInfo)

    val errorCode: Int = getMediationErrorCode(bannerErrorInfo)
    val adErrorCaptor = argumentCaptor<AdError>()
    verify(bannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(errorCode)
    assertThat(capturedError.message).isEqualTo(bannerErrorInfo.errorMessage)
    assertThat(capturedError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onBannerLeftApplication_invokesOnAdLeftApplication() {
    // Simulate a successful Banner load to instantiate bannerAdCallback
    unityMediationBannerAd.onBannerLoaded(bannerView)

    unityMediationBannerAd.onBannerLeftApplication(bannerView)

    verify(bannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun onBannerShown_invokesReportAdImpression() {
    // Simulate a successful Banner load to instantiate bannerAdCallback
    unityMediationBannerAd.onBannerLoaded(bannerView)

    unityMediationBannerAd.onBannerShown(bannerView)

    verify(bannerAdCallback).reportAdImpression()
  }

  companion object {
    private const val TEST_PLACEMENT_ID = "test_placement_id"
    private const val TEST_GAME_ID = "test_game_id"
    private const val ERROR_MESSAGE = "test_error_message"
  }
}
