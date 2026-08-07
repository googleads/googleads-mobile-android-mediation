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
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationRewardedAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
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
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
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

/** Unit tests for [PangleRewardedAd]. */
@RunWith(RobolectricTestParameterInjector::class)
class PangleRewardedAdTest {

  // Subject under test
  private lateinit var rewardedAd: PangleRewardedAd
  private lateinit var mediationRewardedAdConfig: MediationRewardedAdConfiguration
  private var serverParameters: Bundle = Bundle()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val rewardedAdCallback = FakeMediationRewardedAdCallback()
  private val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>(
      rewardedAdCallback
    )
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
    // Given a mediation rewarded ad configuration without placement ID in serverParameters.
    serverParameters.remove(PangleConstants.PLACEMENT_ID)
    initializeRewardedAd()

    rewardedAd.render(mediationRewardedAdConfig)

    // The onFailure method of the mediationAdLoadCallback is called with the
    // ERROR_INVALID_SERVER_PARAMETERS code.
    val expectedAdError =
      PangleConstants.createAdapterError(
        PangleConstants.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load rewarded ad from Pangle. Missing or invalid Placement ID.",
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun render_withProperConfigurations_dosNotCallTheCallbackOnFailure() {
    // Given the rewardedAd with its proper configuration: placementId and an appId in the
    // serverParameters and a bidResponse.
    initializeRewardedAd()

    rewardedAd.render(mediationRewardedAdConfig)

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
    initializeRewardedAd(bidResponse = BID_RESPONSE)

    rewardedAd.render(mediationRewardedAdConfig)

    verify(pagRewardedRequest).setAdString(TestConstants.BID_RESPONSE)
    verify(pagRewardedRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo).containsKey(ADMOB_WATERMARK_KEY)
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

    rewardedAd.render(mediationRewardedAdConfig)

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

    rewardedAd.render(mediationRewardedAdConfig)

    assertThat(mediationAdLoadCallback).hasSucceededWith(rewardedAd)
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

    rewardedAd.render(mediationRewardedAdConfig)

    val expectedAdError =
      PangleConstants.createSdkError(
        FAILURE_CODE_PANGLE_REWARDED_LOAD,
        FAILURE_MESSAGE_PANGLE_REWARDED_LOAD,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun render_ifPangleInitializationFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    initializeRewardedAd()

    rewardedAd.render(mediationRewardedAdConfig)

    val expectedAdError =
      PangleConstants.createSdkError(
        TestConstants.PANGLE_INIT_FAILURE_CODE,
        TestConstants.PANGLE_INIT_FAILURE_MESSAGE,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
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

    assertThat(rewardedAdCallback.isOpened).isTrue()
    assertThat(rewardedAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun showAd_ifAdIsClicked_reportsAdClicked() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)
    // Capture PAGRewardedAdInteractionListener.
    verify(pagRewardedAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is clicked.
    pagAdInteractionListenerCaptor.firstValue.onAdClicked()

    assertThat(rewardedAdCallback.isClicked).isTrue()
  }

  @Test
  fun showAd_ifAdIsDismissed_reportsAdClosed() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)
    // Capture PAGRewardedAdInteractionListener.
    verify(pagRewardedAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the ad is dismissed.
    pagAdInteractionListenerCaptor.firstValue.onAdDismissed()

    assertThat(rewardedAdCallback.isClosed).isTrue()
  }

  @Test
  fun showAd_ifUserEarnsReward_reportsUserEarnedReward() {
    loadPangleRewardedAd()

    rewardedAd.showAd(context)
    // Capture PAGRewardedAdInteractionListener.
    verify(pagRewardedAd).setAdInteractionListener(pagAdInteractionListenerCaptor.capture())
    // Mock that the user earns reward.
    pagAdInteractionListenerCaptor.firstValue.onUserEarnedReward(pagRewardItem)

    assertThat(rewardedAdCallback.isUserEarnedReward).isTrue()
  }

  private fun initializeRewardedAd(
    @TagForChildDirectedTreatment
    tagForChildDirectedTreatment: Int =
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    bidResponse: String = TestConstants.BID_RESPONSE,
    watermark: String = WATERMARK,
  ) {
    mediationRewardedAdConfig =
      createMediationRewardedAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        bidResponse = bidResponse,
        watermark = watermark,
        taggedForChildDirectedTreatment = tagForChildDirectedTreatment,
      )

    rewardedAd =
      PangleRewardedAd(mediationAdLoadCallback, pangleInitializer, pangleSdkWrapper, pangleFactory)
  }

  /** Mock a Pangle rewarded ad load. */
  private fun loadPangleRewardedAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleRewardedAdLoadToSucceed()
    initializeRewardedAd()
    rewardedAd.render(mediationRewardedAdConfig)
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
