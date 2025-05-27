package com.google.ads.mediation.fyber

import android.content.Context
import android.widget.RelativeLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
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

@RunWith(AndroidJUnit4::class)
class DTExchangeBannerAdTest {
  // Subject of testing.
  private lateinit var dtExchangeBannerAd: DTExchangeBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockBannerAdCallback: MediationBannerAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockBannerAdCallback
    }

  @Before
  fun setUp() {
    val adConfiguration =
      createMediationBannerAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
    dtExchangeBannerAd = DTExchangeBannerAd(adConfiguration, mockAdLoadCallback)
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
          DTExchangeErrorCodes.ERROR_AD_NOT_READY,
          "DT Exchange's banner ad spot is not ready.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      dtExchangeBannerAd.loadAd()

      dtExchangeBannerAd.onInneractiveSuccessfulAdRequest(mock())

      verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
      verify(mockAdSpot).destroy()
    }
  }

  @Test
  fun onInneractiveSuccessfulAdRequest_withInvalidUnitController_invokesOnFailure() {
    mockStatic(InneractiveAdSpotManager::class.java).use {
      val mockAdSpot =
        mock<InneractiveAdSpot> {
          on { isReady } doReturn true
          on { selectedUnitController } doReturn null
        }
      val mockInneractiveAdSpotManager =
        mock<InneractiveAdSpotManager> { on { createSpot() } doReturn mockAdSpot }
      whenever(InneractiveAdSpotManager.get()) doReturn mockInneractiveAdSpotManager
      val expectedAdError =
        AdError(
          DTExchangeErrorCodes.ERROR_WRONG_CONTROLLER_TYPE,
          "Unexpected controller type.",
          DTExchangeErrorCodes.ERROR_DOMAIN,
        )
      dtExchangeBannerAd.loadAd()

      dtExchangeBannerAd.onInneractiveSuccessfulAdRequest(mock())

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
      dtExchangeBannerAd.loadAd()

      dtExchangeBannerAd.onInneractiveSuccessfulAdRequest(mock())
      val bannerView = dtExchangeBannerAd.view

      verify(mockAdViewController).bindView(any<RelativeLayout>())
      verify(mockAdLoadCallback).onSuccess(eq(dtExchangeBannerAd))
      assertThat(bannerView).isInstanceOf(RelativeLayout::class.java)
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
        DTExchangeErrorCodes.ERROR_DOMAIN,
      )

    dtExchangeBannerAd.onInneractiveFailedAdRequest(mockAdSpot, iErrorCode)

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockAdSpot).destroy()
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
      dtExchangeBannerAd.loadAd()
      dtExchangeBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeBannerAd.onAdImpression(mock())

      verify(mockBannerAdCallback).reportAdImpression()
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
      dtExchangeBannerAd.loadAd()
      dtExchangeBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeBannerAd.onAdClicked(mock())

      verify(mockBannerAdCallback).reportAdClicked()
    }
  }

  @Test
  fun onAdWillCloseInternalBrowser_invokesOnAdClosed() {
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
      dtExchangeBannerAd.loadAd()
      dtExchangeBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeBannerAd.onAdWillCloseInternalBrowser(mock())

      verify(mockBannerAdCallback).onAdClosed()
    }
  }

  @Test
  fun onAdWillOpenExternalApp_invokesOnAdOpenedAndOnAdLeftApplication() {
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
      dtExchangeBannerAd.loadAd()
      dtExchangeBannerAd.onInneractiveSuccessfulAdRequest(mock())

      dtExchangeBannerAd.onAdWillOpenExternalApp(mock())

      verify(mockBannerAdCallback).onAdOpened()
      verify(mockBannerAdCallback).onAdLeftApplication()
    }
  }

  @Test
  fun onAdEnteredErrorState_throwsNoException() {
    dtExchangeBannerAd.onAdEnteredErrorState(mock(), mock())
  }

  @Test
  fun onAdExpanded_throwsNoException() {
    dtExchangeBannerAd.onAdExpanded(mock())
  }

  @Test
  fun onAdResized_throwsNoException() {
    dtExchangeBannerAd.onAdResized(mock())
  }

  @Test
  fun onAdCollapsed_throwsNoException() {
    dtExchangeBannerAd.onAdCollapsed(mock())
  }
}
