package com.google.ads.mediation.pangle.renderer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationInterstitialAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.pangle.PangleConstants
import com.google.ads.mediation.pangle.PangleFactory
import com.google.ads.mediation.pangle.PangleInitializer
import com.google.ads.mediation.pangle.PangleRequestHelper.ADMOB_WATERMARK_KEY
import com.google.ads.mediation.pangle.PangleSdkWrapper
import com.google.ads.mediation.pangle.utils.TestConstants
import com.google.ads.mediation.pangle.utils.TestConstants.APP_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.BID_RESPONSE
import com.google.ads.mediation.pangle.utils.TestConstants.PLACEMENT_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.WATERMARK
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationFailure
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationSuccess
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector

/** Unit tests for [PangleInterstitialAd] */
@RunWith(RobolectricTestParameterInjector::class)
class PangleInterstitialAdTest {
  // Subject of tests
  private lateinit var interstitialAd: PangleInterstitialAd
  private lateinit var mediationInterstitialAdConfig: MediationInterstitialAdConfiguration
  private var serverParameters: Bundle = Bundle()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val interstitialAdCallback = FakeMediationInterstitialAdCallback()
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>(
      interstitialAdCallback
    )
  private val pangleInitializer: PangleInitializer = mock()
  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val pagInterstitialRequest: PAGInterstitialRequest = mock()
  private val pangleFactory: PangleFactory = mock {
    on { createPagInterstitialRequest() } doReturn pagInterstitialRequest
  }
  private val pagInterstitialAd: PAGInterstitialAd = mock()
  private val pagAdInteractionListenerCaptor =
    argumentCaptor<PAGInterstitialAdInteractionListener>()
  private val extraInfoCaptor = argumentCaptor<Map<String, Any>>()

  @Before
  fun setUp() {
    // This is the expected minimum serverParameters for interstitialAd to load
    serverParameters =
      bundleOf(
        PangleConstants.APP_ID to APP_ID_VALUE,
        PangleConstants.PLACEMENT_ID to PLACEMENT_ID_VALUE,
      )
    initializeInterstitialAd()
  }

  @Test
  fun render_withoutPlacementId_callsOnFailureOnCallbackWithProperErrorCode() {
    serverParameters.remove(PangleConstants.PLACEMENT_ID)
    initializeInterstitialAd()

    interstitialAd.render(mediationInterstitialAdConfig)

    // The onFailure method of the mediationAdLoadCallback is called with the
    // ERROR_INVALID_SERVER_PARAMETERS code.
    val expectedAdError =
      PangleConstants.createAdapterError(
        PangleConstants.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load interstitial ad from Pangle. Missing or invalid Placement ID.",
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun render_withProperConfigurations_doesNotCallTheCallbackOnFailure() {
    // Given the interstitialAd with its proper configuration: placementId and an appId in the
    // serverParameters and a bidResponse.
    initializeInterstitialAd()

    interstitialAd.render(mediationInterstitialAdConfig)

    // No onFailure should be triggered.
    assertThat(mediationAdLoadCallback).hasNotFailed()
  }

  /**
   * render() test for the case where bid response is available. This is how render() will be called
   * for RTB.
   */
  @Test
  fun render_ifBidResponseIsAvailable_setsBidResponseAndWatermarkAndLoadsPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Initialize interstitial ad with BID_RESPONSE as bid response.
    initializeInterstitialAd()

    interstitialAd.render(mediationInterstitialAdConfig)

    verify(pagInterstitialRequest).setAdString(BID_RESPONSE)
    verify(pagInterstitialRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo).containsKey(ADMOB_WATERMARK_KEY)
    assertThat(extraInfo[ADMOB_WATERMARK_KEY]).isEqualTo(WATERMARK)
    verify(pangleSdkWrapper)
      .loadInterstitialAd(eq(PLACEMENT_ID_VALUE), eq(pagInterstitialRequest), any())
  }

  /**
   * render() test for the case where bid response and watermark are empty. This is how render()
   * will be called for waterfall.
   */
  @Test
  fun render_ifBidResponseIsEmpty_setsEmptyBidResponseAndLoadsPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    initializeInterstitialAd(bidResponse = "", watermark = "")

    interstitialAd.render(mediationInterstitialAdConfig)

    verify(pagInterstitialRequest).setAdString("")
    // Verify that setExtraInfo is not called when watermark is empty.
    verify(pagInterstitialRequest, never()).setExtraInfo(any())
    verify(pangleSdkWrapper)
      .loadInterstitialAd(eq(PLACEMENT_ID_VALUE), eq(pagInterstitialRequest), any())
  }

  @Test
  fun render_ifPangleAdLoadSucceeds_callsLoadSuccessCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleInterstitialAdLoadToSucceed()
    initializeInterstitialAd()

    interstitialAd.render(mediationInterstitialAdConfig)

