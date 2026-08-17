package com.google.ads.mediation.facebook.rtb

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.os.BundleSubject.assertThat
import com.facebook.ads.MediaView
import com.facebook.ads.MediaViewListener
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdBase
import com.facebook.ads.NativeAdBase.Image
import com.facebook.ads.NativeAdListener
import com.facebook.ads.NativeAdOptionsViewPosition
import com.facebook.ads.NativeBannerAd
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationNativeAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.facebook.FacebookMediationAdapter
import com.google.ads.mediation.facebook.FacebookMediationAdapter.KEY_ID
import com.google.ads.mediation.facebook.FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET
import com.google.ads.mediation.facebook.MetaFactory
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.formats.UnifiedNativeAdAssetNames.ASSET_ICON
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_LEFT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_RIGHT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Unit tests for public API calls implemented by [FacebookRtbNativeAd]. */
@RunWith(AndroidJUnit4::class)
class FacebookRtbNativeAdTest {

  private lateinit var facebookRtbNativeAd: FacebookRtbNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val serverParameters =
    bundleOf(
      FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID
    )
  private val mediationNativeAdConfiguration =
    createMediationNativeAdConfiguration(
      context = context,
      serverParameters = serverParameters,
      taggedForChildDirectedTreatment = 1,
      watermark = TEST_WATERMARK,
      bidResponse = AdapterTestKitConstants.TEST_BID_RESPONSE,
    )
  private val nativeAdCallback = FakeMediationNativeAdCallback()
  private val nativeAdLoadCallback =
    FakeMediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>(nativeAdCallback)
  private val metaNativeAdLoadConfig: NativeAdBase.NativeLoadAdConfig = mock()
  private val metaNativeAdLoadConfigBuilder: NativeAdBase.NativeAdLoadConfigBuilder = mock {
    on { withBid(any()) } doReturn this.mock
    on { withAdListener(any()) } doReturn this.mock
    on { withMediaCacheFlag(any()) } doReturn this.mock
    on {
      withPreloadedIconView(
        NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
        NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
      )
    } doReturn this.mock
    on { build() } doReturn metaNativeAdLoadConfig
  }
  private val metaAdIcon = mock<Image> { on { url } doReturn META_AD_ICON_URI }
  private val metaAdCoverImage = mock<Image> { on { url } doReturn META_AD_COVER_IMAGE_URI }
  private val metaNativeAd: NativeAd = mock {
    on { buildLoadAdConfig() } doReturn metaNativeAdLoadConfigBuilder
    on { adHeadline } doReturn META_AD_HEADLINE
    on { adBodyText } doReturn META_AD_BODY_TEXT
    on { adCallToAction } doReturn META_AD_CALL_TO_ACTION
    on { adIcon } doReturn metaAdIcon
    on { adCoverImage } doReturn metaAdCoverImage
    on { advertiserName } doReturn META_ADVERTISER_NAME
    on { id } doReturn META_AD_ID
    on { adSocialContext } doReturn META_AD_SOCIAL_CONTEXT
  }
  private val metaNativeBannerAd: NativeBannerAd = mock {
    on { buildLoadAdConfig() } doReturn metaNativeAdLoadConfigBuilder
    on { adHeadline } doReturn META_AD_HEADLINE
    on { adBodyText } doReturn META_AD_BODY_TEXT
    on { adCallToAction } doReturn META_AD_CALL_TO_ACTION
    on { adIcon } doReturn metaAdIcon
    on { advertiserName } doReturn META_ADVERTISER_NAME
    on { id } doReturn META_AD_ID
    on { adSocialContext } doReturn META_AD_SOCIAL_CONTEXT
  }
  private val nativeListenerCaptor = argumentCaptor<NativeAdListener>()
  private val metaMediaView = mock<MediaView>()
  private val metaFactory =
    mock<MetaFactory> {
      on { createMediaView(any()) } doReturn metaMediaView
      on { createNativeAdFromBidPayload(any(), any(), any()) } doReturn metaNativeAd
    }
  private val iconViewDrawable = mock<Drawable>()
  private val gmaContainerView = mock<View>()

