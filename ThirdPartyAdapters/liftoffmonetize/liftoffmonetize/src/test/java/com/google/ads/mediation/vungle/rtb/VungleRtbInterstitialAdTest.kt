package com.google.ads.mediation.vungle.rtb

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleError
import com.vungle.ads.internal.protos.Sdk.SDKError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Tests for [VungleRtbInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class VungleRtbInterstitialAdTest {

  /** Unit under test. */
  private lateinit var adapterRtbInterstitialAd: VungleRtbInterstitialAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val interstitialAdCallback = mock<MediationInterstitialAdCallback>()
  private val interstitialAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>> {
      on { onSuccess(any()) } doReturn interstitialAdCallback
    }
  private val mockVungleInitializer = mock<VungleInitializer>()
  private val vungleInterstitialAd = mock<InterstitialAd>()
  private val vungleFactory =
    mock<VungleFactory> {
      on { createInterstitialAd(any(), any(), any()) } doReturn vungleInterstitialAd
      on { createAdConfig() } doReturn mock()
    }

  @Before
  fun setUp() {
    adapterRtbInterstitialAd =
      VungleRtbInterstitialAd(
        createMediationInterstitialAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        interstitialAdLoadCallback,
        vungleFactory,
      )

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_callsLoadSuccess() {
    adapterRtbInterstitialAd.onAdLoaded(vungleInterstitialAd)

    verify(interstitialAdLoadCallback).onSuccess(adapterRtbInterstitialAd)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.API_REQUEST_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK interstitial ad load failed."
      }

    adapterRtbInterstitialAd.onAdFailedToLoad(vungleInterstitialAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(interstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun showAd_playsLiftoffAd() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterRtbInterstitialAd.render()
    }

    adapterRtbInterstitialAd.showAd(context)

    verify(vungleInterstitialAd).play(context)
  }

  private fun renderAdAndMockLoadSuccess() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterRtbInterstitialAd.render()
    }
    adapterRtbInterstitialAd.onAdLoaded(vungleInterstitialAd)
  }

  @Test
  fun onAdStart_callsOnAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterRtbInterstitialAd.onAdStart(vungleInterstitialAd)

    verify(interstitialAdCallback).onAdOpened()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdEnd_callsOnAdClosed() {
    renderAdAndMockLoadSuccess()

    adapterRtbInterstitialAd.onAdEnd(vungleInterstitialAd)

    verify(interstitialAdCallback).onAdClosed()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    renderAdAndMockLoadSuccess()

    adapterRtbInterstitialAd.onAdClicked(vungleInterstitialAd)

    verify(interstitialAdCallback).reportAdClicked()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdLeftApplication_callsOnAdLeftApplication() {
    renderAdAndMockLoadSuccess()

    adapterRtbInterstitialAd.onAdLeftApplication(vungleInterstitialAd)

    verify(interstitialAdCallback).onAdLeftApplication()
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdFailedToPlay_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.AD_NOT_LOADED_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK interstitial ad play failed."
      }

    adapterRtbInterstitialAd.onAdFailedToPlay(vungleInterstitialAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(interstitialAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
    verifyNoMoreInteractions(interstitialAdCallback)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterRtbInterstitialAd.onAdImpression(vungleInterstitialAd)

    verify(interstitialAdCallback).reportAdImpression()
    verifyNoMoreInteractions(interstitialAdCallback)
  }
}
