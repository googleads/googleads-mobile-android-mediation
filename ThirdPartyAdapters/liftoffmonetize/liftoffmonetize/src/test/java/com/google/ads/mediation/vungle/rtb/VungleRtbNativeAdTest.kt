package com.google.ads.mediation.vungle.rtb

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_APP_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID
import com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.formats.UnifiedNativeAdAssetNames.ASSET_ICON
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.NativeAd
import com.vungle.ads.VungleError
import com.vungle.ads.internal.protos.Sdk.SDKError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Tests for [VungleRtbNativeAd]. */
@RunWith(AndroidJUnit4::class)
class VungleRtbNativeAdTest {

  /** Unit under test. */
  private lateinit var adapterRtbNativeAd: VungleRtbNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val vungleInitializer = mock<VungleInitializer>()
  private val nativeAdCallback = mock<MediationNativeAdCallback>()
  private val nativeAdLoadCallback =
    mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>> {
      on { onSuccess(any()) } doReturn nativeAdCallback
    }
  private val vungleNativeAd =
    mock<NativeAd> {
      on { getAdTitle() } doReturn AD_TITLE
      on { getAdBodyText() } doReturn AD_BODY_TEXT
      on { getAdCallToActionText() } doReturn AD_CALL_TO_ACTION_TEXT
      on { getAdStarRating() } doReturn AD_STAR_RATING
      on { getAdSponsoredText() } doReturn AD_SPONSORED_TEXT
      on { getAppIcon() } doReturn APP_ICON_URL
      on { canPlayAd() } doReturn true
    }
  private val vungleFactory =
    mock<VungleFactory> { on { createNativeAd(any(), any()) } doReturn vungleNativeAd }
  private val containerView = FrameLayout(context)
  private val mediationNativeAdConfiguration =
    createMediationNativeAdConfiguration(
      context = context,
      serverParameters = bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      bidResponse = TEST_BID_RESPONSE,
    )

  @Before
  fun setUp() {
    adapterRtbNativeAd = VungleRtbNativeAd(nativeAdLoadCallback, vungleFactory)

    doAnswer { invocation ->
        val args: Array<Any> = invocation.arguments
        (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
      }
      .whenever(vungleInitializer)
      .initialize(any(), any(), any())
  }

  @Test
  fun onAdLoaded_mapsLiftoffNativeAdAssetsToGmaAssetsAndCallsLoadSuccess() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbNativeAd.render(mediationNativeAdConfiguration)
    }

    adapterRtbNativeAd.onAdLoaded(vungleNativeAd)