  @Before
  fun setUp() {
    facebookRtbNativeAd = FacebookRtbNativeAd(nativeAdLoadCallback, metaFactory)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withWrongAd_invokesLoadFailure() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val aWrongNativeAd: NativeAdBase = mock()

    nativeListenerCaptor.firstValue.onAdLoaded(aWrongNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_WRONG_NATIVE_TYPE,
        "Ad Loaded is not a Native Ad.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdHeadline_invokesLoadFailure() {
    whenever(metaNativeAd.adHeadline) doReturn null
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdBodyText_invokesLoadFailure() {
    whenever(metaNativeAd.adBodyText) doReturn null
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdIcon_invokesLoadFailure() {
    whenever(metaNativeAd.adIcon) doReturn null
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdCallToAction_invokesLoadFailure() {
    whenever(metaNativeAd.adCallToAction) doReturn null
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdCoverImage_invokesLoadFailure() {
    whenever(metaNativeAd.adCoverImage) doReturn null
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_whenMediaViewIsNull_invokesLoadFailure() {
    whenever(metaFactory.createMediaView(any())) doReturn null
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnError_invokesLoadFailure() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val metaAdLoadError = com.facebook.ads.AdError(101, "Load error from Meta")

    nativeListenerCaptor.firstValue.onError(metaNativeAd, metaAdLoadError)

    val expectedAdError =
      AdError(
        metaAdLoadError.errorCode,
        metaAdLoadError.errorMessage,
        FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN,
      )
    assertThat(nativeAdLoadCallback).hasFailedWith(expectedAdError)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_setsNativeAdAssetsAndInvokesLoadSuccess() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    assertThat(facebookRtbNativeAd.headline).isEqualTo(META_AD_HEADLINE)
    val images = facebookRtbNativeAd.images
    val nativeAdImage = images.single()
    assertThat(nativeAdImage.uri.toString()).isEqualTo(META_AD_COVER_IMAGE_URI)
    assertThat(nativeAdImage.scale).isEqualTo(1)
    assertThat(facebookRtbNativeAd.body).isEqualTo(META_AD_BODY_TEXT)
    val nativeAdIcon = facebookRtbNativeAd.icon
    assertThat(nativeAdIcon.uri.toString()).isEqualTo(META_AD_ICON_URI)
    assertThat(nativeAdIcon.drawable).isNull()
    assertThat(nativeAdIcon.scale).isEqualTo(1)
    assertThat(facebookRtbNativeAd.callToAction).isEqualTo(META_AD_CALL_TO_ACTION)
    assertThat(facebookRtbNativeAd.advertiser).isEqualTo(META_ADVERTISER_NAME)
    verify(metaMediaView).setListener(any())
    assertThat(facebookRtbNativeAd.hasVideoContent()).isTrue()
    val extras = facebookRtbNativeAd.extras
    assertThat(extras).hasSize(2)
    assertThat(extras).containsKey(KEY_ID)
    assertThat(extras).string(KEY_ID).isEqualTo(META_AD_ID)
    assertThat(extras).containsKey(KEY_SOCIAL_CONTEXT_ASSET)
    assertThat(extras).string(KEY_SOCIAL_CONTEXT_ASSET).isEqualTo(META_AD_SOCIAL_CONTEXT)
    assertThat(nativeAdLoadCallback).hasSucceededWith(facebookRtbNativeAd)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withPreloadedDrawable_setsNativeAdIconWithDrawable() {
    whenever(metaNativeAd.preloadedIconViewDrawable) doReturn iconViewDrawable
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val nativeAdIcon = facebookRtbNativeAd.icon
    assertThat(nativeAdIcon.uri).isNull()
    assertThat(nativeAdIcon.drawable).isEqualTo(iconViewDrawable)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_forNativeBannerAdWithNoCoverImage_setsAllOtherAssetsAndInvokesLoadSuccess() {
    whenever(metaNativeBannerAd.adCoverImage) doReturn null
    whenever(metaFactory.createNativeAdFromBidPayload(any(), any(), any())) doReturn
      metaNativeBannerAd
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeBannerAd)

    assertThat(facebookRtbNativeAd.headline).isEqualTo(META_AD_HEADLINE)
    assertThat(facebookRtbNativeAd.body).isEqualTo(META_AD_BODY_TEXT)
    val nativeAdIcon = facebookRtbNativeAd.icon
    assertThat(nativeAdIcon.uri.toString()).isEqualTo(META_AD_ICON_URI)
    assertThat(nativeAdIcon.drawable).isNull()
    assertThat(nativeAdIcon.scale).isEqualTo(1)
    assertThat(facebookRtbNativeAd.callToAction).isEqualTo(META_AD_CALL_TO_ACTION)
    assertThat(facebookRtbNativeAd.advertiser).isEqualTo(META_ADVERTISER_NAME)
    assertThat(facebookRtbNativeAd.hasVideoContent()).isTrue()
    val extras = facebookRtbNativeAd.extras
    assertThat(extras).hasSize(2)
    assertThat(extras).containsKey(KEY_ID)
    assertThat(extras).string(KEY_ID).isEqualTo(META_AD_ID)
    assertThat(extras).containsKey(KEY_SOCIAL_CONTEXT_ASSET)
    assertThat(extras).string(KEY_SOCIAL_CONTEXT_ASSET).isEqualTo(META_AD_SOCIAL_CONTEXT)
    assertThat(nativeAdLoadCallback).hasSucceededWith(facebookRtbNativeAd)
  }

  @Test
  fun nativeAdListenerOnMediaDownloaded_doesntCrash() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val nativeAdListener = nativeListenerCaptor.firstValue
    nativeAdListener.onAdLoaded(metaNativeAd)

    nativeAdListener.onMediaDownloaded(metaNativeAd)

    // nativeAdListener.onMediaDownloaded() is a no-op. So, this test is just a sanity-check that
    // there is no crash when it is called.
  }

  @Test
  fun nativeAdListenerOnLoggingImpression_doesntCrash() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val nativeAdListener = nativeListenerCaptor.firstValue
    nativeAdListener.onAdLoaded(metaNativeAd)

    nativeAdListener.onLoggingImpression(metaNativeAd)

    assertThat(nativeAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun nativeAdListenerOnAdClicked_reportsAdClickedAndAdOpenedAndAdLeftApplication() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val nativeAdListener = nativeListenerCaptor.firstValue
    // Load the ad as part of the test setup.
    nativeAdListener.onAdLoaded(metaNativeAd)

    nativeAdListener.onAdClicked(metaNativeAd)

    assertThat(nativeAdCallback.isClicked).isTrue()
    assertThat(nativeAdCallback.isOpened).isTrue()
    assertThat(nativeAdCallback.isLeftApplication).isTrue()
  }

  @Test
  fun mediaViewListenerOnComplete_invokesOnVideoComplete() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val nativeAdListener = nativeListenerCaptor.firstValue
    // Load the ad as part of the test setup.
    nativeAdListener.onAdLoaded(metaNativeAd)
    val mediaViewListenerCaptor = argumentCaptor<MediaViewListener>()
    verify(metaMediaView).setListener(mediaViewListenerCaptor.capture())

    mediaViewListenerCaptor.firstValue.onComplete(metaMediaView)

    assertThat(nativeAdCallback.isVideoCompleted).isTrue()
  }

  @Test
  fun mediaViewListenerNoOpCallbacks_dontCrash() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val nativeAdListener = nativeListenerCaptor.firstValue
    // Load the ad as part of the test setup.
    nativeAdListener.onAdLoaded(metaNativeAd)
    val mediaViewListener =
      argumentCaptor<MediaViewListener>().run {
        verify(metaMediaView).setListener(capture())
        firstValue
      }

    mediaViewListener.onPlay(metaMediaView)
    mediaViewListener.onVolumeChange(metaMediaView, 0.5f)
    mediaViewListener.onPause(metaMediaView)
    mediaViewListener.onEnterFullscreen(metaMediaView)
    mediaViewListener.onExitFullscreen(metaMediaView)
    mediaViewListener.onFullscreenBackground(metaMediaView)
    mediaViewListener.onFullscreenForeground(metaMediaView)

    // All the above calls are no-ops. So, this test is just a sanity-check that there is no crash
    // when any of them is called.
  }

  @Test
  fun trackViews_ifIconIsImageView_registersViewWithIcon() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    val iconView = mock<ImageView>()
    val clickableAssets = mapOf(ASSET_ICON to iconView)

    facebookRtbNativeAd.trackViews(gmaContainerView, clickableAssets, emptyMap())

    verify(metaNativeAd)
      .registerViewForInteraction(gmaContainerView, metaMediaView, iconView, listOf(iconView))
  }

  @Test
  fun trackViews_ifIconIsNotImageView_registersViewWithoutIcon() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    val iconView = mock<View>()
    val clickableAssets = mapOf(ASSET_ICON to iconView)

    facebookRtbNativeAd.trackViews(gmaContainerView, clickableAssets, emptyMap())

    verify(metaNativeAd)
      .registerViewForInteraction(gmaContainerView, metaMediaView, listOf(iconView))
  }

  @Test
  fun trackViews_ifNativeAdIsNativeBannerAdAndIconIsImageView_registersView() {
    whenever(metaFactory.createNativeAdFromBidPayload(any(), any(), any())) doReturn
      metaNativeBannerAd
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    val iconView = mock<ImageView>()
    val clickableAssets = mapOf(ASSET_ICON to iconView)

    facebookRtbNativeAd.trackViews(gmaContainerView, clickableAssets, emptyMap())

    verify(metaNativeBannerAd)
      .registerViewForInteraction(gmaContainerView, iconView, listOf(iconView))
  }

  @Test
  fun trackViews_ifNativeAdIsNativeBannerAdAndIconIsNotImageView_doesNotRegisterView() {
    whenever(metaFactory.createNativeAdFromBidPayload(any(), any(), any())) doReturn
      metaNativeBannerAd
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    val iconView = mock<View>()
    val clickableAssets = mapOf(ASSET_ICON to iconView)

    facebookRtbNativeAd.trackViews(gmaContainerView, clickableAssets, emptyMap())

    verify(metaNativeBannerAd, times(0)).registerViewForInteraction(any(), any<ImageView>(), any())
    verify(metaNativeBannerAd, times(0)).registerViewForInteraction(any(), any<MediaView>(), any())
  }

  @Test
  fun trackViews_ifNativeAdIsNativeBannerAdAndIconIsNull_doesNotRegisterView() {
    whenever(metaFactory.createNativeAdFromBidPayload(any(), any(), any())) doReturn
      metaNativeBannerAd
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)

    facebookRtbNativeAd.trackViews(gmaContainerView, emptyMap(), emptyMap())

    verify(metaNativeBannerAd, times(0)).registerViewForInteraction(any(), any<ImageView>(), any())
    verify(metaNativeBannerAd, times(0)).registerViewForInteraction(any(), any<MediaView>(), any())
  }

  @Test
  fun trackViews_ifNativeAdTypeIsNotNativeAdNorNativeBannerAd_doesNotRegisterView() {
    val nativeAdBase =
      mock<NativeAdBase> { on { buildLoadAdConfig() } doReturn metaNativeAdLoadConfigBuilder }
    whenever(metaFactory.createNativeAdFromBidPayload(any(), any(), any())) doReturn nativeAdBase
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    val iconView = mock<View>()
    val clickableAssets = mapOf(ASSET_ICON to iconView)

    facebookRtbNativeAd.trackViews(gmaContainerView, clickableAssets, emptyMap())

    verify(nativeAdBase).setExtraHints(any())
    verify(nativeAdBase).buildLoadAdConfig()
    verify(nativeAdBase).loadAd(any())
    // Verify no interactions other than the above interactions.
    verifyNoMoreInteractions(nativeAdBase)
  }

  @Test
  fun trackViews_nativeBannerAdWithTopLeftAdChoicesPlacement_setsTopLeftPosition() {
    verifyNativeBannerAdAdChoicesPlacement(ADCHOICES_TOP_LEFT, NativeAdOptionsViewPosition.TOP_LEFT)
  }

  @Test
  fun trackViews_nativeBannerAdWithTopRightAdChoicesPlacement_setsTopRightPosition() {
    verifyNativeBannerAdAdChoicesPlacement(
      ADCHOICES_TOP_RIGHT,
      NativeAdOptionsViewPosition.TOP_RIGHT,
    )
  }

  @Test
  fun trackViews_nativeBannerAdWithBottomRightAdChoicesPlacement_setsBottomRightPosition() {
    verifyNativeBannerAdAdChoicesPlacement(
      ADCHOICES_BOTTOM_RIGHT,
      NativeAdOptionsViewPosition.BOTTOM_RIGHT,
    )
  }

  @Test
  fun trackViews_nativeBannerAdWithBottomLeftAdChoicesPlacement_setsBottomLeftPosition() {
    verifyNativeBannerAdAdChoicesPlacement(
      ADCHOICES_BOTTOM_LEFT,
      NativeAdOptionsViewPosition.BOTTOM_LEFT,
    )
  }

  @Test
  fun trackViews_nativeBannerAdWithNullNativeAdOptions_doesNotSetPreferredAdOptionsViewPosition() {
    verifyNativeBannerAdAdChoicesPlacement(adChoicesPlacement = null, expectedPosition = null)
  }

  @Test
  fun trackViews_nativeAdWithTopLeftAdChoicesPlacement_setsTopLeftPosition() {
    verifyNativeAdAdChoicesPlacement(ADCHOICES_TOP_LEFT, NativeAdOptionsViewPosition.TOP_LEFT)
  }

  @Test
  fun trackViews_nativeAdWithTopRightAdChoicesPlacement_setsTopRightPosition() {
    verifyNativeAdAdChoicesPlacement(ADCHOICES_TOP_RIGHT, NativeAdOptionsViewPosition.TOP_RIGHT)
  }

  @Test
  fun trackViews_nativeAdWithBottomRightAdChoicesPlacement_setsBottomRightPosition() {
    verifyNativeAdAdChoicesPlacement(
      ADCHOICES_BOTTOM_RIGHT,
      NativeAdOptionsViewPosition.BOTTOM_RIGHT,
    )
  }

  @Test
  fun trackViews_nativeAdWithBottomLeftAdChoicesPlacement_setsBottomLeftPosition() {
    verifyNativeAdAdChoicesPlacement(ADCHOICES_BOTTOM_LEFT, NativeAdOptionsViewPosition.BOTTOM_LEFT)
  }

  @Test
  fun trackViews_nativeAdWithNullNativeAdOptions_doesNotSetPreferredAdOptionsViewPosition() {
    verifyNativeAdAdChoicesPlacement(adChoicesPlacement = null, expectedPosition = null)
  }

  @Test
  fun unTrackView_unRegistersView() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)

