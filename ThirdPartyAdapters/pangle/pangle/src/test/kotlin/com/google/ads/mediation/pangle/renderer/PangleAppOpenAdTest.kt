package com.google.ads.mediation.pangle.renderer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAd
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdInteractionListener
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdLoadListener
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenRequest
import com.google.ads.mediation.pangle.PangleConstants
import com.google.ads.mediation.pangle.PangleFactory
import com.google.ads.mediation.pangle.PangleInitializer
import com.google.ads.mediation.pangle.PangleRequestHelper.ADMOB_WATERMARK_KEY
import com.google.ads.mediation.pangle.PangleSdkWrapper
import com.google.ads.mediation.pangle.renderer.PangleAppOpenAd.ERROR_MSG_INVALID_PLACEMENT_ID
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
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
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

/** Unit tests for [PangleAppOpenAd] */
@RunWith(RobolectricTestParameterInjector::class)
class PangleAppOpenAdTest {
  // Subject of tests
  private lateinit var appOpenAd: PangleAppOpenAd
  private lateinit var mediationAppOpenAdConfig: MediationAppOpenAdConfiguration
  private var serverParameters: Bundle = Bundle()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val appOpenAdCallback: MediationAppOpenAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn appOpenAdCallback
    }
  private val pangleInitializer: PangleInitializer = mock()
  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val pagAppOpenRequest: PAGAppOpenRequest = mock()
  private val pangleFactory: PangleFactory = mock {
    on { createPagAppOpenRequest() } doReturn pagAppOpenRequest
  }
  private val pagAppOpenAd: PAGAppOpenAd = mock()
  private val pagAdInteractionListenerCaptor = argumentCaptor<PAGAppOpenAdInteractionListener>()
  private val extraInfoCaptor = argumentCaptor<Map<String, Any>>()

  @Before
  fun setUp() {
    // This is the expected minimum serverParameters for appOpenAd to load
    serverParameters =
      bundleOf(
        PangleConstants.APP_ID to APP_ID_VALUE,
        PangleConstants.PLACEMENT_ID to PLACEMENT_ID_VALUE,
      )
  }

  @Test
  fun render_withoutPlacementId_callsOnFailureOnCallbackWithProperErrorCode() {
    // Given a mediation appOpen ad configuration...
    serverParameters.remove(PangleConstants.PLACEMENT_ID)
    initializeAppOpenAd() // ... without serverParameters send in the Bundle

    appOpenAd.render()

    // The onFailure method of the mediationAdLoadCallback is called with the
    // ERROR_INVALID_SERVER_PARAMETERS code.
    val adError: AdError =
      PangleConstants.createAdapterError(
        PangleConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_MSG_INVALID_PLACEMENT_ID,
      )
    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(adError)))
  }

  @Test
  fun render_withProperConfigurations_doesNotCallTheCallbackOnFailure() {
    // Given the appOpenAd with its proper configuration: placementId and an appId in the
    // serverParameters and a bidResponse.
    initializeAppOpenAd()

    appOpenAd.render()

    // No onFailure should be triggered.
    verify(mediationAdLoadCallback, never()).onFailure(any<AdError>())
  }


  /**
   * render() test for the case where bid response is available. This is how render() will be called
   * for RTB.
   */
  @Test
  fun render_ifBidResponseIsAvailable_setsBidResponseAndWatermarkAndLoadsPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Initialize appOpen ad with BID_RESPONSE as bid response.
    initializeAppOpenAd()

    appOpenAd.render()

    verify(pagAppOpenRequest).setAdString(BID_RESPONSE)
    verify(pagAppOpenRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo.containsKey(ADMOB_WATERMARK_KEY)).isTrue()
    assertThat(extraInfo[ADMOB_WATERMARK_KEY]).isEqualTo(WATERMARK)
    verify(pangleSdkWrapper).loadAppOpenAd(eq(PLACEMENT_ID_VALUE), eq(pagAppOpenRequest), any())
  }

  /**
   * render() test for the case where bid response and watermark are empty. This is how render()
   * will be called for waterfall.
   */
  @Test
  fun render_ifBidResponseIsEmpty_setsEmptyBidResponseAndLoadsPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    initializeAppOpenAd(bidResponse = "", watermark = "")

    appOpenAd.render()

    verify(pagAppOpenRequest).setAdString("")
    // Verify that setExtraInfo is not called when watermark is empty.
    verify(pagAppOpenRequest, never()).setExtraInfo(any())
    verify(pangleSdkWrapper).loadAppOpenAd(eq(PLACEMENT_ID_VALUE), eq(pagAppOpenRequest), any())
  }

  @Test
  fun render_ifPangleAdLoadSucceeds_callsLoadSuccessCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleAppOpenAdLoadToSucceed()
    initializeAppOpenAd()

    appOpenAd.render()

    verify(mediationAdLoadCallback).onSuccess(appOpenAd)
  }

  @Test
  fun render_ifPangleAdLoadFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Stub Pangle appOpen ad load to fail.
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGAppOpenAdLoadListener).onError(
          FAILURE_CODE_PANGLE_APP_OPEN_LOAD,
          FAILURE_MESSAGE_PANGLE_APP_OPEN_LOAD,
        )
      }
      .whenever(pangleSdkWrapper)
      .loadAppOpenAd(any(), any(), any())
    initializeAppOpenAd()

    appOpenAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(FAILURE_CODE_PANGLE_APP_OPEN_LOAD)
    assertThat(adError.message).isEqualTo(FAILURE_MESSAGE_PANGLE_APP_OPEN_LOAD)
    assertThat(adError.domain).isEqualTo(PangleConstants.PANGLE_SDK_ERROR_DOMAIN)
  }

  @Test
  fun render_ifPangleInitializationFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    initializeAppOpenAd()

    appOpenAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(TestConstants.PANGLE_INIT_FAILURE_CODE)
    assertThat(adError.message).isEqualTo(TestConstants.PANGLE_INIT_FAILURE_MESSAGE)
    assertThat(adError.domain).isEqualTo(PangleConstants.PANGLE_SDK_ERROR_DOMAIN)
  }

  @Test
  fun showAd_setsAdInteractionListener() {
    loadPangleAppOpenAd()

    appOpenAd.showAd(context)

    verify(pagAppOpenAd).setAdInteractionListener(any())
  }

  @Test
  fun showAd_ifCalledWithContextThatIsAnActivity_showsPangleAdUsingTheContext() {
    loadPangleAppOpenAd()
    val context = Robolectric.buildActivity(Activity::class.java).get() as Context

    appOpenAd.showAd(context)

    verify(pagAppOpenAd).show(context as Activity)
  }

  @Test
  fun showAd_ifCalledWithContextThatIsNotAnActivity_showsPangleAdWithoutUsingTheContext() {
    loadPangleAppOpenAd()

    // context is ApplicationContext and not an instance of Activity.
    appOpenAd.showAd(context)

    verify(pagAppOpenAd).show(null)
  }

  @Test
  fun showAd_ifAdIsShowed_reportsThatAdHasOpenedAndReportsImpression() {
    loadPangleAppOpenAd()

    appOpenAd.showAd(context)
    // Capture PAGAppOpenAdInteractionListener.
    verify(pagAppOpenAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is showed.
    pagAdInteractionListenerCaptor.firstValue.onAdShowed()

    verify(appOpenAdCallback).onAdOpened()
    verify(appOpenAdCallback).reportAdImpression()
  }

  @Test
  fun showAd_ifAdIsClicked_reportsAdClicked() {
    loadPangleAppOpenAd()

    appOpenAd.showAd(context)
    // Capture PAGAppOpenAdInteractionListener.
    verify(pagAppOpenAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is clicked.
    pagAdInteractionListenerCaptor.firstValue.onAdClicked()

    verify(appOpenAdCallback).reportAdClicked()
  }

  @Test
  fun showAd_ifAdIsDismissed_reportsAdClosed() {
    loadPangleAppOpenAd()

    appOpenAd.showAd(context)
    // Capture PAGAppOpenAdInteractionListener.
    verify(pagAppOpenAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is dismissed.
    pagAdInteractionListenerCaptor.firstValue.onAdDismissed()

    verify(appOpenAdCallback).onAdClosed()
  }

  private fun initializeAppOpenAd(
    @TagForChildDirectedTreatment
    tagForChildDirectedTreatment: Int = TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    bidResponse: String = BID_RESPONSE,
    watermark: String = WATERMARK,
  ) {
    // Constructor of the MediationAppOpenAdConfiguration called by the GMA SDK
    mediationAppOpenAdConfig =
      MediationAppOpenAdConfiguration(
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

    appOpenAd =
      PangleAppOpenAd(
        mediationAppOpenAdConfig,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        pangleFactory
      )
  }

  /** Mock a Pangle appOpen ad load. */
  private fun loadPangleAppOpenAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleAppOpenAdLoadToSucceed()
    initializeAppOpenAd()
    appOpenAd.render()
  }

  // Stub pangleSdkWrapper.loadAppOpenAd() to succeed.
  private fun stubPangleAppOpenAdLoadToSucceed() {
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGAppOpenAdLoadListener).onAdLoaded(pagAppOpenAd)
      }
      .whenever(pangleSdkWrapper)
      .loadAppOpenAd(any(), any(), any())
  }

  companion object {
    private const val FAILURE_CODE_PANGLE_APP_OPEN_LOAD = 5
    private const val FAILURE_MESSAGE_PANGLE_APP_OPEN_LOAD = "Pangle appOpen ad load failed"
  }
}
