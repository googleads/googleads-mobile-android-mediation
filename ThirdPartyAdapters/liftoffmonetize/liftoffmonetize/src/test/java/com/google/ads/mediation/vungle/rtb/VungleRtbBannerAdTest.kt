package com.google.ads.mediation.vungle.rtb

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import android.widget.RelativeLayout.CENTER_HORIZONTAL
import android.widget.RelativeLayout.CENTER_VERTICAL
import android.widget.RelativeLayout.TRUE
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.BaseAd
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleError
import com.vungle.ads.VungleError.Companion.AD_FAILED_TO_DOWNLOAD
import com.vungle.ads.VungleError.Companion.AD_UNABLE_TO_PLAY
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [VungleRtbBannerAd]. */
@RunWith(AndroidJUnit4::class)
class VungleRtbBannerAdTest {

  /** Unit under test. */
  private lateinit var adapterRtbBannerAd: VungleRtbBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val bannerAdCallback = mock<MediationBannerAdCallback>()
  private val bannerAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>> {
      on { onSuccess(any()) } doReturn bannerAdCallback
    }
  private val mockVungleInitializer = mock<VungleInitializer>()
  private val vungleBannerView = mock<VungleBannerView>()
  private val baseAd = mock<BaseAd>()
  private val vungleFactory =
    mock<VungleFactory> { on { createBannerAd(any(), any(), any()) } doReturn vungleBannerView }

  @Before
  fun setUp() {
    adapterRtbBannerAd =
      VungleRtbBannerAd(
        createMediationBannerAdConfiguration(
          context = context,
          serverParameters =
            bundleOf(
              VungleConstants.KEY_APP_ID to TEST_APP_ID,
              VungleConstants.KEY_PLACEMENT_ID to TEST_PLACEMENT_ID
            ),
          bidResponse = AdapterTestKitConstants.TEST_BID_RESPONSE
        ),
        bannerAdLoadCallback,
        vungleFactory
      )

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(mockVungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_addsLiftoffBannerViewToBannerLayoutAndCallsLoadSuccess() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterRtbBannerAd.render()
    }

    adapterRtbBannerAd.onAdLoaded(baseAd)

    val layoutParamsCaptor = argumentCaptor<RelativeLayout.LayoutParams>()
    verify(vungleBannerView, atLeastOnce()).layoutParams = layoutParamsCaptor.capture()
    val layoutParams = layoutParamsCaptor.firstValue
    assertThat(layoutParams.width).isEqualTo(WRAP_CONTENT)
    assertThat(layoutParams.height).isEqualTo(WRAP_CONTENT)
    assertThat(layoutParams.getRule(CENTER_HORIZONTAL)).isEqualTo(TRUE)
    assertThat(layoutParams.getRule(CENTER_VERTICAL)).isEqualTo(TRUE)
    val bannerLayout = adapterRtbBannerAd.view as ViewGroup
    assertThat(bannerLayout.childCount).isEqualTo(1)
    assertThat(bannerLayout.getChildAt(0)).isEqualTo(vungleBannerView)
    verify(bannerAdLoadCallback).onSuccess(adapterRtbBannerAd)
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn AD_FAILED_TO_DOWNLOAD
        on { errorMessage } doReturn "Liftoff Monetize SDK banner ad load failed."
      }

    adapterRtbBannerAd.onAdFailedToLoad(baseAd, liftoffError)

    val expectedError =
      AdError(liftoffError.code, liftoffError.errorMessage, VUNGLE_SDK_ERROR_DOMAIN)
    verify(bannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  private fun renderAdAndMockLoadSuccess() {
    mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer
      adapterRtbBannerAd.render()
    }
    adapterRtbBannerAd.onAdLoaded(baseAd)
  }

  @Test
  fun onAdClicked_reportsAdClickedAndAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterRtbBannerAd.onAdClicked(baseAd)

    verify(bannerAdCallback).reportAdClicked()
    verify(bannerAdCallback).onAdOpened()
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterRtbBannerAd.onAdImpression(baseAd)

    verify(bannerAdCallback).reportAdImpression()
  }

  @Test
  fun onAdLeftApplication_callsOnAdLeftApplication() {
    renderAdAndMockLoadSuccess()

    adapterRtbBannerAd.onAdLeftApplication(baseAd)

    verify(bannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdEnd_noCrash() {
    adapterRtbBannerAd.onAdEnd(baseAd)

    // No matching callback exists on the GMA SDK. This test just verifies that there was no crash.
  }

  @Test
  fun onAdStart_noCrash() {
    adapterRtbBannerAd.onAdStart(baseAd)

    // No matching callback exists on the GMA SDK. This test just verifies that there was no crash.
  }

  @Test
  fun onAdFailedToPlay_noCrash() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn AD_UNABLE_TO_PLAY
        on { errorMessage } doReturn "Liftoff Monetize SDK banner ad play failed."
      }

    adapterRtbBannerAd.onAdFailedToPlay(baseAd, liftoffError)

    // No matching callback exists on the GMA SDK. This test just verifies that there was no crash.
  }
}
