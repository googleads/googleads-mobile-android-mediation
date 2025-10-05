package com.google.ads.mediation.inmobi.waterfall

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiAdapterUtils.KEY_ACCOUNT_ID
import com.google.ads.mediation.inmobi.InMobiAdapterUtils.KEY_PLACEMENT_ID
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiWaterfallInterstitialAdTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val interstitialAdConfiguration =
    mock<MediationInterstitialAdConfiguration>() { on { context } doReturn context }
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiInterstitialWrapper = mock<InMobiInterstitialWrapper>()
  private val mediationInterstitialAdCallback = mock<MediationInterstitialAdCallback>()

  lateinit var waterfallInterstitialAd: InMobiWaterfallInterstitialAd
  lateinit var adMetaInfo: AdMetaInfo

  @Before
  fun setUp() {
    adMetaInfo = AdMetaInfo("fake", null)
    whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationInterstitialAdCallback)

    waterfallInterstitialAd =
      InMobiWaterfallInterstitialAd(mediationAdLoadCallback, inMobiInitializer, inMobiAdFactory)
  }

  @Test
  fun onShowAd_ifInterstitialAdIsReady_AdIsShown() {
    val initializerListenerCaptor = argumentCaptor<InMobiInitializer.Listener>()
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiInterstitialWrapper)
    whenever(inMobiInterstitialWrapper.isReady).thenReturn(true)
    whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationInterstitialAdCallback)
    val placementId = 67890L
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(KEY_ACCOUNT_ID to "accountTest", KEY_PLACEMENT_ID to placementId.toString())
    waterfallInterstitialAd.loadAd(interstitialAdConfiguration)
    verify(inMobiInitializer)
      .init(eq(context), eq("accountTest"), initializerListenerCaptor.capture())
    initializerListenerCaptor.firstValue.onInitializeSuccess()

    waterfallInterstitialAd.showAd(context)

    verify(inMobiInterstitialWrapper).show()
  }

  @Test
  fun onShowAd_ifInterstitialAdNotReady_invokesOnAdFailedToShowCallback() {
    val initializerListenerCaptor = argumentCaptor<InMobiInitializer.Listener>()
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiInterstitialWrapper)
    whenever(inMobiInterstitialWrapper.isReady).thenReturn(false)
    val placementId = 67890L
    whenever(interstitialAdConfiguration.serverParameters) doReturn
      bundleOf(KEY_ACCOUNT_ID to "accountTest", KEY_PLACEMENT_ID to placementId.toString())
    waterfallInterstitialAd.loadAd(interstitialAdConfiguration)
    verify(inMobiInitializer)
      .init(eq(context), eq("accountTest"), initializerListenerCaptor.capture())
    initializerListenerCaptor.firstValue.onInitializeSuccess()
    // mimic an ad load.
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )

    waterfallInterstitialAd.showAd(context)

    val captor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdCallback).onAdFailedToShow(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_AD_NOT_READY)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
    verify(inMobiInterstitialWrapper, never()).show()
  }

  @Test
  fun onUserLeftApplication_invokesOnAdLeftApplicationCallback() {
    // mimic an ad load
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )
    waterfallInterstitialAd.onUserLeftApplication(inMobiInterstitialWrapper.inMobiInterstitial)

    verify(mediationInterstitialAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdLoadSucceeded_invokesOnSuccessCallback() {
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )

    verify(mediationAdLoadCallback)
      .onSuccess(ArgumentMatchers.any(waterfallInterstitialAd::class.java))
  }

  @Test
  fun onAdLoadFailed_invokesOnFailureCallback() {
    var inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)

    waterfallInterstitialAd.onAdLoadFailed(
      inMobiInterstitialWrapper.inMobiInterstitial,
      inMobiAdRequestStatus,
    )

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code)
      .isEqualTo(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus))
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.INMOBI_SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdDisplayed_invokesOnAdOpenedCallback() {
    // mimic an ad load
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )
    waterfallInterstitialAd.onAdDisplayed(inMobiInterstitialWrapper.inMobiInterstitial, adMetaInfo)

    verify(mediationInterstitialAdCallback).onAdOpened()
  }

  @Test
  fun onAdDisplayFailed_invokesOnAdFailedToShowCallback() {
    // mimic an ad load
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )

    waterfallInterstitialAd.onAdDisplayFailed(inMobiInterstitialWrapper.inMobiInterstitial)

    val captor = argumentCaptor<AdError>()
    verify(mediationInterstitialAdCallback).onAdFailedToShow(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_AD_DISPLAY_FAILED)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun onAdDismissed_invokedOnAdClosedCallback() {
    // mimic an ad load
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )
    waterfallInterstitialAd.onAdDismissed(inMobiInterstitialWrapper.inMobiInterstitial)

    verify(mediationInterstitialAdCallback).onAdClosed()
  }

  @Test
  fun onAdClicked_invokedOnAdClickedCallback() {
    // mimic an ad load
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )
    waterfallInterstitialAd.onAdClicked(inMobiInterstitialWrapper.inMobiInterstitial, null)

    verify(mediationInterstitialAdCallback).reportAdClicked()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    // mimic an ad load
    waterfallInterstitialAd.onAdLoadSucceeded(
      inMobiInterstitialWrapper.inMobiInterstitial,
      adMetaInfo,
    )
    waterfallInterstitialAd.onAdImpression(inMobiInterstitialWrapper.inMobiInterstitial)

    verify(mediationInterstitialAdCallback).reportAdImpression()
  }
}
