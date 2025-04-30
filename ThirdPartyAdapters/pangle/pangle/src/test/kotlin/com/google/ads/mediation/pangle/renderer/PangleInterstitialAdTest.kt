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
import com.google.ads.mediation.pangle.PangleConstants
import com.google.ads.mediation.pangle.PangleConstants.PANGLE_SDK_ERROR_DOMAIN
import com.google.ads.mediation.pangle.PangleFactory
import com.google.ads.mediation.pangle.PangleInitializer
import com.google.ads.mediation.pangle.PanglePrivacyConfig
import com.google.ads.mediation.pangle.PangleRequestHelper.ADMOB_WATERMARK_KEY
import com.google.ads.mediation.pangle.PangleSdkWrapper
import com.google.ads.mediation.pangle.utils.AdErrorMatcher
import com.google.ads.mediation.pangle.utils.GmaChildDirectedTagsProvider
import com.google.ads.mediation.pangle.utils.TestConstants
import com.google.ads.mediation.pangle.utils.TestConstants.APP_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.BID_RESPONSE
import com.google.ads.mediation.pangle.utils.TestConstants.PLACEMENT_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.WATERMARK
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationFailure
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationSuccess
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
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
  private val interstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn interstitialAdCallback
    }
  private val pangleInitializer: PangleInitializer = mock()
  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val pagInterstitialRequest: PAGInterstitialRequest = mock()
  private val pangleFactory: PangleFactory = mock {
    on { createPagInterstitialRequest() } doReturn pagInterstitialRequest
  }
  private val panglePrivacyConfig: PanglePrivacyConfig = mock()
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
  }

  @Test
  fun render_withoutPlacementId_callsOnFailureOnCallbackWithProperErrorCode() {
    // Given a mediation interstitial ad configuration...
    serverParameters.remove(PangleConstants.PLACEMENT_ID)
    initializeInterstitialAd() // ... without serverParameters send in the Bundle

    interstitialAd.render()

    // The onFailure method of the mediationAdLoadCallback is called with the
    // ERROR_INVALID_SERVER_PARAMETERS code.
    val adError: AdError =
      PangleConstants.createAdapterError(
        PangleConstants.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load interstitial ad from Pangle. Missing or invalid Placement ID.",
      )
    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(adError)))
  }

  @Test
  fun render_withProperConfigurations_doesNotCallTheCallbackOnFailure() {
    // Given the interstitialAd with its proper configuration: placementId and an appId in the
    // serverParameters and a bidResponse.
    initializeInterstitialAd()

    interstitialAd.render()

    // No onFailure should be triggered.
    verify(mediationAdLoadCallback, never()).onFailure(any<AdError>())
  }

  @Test
  fun render_setsCoppaAndThenInitializesPangleSdk(
    @TestParameter(valuesProvider = GmaChildDirectedTagsProvider::class) gmaChildDirectedTag: Int
  ) {
    // Given an interstitialAd
    initializeInterstitialAd(gmaChildDirectedTag)

    interstitialAd.render()

    // pangleInitializer reads the coppa value from panglePrivacyConfig. So, we should ensure that
    // panglePrivacyConfig.setCoppa() is called before pangleInitializer.initialize().
    inOrder(panglePrivacyConfig, pangleInitializer) {
      verify(panglePrivacyConfig).setCoppa(gmaChildDirectedTag)
      verify(pangleInitializer).initialize(eq(context), eq(APP_ID_VALUE), any())
    }
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

    interstitialAd.render()

    verify(pagInterstitialRequest).setAdString(BID_RESPONSE)
    verify(pagInterstitialRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo.containsKey(ADMOB_WATERMARK_KEY)).isTrue()
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

    interstitialAd.render()

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

    interstitialAd.render()

    verify(mediationAdLoadCallback).onSuccess(interstitialAd)
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

    interstitialAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(FAILURE_CODE_PANGLE_INTERSTITIAL_LOAD)
    assertThat(adError.message).isEqualTo(FAILURE_MESSAGE_PANGLE_INTERSTITIAL_LOAD)
    assertThat(adError.domain).isEqualTo(PangleConstants.PANGLE_SDK_ERROR_DOMAIN)
  }

  @Test
  fun render_ifPangleInitializationFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    initializeInterstitialAd()

    interstitialAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(TestConstants.PANGLE_INIT_FAILURE_CODE)
    assertThat(adError.message).isEqualTo(TestConstants.PANGLE_INIT_FAILURE_MESSAGE)
    assertThat(adError.domain).isEqualTo(PANGLE_SDK_ERROR_DOMAIN)
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

    verify(interstitialAdCallback).onAdOpened()
    verify(interstitialAdCallback).reportAdImpression()
  }

  @Test
  fun showAd_ifAdIsClicked_reportsAdClicked() {
    loadPangleInterstitialAd()

    interstitialAd.showAd(context)
    // Capture PAGInterstitialAdInteractionListener.
    verify(pagInterstitialAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is clicked.
    pagAdInteractionListenerCaptor.firstValue.onAdClicked()

    verify(interstitialAdCallback).reportAdClicked()
  }

  @Test
  fun showAd_ifAdIsDismissed_reportsAdClosed() {
    loadPangleInterstitialAd()

    interstitialAd.showAd(context)
    // Capture PAGInterstitialAdInteractionListener.
    verify(pagInterstitialAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is dismissed.
    pagAdInteractionListenerCaptor.firstValue.onAdDismissed()

    verify(interstitialAdCallback).onAdClosed()
  }

  private fun initializeInterstitialAd(
    @TagForChildDirectedTreatment
    tagForChildDirectedTreatment: Int = TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    bidResponse: String = BID_RESPONSE,
    watermark: String = WATERMARK,
  ) {
    // Constructor of the MediationInterstitialAdConfiguration called by the GMA SDK
    mediationInterstitialAdConfig =
      MediationInterstitialAdConfiguration(
        context,
        bidResponse,
        serverParameters,
        /*mediationExtras=*/ Bundle(),
        /*isTesting=*/ true,
        /*location=*/ null,
        tagForChildDirectedTreatment,
        /*taggedForUnderAgeTreatment=*/ -1,
        /*maxAdContentRating=*/ null,
        watermark,
      )

    interstitialAd =
      PangleInterstitialAd(
        mediationInterstitialAdConfig,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        pangleFactory,
        panglePrivacyConfig,
      )
  }

  /** Mock a Pangle interstitial ad load. */
  private fun loadPangleInterstitialAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleInterstitialAdLoadToSucceed()
    initializeInterstitialAd()
    interstitialAd.render()
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
