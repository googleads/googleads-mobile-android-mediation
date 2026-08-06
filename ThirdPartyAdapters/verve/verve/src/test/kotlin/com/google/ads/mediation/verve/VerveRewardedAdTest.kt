package com.google.ads.mediation.verve

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.common.truth.Truth.assertThat
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VerveRewardedAdTest {
  // Subject of testing.
  private lateinit var verveRewardedAd: VerveRewardedAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockHyBidRewardedAd = mock<HyBidRewardedAd>()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val rewardedAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )

  @Before
  fun setUp() {
    val adConfiguration =
      createMediationRewardedAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
    VerveRewardedAd.newInstance(adConfiguration, rewardedAdLoadCallback).onSuccess {
      verveRewardedAd = it
    }
    VerveSdkFactory.delegate = mock {
      on { createHyBidRewardedAd(context, verveRewardedAd) } doReturn mockHyBidRewardedAd
    }
  }

  @Test
  fun loadAd_invokesHyBidLoad() {
    verveRewardedAd.loadAd(context)

    verify(mockHyBidRewardedAd).prepareAd(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun showAd_invokesHyBidShow() {
    verveRewardedAd.loadAd(context)

    verveRewardedAd.showAd(context)

    verify(mockHyBidRewardedAd).show()
  }

  @Test
  fun showAd_withNullHyBidRewardedAd_invokesOnAdFailedToShow() {
    val expectedAdError =
      AdError(
        ERROR_CODE_FULLSCREEN_AD_IS_NULL,
        ERROR_MSG_FULLSCREEN_AD_IS_NULL,
        ADAPTER_ERROR_DOMAIN,
      )
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.showAd(context)

    assertThat(rewardedAdCallback.isFailedToShow).isTrue()
    assertThat(rewardedAdCallback.adFailedToShowError).isEqualTo(expectedAdError)
    verify(mockHyBidRewardedAd, never()).show()
  }

  @Test
  fun onRewardedLoaded_invokesOnSuccess() {
    verveRewardedAd.onRewardedLoaded()

    assertThat(rewardedAdLoadCallback).hasSucceededWith(verveRewardedAd)
  }

  @Test
  fun onRewardedLoadFailed_invokesOnFailure() {
    val testError = Throwable("TestError")
    val expectedAdError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "HyBid Error - Could not load rewarded ad: TestError",
        SDK_ERROR_DOMAIN,
      )

    verveRewardedAd.onRewardedLoadFailed(testError)

    assertThat(rewardedAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun onRewardedClosed_invokesOnAdClosed() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onRewardedClosed()

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun onRewardedOpened_invokesOnAdOpenedAndReportAdImpression() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onRewardedOpened()

    assertThat(rewardedAdCallback.isOpened).isTrue()
    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onRewardedClick_invokesOnAdOpenedReportAdClicked() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onRewardedClick()

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun onReward_invokesOnAdOpenedReportAdClicked() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onReward()

    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }
}
