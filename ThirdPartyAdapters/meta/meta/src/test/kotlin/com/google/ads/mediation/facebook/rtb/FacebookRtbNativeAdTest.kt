package com.google.ads.mediation.facebook.rtb

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.MediaView
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdBase
import com.facebook.ads.NativeAdBase.Image
import com.facebook.ads.NativeAdListener
import com.facebook.ads.NativeBannerAd
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.facebook.FacebookMediationAdapter
import com.google.ads.mediation.facebook.FacebookMediationAdapter.KEY_ID
import com.google.ads.mediation.facebook.FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET
import com.google.ads.mediation.facebook.MetaFactory
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for public API calls implemented by [FacebookRtbNativeAd]. */
@RunWith(AndroidJUnit4::class)
class FacebookRtbNativeAdTest {

  private lateinit var facebookRtbNativeAd: FacebookRtbNativeAd
  private lateinit var mediationNativeAdConfiguration: MediationNativeAdConfiguration
  private lateinit var metaNativeAd: NativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val nativeAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> =
    mock()
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
  private val nativeListenerCaptor = argumentCaptor<NativeAdListener>()
  private val serverParameters =
    bundleOf(
      FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER to AdapterTestKitConstants.TEST_PLACEMENT_ID
    )
  private val metaMediaView = mock<MediaView>()
  private val metaFactory =
    mock<MetaFactory> { on { createMediaView(any()) } doReturn metaMediaView }
  private val iconViewDrawable = mock<Drawable>()

  @Before
  fun setUp() {
    mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        serverParameters = serverParameters,
        taggedForChildDirectedTreatment = 1,
        watermark = TEST_WATERMARK,
        bidResponse = AdapterTestKitConstants.TEST_BID_RESPONSE
      )
    facebookRtbNativeAd =
      FacebookRtbNativeAd(mediationNativeAdConfiguration, nativeAdLoadCallback, metaFactory)
    metaNativeAd = mock {
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
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withWrongAd_invokesLoadFailure() {
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val aWrongNativeAd: NativeAdBase = mock()

    nativeListenerCaptor.firstValue.onAdLoaded(aWrongNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_WRONG_NATIVE_TYPE,
        "Ad Loaded is not a Native Ad.",
        FacebookMediationAdapter.ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdHeadline_invokesLoadFailure() {
    whenever(metaNativeAd.adHeadline) doReturn null
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdBodyText_invokesLoadFailure() {
    whenever(metaNativeAd.adBodyText) doReturn null
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdIcon_invokesLoadFailure() {
    whenever(metaNativeAd.adIcon) doReturn null
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdCallToAction_invokesLoadFailure() {
    whenever(metaNativeAd.adCallToAction) doReturn null
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withoutAdCoverImage_invokesLoadFailure() {
    whenever(metaNativeAd.adCoverImage) doReturn null
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_whenMediaViewIsNull_invokesLoadFailure() {
    whenever(metaFactory.createMediaView(any())) doReturn null
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val expectedAdError =
      AdError(
        FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS,
        "Ad from Meta Audience Network doesn't have all required assets.",
        FacebookMediationAdapter.ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnError_invokesLoadFailure() {
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())
    val metaAdLoadError = com.facebook.ads.AdError(101, "Load error from Meta")

    nativeListenerCaptor.firstValue.onError(metaNativeAd, metaAdLoadError)

    val expectedAdError =
      AdError(
        metaAdLoadError.errorCode,
        metaAdLoadError.errorMessage,
        FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN
      )
    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_setsNativeAdAssetsAndInvokesLoadSuccess() {
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
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
    assertThat(extras.size()).isEqualTo(2)
    assertThat(extras.containsKey(KEY_ID)).isTrue()
    assertThat(extras.getString(KEY_ID)).isEqualTo(META_AD_ID)
    assertThat(extras.containsKey(KEY_SOCIAL_CONTEXT_ASSET)).isTrue()
    assertThat(extras.getString(KEY_SOCIAL_CONTEXT_ASSET)).isEqualTo(META_AD_SOCIAL_CONTEXT)
    assertThat(facebookRtbNativeAd.adChoicesContent).isNotNull()
    verify(nativeAdLoadCallback).onSuccess(eq(facebookRtbNativeAd))
  }

  @Test
  fun nativeAdListenerOnAdLoaded_withPreloadedDrawable_setsNativeAdIconWithDrawable() {
    whenever(metaNativeAd.preloadedIconViewDrawable) doReturn iconViewDrawable
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeAd
      facebookRtbNativeAd.render()
    }
    verify(metaNativeAdLoadConfigBuilder).withAdListener(nativeListenerCaptor.capture())

    nativeListenerCaptor.firstValue.onAdLoaded(metaNativeAd)

    val nativeAdIcon = facebookRtbNativeAd.icon
    assertThat(nativeAdIcon.uri).isNull()
    assertThat(nativeAdIcon.drawable).isEqualTo(iconViewDrawable)
  }

  @Test
  fun nativeAdListenerOnAdLoaded_forNativeBannerAdWithNoCoverImage_setsAllOtherAssetsAndInvokesLoadSuccess() {
    val metaNativeBannerAd =
      mock<NativeBannerAd> {
        on { buildLoadAdConfig() } doReturn metaNativeAdLoadConfigBuilder
        on { adHeadline } doReturn META_AD_HEADLINE
        on { adBodyText } doReturn META_AD_BODY_TEXT
        on { adCallToAction } doReturn META_AD_CALL_TO_ACTION
        on { adIcon } doReturn metaAdIcon
        on { advertiserName } doReturn META_ADVERTISER_NAME
        on { id } doReturn META_AD_ID
        on { adSocialContext } doReturn META_AD_SOCIAL_CONTEXT
      }
    Mockito.mockStatic(NativeAdBase::class.java).use {
      whenever(NativeAdBase.fromBidPayload(any(), any(), any())) doReturn metaNativeBannerAd
      facebookRtbNativeAd.render()
    }
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
    assertThat(extras.size()).isEqualTo(2)
    assertThat(extras.containsKey(KEY_ID)).isTrue()
    assertThat(extras.getString(KEY_ID)).isEqualTo(META_AD_ID)
    assertThat(extras.containsKey(KEY_SOCIAL_CONTEXT_ASSET)).isTrue()
    assertThat(extras.getString(KEY_SOCIAL_CONTEXT_ASSET)).isEqualTo(META_AD_SOCIAL_CONTEXT)
    assertThat(facebookRtbNativeAd.adChoicesContent).isNotNull()
    verify(nativeAdLoadCallback).onSuccess(eq(facebookRtbNativeAd))
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
