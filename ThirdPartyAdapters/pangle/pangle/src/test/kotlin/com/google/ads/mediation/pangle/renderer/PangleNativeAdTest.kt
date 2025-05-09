package com.google.ads.mediation.pangle.renderer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGImageItem
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGMediaView
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionListener
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest
import com.google.ads.mediation.pangle.PangleConstants
import com.google.ads.mediation.pangle.PangleConstants.PANGLE_SDK_ERROR_DOMAIN
import com.google.ads.mediation.pangle.PangleFactory
import com.google.ads.mediation.pangle.PangleInitializer
import com.google.ads.mediation.pangle.PangleRequestHelper.ADMOB_WATERMARK_KEY
import com.google.ads.mediation.pangle.PangleSdkWrapper
import com.google.ads.mediation.pangle.renderer.PangleNativeAd.ASSET_ID_ADCHOICES_TEXT_VIEW
import com.google.ads.mediation.pangle.renderer.PangleNativeAd.PANGLE_SDK_IMAGE_SCALE
import com.google.ads.mediation.pangle.utils.AdErrorMatcher
import com.google.ads.mediation.pangle.utils.TestConstants.APP_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.BID_RESPONSE
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_CODE
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_MESSAGE
import com.google.ads.mediation.pangle.utils.TestConstants.PLACEMENT_ID_VALUE
import com.google.ads.mediation.pangle.utils.TestConstants.WATERMARK
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationFailure
import com.google.ads.mediation.pangle.utils.mockPangleSdkInitializationSuccess
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_CALL_TO_ACTION
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector

