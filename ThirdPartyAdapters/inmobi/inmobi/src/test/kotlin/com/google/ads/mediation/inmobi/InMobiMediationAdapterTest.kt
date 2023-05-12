package com.google.ads.mediation.inmobi

import android.content.Context
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener
import com.google.ads.mediation.inmobi.rtb.InMobiRtbBannerAd
import com.google.ads.mediation.inmobi.waterfall.InMobiWaterfallBannerAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.InMobiBanner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiMediationAdapterTest {

  val mediationConfiguration: MediationConfiguration = mock()
  val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val rtbSignalData = mock<RtbSignalData>()
  private val signalCallbacks = mock<SignalCallbacks>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val bannerAdConfiguration = mock<MediationBannerAdConfiguration>()
  private val inMobiBannerWrapper = mock<InMobiBannerWrapper>()
  private val inMobiAdViewHolder = mock<InMobiAdViewHolder>()
  private val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()

  lateinit var serverParameters: Bundle
  lateinit var adapter: InMobiMediationAdapter

  @Before
  fun setUp() {
    serverParameters = Bundle()
    serverParameters.putString(InMobiAdapterUtils.KEY_ACCOUNT_ID, "12345")
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "67890")

    whenever(bannerAdConfiguration.context).thenReturn(context)
    whenever(bannerAdConfiguration.adSize).thenReturn(AdSize(350, 50))
    whenever(bannerAdConfiguration.serverParameters).thenReturn(serverParameters)
    whenever(inMobiAdFactory.createInMobiBannerWrapper(any(), any()))
      .thenReturn(inMobiBannerWrapper)
    whenever(inMobiAdFactory.createInMobiAdViewHolder(any())).thenReturn(inMobiAdViewHolder)
    whenever(inMobiAdViewHolder.addView(any())).doAnswer { null }

    adapter = InMobiMediationAdapter(inMobiInitializer, inMobiAdFactory, inMobiSdkWrapper)
  }

  @Test
  fun collectSignals_invokesOnSuccessCallbackWithBiddingToken() {
    val biddingToken = "inMobiToken"
    whenever(inMobiSdkWrapper.getToken(any(), any())).thenReturn(biddingToken)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(signalCallbacks).onSuccess(biddingToken)
  }

  @Test
  fun loadBannerAd_invalidBannerSize_invokesFailureCallback() {
    whenever(bannerAdConfiguration.adSize).thenReturn(AdSize(350, 100))

    adapter.loadBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_BANNER_SIZE_MISMATCH)
  }

  @Test
  fun loadBannerAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadBannerAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadBannerAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadBannerAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    verify(mediationAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadBannerAd_ifInMobiSDKInitialized_loadsBannerAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }

    adapter.loadBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    verify(inMobiBannerWrapper).setEnableAutoRefresh(eq(false))
    verify(inMobiBannerWrapper).setAnimationType(eq(InMobiBanner.AnimationType.ANIMATION_OFF))
    verify(inMobiBannerWrapper).setListener(any(InMobiWaterfallBannerAd::class.java))
    val frameLayoutParamsCaptor = argumentCaptor<FrameLayout.LayoutParams>()
    verify(inMobiAdViewHolder).setLayoutParams(frameLayoutParamsCaptor.capture())
    frameLayoutParamsCaptor.firstValue.apply {
      assertThat(width)
        .isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height)
        .isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
    }
    val linearLayoutParamsCaptor = argumentCaptor<LinearLayout.LayoutParams>()
    verify(inMobiBannerWrapper).setLayoutParams(linearLayoutParamsCaptor.capture())
    linearLayoutParamsCaptor.firstValue.apply {
      assertThat(width)
        .isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height)
        .isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
    }
    verify(inMobiAdViewHolder).addView(eq(inMobiBannerWrapper))
    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiBannerWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_WATERFALL)
    verify(inMobiBannerWrapper).setKeywords(anyString())
    verify(inMobiBannerWrapper).load()
  }

  @Test
  fun loadRtbBannerAd_invalidBannerSize_invokesFailureCallback() {
    whenever(bannerAdConfiguration.adSize).thenReturn(AdSize(350, 100))

    adapter.loadRtbBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_BANNER_SIZE_MISMATCH)
  }

  @Test
  fun loadRtbBannerAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadRtbBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadRtbBannerAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadRtbBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadRtbBannerAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadRtbBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS)
  }

  @Test
  fun loadRtbBannerAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadRtbBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    verify(mediationAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadRtbBannerAd_ifInMobiSDKInitialized_loadsBannerAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }
    whenever(bannerAdConfiguration.bidResponse).thenReturn(biddingToken)

    adapter.loadRtbBannerAd(bannerAdConfiguration, mediationAdLoadCallback)

    verify(inMobiBannerWrapper).setEnableAutoRefresh(eq(false))
    verify(inMobiBannerWrapper).setAnimationType(eq(InMobiBanner.AnimationType.ANIMATION_OFF))
    verify(inMobiBannerWrapper).setListener(any(InMobiRtbBannerAd::class.java))
    val frameLayoutParamsCaptor = argumentCaptor<FrameLayout.LayoutParams>()
    verify(inMobiAdViewHolder).setLayoutParams(frameLayoutParamsCaptor.capture())
    frameLayoutParamsCaptor.firstValue.apply {
      assertThat(width)
        .isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height)
        .isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
    }
    val linearLayoutParamsCaptor = argumentCaptor<LinearLayout.LayoutParams>()
    verify(inMobiBannerWrapper).setLayoutParams(linearLayoutParamsCaptor.capture())
    linearLayoutParamsCaptor.firstValue.apply {
      assertThat(width)
        .isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height)
        .isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
    }
    verify(inMobiAdViewHolder).addView(eq(inMobiBannerWrapper))
    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiBannerWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_RTB)
    verify(inMobiBannerWrapper).setKeywords(anyString())
    val tokenCaptor = argumentCaptor<ByteArray>()
    verify(inMobiBannerWrapper).load(tokenCaptor.capture())
    assertThat(tokenCaptor.firstValue).isEqualTo(biddingToken.toByteArray())
  }

  private fun assertFailureCallbackAdError(error: Int) {
    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(error)
  }

  companion object {
    private const val biddingToken = "BiddingToken"
  }
}