    assertThat(adapterRtbNativeAd.headline).isEqualTo(AD_TITLE)
    assertThat(adapterRtbNativeAd.body).isEqualTo(AD_BODY_TEXT)
    assertThat(adapterRtbNativeAd.callToAction).isEqualTo(AD_CALL_TO_ACTION_TEXT)
    assertThat(adapterRtbNativeAd.starRating).isEqualTo(AD_STAR_RATING)
    assertThat(adapterRtbNativeAd.advertiser).isEqualTo(AD_SPONSORED_TEXT)
    val adIcon = adapterRtbNativeAd.icon
    assertThat(adIcon.drawable).isNull()
    assertThat(adIcon.uri.toString()).isEqualTo(APP_ICON_URL)
    assertThat(adIcon.scale).isEqualTo(1)
    assertThat(adapterRtbNativeAd.overrideImpressionRecording).isTrue()
    assertThat(adapterRtbNativeAd.overrideClickHandling).isTrue()
    verify(nativeAdLoadCallback).onSuccess(adapterRtbNativeAd)
  }

  @Test
  fun onAdLoaded_ifRuntimeGmaSdkDoesNotListenToAdapterReportedImpressions_doesNotSetOverrideImpressionRecordingAsTrue() {
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters =
          bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      )
    adapterRtbNativeAd = VungleRtbNativeAd(nativeAdLoadCallback, vungleFactory)
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbNativeAd.render(adConfiguration)
    }

    Mockito.mockStatic(MobileAds::class.java).use {
      // Return a version of GMA SDK that doesn't listen to adapter-reported impressions.
      whenever(MobileAds.getVersion()) doReturn VersionInfo(24, 3, 0)
      adapterRtbNativeAd.onAdLoaded(vungleNativeAd)
    }

    assertThat(adapterRtbNativeAd.overrideImpressionRecording).isFalse()
  }

  @Test
  fun onAdLoaded_ifRuntimeGmaSdkDoesNotListenToAdapterReportedImpressions_doesNotSetOverrideImpressionRecordingAsTrueForLowerVersions() {
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters =
          bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      )
    adapterRtbNativeAd = VungleRtbNativeAd(nativeAdLoadCallback, vungleFactory)
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbNativeAd.render(adConfiguration)
    }

    Mockito.mockStatic(MobileAds::class.java).use {
      // Return a version of GMA SDK that doesn't listen to adapter-reported impressions.
      whenever(MobileAds.getVersion()) doReturn VersionInfo(0, 17, 0)
      adapterRtbNativeAd.onAdLoaded(vungleNativeAd)
    }

    assertThat(adapterRtbNativeAd.overrideImpressionRecording).isFalse()
  }

  @Test
  fun onAdLoaded_ifRuntimeGmaSdkListensToAdapterReportedImpressions_setsOverrideImpressionRecordingAsTrueForHigherVersions() {
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters =
          bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      )
    adapterRtbNativeAd = VungleRtbNativeAd(nativeAdLoadCallback, vungleFactory)
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbNativeAd.render(adConfiguration)
    }

    Mockito.mockStatic(MobileAds::class.java).use {
      // Return a version of GMA SDK that listens to adapter-reported impressions.
      whenever(MobileAds.getVersion()) doReturn VersionInfo(24, 4, 0)
      adapterRtbNativeAd.onAdLoaded(vungleNativeAd)
    }

    assertThat(adapterRtbNativeAd.overrideImpressionRecording).isTrue()
  }

  @Test
  fun onAdLoaded_ifRuntimeGmaSdkListensToAdapterReportedImpressions_setsOverrideImpressionRecordingAsTrueForLowerVersions() {
    val adConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters =
          bundleOf(KEY_APP_ID to TEST_APP_ID, KEY_PLACEMENT_ID to TEST_PLACEMENT_ID),
      )
    adapterRtbNativeAd = VungleRtbNativeAd(nativeAdLoadCallback, vungleFactory)
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbNativeAd.render(adConfiguration)
    }

    Mockito.mockStatic(MobileAds::class.java).use {
      // Return a version of GMA SDK that listens to adapter-reported impressions.
      whenever(MobileAds.getVersion()) doReturn VersionInfo(6, 4, 0)
      adapterRtbNativeAd.onAdLoaded(vungleNativeAd)
    }

    assertThat(adapterRtbNativeAd.overrideImpressionRecording).isTrue()
  }

  @Test
  fun onAdFailedToLoad_callsLoadFailure() {
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.API_REQUEST_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK native ad load failed."
      }

    adapterRtbNativeAd.onAdFailedToLoad(vungleNativeAd, liftoffError)

    val expectedError =
      AdError(
        liftoffError.code,
        liftoffError.errorMessage,
        VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN,
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedError)))
  }

  private fun renderAdAndMockLoadSuccess() {
    Mockito.mockStatic(VungleInitializer::class.java).use {
      whenever(VungleInitializer.getInstance()) doReturn vungleInitializer
      adapterRtbNativeAd.render(mediationNativeAdConfiguration)
    }
    adapterRtbNativeAd.onAdLoaded(vungleNativeAd)
  }

  @Test
  fun onAdFailedToPlay_noInteractions() {
    renderAdAndMockLoadSuccess()
    val liftoffError =
      mock<VungleError> {
        on { code } doReturn SDKError.Reason.AD_NOT_LOADED_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK rewarded ad play failed."
      }

    adapterRtbNativeAd.onAdFailedToPlay(vungleNativeAd, liftoffError)

    verifyNoInteractions(nativeAdCallback)
  }

  @Test
  fun onAdClicked_reportsAdClickedAndAdOpened() {
    renderAdAndMockLoadSuccess()

    adapterRtbNativeAd.onAdClicked(vungleNativeAd)

    verify(nativeAdCallback).reportAdClicked()
    verify(nativeAdCallback).onAdOpened()
    verifyNoMoreInteractions(nativeAdCallback)
  }

  @Test
  fun onAdLeftApplication_callsOnAdLeftApplication() {
    renderAdAndMockLoadSuccess()

    adapterRtbNativeAd.onAdLeftApplication(vungleNativeAd)

    verify(nativeAdCallback).onAdLeftApplication()
    verifyNoMoreInteractions(nativeAdCallback)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    renderAdAndMockLoadSuccess()

    adapterRtbNativeAd.onAdImpression(vungleNativeAd)

    verify(nativeAdCallback).reportAdImpression()
    verifyNoMoreInteractions(nativeAdCallback)
  }

  @Test
  fun onAdStart_noInteractions() {
    renderAdAndMockLoadSuccess()

    adapterRtbNativeAd.onAdStart(vungleNativeAd)

    verifyNoInteractions(nativeAdCallback)
  }

  @Test
  fun onAdEnd_noInteractions() {
    renderAdAndMockLoadSuccess()

    adapterRtbNativeAd.onAdEnd(vungleNativeAd)

    verifyNoInteractions(nativeAdCallback)
  }

  @Test
  fun trackViews_registersViewForInteraction() {
    renderAdAndMockLoadSuccess()
    val iconView = ImageView(context)
    val clickableAssets = mapOf(ASSET_ICON to iconView)
    val overlayView = FrameLayout(context)
    containerView.addView(overlayView)

    adapterRtbNativeAd.trackViews(containerView, clickableAssets, emptyMap())

    verify(vungleNativeAd)
      .registerViewForInteraction(eq(overlayView), any(), eq(iconView), eq(listOf(iconView)))
  }

  @Test
  fun trackViews_ifIconViewIsNotImageView_registersViewForInteractionWithNullIconView() {
    renderAdAndMockLoadSuccess()
    val iconView = View(context)
    val clickableAssets = mapOf(ASSET_ICON to iconView)
    val overlayView = FrameLayout(context)
    containerView.addView(overlayView)

    adapterRtbNativeAd.trackViews(containerView, clickableAssets, emptyMap())

    verify(vungleNativeAd)
      .registerViewForInteraction(
        eq(overlayView),
        any(),
        /* iconView = */ eq(null),
        eq(listOf(iconView)),
      )
  }

  @Test
  fun trackViews_ifContainerViewIsNotViewGroup_doesNotRegisterView() {
    renderAdAndMockLoadSuccess()
    val iconView = ImageView(context)
    val clickableAssets = mapOf(ASSET_ICON to iconView)
    val containerView = View(context)

    adapterRtbNativeAd.trackViews(containerView, clickableAssets, emptyMap())

    verify(vungleNativeAd, never()).registerViewForInteraction(any(), any(), any(), any())
  }

  @Test
  fun trackViews_ifVungleNativeAdIsNotLoaded_doesNotRegisterView() {
    val iconView = ImageView(context)
    val clickableAssets = mapOf(ASSET_ICON to iconView)
    val overlayView = FrameLayout(context)
    containerView.addView(overlayView)

    adapterRtbNativeAd.trackViews(containerView, clickableAssets, emptyMap())

    verify(vungleNativeAd, never()).registerViewForInteraction(any(), any(), any(), any())
  }

  @Test
  fun trackViews_ifOverlayViewIsNotFrameLayout_doesNotRegisterView() {
    renderAdAndMockLoadSuccess()
    val iconView = ImageView(context)
    val clickableAssets = mapOf(ASSET_ICON to iconView)
    val overlayView = RelativeLayout(context)
    containerView.addView(overlayView)

    adapterRtbNativeAd.trackViews(containerView, clickableAssets, emptyMap())

    verify(vungleNativeAd, never()).registerViewForInteraction(any(), any(), any(), any())
  }

  @Test
  fun untrackView_unregistersView() {
    renderAdAndMockLoadSuccess()

    adapterRtbNativeAd.untrackView(containerView)

    verify(vungleNativeAd).unregisterView()
  }

  @Test
  fun untrackView_ifVungleNativeAdIsNotLoaded_doesNotUnregisterView() {
    adapterRtbNativeAd.untrackView(containerView)

    verifyNoInteractions(vungleNativeAd)
  }

  private companion object {
    const val AD_TITLE = "Ad title"
    const val AD_BODY_TEXT = "Ad body text"
    const val AD_CALL_TO_ACTION_TEXT = "Ad call to action text"
    const val AD_STAR_RATING = 4.5
    const val AD_SPONSORED_TEXT = "Ad sponsored text"
    const val APP_ICON_URL = "file://liftoffmonetize/app/icon"
  }
}
