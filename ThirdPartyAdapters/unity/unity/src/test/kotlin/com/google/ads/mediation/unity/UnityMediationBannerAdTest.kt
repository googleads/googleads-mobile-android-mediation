package com.google.ads.mediation.unity

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode
import com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class UnityMediationBannerAdTest {
  // Subject of tests
  private lateinit var unityMediationBannerAd: UnityMediationBannerAd
  private lateinit var bannerView: BannerView

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val bannerAdConfiguration: MediationBannerAdConfiguration = mock()
  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private val adSize: AdSize = AdSize.BANNER
  private val unityBannerViewFactory: UnityBannerViewFactory = mock()
  private val unityBannerViewWrapper: UnityBannerViewWrapper = mock()
  private val unityAdsLoader: UnityAdsLoader = mock()
  private val unityInitializer: UnityInitializer = spy(UnityInitializer.getInstance())
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    whenever(mediationUtils.findClosestSize(eq(activity), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    val unityBannerSize: UnityBannerSize? =
      UnityAdsAdapterUtils.getUnityBannerSize(activity, adSize, /* isRtb= */ false, mediationUtils)
    bannerView = BannerView(activity, TEST_PLACEMENT_ID, unityBannerSize)
    unityMediationBannerAd =
      UnityMediationBannerAd(
        bannerAdLoadCallback,
        unityInitializer,
        unityBannerViewFactory,
        unityAdsLoader,
      )
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
    unityMediationBannerAd.loadAd(bannerAdConfiguration, mediationUtils)

    val actualBannerView = unityMediationBannerAd.getView()

    assertThat(actualBannerView).isEqualTo(bannerView)
  }

  @Test
  fun onBannerLoaded_invokesOnSuccess() {
    unityMediationBannerAd.onBannerLoaded(bannerView)

    assertThat(bannerAdLoadCallback).hasSucceededWith(unityMediationBannerAd)
  }

  @Test
  fun onBannerClick_invokesReportAdClicked() {
    // Simulate a successful Banner load to instantiate bannerAdCallback
    unityMediationBannerAd.onBannerLoaded(bannerView)

    unityMediationBannerAd.onBannerClick(bannerView)

    assertThat(bannerAdCallback.isClicked).isTrue()
    assertThat(bannerAdCallback.isOpened).isTrue()
  }

  @Test
  fun onBannerFailedToLoad_invokesOnFailure() {
    val bannerErrorInfo = BannerErrorInfo(ERROR_MESSAGE, BannerErrorCode.NO_FILL)

    unityMediationBannerAd.onBannerFailedToLoad(bannerView, bannerErrorInfo)

    val errorCode: Int = getMediationErrorCode(bannerErrorInfo)
    val expectedAdError = AdError(errorCode, bannerErrorInfo.errorMessage, SDK_ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onBannerLeftApplication_invokesOnAdLeftApplication() {
    // Simulate a successful Banner load to instantiate bannerAdCallback
    unityMediationBannerAd.onBannerLoaded(bannerView)

    unityMediationBannerAd.onBannerLeftApplication(bannerView)

    assertThat(bannerAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun onBannerShown_invokesReportAdImpression() {
    // Simulate a successful Banner load to instantiate bannerAdCallback
    unityMediationBannerAd.onBannerLoaded(bannerView)

    unityMediationBannerAd.onBannerShown(bannerView)

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  companion object {
    private const val TEST_PLACEMENT_ID = "test_placement_id"
    private const val TEST_GAME_ID = "test_game_id"
    private const val ERROR_MESSAGE = "test_error_message"
  }
}
