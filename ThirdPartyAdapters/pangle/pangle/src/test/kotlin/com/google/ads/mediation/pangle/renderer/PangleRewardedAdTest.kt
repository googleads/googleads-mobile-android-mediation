package com.google.ads.mediation.pangle.renderer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest
import com.google.ads.mediation.pangle.PangleConstants
import com.google.ads.mediation.pangle.PangleFactory
import com.google.ads.mediation.pangle.PangleInitializer
import com.google.ads.mediation.pangle.PangleRequestHelper.ADMOB_WATERMARK_KEY
import com.google.ads.mediation.pangle.PangleSdkWrapper
import com.google.ads.mediation.pangle.utils.AdErrorMatcher
import com.google.ads.mediation.pangle.utils.TestConstants
import com.google.ads.mediation.pangle.utils.TestConstants.APP_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.BID_RESPONSE
import com.google.ads.mediation.pangle.utils.TestConstants.PLACEMENT_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.WATERMARK
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationFailure
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationSuccess
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
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

/** Unit tests for [PangleRewardedAd]. */
@RunWith(RobolectricTestParameterInjector::class)
class PangleRewardedAdTest {

  // Subject under test
  private lateinit var rewardedAd: PangleRewardedAd
  private lateinit var mediationRewardedAdConfig: MediationRewardedAdConfiguration
  private var serverParameters: Bundle = Bundle()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val rewardedAdCallback: MediationRewardedAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn rewardedAdCallback
    }
  private val pangleInitializer: PangleInitializer = mock()
  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val pagRewardedRequest: PAGRewardedRequest = mock()
  private val pangleFactory: PangleFactory = mock {
    on { createPagRewardedRequest() } doReturn pagRewardedRequest
  }
  private val pagRewardedAd: PAGRewardedAd = mock()
  private val pagAdInteractionListenerCaptor = argumentCaptor<PAGRewardedAdInteractionListener>()
  private val pagRewardItem: PAGRewardItem = mock()
  private val extraInfoCaptor = argumentCaptor<Map<String, Any>>()

  @Before
  fun setUp() {
    // This is the expected minimum serverParameters for rewardedAd to load
    serverParameters =
      bundleOf(
        PangleConstants.PLACEMENT_ID to PLACEMENT_ID_VALUE,
        PangleConstants.APP_ID to APP_ID_VALUE,
      )
  }

  @Test
  fun render_withoutPlacementId_callsOnFailureOnCallbackWithProperErrorCode() {
    // Given a mediation rewarded ad configuration
    serverParameters.remove(PangleConstants.PLACEMENT_ID)
    initializeRewardedAd() // ... without serverParameters send in the Bundle

    rewardedAd.render()

    // The onFailure method of the mediationAdLoadCallback is called with the
    // ERROR_INVALID_SERVER_PARAMETERS code.
    val adError: AdError =
      PangleConstants.createAdapterError(
        PangleConstants.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load rewarded ad from Pangle. Missing or invalid Placement ID.",
      )
    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(adError)))
  }

  @Test
  fun render_withProperConfigurations_dosNotCallTheCallbackOnFailure() {
    // Given the rewardedAd with its proper configuration: placementId and an appId in the
    // serverParameters and a bidResponse.
    initializeRewardedAd()

    rewardedAd.render()

    // No onFailure should be triggered.
    verify(mediationAdLoadCallback, never()).onFailure(any<AdError>())
    // TODO(b/272102212): Refactor Pangle Rtb classes for better unit testing.
  }

  /**
   * render() test for the case where bid response is available. This is how render() will be called
   * for RTB.
   */
  @Test
  fun render_ifBidResponseIsAvailable_setsBidResponseAndWatermarkAndLoadsPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    initializeRewardedAd(bidResponse = BID_RESPONSE)

    rewardedAd.render()

    verify(pagRewardedRequest).setAdString(TestConstants.BID_RESPONSE)
    verify(pagRewardedRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo.containsKey(ADMOB_WATERMARK_KEY)).isTrue()
    assertThat(extraInfo[ADMOB_WATERMARK_KEY]).isEqualTo(WATERMARK)
    verify(pangleSdkWrapper)
      .loadRewardedAd(eq(TestConstants.PLACEMENT_ID_VALUE), eq(pagRewardedRequest), any())
  }

  /**
   * render() test for the case where bid response and watermark are empty. This is how render()
   * will be called for waterfall.
   */
  @Test
  fun render_ifBidResponseIsEmpty_setsEmptyBidResponseAndLoadsPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    initializeRewardedAd(bidResponse = "", watermark = "")

    rewardedAd.render()

    verify(pagRewardedRequest).setAdString("")
    // Verify that setExtraInfo is not called when watermark is empty.
    verify(pagRewardedRequest, never()).setExtraInfo(any())
    verify(pangleSdkWrapper)
      .loadRewardedAd(eq(TestConstants.PLACEMENT_ID_VALUE), eq(pagRewardedRequest), any())
  }

  @Test
  fun render_ifPangleAdLoadSucceeds_callsLoadSuccessCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleRewardedAdLoadToSucceed()
    initializeRewardedAd()

    rewardedAd.render()

    verify(mediationAdLoadCallback).onSuccess(rewardedAd)
  }

  @Test
  fun render_ifPangleAdLoadFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Stub Pangle rewarded ad load to fail.
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGRewardedAdLoadListener).onError(
          FAILURE_CODE_PANGLE_REWARDED_LOAD,
          FAILURE_MESSAGE_PANGLE_REWARDED_LOAD,
        )
      }
      .whenever(pangleSdkWrapper)
      .loadRewardedAd(any(), any(), any())
    initializeRewardedAd()

    rewardedAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(FAILURE_CODE_PANGLE_REWARDED_LOAD)
    assertThat(adError.message).isEqualTo(FAILURE_MESSAGE_PANGLE_REWARDED_LOAD)
    assertThat(adError.domain).isEqualTo(PangleConstants.PANGLE_SDK_ERROR_DOMAIN)
  }

  @Test
  fun render_ifPangleInitializationFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    initializeRewardedAd()

    rewardedAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(TestConstants.PANGLE_INIT_FAILURE_CODE)
    assertThat(adError.message).isEqualTo(TestConstants.PANGLE_INIT_FAILURE_MESSAGE)
    assertThat(adError.domain).isEqualTo(PangleConstants.PANGLE_SDK_ERROR_DOMAIN)
  }

  @Test
  fun showAd_setsAdInteractionListener() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)

    verify(pagRewardedAd).setAdInteractionListener(any())
  }

  @Test
  fun showAd_ifCalledWithContextThatIsAnActivity_showsPangleAdUsingTheContext() {
    loadPangleRewardedAd()
    val context = Robolectric.buildActivity(Activity::class.java).get() as Context

    rewardedAd.showAd(context)

    verify(pagRewardedAd).show(context as Activity)
  }

  @Test
  fun showAd_ifCalledWithContextThatIsNotAnActivity_showsPangleAdWithoutUsingTheContext() {
    loadPangleRewardedAd()

    // context is ApplicationContext and not an instance of Activity.
    rewardedAd.showAd(context)

    verify(pagRewardedAd).show(null)
  }

  @Test
  fun showAd_ifAdIsShowed_reportsThatAdHasOpenedAndReportsImpression() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)
    // Capture PAGRewardedAdInteractionListener.
    verify(pagRewardedAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is showed.
    pagAdInteractionListenerCaptor.firstValue.onAdShowed()

    verify(rewardedAdCallback).onAdOpened()
    verify(rewardedAdCallback).reportAdImpression()
  }

  @Test
  fun showAd_ifAdIsClicked_reportsAdClicked() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)
    // Capture PAGRewardedAdInteractionListener.
    verify(pagRewardedAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is clicked.
    pagAdInteractionListenerCaptor.firstValue.onAdClicked()

    verify(rewardedAdCallback).reportAdClicked()
  }

  @Test
  fun showAd_ifAdIsDismissed_reportsAdClosed() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)
    // Capture PAGRewardedAdInteractionListener.
    verify(pagRewardedAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is dismissed.
    pagAdInteractionListenerCaptor.firstValue.onAdDismissed()

    verify(rewardedAdCallback).onAdClosed()
  }

  @Test
  fun showAd_ifUserEarnsReward_reportsUserEarnedReward() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)
    // Capture PAGRewardedAdInteractionListener.
    verify(pagRewardedAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the user earns reward.
    pagAdInteractionListenerCaptor.firstValue.onUserEarnedReward(pagRewardItem)

    verify(rewardedAdCallback).onUserEarnedReward()
  }

  private fun initializeRewardedAd(
    @TagForChildDirectedTreatment
    tagForChildDirectedTreatment: Int =
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    bidResponse: String = TestConstants.BID_RESPONSE,
    watermark: String = WATERMARK,
  ) {
    // Constructor of the MediationRewardedAdConfiguration called by the GMA SDK
    mediationRewardedAdConfig =
      MediationRewardedAdConfiguration(
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

    rewardedAd =
      PangleRewardedAd(
        mediationRewardedAdConfig,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        pangleFactory,
      )
  }

  /** Mock a Pangle rewarded ad load. */
  private fun loadPangleRewardedAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleRewardedAdLoadToSucceed()
    initializeRewardedAd()
    rewardedAd.render()
  }

  // Stub pangleSdkWrapper.loadRewardedAd() to succeed.
  private fun stubPangleRewardedAdLoadToSucceed() {
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGRewardedAdLoadListener).onAdLoaded(pagRewardedAd)
      }
      .whenever(pangleSdkWrapper)
      .loadRewardedAd(any(), any(), any())
  }

  companion object {
    private const val FAILURE_CODE_PANGLE_REWARDED_LOAD = 4
    private const val FAILURE_MESSAGE_PANGLE_REWARDED_LOAD = "Pangle rewarded ad load failed"
  }
}
