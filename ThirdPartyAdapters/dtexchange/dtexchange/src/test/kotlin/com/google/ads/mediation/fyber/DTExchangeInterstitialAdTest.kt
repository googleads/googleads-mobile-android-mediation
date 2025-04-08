package com.google.ads.mediation.fyber

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_AD_NOT_READY
import com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class DTExchangeInterstitialAdTest {
  // Subject of testing.
  private lateinit var dtExchangeInterstitialAd: DTExchangeInterstitialAd

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockInterstitialAdCallback
    }

  @Before
  fun setUp() {
    val adConfiguration =
      createMediationInterstitialAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
    dtExchangeInterstitialAd = DTExchangeInterstitialAd(adConfiguration, mockAdLoadCallback)
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_withAdSpotNotReady_invokesOnFailure() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdSpot = mock<InneractiveAdSpot> { on { isReady } doReturn false }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val expectedAdError =
        AdError(
          ERROR_AD_NOT_READY,
          "DT Exchange's interstitial ad spot is not ready.",
          ERROR_DOMAIN,
        )
      dtExchangeInterstitialAd.loadAd()

      dtExchangeInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(mockAdSpot).destroy()
    }
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_invokesOnSuccess() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeInterstitialAd.loadAd()

      dtExchangeInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      verify(mockAdLoadCallback).onSuccess(eq(dtExchangeInterstitialAd))
    }
  }

  @Test
  fun onInneractiveFailedAdRequest_invokesOnFailure() {
    val mockAdSpot = mock<InneractiveAdSpot>()
    val iErrorCode = InneractiveErrorCode.LOAD_TIMEOUT
    val expectedAdError =
      AdError(
        307,
        "DT Exchange failed to request ad with reason: Failed Due To load timeout",
        ERROR_DOMAIN,
      )

    dtExchangeInterstitialAd.onInneractiveFailedAdRequest(mockAdSpot, iErrorCode)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockAdSpot).destroy()
  }

  @Test
  fun showAd_withInvalidUnitController_invokesOnFailure() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn null
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeInterstitialAd.loadAd()
      dtExchangeInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeInterstitialAd.showAd(context)

      verify(mockInterstitialAdCallback).onAdOpened()
      verify(mockInterstitialAdCallback).onAdClosed()
      verify(mockAdSpot).destroy()
    }
  }

  @Test
  fun showAd_invokesShow() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockFullscreenController = mock<InneractiveFullscreenUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockFullscreenController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeInterstitialAd.loadAd()
      dtExchangeInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeInterstitialAd.showAd(context)

      verify(mockFullscreenController).show(context)
    }
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeInterstitialAd.loadAd()
      dtExchangeInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeInterstitialAd.onAdImpression(mock())

      verify(mockInterstitialAdCallback).reportAdImpression()
    }
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeInterstitialAd.loadAd()
      dtExchangeInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeInterstitialAd.onAdClicked(mock())

      verify(mockInterstitialAdCallback).reportAdClicked()
    }
  }

  @Test
  fun onAdWillOpenExternalApp_invokesOnAdLeftApplication() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdViewController = mock<InneractiveAdViewUnitController>()
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn mockAdViewController
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      dtExchangeInterstitialAd.loadAd()
      dtExchangeInterstitialAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeInterstitialAd.onAdWillOpenExternalApp(mock())

      verify(mockInterstitialAdCallback).onAdLeftApplication()
    }
  }

  @Test
  fun onAdEnteredErrorState_throwsNoException() {
    dtExchangeInterstitialAd.onAdEnteredErrorState(mock(), mock())
  }

  @Test
  fun onAdWillCloseInternalBrowser_throwsNoException() {
    dtExchangeInterstitialAd.onAdWillCloseInternalBrowser(mock())
  }
}
