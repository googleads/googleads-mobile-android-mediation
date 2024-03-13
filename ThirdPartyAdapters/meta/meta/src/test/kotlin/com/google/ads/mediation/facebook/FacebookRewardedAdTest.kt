package com.google.ads.mediation.facebook

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.Ad
import com.facebook.ads.RewardedVideoAd
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardItem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [FacebookRewardedAd]. */
@RunWith(AndroidJUnit4::class)
class FacebookRewardedAdTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mediationRewardedAdCallback = mock<MediationRewardedAdCallback>()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mediationRewardedAdCallback
    }
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
    val serverParameters =
      bundleOf(
        FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER to
          AdapterTestKitConstants.TEST_PLACEMENT_ID
      )
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(context = context, serverParameters = serverParameters)

    adapterRewardedAd =
      FacebookRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback, metaFactory)
  }

  @Test
  fun onAdLoaded_invokesMediationAdLoadCallback() {
    adapterRewardedAd.onAdLoaded(facebookAd)

    verify(mediationAdLoadCallback).onSuccess(ArgumentMatchers.any(FacebookRewardedAd::class.java))
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
        FacebookMediationAdapter.ERROR_DOMAIN
      )

    // invoke the showAd callback
    adapterRewardedAd.showAd(context)

    verify(mediationRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    verify(facebookRewardedAd).destroy()
  }

  @Test
  fun onShowAd_invokedOnVideoStartAndOnAdOpenedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()
    whenever(facebookRewardedAd.show()) doReturn true

    // invoke the showAd callback
    adapterRewardedAd.showAd(context)

    verify(mediationRewardedAdCallback).onVideoStart()
    verify(mediationRewardedAdCallback).onAdOpened()
  }

  @Test
  fun onRewardedVideoCompleted_invokesOnVideoCompleteAndOnUserEarnedRewardCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoCompleted callback
    adapterRewardedAd.onRewardedVideoCompleted()

    verify(mediationRewardedAdCallback).onVideoComplete()
    verify(mediationRewardedAdCallback)
      .onUserEarnedReward(ArgumentMatchers.any(RewardItem::class.java))
  }

  @Test
  fun onRewardedVideoClosed_invokesOnAdClosedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoClpsed callback
    adapterRewardedAd.onRewardedVideoClosed()

    verify(mediationRewardedAdCallback).onAdClosed()
    verify(facebookRewardedAd).destroy()
  }

  @Test
  fun onRewardedVideoClosed_videoAlreadyClosed_invokesOnAdClosedCallbackOnlyOnce() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoClpsed callback
    adapterRewardedAd.onRewardedVideoClosed()
    // make a second callback
    adapterRewardedAd.onRewardedVideoClosed()

    verify(mediationRewardedAdCallback, times(1)).onAdClosed()
    verify(facebookRewardedAd, times(2)).destroy()
  }

  @Test
  fun onRewardedVideoActivityDestroyed_invokesOnAdClosedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the onRewardedVideoActivity destroyed callback
    adapterRewardedAd.onRewardedVideoActivityDestroyed()

    verify(mediationRewardedAdCallback).onAdClosed()
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

    verify(mediationRewardedAdCallback, times(1)).onAdClosed()
    verify(facebookRewardedAd, times(2)).destroy()
  }

  @Test
  fun onAdClicked_invokesReportAdClickedCallback() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the Ad clicked callback
    adapterRewardedAd.onAdClicked(facebookAd)

    verify(mediationRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onLoggingImpression_invokesReportAdImpression() {
    // simulate a successful render and show
    renderAndLoadSuccessfully()

    // invoke the logging impression callback
    adapterRewardedAd.onLoggingImpression(facebookAd)

    verify(mediationRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onError_onShowAdAlreadyCalled_invokesOnAdFailedToShowCallback() {
    val metaAdError = com.facebook.ads.AdError(101, "Error from meta")
    val expectedAdError =
      AdError(
        metaAdError.errorCode,
        metaAdError.errorMessage,
        FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN
      )
    whenever(facebookRewardedAd.show()) doReturn true

    // need to mimic a successful render and load in order for show
    // to be able to be called
    renderAndLoadSuccessfully()
    // simulate show ad called
    adapterRewardedAd.showAd(context)
    // invoke onError callback
    adapterRewardedAd.onError(facebookAd, metaAdError)

    verify(mediationRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onError_onShowAdNotYetCalled_invokesAdLoadCallbackWithFailure() {
    val metaAdError = com.facebook.ads.AdError(101, "Error from meta")
    val expectedAdError =
      AdError(
        metaAdError.errorCode,
        metaAdError.errorMessage,
        FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN
      )

    // mimic an ad render
    adapterRewardedAd.render()
    // invoke onError callback
    adapterRewardedAd.onError(facebookAd, metaAdError)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  private fun renderAndLoadSuccessfully() {
    adapterRewardedAd.render()

    adapterRewardedAd.onAdLoaded(facebookAd)
  }
}
