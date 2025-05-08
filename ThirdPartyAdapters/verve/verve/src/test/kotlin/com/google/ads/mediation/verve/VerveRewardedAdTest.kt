package com.google.ads.mediation.verve

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_MSG_FULLSCREEN_AD_IS_NULL
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
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
  private val mockRewardedAdCallback: MediationRewardedAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockRewardedAdCallback
    }

  @Before
  fun setUp() {
    val adConfiguration =
      createMediationRewardedAdConfiguration(context = context, bidResponse = TEST_BID_RESPONSE)
    VerveRewardedAd.newInstance(adConfiguration, mockAdLoadCallback).onSuccess {
      verveRewardedAd = it
    }
    VerveSdkFactory.delegate = mock {
      on { createHyBidRewardedAd(context, verveRewardedAd) } doReturn mockHyBidRewardedAd
    }
  }

  @Test
  fun loadAd_invokesHyBidLoad() {
    verveRewardedAd.loadAd()

    verify(mockHyBidRewardedAd).prepareAd(eq(TEST_BID_RESPONSE))
  }

  @Test
  fun showAd_invokesHyBidShow() {
    verveRewardedAd.loadAd()

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

    verify(mockRewardedAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
    verify(mockHyBidRewardedAd, never()).show()
  }

  @Test
  fun onRewardedLoaded_invokesOnSuccess() {
    verveRewardedAd.onRewardedLoaded()

    verify(mockAdLoadCallback).onSuccess(eq(verveRewardedAd))
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

    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onRewardedClosed_invokesOnAdClosed() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onRewardedClosed()

    verify(mockRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onRewardedOpened_invokesOnAdOpenedAndReportAdImpression() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onRewardedOpened()

    verify(mockRewardedAdCallback).onAdOpened()
    verify(mockRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onRewardedClick_invokesOnAdOpenedReportAdClicked() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onRewardedClick()

    verify(mockRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onReward_invokesOnAdOpenedReportAdClicked() {
    verveRewardedAd.onRewardedLoaded()

    verveRewardedAd.onReward()

    verify(mockRewardedAdCallback).onUserEarnedReward()
  }
}