    assertThat(mediationAdLoadCallback).hasSucceededWith(interstitialAd)
  }

  @Test
  fun render_ifPangleAdLoadFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Stub Pangle interstitial ad load to fail.
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGInterstitialAdLoadListener).onError(
          FAILURE_CODE_PANGLE_INTERSTITIAL_LOAD,
          FAILURE_MESSAGE_PANGLE_INTERSTITIAL_LOAD,
        )
      }
      .whenever(pangleSdkWrapper)
      .loadInterstitialAd(any(), any(), any())
    initializeInterstitialAd()

    interstitialAd.render(mediationInterstitialAdConfig)

    val expectedAdError =
      PangleConstants.createSdkError(
        FAILURE_CODE_PANGLE_INTERSTITIAL_LOAD,
        FAILURE_MESSAGE_PANGLE_INTERSTITIAL_LOAD,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun render_ifPangleInitializationFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    initializeInterstitialAd()

    interstitialAd.render(mediationInterstitialAdConfig)

    val expectedAdError =
      PangleConstants.createSdkError(
        TestConstants.PANGLE_INIT_FAILURE_CODE,
        TestConstants.PANGLE_INIT_FAILURE_MESSAGE,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun showAd_setsAdInteractionListener() {
    loadPangleInterstitialAd()

    interstitialAd.showAd(context)

    verify(pagInterstitialAd).setAdInteractionListener(any())
  }

  @Test
  fun showAd_ifCalledWithContextThatIsAnActivity_showsPangleAdUsingTheContext() {
    loadPangleInterstitialAd()
    val context = Robolectric.buildActivity(Activity::class.java).get() as Context

    interstitialAd.showAd(context)

    verify(pagInterstitialAd).show(context as Activity)
  }

  @Test
  fun showAd_ifCalledWithContextThatIsNotAnActivity_showsPangleAdWithoutUsingTheContext() {
    loadPangleInterstitialAd()

    // context is ApplicationContext and not an instance of Activity.
    interstitialAd.showAd(context)

    verify(pagInterstitialAd).show(null)
  }

  @Test
  fun showAd_ifAdIsShowed_reportsThatAdHasOpenedAndReportsImpression() {
    loadPangleInterstitialAd()

    interstitialAd.showAd(context)
    // Capture PAGInterstitialAdInteractionListener.
    verify(pagInterstitialAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is showed.
    pagAdInteractionListenerCaptor.firstValue.onAdShowed()

    assertThat(interstitialAdCallback.isOpened).isTrue()
    assertThat(interstitialAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun showAd_ifAdIsClicked_reportsAdClicked() {
    loadPangleInterstitialAd()

    interstitialAd.showAd(context)
    // Capture PAGInterstitialAdInteractionListener.
    verify(pagInterstitialAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is clicked.
    pagAdInteractionListenerCaptor.firstValue.onAdClicked()

    assertThat(interstitialAdCallback.isClicked).isTrue()
  }

  @Test
  fun showAd_ifAdIsDismissed_reportsAdClosed() {
    loadPangleInterstitialAd()

    interstitialAd.showAd(context)
    // Capture PAGInterstitialAdInteractionListener.
    verify(pagInterstitialAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is dismissed.
    pagAdInteractionListenerCaptor.firstValue.onAdDismissed()

    assertThat(interstitialAdCallback.isClosed).isTrue()
  }

  private fun initializeInterstitialAd(
    @TagForChildDirectedTreatment
    tagForChildDirectedTreatment: Int = TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    bidResponse: String = BID_RESPONSE,
    watermark: String = WATERMARK,
  ) {
    mediationInterstitialAdConfig =
      createMediationInterstitialAdConfiguration(
        context,
        bidResponse = bidResponse,
        serverParameters = serverParameters,
        taggedForChildDirectedTreatment = tagForChildDirectedTreatment,
        watermark = watermark,
      )

    interstitialAd =
      PangleInterstitialAd(
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        pangleFactory,
      )
  }

  /** Mock a Pangle interstitial ad load. */
  private fun loadPangleInterstitialAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleInterstitialAdLoadToSucceed()
    initializeInterstitialAd()
    interstitialAd.render(mediationInterstitialAdConfig)
  }

  // Stub pangleSdkWrapper.loadInterstitialAd() to succeed.
  private fun stubPangleInterstitialAdLoadToSucceed() {
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGInterstitialAdLoadListener).onAdLoaded(pagInterstitialAd)
      }
      .whenever(pangleSdkWrapper)
      .loadInterstitialAd(any(), any(), any())
  }

  companion object {
    private const val FAILURE_CODE_PANGLE_INTERSTITIAL_LOAD = 3
    private const val FAILURE_MESSAGE_PANGLE_INTERSTITIAL_LOAD =
      "Pangle interstitial ad load failed"
  }
}
