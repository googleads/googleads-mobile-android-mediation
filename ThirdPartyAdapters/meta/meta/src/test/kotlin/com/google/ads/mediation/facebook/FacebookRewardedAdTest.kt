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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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

  private fun renderAndLoadSuccessfully() {
    adapterRewardedAd.render()

    adapterRewardedAd.onAdLoaded(facebookAd)
  }
}