    facebookRtbNativeAd.untrackView(gmaContainerView)

    verify(metaNativeAd).unregisterView()
    verify(metaNativeAd).destroy()
    verify(metaMediaView).destroy()
  }

  @Test
  fun untrackView_withoutRender_doesNotCrash() {
    facebookRtbNativeAd.untrackView(gmaContainerView)
    // No crash indicates success.
  }

  @Test
  fun untrackView_multipleTimes_destroysMetaAdAssetsOnce() {
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)

    facebookRtbNativeAd.untrackView(gmaContainerView)
    facebookRtbNativeAd.untrackView(gmaContainerView)

    verify(metaNativeAd).unregisterView()
    verify(metaNativeAd).destroy()
    verify(metaMediaView).destroy()
  }

  private fun verifyNativeBannerAdAdChoicesPlacement(
    adChoicesPlacement: Int?,
    expectedPosition: NativeAdOptionsViewPosition?,
  ) {
    val nativeAdOptions =
      if (adChoicesPlacement != null) {
        NativeAdOptions.Builder().setAdChoicesPlacement(adChoicesPlacement).build()
      } else {
        null
      }
    doReturn(nativeAdOptions).whenever(mediationNativeAdConfiguration).nativeAdOptions
    whenever(metaFactory.createNativeAdFromBidPayload(any(), any(), any())) doReturn
      metaNativeBannerAd
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    val iconView = mock<ImageView>()
    val clickableAssets = mapOf(ASSET_ICON to iconView)

    facebookRtbNativeAd.trackViews(gmaContainerView, clickableAssets, emptyMap())

    if (expectedPosition != null) {
      verify(metaNativeBannerAd).setPreferredAdOptionsViewPosition(expectedPosition)
    } else {
      verify(metaNativeBannerAd, times(0)).setPreferredAdOptionsViewPosition(any())
    }
  }

  private fun verifyNativeAdAdChoicesPlacement(
    adChoicesPlacement: Int?,
    expectedPosition: NativeAdOptionsViewPosition?,
  ) {
    val nativeAdOptions =
      if (adChoicesPlacement != null) {
        NativeAdOptions.Builder().setAdChoicesPlacement(adChoicesPlacement).build()
      } else {
        null
      }
    doReturn(nativeAdOptions).whenever(mediationNativeAdConfiguration).nativeAdOptions
    whenever(metaFactory.createNativeAdFromBidPayload(any(), any(), any())) doReturn metaNativeAd
    facebookRtbNativeAd.render(mediationNativeAdConfiguration)
    val iconView = mock<ImageView>()
    val clickableAssets = mapOf(ASSET_ICON to iconView)

    facebookRtbNativeAd.trackViews(gmaContainerView, clickableAssets, emptyMap())

    if (expectedPosition != null) {
      verify(metaNativeAd).setPreferredAdOptionsViewPosition(expectedPosition)
    } else {
      verify(metaNativeAd, times(0)).setPreferredAdOptionsViewPosition(any())
    }
  }

  private companion object {
    const val META_AD_HEADLINE = "meta_ad_headline"
    const val META_AD_BODY_TEXT = "meta_ad_body_text"
    const val META_AD_CALL_TO_ACTION = "meta_ad_call_to_action"
    const val META_AD_COVER_IMAGE_URI = "http://meta.com/ad-cover-image"
    const val META_AD_ICON_URI = "http://meta.com/ad-icon"
    const val META_ADVERTISER_NAME = "meta_advertiser_name"
    const val META_AD_ID = "skq2321d0Ad"
    const val META_AD_SOCIAL_CONTEXT = "Meta ad social context"
  }
}
