package com.google.ads.mediation.pangle.renderer

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.get
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.pangle.PangleConstants
import com.google.ads.mediation.pangle.PangleFactory
import com.google.ads.mediation.pangle.PangleInitializer
import com.google.ads.mediation.pangle.PangleRequestHelper.ADMOB_WATERMARK_KEY
import com.google.ads.mediation.pangle.PangleSdkWrapper
import com.google.ads.mediation.pangle.utils.TestConstants.APP_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.BID_RESPONSE
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_CODE
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_MESSAGE
import com.google.ads.mediation.pangle.utils.TestConstants.PLACEMENT_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.WATERMARK
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationFailure
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationSuccess
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
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
import org.robolectric.RobolectricTestParameterInjector

/** Unit tests class for [PangleBannerAd]. */
@RunWith(RobolectricTestParameterInjector::class)
class PangleBannerAdTest {

  // Subject of tests
  lateinit var bannerAd: PangleBannerAd

  lateinit var serverParameters: Bundle
  lateinit var mediationBannerAdConfig: MediationBannerAdConfiguration

  private val bannerAdCallback = FakeMediationBannerAdCallback()
  val mediationAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private val pangleInitializer: PangleInitializer = mock()
  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val pagBannerRequest: PAGBannerRequest = mock()
  private val pangleFactory: PangleFactory = mock {
    on { createPagBannerRequest(any()) } doReturn pagBannerRequest
  }
  val context = ApplicationProvider.getApplicationContext<Context>()
  private val extraInfoCaptor = argumentCaptor<Map<String, Any>>()

  @Before
  fun setUp() {
    // This is the expected minimum serverParameters for BannerAd to load
    serverParameters = Bundle()
    serverParameters.putString(PangleConstants.PLACEMENT_ID, PLACEMENT_ID_VALUE)
    serverParameters.putString(PangleConstants.APP_ID, APP_ID_VALUE)

    initializeBannerAd()
  }

  @Test
  fun render_withoutPlacementId_callsOnFailureOnCallbackWithProperErrorCode() {
    serverParameters.remove(PangleConstants.PLACEMENT_ID)
    initializeBannerAd()

    // When render() is called.
    bannerAd.render(mediationBannerAdConfig)

    // The onFailure method of the mediationAdLoadCallback is called with the
    // ERROR_INVALID_SERVER_PARAMETERS code.
    val expectedAdError =
      PangleConstants.createAdapterError(
        PangleConstants.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load banner ad from Pangle. Missing or invalid Placement ID.",
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun render_withProperConfigurations_doesNotCallTheCallbackOnFailure() {
    // Given the bannerAd with its proper configuration: placementId and an appId in the
    // serverParameters and a bidResponse.
    initializeBannerAd()

    // When render() is called.
    bannerAd.render(mediationBannerAdConfig)

    // No onFailure should be triggered.
    assertThat(mediationAdLoadCallback).hasNotFailed()
  }

  /**
   * render() test for the case where bid response is available. This is how render() will be called
   * for RTB.
   */
  @Test
  fun render_ifBidResponseIsAvailable_setsBidResponseAndWatermarkAndLoadsPangleBannerAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Initialize banner ad with BID_RESPONSE as bid response.
    initializeBannerAd()

    bannerAd.render(mediationBannerAdConfig)

    verify(pagBannerRequest).setAdString(BID_RESPONSE)
    verify(pagBannerRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo).containsKey(ADMOB_WATERMARK_KEY)
    assertThat(extraInfo[ADMOB_WATERMARK_KEY]).isEqualTo(WATERMARK)
    // TODO(b/285772989): Also check that the correct banner size is set on pagBannerRequest. That
    // would be easier to check if we used the real SDK (and thus used real SDK's implementation of
    // PAGBannerSize).
    verify(pangleSdkWrapper).loadBannerAd(eq(PLACEMENT_ID_VALUE), eq(pagBannerRequest), any())
  }

  /**
   * render() test for the case where bid response and watermark are empty. This is how render()
   * will be called for waterfall.
   */
  @Test
  fun render_ifBidResponseIsEmpty_setsEmptyBidResponseAndLoadsPangleBannerAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    initializeBannerAd(bidResponse = "", watermark = "")

    bannerAd.render(mediationBannerAdConfig)

    verify(pagBannerRequest).setAdString("")
    // Verify that setExtraInfo is not called when watermark is empty.
    verify(pagBannerRequest, never()).setExtraInfo(any())
    verify(pangleSdkWrapper).loadBannerAd(eq(PLACEMENT_ID_VALUE), eq(pagBannerRequest), any())
  }

