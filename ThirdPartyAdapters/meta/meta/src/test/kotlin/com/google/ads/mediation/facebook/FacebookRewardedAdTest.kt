package com.google.ads.mediation.facebook

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.Ad
import com.facebook.ads.RewardedVideoAd
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [FacebookRewardedAd]. */
@RunWith(AndroidJUnit4::class)
class FacebookRewardedAdTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val serverParameters =
    bundleOf(
      FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID
    )
  private val mediationRewardedAdConfiguration =
    createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)
  private val mediationRewardedAdCallback = FakeMediationRewardedAdCallback()
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      mediationRewardedAdCallback
    )
  private val metaRewardedAdLoadConfig: RewardedVideoAd.RewardedVideoLoadAdConfig = mock()
  private val metaRewardedAdLoadConfigBuilder: RewardedVideoAd.RewardedVideoAdLoadConfigBuilder =
    mock {
      on { withBid(ArgumentMatchers.any()) } doReturn this.mock
      on { withAdListener(ArgumentMatchers.any()) } doReturn this.mock
      on { withAdExperience(ArgumentMatchers.any()) } doReturn this.mock
      on { build() } doReturn metaRewardedAdLoadConfig
    }
  private val facebookRewardedAd: RewardedVideoAd = mock {
    on { buildLoadAdConfig() } doReturn metaRewardedAdLoadConfigBuilder
  }
  private val metaFactory: MetaFactory = mock {
    on { createRewardedAd(any(), any()) } doReturn facebookRewardedAd
  }
  private val facebookAd: Ad = mock()

  /** The unit under test. */
  private lateinit var adapterRewardedAd: FacebookRewardedAd

  @Before
  fun setup() {
    adapterRewardedAd = FacebookRewardedAd(mediationAdLoadCallback, metaFactory)
  }

  @Test
  fun onAdLoaded_invokesMediationAdLoadCallback() {
    adapterRewardedAd.onAdLoaded(facebookAd)

    assertThat(mediationAdLoadCallback).hasSucceededWith(adapterRewardedAd)
  }

  @Test
  fun onShowAd_showError_invokesOnAdFailedToShowCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()
    whenever(facebookRewardedAd.show()) doReturn false
    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_FAILED_TO_PRESENT_AD,
        "Failed to present rewarded ad.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )

    // invoke the showAd callback
    adapterRewardedAd.showAd(context)

    assertThat(mediationRewardedAdCallback.isFailedToShow).isTrue()
    assertThat(mediationRewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
    verify(facebookRewardedAd).destroy()
  }

  @Test
  fun onShowAd_invokedOnVideoStartAndOnAdOpenedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()
    whenever(facebookRewardedAd.show()) doReturn true

    // invoke the showAd callback
    adapterRewardedAd.showAd(context)

    assertThat(mediationRewardedAdCallback.isVideoStarted).isTrue()
    assertThat(mediationRewardedAdCallback.isOpened).isTrue()
  }

  @Test
  fun onRewardedVideoCompleted_invokesOnVideoCompleteAndOnUserEarnedRewardCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoCompleted callback
    adapterRewardedAd.onRewardedVideoCompleted()

    assertThat(mediationRewardedAdCallback.isVideoCompleted).isTrue()
    assertThat(mediationRewardedAdCallback.isUserEarnedReward).isTrue()
  }

  @Test
  fun onRewardedVideoClosed_invokesOnAdClosedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoClosed callback
    adapterRewardedAd.onRewardedVideoClosed()

    assertThat(mediationRewardedAdCallback.isClosed).isTrue()
    verify(facebookRewardedAd).destroy()
  }

  @Test
  fun onRewardedVideoClosed_videoAlreadyClosed_invokesOnAdClosedCallbackOnlyOnce() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoClosed callback
    adapterRewardedAd.onRewardedVideoClosed()
    // make a second callback
    adapterRewardedAd.onRewardedVideoClosed()

    assertThat(mediationRewardedAdCallback.onAdClosedInvokeCount).isEqualTo(1)
    verify(facebookRewardedAd, times(2)).destroy()
  }

  @Test
  fun onRewardedVideoActivityDestroyed_invokesOnAdClosedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoActivity destroyed callback
    adapterRewardedAd.onRewardedVideoActivityDestroyed()

    assertThat(mediationRewardedAdCallback.isClosed).isTrue()
    verify(facebookRewardedAd).destroy()
  }

  @Test
  fun onRewardedVideoActivityDestroyed_videoAlreadyClosed_invokesOnAdClosedCallbackOnlyOnce() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoActivity destroyed callback
    adapterRewardedAd.onRewardedVideoActivityDestroyed()
    // make a second callback
    adapterRewardedAd.onRewardedVideoActivityDestroyed()

    assertThat(mediationRewardedAdCallback.onAdClosedInvokeCount).isEqualTo(1)
    verify(facebookRewardedAd, times(2)).destroy()
  }

  @Test
  fun onAdClicked_invokesReportAdClickedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the Ad clicked callback
    adapterRewardedAd.onAdClicked(facebookAd)

    assertThat(mediationRewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onLoggingImpression_invokesReportAdImpression() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the logging impression callback
    adapterRewardedAd.onLoggingImpression(facebookAd)

    assertThat(mediationRewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onError_onShowAdAlreadyCalled_invokesOnAdFailedToShowCallback() {
    val metaAdError = com.facebook.ads.AdError(101, "Error from meta")
    val expectedAdError =
      AdError(
        metaAdError.errorCode,
        metaAdError.errorMessage,
        FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN,
      )
    whenever(facebookRewardedAd.show()) doReturn true

    // need to mimic a successful render and load in order for show
    // to be able to be called
    renderAndLoadSuccessfully()
    // simulate show ad called
    adapterRewardedAd.showAd(context)
    // invoke onError callback
    adapterRewardedAd.onError(facebookAd, metaAdError)

    assertThat(mediationRewardedAdCallback.isFailedToShow).isTrue()
    assertThat(mediationRewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
  }

  @Test
  fun onError_onShowAdNotYetCalled_invokesAdLoadCallbackWithFailure() {
    val metaAdError = com.facebook.ads.AdError(101, "Error from meta")
    val expectedAdError =
      AdError(
        metaAdError.errorCode,
        metaAdError.errorMessage,
        FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN,
      )

    // mimic an ad render
    adapterRewardedAd.render(mediationRewardedAdConfiguration)
    // invoke onError callback
    adapterRewardedAd.onError(facebookAd, metaAdError)

    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  private fun renderAndLoadSuccessfully() {
    adapterRewardedAd.render(mediationRewardedAdConfiguration)

    adapterRewardedAd.onAdLoaded(facebookAd)
  }
}
