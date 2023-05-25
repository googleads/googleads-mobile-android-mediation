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
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
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
  private val bannerAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
  private val interstitialAdConfiguration = mock<MediationInterstitialAdConfiguration>()
  private val interstitialAdLoadCallback =
    mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
  private val inMobiInterstitialWrapper = mock<InMobiInterstitialWrapper>()
  private val rewardedAdConfiguration = mock<MediationRewardedAdConfiguration>()
  private val rewardedAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
  private val nativeAdConfiguration = mock<MediationNativeAdConfiguration>()
  private val nativeAdLoadCallback =
    mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()
  private val inMobiNativeWrapper = mock<InMobiNativeWrapper>()

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
    whenever(interstitialAdConfiguration.context).thenReturn(context)
    whenever(interstitialAdConfiguration.serverParameters).thenReturn(serverParameters)
    whenever(inMobiAdFactory.createInMobiInterstitialWrapper(any(), any(), any()))
      .thenReturn(inMobiInterstitialWrapper)

    whenever(rewardedAdConfiguration.context).thenReturn(context)
    whenever(rewardedAdConfiguration.serverParameters).thenReturn(serverParameters)
    whenever(nativeAdConfiguration.context).thenReturn(context)
    whenever(nativeAdConfiguration.serverParameters).thenReturn(serverParameters)
    whenever(inMobiAdFactory.createInMobiNativeWrapper(any(), any(), any()))
      .thenReturn(inMobiNativeWrapper)

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

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_BANNER_SIZE_MISMATCH, bannerAdLoadCallback)
  }

  @Test
  fun loadBannerAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      bannerAdLoadCallback
    )
  }

  @Test
  fun loadBannerAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      bannerAdLoadCallback
    )
  }

  @Test
  fun loadBannerAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      bannerAdLoadCallback
    )
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

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    verify(bannerAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadBannerAd_ifInMobiSDKInitialized_loadsBannerAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }

    adapter.loadBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    verify(inMobiBannerWrapper).setEnableAutoRefresh(eq(false))
    verify(inMobiBannerWrapper).setAnimationType(eq(InMobiBanner.AnimationType.ANIMATION_OFF))
    verify(inMobiBannerWrapper).setListener(any(InMobiWaterfallBannerAd::class.java))
    val frameLayoutParamsCaptor = argumentCaptor<FrameLayout.LayoutParams>()
    verify(inMobiAdViewHolder).setLayoutParams(frameLayoutParamsCaptor.capture())
    frameLayoutParamsCaptor.firstValue.apply {
      assertThat(width).isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height).isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
    }
    val linearLayoutParamsCaptor = argumentCaptor<LinearLayout.LayoutParams>()
    verify(inMobiBannerWrapper).setLayoutParams(linearLayoutParamsCaptor.capture())
    linearLayoutParamsCaptor.firstValue.apply {
      assertThat(width).isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height).isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
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

    adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(InMobiConstants.ERROR_BANNER_SIZE_MISMATCH, bannerAdLoadCallback)
  }

  @Test
  fun loadRtbBannerAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      bannerAdLoadCallback
    )
  }

  @Test
  fun loadRtbBannerAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      bannerAdLoadCallback
    )
  }

  @Test
  fun loadRtbBannerAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      bannerAdLoadCallback
    )
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

    adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    verify(bannerAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadRtbBannerAd_ifInMobiSDKInitialized_loadsBannerAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }
    whenever(bannerAdConfiguration.bidResponse).thenReturn(biddingToken)

    adapter.loadRtbBannerAd(bannerAdConfiguration, bannerAdLoadCallback)

    verify(inMobiBannerWrapper).setEnableAutoRefresh(eq(false))
    verify(inMobiBannerWrapper).setAnimationType(eq(InMobiBanner.AnimationType.ANIMATION_OFF))
    verify(inMobiBannerWrapper).setListener(any(InMobiRtbBannerAd::class.java))
    val frameLayoutParamsCaptor = argumentCaptor<FrameLayout.LayoutParams>()
    verify(inMobiAdViewHolder).setLayoutParams(frameLayoutParamsCaptor.capture())
    frameLayoutParamsCaptor.firstValue.apply {
      assertThat(width).isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height).isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
    }
    val linearLayoutParamsCaptor = argumentCaptor<LinearLayout.LayoutParams>()
    verify(inMobiBannerWrapper).setLayoutParams(linearLayoutParamsCaptor.capture())
    linearLayoutParamsCaptor.firstValue.apply {
      assertThat(width).isEqualTo(bannerAdConfiguration.adSize.getWidthInPixels(context))
      assertThat(height).isEqualTo(bannerAdConfiguration.adSize.getHeightInPixels(context))
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

  @Test
  fun loadInterstitialAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      interstitialAdLoadCallback
    )
  }

  @Test
  fun loadInterstitialAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      interstitialAdLoadCallback
    )
  }

  @Test
  fun loadInterstitialAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      interstitialAdLoadCallback
    )
  }

  @Test
  fun loadInterstitialAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    verify(interstitialAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadInterstitialAd_ifInMobiSDKInitialized_loadsInterstitialAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }

    adapter.loadInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiInterstitialWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_WATERFALL)
    verify(inMobiInterstitialWrapper).setKeywords(anyString())
    verify(inMobiInterstitialWrapper).load()
  }

  @Test
  fun loadRtbInterstitialAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadRtbInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      interstitialAdLoadCallback
    )
  }

  @Test
  fun loadRtbInterstitialAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadRtbInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      interstitialAdLoadCallback
    )
  }

  @Test
  fun loadRtbInterstitialAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadRtbInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      interstitialAdLoadCallback
    )
  }

  @Test
  fun loadRtbInterstitialAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadRtbInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    verify(interstitialAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadRtbInterstitialAd_ifInMobiSDKInitialized_loadsInterstitialAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }
    whenever(interstitialAdConfiguration.bidResponse).thenReturn(biddingToken)

    adapter.loadRtbInterstitialAd(interstitialAdConfiguration, interstitialAdLoadCallback)

    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiInterstitialWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_RTB)
    verify(inMobiInterstitialWrapper).setKeywords(anyString())
    val tokenCaptor = argumentCaptor<ByteArray>()
    verify(inMobiInterstitialWrapper).load(tokenCaptor.capture())
    assertThat(tokenCaptor.firstValue).isEqualTo(biddingToken.toByteArray())
  }

  @Test
  fun loadRewardedAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      rewardedAdLoadCallback
    )
  }

  @Test
  fun loadRewardedAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      rewardedAdLoadCallback
    )
  }

  @Test
  fun loadRewardedAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      rewardedAdLoadCallback
    )
  }

  @Test
  fun loadRewardedAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    verify(rewardedAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadRewardedAd_ifInMobiSDKInitialized_loadsRewardedAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }

    adapter.loadRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiInterstitialWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_WATERFALL)
    verify(inMobiInterstitialWrapper).setKeywords(anyString())
    verify(inMobiInterstitialWrapper).load()
  }

  @Test
  fun loadRtbRewardedAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadRtbRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      rewardedAdLoadCallback
    )
  }

  @Test
  fun loadRtbRewardedAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadRtbRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      rewardedAdLoadCallback
    )
  }

  @Test
  fun loadRtbRewardedAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadRtbRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      rewardedAdLoadCallback
    )
  }

  @Test
  fun loadRtbRewardedAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadRtbRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    verify(rewardedAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadRtbRewardedAd_ifInMobiSDKInitialized_loadsRewardedAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }
    whenever(rewardedAdConfiguration.bidResponse).thenReturn(biddingToken)

    adapter.loadRtbRewardedAd(rewardedAdConfiguration, rewardedAdLoadCallback)

    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiInterstitialWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_RTB)
    verify(inMobiInterstitialWrapper).setKeywords(anyString())
    val tokenCaptor = argumentCaptor<ByteArray>()
    verify(inMobiInterstitialWrapper).load(tokenCaptor.capture())
    assertThat(tokenCaptor.firstValue).isEqualTo(biddingToken.toByteArray())
  }

  @Test
  fun loadNativeAd_withoutAccountId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_ACCOUNT_ID)

    adapter.loadNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      nativeAdLoadCallback
    )
  }

  @Test
  fun loadNativeAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      nativeAdLoadCallback
    )
  }

  @Test
  fun loadNativeAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      nativeAdLoadCallback
    )
  }

  @Test
  fun loadNativeAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    verify(nativeAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadNativeAd_ifInMobiSDkInitialized_loadsNativeAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }

    adapter.loadNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiNativeWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_WATERFALL)
    verify(inMobiNativeWrapper).setKeywords(anyString())
    verify(inMobiNativeWrapper).load()
  }

  @Test
  fun loadRtbNativeAd_withoutPlacementId_invokesFailureCallback() {
    serverParameters.remove(InMobiAdapterUtils.KEY_PLACEMENT_ID)

    adapter.loadRtbNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      nativeAdLoadCallback
    )
  }

  @Test
  fun loadRtbNativeAd_invalidPlacementId_invokesFailureCallback() {
    serverParameters.putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "-12345")

    adapter.loadRtbNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    assertFailureCallbackAdError(
      InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS,
      nativeAdLoadCallback
    )
  }

  @Test
  fun loadRtbNativeAd_InMobiSDKInitializationFailed_invokesFailureCallback() {
    val error =
      InMobiConstants.createAdapterError(
        InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION,
        "InMobi SDK initialization failed"
      )

    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeError(error)
    }

    adapter.loadRtbNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    verify(nativeAdLoadCallback).onFailure(error)
  }

  @Test
  fun loadRtbNativeAd_ifInMobiSDkInitialized_loadsNativeAd() {
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }
    whenever(nativeAdConfiguration.bidResponse).thenReturn(biddingToken)

    adapter.loadRtbNativeAd(nativeAdConfiguration, nativeAdLoadCallback)

    val extrasCaptor = argumentCaptor<Map<String, String>>()
    verify(inMobiNativeWrapper).setExtras(extrasCaptor.capture())
    assertThat(extrasCaptor.firstValue["tp"]).isEqualTo(InMobiAdapterUtils.PROTOCOL_RTB)
    verify(inMobiNativeWrapper).setKeywords(anyString())
    val tokenCaptor = argumentCaptor<ByteArray>()
    verify(inMobiNativeWrapper).load(tokenCaptor.capture())
    assertThat(tokenCaptor.firstValue).isEqualTo(biddingToken.toByteArray())
  }

  private fun <MediationAdT, MediationAdCallbackT> assertFailureCallbackAdError(
    error: Int,
    adLoadCallback: MediationAdLoadCallback<MediationAdT, MediationAdCallbackT>
  ) {
    val captor = argumentCaptor<AdError>()
    verify(adLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(error)
  }

  companion object {
    private const val biddingToken = "BiddingToken"
  }
}
