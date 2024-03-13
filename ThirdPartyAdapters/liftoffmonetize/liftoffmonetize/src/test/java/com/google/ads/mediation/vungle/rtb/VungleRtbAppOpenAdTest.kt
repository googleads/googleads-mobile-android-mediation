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
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_CANNOT_PLAY_AD
import com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Tests for [VungleRtbAppOpenAd]. */
@RunWith(AndroidJUnit4::class)
class VungleRtbAppOpenAdTest {

  /** Unit under test. */
  private lateinit var adapterRtbAppOpenAd: VungleRtbAppOpenAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val appOpenAdCallback = mock<MediationAppOpenAdCallback>()
  private val appOpenAdLoadCallback =
    mock<MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>> {
      on { onSuccess(any()) } doReturn appOpenAdCallback
    }
  private val vungleInitializer = mock<VungleInitializer>()
  private val vungleAppOpenAd = mock<InterstitialAd>()
  private val vungleFactory =
    mock<VungleFactory> {
      on { createInterstitialAd(any(), any(), any()) } doReturn vungleAppOpenAd
      on { createAdConfig() } doReturn mock()
    }

  @Before
  fun setUp() {
    adapterRtbAppOpenAd =
      VungleRtbAppOpenAd(
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK,
          mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
        ),
        appOpenAdLoadCallback,
        vungleFactory,
      )

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(vungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_callsLoadSuccess() {
    adapterRtbAppOpenAd.onAdLoaded(vungleAppOpenAd)

    verify(appOpenAdLoadCallback).onSuccess(adapterRtbAppOpenAd)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn VungleError.AD_FAILED_TO_DOWNLOAD
        on { errorMessage } doReturn "Liftoff Monetize SDK appOpen ad load failed."
      }

    adapterRtbAppOpenAd.onAdFailedToLoad(vungleAppOpenAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(appOpenAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun showAd_ifLiftoffCanPlayAd_playsLiftoffAd() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbAppOpenAd.render()
    }
    whenever(vungleAppOpenAd.canPlayAd()) doReturn true

    adapterRtbAppOpenAd.showAd(context)

    verify(vungleAppOpenAd).play(context)
  }

  @Test
  fun showAd_ifLiftoffCannotPlayAd_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    whenever(vungleAppOpenAd.canPlayAd()) doReturn false

    adapterRtbAppOpenAd.showAd(context)

    val expectedError =
      AdError(
        ERROR_CANNOT_PLAY_AD,
        "Failed to show app open ad from Liftoff Monetize.",
        ERROR_DOMAIN,
      )
    verify(appOpenAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
    verifyNoMoreInteractions(appOpenAdCallback)
  }

  private fun renderAdAndMockLoadSuccess() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbAppOpenAd.render()
    }
    adapterRtbAppOpenAd.onAdLoaded(vungleAppOpenAd)
  }

  @Test
  fun onAdStart_callsOnAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterRtbAppOpenAd.onAdStart(vungleAppOpenAd)

    verify(appOpenAdCallback).onAdOpened()
    verifyNoMoreInteractions(appOpenAdCallback)
  }

  @Test
  fun onAdEnd_callsOnAdClosed() {
    renderAdAndMockLoadSuccess()

    adapterRtbAppOpenAd.onAdEnd(vungleAppOpenAd)

    verify(appOpenAdCallback).onAdClosed()
    verifyNoMoreInteractions(appOpenAdCallback)
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    renderAdAndMockLoadSuccess()

    adapterRtbAppOpenAd.onAdClicked(vungleAppOpenAd)

    verify(appOpenAdCallback).reportAdClicked()
    verifyNoMoreInteractions(appOpenAdCallback)
  }

  @Test
  fun onAdLeftApplication_noInteractions() {
    renderAdAndMockLoadSuccess()

    adapterRtbAppOpenAd.onAdLeftApplication(vungleAppOpenAd)

    verifyNoMoreInteractions(appOpenAdCallback)
  }

  @Test
  fun onAdFailedToPlay_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn VungleError.AD_UNABLE_TO_PLAY
        on { errorMessage } doReturn "Liftoff Monetize SDK ad play failed."
      }

    adapterRtbAppOpenAd.onAdFailedToPlay(vungleAppOpenAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(appOpenAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
    verifyNoMoreInteractions(appOpenAdCallback)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterRtbAppOpenAd.onAdImpression(vungleAppOpenAd)

    verify(appOpenAdCallback).reportAdImpression()
    verifyNoMoreInteractions(appOpenAdCallback)
  }
}
