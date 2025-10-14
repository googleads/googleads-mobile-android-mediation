package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_ERROR_MESSAGE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.ERROR_MINTEGRAL_SDK
import com.google.ads.mediation.mintegral.MintegralConstants.MINTEGRAL_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.waterfall.MintegralWaterfallInterstitialAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class MintegralInterstitialAdTest {
  // Subject under testing
  private lateinit var mintegralInterstitialAd: MintegralWaterfallInterstitialAd

  private val activity = Robolectric.buildActivity(Activity::class.java).get()
  private val serverParameters =
    bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
  private val mockMintegralNewInterstitialAdWrapper: MintegralNewInterstitialAdWrapper = mock()
  private val mockAdCallback: MediationInterstitialAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockAdCallback
    }
  private val adConfiguration =
    createMediationInterstitialAdConfiguration(
      context = activity,
      serverParameters = serverParameters,
    )
  private val flagValueGetter: FlagValueGetter = mock {
    on { shouldRestrictMultipleAdLoads() } doReturn false
  }

  @Before
  fun setUp() {
    mintegralInterstitialAd =
      MintegralWaterfallInterstitialAd(adConfiguration, mockAdLoadCallback, flagValueGetter)
  }

  @Test
  fun onResourceLoadFailWithCode_invokesOnFailureWithGivenAdError() {
    mintegralInterstitialAd.onResourceLoadFailWithCode(
      /*mBridgeIds=*/ null,
      /*errorCode=*/ 2,
      TEST_ERROR_MESSAGE,
    )

    val expectedError = AdError(2, TEST_ERROR_MESSAGE, MINTEGRAL_SDK_ERROR_DOMAIN)
    verify(mockAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun onResourceLoadSuccess_invokesOnSuccess() {
    mintegralInterstitialAd.onResourceLoadSuccess(/* mBridgeIds= */ null)

    verify(mockAdLoadCallback).onSuccess(mintegralInterstitialAd)
  }

  @Test
  fun onAdShow_invokesOnAdOpenedAndReportAdImpression() {
    mintegralInterstitialAd.onResourceLoadSuccess(/* mBridgeIds= */ null)

    mintegralInterstitialAd.onAdShow(/* mBridgeIds= */ null)

    verify(mockAdCallback).onAdOpened()
    verify(mockAdCallback).reportAdImpression()
  }

  @Test
  fun onShowFailWithCode_invokesOnAdFailedToShow() {
    mintegralInterstitialAd.onResourceLoadSuccess(/* mBridgeIds= */ null)

    mintegralInterstitialAd.onShowFailWithCode(
      /* mBridgeIds= */ null,
      ERROR_MINTEGRAL_SDK,
      TEST_ERROR_MESSAGE,
    )

    val expectedError = AdError(ERROR_MINTEGRAL_SDK, TEST_ERROR_MESSAGE, MINTEGRAL_SDK_ERROR_DOMAIN)
    verify(mockAdCallback).onAdFailedToShow(argThat(AdErrorMatcher(expectedError)))
  }

  @Test
  fun showAd_forWaterfallInterstitial_invokesShowAd() {
    Mockito.mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createInterstitialHandler()) doReturn
        mockMintegralNewInterstitialAdWrapper
      mintegralInterstitialAd.loadAd(adConfiguration)

      mintegralInterstitialAd.showAd(activity)

      verify(mockMintegralNewInterstitialAdWrapper).playVideoMute(any())
      verify(mockMintegralNewInterstitialAdWrapper).show()
    }
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    mintegralInterstitialAd.onResourceLoadSuccess(/* mBridgeIds= */ null)

    mintegralInterstitialAd.onAdClicked(/* mBridgeIds= */ null)

    verify(mockAdCallback).reportAdClicked()
  }

  @Test
  fun onAdClose_invokesOnAdClosed() {
    mintegralInterstitialAd.onResourceLoadSuccess(/* mBridgeIds= */ null)

    mintegralInterstitialAd.onAdClose(/* mBridgeIds= */ null, /* rewardInfo= */ null)

    verify(mockAdCallback).onAdClosed()
  }

  @Test
  fun onLoadCampaignSuccess_throwsNoException() {
    mintegralInterstitialAd.onLoadCampaignSuccess(/* mBridgeIds= */ null)
  }

  @Test
  fun onVideoComplete_throwsNoException() {
    mintegralInterstitialAd.onVideoComplete(/* mBridgeIds= */ null)
  }

  @Test
  fun onAdCloseWithNIReward_throwsNoException() {
    mintegralInterstitialAd.onAdCloseWithNIReward(/* mBridgeIds= */ null, /* rewardInfo= */ null)
  }

  @Test
  fun onEndcardShow_throwsNoException() {
    mintegralInterstitialAd.onEndcardShow(/* mBridgeIds= */ null)
  }
}
