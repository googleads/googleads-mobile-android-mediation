package com.google.ads.mediation.vungle.waterfall

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationAppOpenAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
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
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.AdConfig.Companion.LANDSCAPE
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleError
import com.vungle.ads.internal.protos.Sdk.SDKError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [VungleWaterfallAppOpenAd]. */
@RunWith(AndroidJUnit4::class)
class VungleWaterfallAppOpenAdTest {

  /** Unit under test. */
  private lateinit var adapterWaterfallAppOpenAd: VungleWaterfallAppOpenAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val appOpenAdCallback = FakeMediationAppOpenAdCallback()
  private val appOpenAdLoadCallback =
    FakeMediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>(appOpenAdCallback)
  private val vungleInitializer = mock<VungleInitializer>()
  private val vungleAppOpenAd = mock<InterstitialAd>()
  private val vungleFactory =
    mock<VungleFactory> {
      on { createInterstitialAd(any(), any(), any()) } doReturn vungleAppOpenAd
      on { createAdConfig() } doReturn mock()
    }
  private val mediationAppOpenAdConfiguration =
    createMediationAppOpenAdConfiguration(
      context = context,
      serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      bidResponse = TEST_BID_RESPONSE,
      watermark = TEST_WATERMARK,
      mediationExtras = bundleOf(KEY_ORIENTATION to LANDSCAPE),
    )

  @Before
  fun setUp() {
    adapterWaterfallAppOpenAd = VungleWaterfallAppOpenAd(appOpenAdLoadCallback, vungleFactory)

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(vungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_callsLoadSuccess() {
    adapterWaterfallAppOpenAd.onAdLoaded(vungleAppOpenAd)

    assertThat(appOpenAdLoadCallback).hasSucceededWith(adapterWaterfallAppOpenAd)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.API_REQUEST_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK appOpen ad load failed."
      }

    adapterWaterfallAppOpenAd.onAdFailedToLoad(vungleAppOpenAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    assertThat(appOpenAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun showAd_ifLiftoffCanPlayAd_playsLiftoffAd() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterWaterfallAppOpenAd.render(mediationAppOpenAdConfiguration)
    }
    whenever(vungleAppOpenAd.canPlayAd()) doReturn true

    adapterWaterfallAppOpenAd.showAd(context)

    verify(vungleAppOpenAd).play(context)
  }

  @Test
  fun showAd_ifAppOpenAdIsNull_callsOnAdFailedToShow() {
    adapterWaterfallAppOpenAd.onAdLoaded(vungleAppOpenAd)

    adapterWaterfallAppOpenAd.showAd(context)

    val expectedError =
      AdError(
        ERROR_CANNOT_PLAY_AD,
        "Failed to show app open ad from Liftoff Monetize.",
        ERROR_DOMAIN,
      )
    assertThat(appOpenAdCallback.isFailedToShow).isTrue()
    assertThat(appOpenAdCallback.adFailedToShowError).isEqualTo(expectedError)
  }

  private fun renderAdAndMockLoadSuccess() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterWaterfallAppOpenAd.render(mediationAppOpenAdConfiguration)
    }
    adapterWaterfallAppOpenAd.onAdLoaded(vungleAppOpenAd)
  }

  @Test
  fun onAdStart_callsOnAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallAppOpenAd.onAdStart(vungleAppOpenAd)

    assertThat(appOpenAdCallback.isOpened).isTrue()
  }

  @Test
  fun onAdEnd_callsOnAdClosed() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallAppOpenAd.onAdEnd(vungleAppOpenAd)

    assertThat(appOpenAdCallback.isClosed).isTrue()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallAppOpenAd.onAdClicked(vungleAppOpenAd)

    assertThat(appOpenAdCallback.isClicked).isTrue()
  }

  @Test
  fun onAdLeftApplication_noInteractions() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallAppOpenAd.onAdLeftApplication(vungleAppOpenAd)

    assertThat(appOpenAdCallback.isOpened).isFalse()
    assertThat(appOpenAdCallback.isClosed).isFalse()
    assertThat(appOpenAdCallback.isClicked).isFalse()
    assertThat(appOpenAdCallback.isImpressionReported).isFalse()
  }

  @Test
  fun onAdFailedToPlay_callsOnAdFailedToShow() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.AD_NOT_LOADED_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK ad play failed."
      }

    adapterWaterfallAppOpenAd.onAdFailedToPlay(vungleAppOpenAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    assertThat(appOpenAdCallback.isFailedToShow).isTrue()
    assertThat(appOpenAdCallback.adFailedToShowError).isEqualTo(expectedError)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterWaterfallAppOpenAd.onAdImpression(vungleAppOpenAd)

    assertThat(appOpenAdCallback.isImpressionReported).isTrue()
  }
}