/** Unit tests for [PangleNativeAd]. */
@RunWith(RobolectricTestParameterInjector::class)
class PangleNativeAdTest {
  /** Subject under test */
  private lateinit var nativeAd: PangleNativeAd
  private lateinit var mediationNativeAdConfig: MediationNativeAdConfiguration
  private var serverParameters: Bundle = Bundle()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val nativeAdCallback: MediationNativeAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn nativeAdCallback
    }
  private val pangleInitializer: PangleInitializer = mock()
  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val pagNativeRequest: PAGNativeRequest = mock()
  private val pangleFactory: PangleFactory = mock {
    on { createPagNativeRequest() } doReturn pagNativeRequest
  }
  private val pagNativeAdIcon: PAGImageItem = mock {
    on { imageUrl } doReturn PANGLE_NATIVE_AD_ICON_URL
  }
  private val mediaView = mock<PAGMediaView>()
  private val adLogoView = View(context)
  private val pagNativeAdData: PAGNativeAdData = mock {
    on { title } doReturn PANGLE_NATIVE_AD_TITLE
    on { description } doReturn PANGLE_NATIVE_AD_DESCRIPTION
    on { buttonText } doReturn PANGLE_NATIVE_AD_BUTTON_TEXT
    on { icon } doReturn pagNativeAdIcon
    on { mediaView } doReturn mediaView
    on { adLogoView } doReturn adLogoView
  }
  private val pagNativeAd: PAGNativeAd = mock { on { nativeAdData } doReturn pagNativeAdData }
  private val assetViewsCaptor = argumentCaptor<ArrayList<View>>()
  private val creativeViewsCaptor = argumentCaptor<ArrayList<View>>()
  /** A fake native ad container view for testing purpose. */
  private val containerView =
    object : ViewGroup(context) {
      override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Not implemented.
      }
    }
  private val callToActionAssetView = View(context)
  private val clickableAssetViews =
    mapOf(
      ASSET_ADCHOICES_CONTAINER_VIEW to View(context),
      ASSET_ID_ADCHOICES_TEXT_VIEW to View(context),
      ASSET_CALL_TO_ACTION to callToActionAssetView,
    )
  private val pagAdInteractionListenerCaptor = argumentCaptor<PAGNativeAdInteractionListener>()
  private val adChoicesView = View(context)
  private val extraInfoCaptor = argumentCaptor<Map<String, Any>>()

  @Before
  fun setUp() {
    // This is the expected minimum serverParameters for nativeAd to load
    serverParameters.apply {
      serverParameters =
        bundleOf(
          PangleConstants.PLACEMENT_ID to PLACEMENT_ID_VALUE,
          PangleConstants.APP_ID to APP_ID_VALUE,
        )
    }
  }

  @Test
  fun render_withoutPlacementId_callsOnFailureOnCallbackWithProperErrorCode() {
    // Given a mediation native ad configuration
    serverParameters.remove(
      PangleConstants.PLACEMENT_ID
    ) // ... without serverParameters send in the Bundle
    initializeNativeAd()

    nativeAd.render()

    // The onFailure method of the mediationAdLoadCallback is called with the
    // ERROR_INVALID_SERVER_PARAMETERS code.
    val adError: AdError =
      PangleConstants.createAdapterError(
        PangleConstants.ERROR_INVALID_SERVER_PARAMETERS,
        "Failed to load native ad from Pangle. Missing or invalid Placement ID.",
      )
    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(adError)))
  }

  @Test
  fun render_withProperConfigurations_dosNotCallTheCallbackOnFailure() {
    // Given the nativeAd with its proper configuration: placementId and an appId in the
    // serverParameters and a bidResponse.
    initializeNativeAd()

    nativeAd.render()

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
    initializeNativeAd(bidResponse = BID_RESPONSE)

    nativeAd.render()

    verify(pagNativeRequest).setAdString(BID_RESPONSE)
    verify(pagNativeRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo.containsKey(ADMOB_WATERMARK_KEY)).isTrue()
    assertThat(extraInfo[ADMOB_WATERMARK_KEY]).isEqualTo(WATERMARK)
    verify(pangleSdkWrapper).loadNativeAd(eq(PLACEMENT_ID_VALUE), eq(pagNativeRequest), any())
  }

  /**
   * render() test for the case where bid response and watermark are empty. This is how render()
   * will be called for waterfall.
   */
  @Test
  fun render_ifBidResponseIsEmpty_setsEmptyBidResponseAndLoadsPangleAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    initializeNativeAd(bidResponse = "", watermark = "")

    nativeAd.render()

    verify(pagNativeRequest).setAdString("")
    // Verify that setExtraInfo is not called when watermark is empty.
    verify(pagNativeRequest, never()).setExtraInfo(any())
    verify(pangleSdkWrapper).loadNativeAd(eq(PLACEMENT_ID_VALUE), eq(pagNativeRequest), any())
  }

  @Test
  fun render_ifPangleAdLoadSucceeds_mapsNativeAdAndCallsLoadSuccessCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleNativeAdLoadToSucceed()
    initializeNativeAd()

    nativeAd.render()

    with(nativeAd) {
      assertThat(headline).isEqualTo(PANGLE_NATIVE_AD_TITLE)
      assertThat(body).isEqualTo(PANGLE_NATIVE_AD_DESCRIPTION)
      assertThat(callToAction).isEqualTo(PANGLE_NATIVE_AD_BUTTON_TEXT)
      assertThat(icon.drawable).isNull()
      assertThat(icon.uri).isEqualTo(Uri.parse(PANGLE_NATIVE_AD_ICON_URL))
      assertThat(icon.scale).isEqualTo(PANGLE_SDK_IMAGE_SCALE)
      assertThat(overrideClickHandling).isTrue()
      assertThat(mediaView).isEqualTo(mediaView)
      assertThat(adChoicesContent).isEqualTo(adLogoView)
    }
    verify(mediationAdLoadCallback).onSuccess(nativeAd)
  }

  @Test
  fun render_ifPangleAdLoadFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    // Stub Pangle native ad load to fail.
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGNativeAdLoadListener).onError(
          FAILURE_CODE_PANGLE_NATIVE_LOAD,
          FAILURE_MESSAGE_PANGLE_NATIVE_LOAD,
        )
      }
      .whenever(pangleSdkWrapper)
      .loadNativeAd(any(), any(), any())
    initializeNativeAd()

    nativeAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(FAILURE_CODE_PANGLE_NATIVE_LOAD)
    assertThat(adError.message).isEqualTo(FAILURE_MESSAGE_PANGLE_NATIVE_LOAD)
    assertThat(adError.domain).isEqualTo(PANGLE_SDK_ERROR_DOMAIN)
  }

  @Test
  fun render_ifPangleInitializationFails_callsLoadFailureCallback() {
    mockPangleSdkInitializationFailure(pangleInitializer)
    initializeNativeAd()

    nativeAd.render()

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(PANGLE_INIT_FAILURE_CODE)
    assertThat(adError.message).isEqualTo(PANGLE_INIT_FAILURE_MESSAGE)
    assertThat(adError.domain).isEqualTo(PANGLE_SDK_ERROR_DOMAIN)
  }

  @Test
  fun trackViews_registersViewForInteraction() {
    loadPangleNativeAd()

    nativeAd.trackViews(containerView, clickableAssetViews, emptyMap())

    verify(pagNativeAd)
      .registerViewForInteraction(
        eq(containerView),
        assetViewsCaptor.capture(),
        creativeViewsCaptor.capture(),
        eq(null),
        any<PAGNativeAdInteractionListener>(),
      )
    val assetViews = assetViewsCaptor.firstValue
    assertThat(assetViews.size).isEqualTo(1)
    assertThat(assetViews.get(0)).isEqualTo(callToActionAssetView)
    val creativeViews = creativeViewsCaptor.firstValue
    assertThat(creativeViews.size).isEqualTo(1)
    assertThat(creativeViews.get(0)).isEqualTo(callToActionAssetView)
  }

  @Test
  fun trackViews_ifAdIsClicked_reportsAdClicked() {
    loadPangleNativeAd()

    nativeAd.trackViews(containerView, clickableAssetViews, emptyMap())
    // Capture PAGNativeAdInteractionListener.
    verify(pagNativeAd)
      .registerViewForInteraction(
        any(),
        any(),
        any(),
        eq(null),
        pagAdInteractionListenerCaptor.capture(),
      )
    // Mock that the ad is clicked.
    pagAdInteractionListenerCaptor.firstValue.onAdClicked()

    verify(nativeAdCallback).reportAdClicked()
  }

  @Test
  fun trackViews_ifAdIsShowed_reportsAdImpression() {
    loadPangleNativeAd()

    nativeAd.trackViews(containerView, clickableAssetViews, emptyMap())
    // Capture PAGNativeAdInteractionListener.
    verify(pagNativeAd)
      .registerViewForInteraction(
        any(),
        any(),
        any(),
        eq(null),
        pagAdInteractionListenerCaptor.capture(),
      )
    // Mock that the ad is showed.
    pagAdInteractionListenerCaptor.firstValue.onAdShowed()

    verify(nativeAdCallback).reportAdImpression()
  }

  @Test
  fun trackViews_ifAdIsDismissed_reportsNothing() {
    loadPangleNativeAd()

    nativeAd.trackViews(containerView, clickableAssetViews, emptyMap())
    // Capture PAGNativeAdInteractionListener.
    verify(pagNativeAd)
      .registerViewForInteraction(
        any(),
        any(),
        any(),
        eq(null),
        pagAdInteractionListenerCaptor.capture(),
      )
    // Mock that the ad is dismissed.
    pagAdInteractionListenerCaptor.firstValue.onAdDismissed()

    // Google Mobile Ads SDK doesn't have a matching event for onAdDismissed(). Therefore, nothing
    // should be reported through nativeAdCallback.
    verifyNoInteractions(nativeAdCallback)
  }

  @Test
  fun trackViews_attachesClickListenerToAdChoicesView() {
    loadPangleNativeAd()
    nativeAd.adChoicesContent = adChoicesView
    // Before calling trackViews(), ascertain that no click listeners are attached to adChoicesView.
    assertThat(adChoicesView.hasOnClickListeners()).isFalse()

    nativeAd.trackViews(containerView, clickableAssetViews, emptyMap())

    assertThat(adChoicesView.hasOnClickListeners()).isTrue()
  }

  @Test
  fun trackViews_ifAdChoicesViewIsClicked_showsPanglesPrivacyPolicyPage() {
    loadPangleNativeAd()
    nativeAd.adChoicesContent = adChoicesView

    nativeAd.trackViews(containerView, clickableAssetViews, emptyMap())
    adChoicesView.callOnClick()

    // Verify that Pangle's Privacy Policy page is shown.
    verify(pagNativeAd).showPrivacyActivity()
  }

  private fun initializeNativeAd(
    @TagForChildDirectedTreatment
    tagForChildDirectedTreatment: Int = TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
    bidResponse: String = BID_RESPONSE,
    watermark: String = WATERMARK,
  ) {
    // Constructor of the MediationNativeAdConfiguration called by the GMA SDK
    mediationNativeAdConfig =
      MediationNativeAdConfiguration(
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
        /*nativeAdOptions=*/ null,
      )

    nativeAd =
      PangleNativeAd(
        mediationNativeAdConfig,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        pangleFactory
      )
  }

  /** Mock a Pangle native ad load. */
  private fun loadPangleNativeAd() {
    mockPangleSdkInitializationSuccess(pangleInitializer)
    stubPangleNativeAdLoadToSucceed()
    initializeNativeAd()
    nativeAd.render()
  }

  /** Stub pangleSdkWrapper.loadNativeAd() to succeed. */
  private fun stubPangleNativeAdLoadToSucceed() {
    doAnswer { invocation ->
        val args: Array<Any> = invocation.getArguments()
        (args[2] as PAGNativeAdLoadListener).onAdLoaded(pagNativeAd)
      }
      .whenever(pangleSdkWrapper)
      .loadNativeAd(any(), any(), any())
  }

  companion object {
    private const val FAILURE_CODE_PANGLE_NATIVE_LOAD = 6
    private const val FAILURE_MESSAGE_PANGLE_NATIVE_LOAD = "Pangle native ad load failed"
    private const val PANGLE_NATIVE_AD_ICON_URL = "native_ad_icon_url_placeholder"
    private const val PANGLE_NATIVE_AD_TITLE = "example native ad title"
    private const val PANGLE_NATIVE_AD_DESCRIPTION = "example native ad description"
    private const val PANGLE_NATIVE_AD_BUTTON_TEXT = "example native ad button text"
  }
}