  @Test
  fun render_ifPangleAdLoadSucceeds_setsInteractionListenerAndAddsViewAndCallsSuccessCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    val pagBannerAd: PAGBannerAd = mock()
    val pangleBannerView = View(context)
    whenever(pagBannerAd.bannerView) doReturn pangleBannerView
    // Mock that the Pangle ad loads successfully.
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGBannerAdLoadListener).onAdLoaded(pagBannerAd)
      }
      .whenever(pangleSdkWrapper)
      .loadBannerAd(any(), any(), any())
    initializeBannerAd()

    bannerAd.render(mediationBannerAdConfig)

    verify(pagBannerAd).setAdInteractionListener(any())
    assertThat(bannerAd.wrappedAdView.childCount).isEqualTo(1)
    assertThat(bannerAd.wrappedAdView[0]).isEqualTo(pangleBannerView)
    assertThat(mediationAdLoadCallback).hasSucceededWith(bannerAd)
  }

  @Test
  fun render_ifPangleAdLoadFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Mock that the Pangle ad load fails.
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGBannerAdLoadListener).onError(
          PANGLE_BANNER_AD_LOAD_FAILURE_CODE,
          PANGLE_BANNER_AD_LOAD_FAILURE_MESSAGE,
        )
      }
      .whenever(pangleSdkWrapper)
      .loadBannerAd(any(), any(), any())
    initializeBannerAd()

    bannerAd.render(mediationBannerAdConfig)

    val expectedAdError =
      PangleConstants.createSdkError(
        PANGLE_BANNER_AD_LOAD_FAILURE_CODE,
        PANGLE_BANNER_AD_LOAD_FAILURE_MESSAGE,
      )
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun render_ifPangleInitializationFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    initializeBannerAd()

    bannerAd.render(mediationBannerAdConfig)

    val expectedAdError =
      PangleConstants.createSdkError(PANGLE_INIT_FAILURE_CODE, PANGLE_INIT_FAILURE_MESSAGE)
    assertThat(mediationAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun getView_returnsWrappedAdView() {
    // Load Pangle ad so that wrappedAdView is set.
    loadPangleAd()

    assertThat(bannerAd.view).isNotNull()
    assertThat(bannerAd.view).isEqualTo(bannerAd.wrappedAdView)
  }

  @Test
  fun onAdShowed_reportsAdImpression() {
    // Load Pangle ad so that bannerAdCallback is set.
    loadPangleAd()

    bannerAd.onAdShowed()

    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    // Load Pangle ad so that bannerAdCallback is set.
    loadPangleAd()

    bannerAd.onAdClicked()

    assertThat(bannerAdCallback.isClicked).isTrue()
  }

  private fun loadPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    val pagBannerAd: PAGBannerAd = mock()
    whenever(pagBannerAd.bannerView) doReturn View(context)
    // Mock that the Pangle ad loads successfully.
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGBannerAdLoadListener).onAdLoaded(pagBannerAd)
      }
      .whenever(pangleSdkWrapper)
      .loadBannerAd(any(), any(), any())
    initializeBannerAd()
    // Load a Pangle banner ad.
    bannerAd.render(mediationBannerAdConfig)
  }

  private fun initializeBannerAd(
    @TagForChildDirectedTreatment
    tagForChildDirectedTreatment: Int = TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    bidResponse: String = BID_RESPONSE,
    watermark: String = WATERMARK,
    adSize: AdSize = AdSize.BANNER,
  ) {
    mediationBannerAdConfig =
      createMediationBannerAdConfiguration(
        context,
        bidResponse = bidResponse,
        serverParameters = serverParameters,
        taggedForChildDirectedTreatment = tagForChildDirectedTreatment,
        adSize = adSize,
        watermark = watermark,
      )
    bannerAd =
      PangleBannerAd(mediationAdLoadCallback, pangleInitializer, pangleSdkWrapper, pangleFactory)
  }

  companion object {
    private const val PANGLE_BANNER_AD_LOAD_FAILURE_CODE = 2
    private const val PANGLE_BANNER_AD_LOAD_FAILURE_MESSAGE = "Pangle banner ad load failed"
  }
}
