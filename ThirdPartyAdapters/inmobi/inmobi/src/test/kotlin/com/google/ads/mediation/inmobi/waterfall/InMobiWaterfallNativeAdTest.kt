package com.google.ads.mediation.inmobi.waterfall

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener
import com.google.ads.mediation.inmobi.InMobiNativeWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdAssetNames
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.common.truth.Truth.assertThat
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiNative
import com.inmobi.ads.listeners.VideoEventListener
import com.inmobi.media.ads.nativeAd.InMobiNativeViewData
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowBitmapFactory

@RunWith(AndroidJUnit4::class)
class InMobiWaterfallNativeAdTest {
  private val nativeAdConfiguration = mock<MediationNativeAdConfiguration>()
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiNative = mock<InMobiNative>()
  private val inMobiNativeWrapper = mock<InMobiNativeWrapper>()
  private val wrappedNativeAd = mock<InMobiNativeWrapper>()
  private val mediationNativeAdCallback = mock<MediationNativeAdCallback>()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  lateinit var waterfallNativeAd: InMobiWaterfallNativeAd
  private lateinit var adMetaInfo: AdMetaInfo
  private lateinit var nativeAdOptions: NativeAdOptions

  @Before
  fun setUp() {
    adMetaInfo = AdMetaInfo("fake", null)
    whenever(inMobiNativeWrapper.inMobiNative).thenReturn(inMobiNative)
    whenever(wrappedNativeAd.inMobiNative).thenReturn(inMobiNative)
    whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationNativeAdCallback)
    whenever(inMobiAdFactory.createInMobiNativeWrapper(anyOrNull())).thenReturn(wrappedNativeAd)
    setupWrappedInMobiNativeAd()
    whenever(nativeAdConfiguration.context).thenReturn(context)
    nativeAdOptions = NativeAdOptions.Builder().setReturnUrlsForImageAssets(true).build()
    whenever(nativeAdConfiguration.nativeAdOptions).thenReturn(nativeAdOptions)

    waterfallNativeAd =
      InMobiWaterfallNativeAd(
        nativeAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory,
      )
  }

  @Test
  fun onAdLoadSucceeded_whenNativeAdOptionsNotNullAndValid_invokesOnSuccessCallback() {
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.headline)
      .isEqualTo(wrappedNativeAd.adTitle)
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.body)
      .isEqualTo(wrappedNativeAd.adDescription)
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.callToAction)
      .isEqualTo(wrappedNativeAd.adCtaText)
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.icon.drawable).isNull()
    val iconURL = URL(wrappedNativeAd.adIconUrl)
    val iconUri = Uri.parse(iconURL.toURI().toString())
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.icon.uri).isEqualTo(iconUri)
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.icon.scale).isEqualTo(1.0)
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.hasVideoContent()).isTrue()

    verify(mediationAdLoadCallback).onSuccess(any())
  }

  @Test
  fun onAdLoadFailed_invokesFailureCallback() {
    var inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)

    waterfallNativeAd.onAdLoadFailed(inMobiNativeWrapper.inMobiNative, inMobiAdRequestStatus)

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code)
      .isEqualTo(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus))
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.INMOBI_SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdFullScreenDismissed_invokesOnAdClosed() {
    // mimic an ad load first
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    waterfallNativeAd.onAdFullScreenDismissed(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).onAdClosed()
  }

  @Test
  fun onAdFullScreenDisplayed_invokesOnAdOpened() {
    // mimic an ad load first
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    waterfallNativeAd.onAdFullScreenDisplayed(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).onAdOpened()
  }

  @Test
  fun onUserWillLeaveApplication_invokesOnAdLeftApplication() {
    // mimic an ad load first
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    waterfallNativeAd.onUserWillLeaveApplication(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    // mimic an ad load first
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    waterfallNativeAd.onAdClicked(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).reportAdClicked()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    // mimic an ad load first
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    waterfallNativeAd.onAdImpression(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).reportAdImpression()
  }

  @Test
  fun untrackView_invokesUntrackView() {
    // mimic an ad load first
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    waterfallNativeAd.inMobiUnifiedNativeAdMapper.untrackView(View(context))

    verify(wrappedNativeAd).unTrackViews()
  }

  @Test
  fun onAdLoadSucceeded_whenShouldReturnUrlsForImageAssetsFalse_downloadsDrawables_invokesOnSuccessCallback() {
    ShadowBitmapFactory.setAllowInvalidImageData(true)
    val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val tempFile = File.createTempFile("test_icon", ".png", context.cacheDir)
    tempFile.deleteOnExit()
    FileOutputStream(tempFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    val fileUrl = tempFile.toURI().toURL().toString()
    whenever(wrappedNativeAd.adIconUrl).thenReturn(fileUrl)

    nativeAdOptions = NativeAdOptions.Builder().setReturnUrlsForImageAssets(false).build()
    whenever(nativeAdConfiguration.nativeAdOptions).thenReturn(nativeAdOptions)

    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    for (i in 1..20) {
      Thread.sleep(50)
      shadowOf(Looper.getMainLooper()).idle()
    }

    verify(mediationAdLoadCallback).onSuccess(any())
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.icon.drawable).isNotNull()
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.images).isNotEmpty()
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.images[0].drawable).isNotNull()
  }

  @Test
  fun onAdLoadSucceeded_whenShouldReturnUrlsForImageAssetsFalse_downloadFailure_invokesFailureCallback() {
    whenever(wrappedNativeAd.adIconUrl).thenReturn("http://127.0.0.1:9999/non_existent.png")
    nativeAdOptions = NativeAdOptions.Builder().setReturnUrlsForImageAssets(false).build()
    whenever(nativeAdConfiguration.nativeAdOptions).thenReturn(nativeAdOptions)

    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    for (i in 1..20) {
      Thread.sleep(50)
      shadowOf(Looper.getMainLooper()).idle()
    }

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_NATIVE_ASSET_DOWNLOAD_FAILED)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun onAdLoadSucceeded_malformedIconUrl_invokesFailureCallback() {
    whenever(wrappedNativeAd.adIconUrl).thenReturn("not a valid url with spaces and !@#$")

    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_MALFORMED_IMAGE_URL)
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun trackViews_registersClickableAssetViewsAndCallsRegisterForTracking() {
    // Mimic an ad load first
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    val containerView = FrameLayout(context)
    val headlineView = TextView(context)
    val bodyView = TextView(context)
    val iconView = ImageView(context)
    val ctaView = Button(context)
    val ratingView = View(context)
    val advertiserView = TextView(context)

    val clickableAssetViews =
      mapOf(
        NativeAdAssetNames.ASSET_HEADLINE to headlineView,
        NativeAdAssetNames.ASSET_BODY to bodyView,
        NativeAdAssetNames.ASSET_ICON to iconView,
        NativeAdAssetNames.ASSET_CALL_TO_ACTION to ctaView,
        NativeAdAssetNames.ASSET_STAR_RATING to ratingView,
        NativeAdAssetNames.ASSET_ADVERTISER to advertiserView,
      )

    waterfallNativeAd.inMobiUnifiedNativeAdMapper.trackViews(
      containerView,
      clickableAssetViews,
      emptyMap(),
    )

    val captor = argumentCaptor<InMobiNativeViewData>()
    verify(wrappedNativeAd).registerForTracking(captor.capture())
    assertThat(captor.firstValue).isNotNull()
    assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.overrideClickHandling).isTrue()
  }

  @Test
  fun onVideoCompleted_invokesOnVideoComplete() {
    val serverParameters =
      Bundle().apply {
        putString(InMobiAdapterUtils.KEY_ACCOUNT_ID, "12345")
        putString(InMobiAdapterUtils.KEY_PLACEMENT_ID, "67890")
      }
    whenever(nativeAdConfiguration.serverParameters).thenReturn(serverParameters)
    whenever(inMobiInitializer.init(any(), any(), any())).doAnswer {
      val listener = it.arguments[2] as Listener
      listener.onInitializeSuccess()
    }
    whenever(
        inMobiAdFactory.createInMobiNativeWrapper(eq(context), eq(67890L), eq(waterfallNativeAd))
      )
      .thenReturn(inMobiNativeWrapper)

    waterfallNativeAd.loadAd()

    val videoEventListenerCaptor = argumentCaptor<VideoEventListener>()
    verify(inMobiNativeWrapper).setVideoEventListener(videoEventListenerCaptor.capture())

    // Complete the ad load to set mediationNativeAdCallback
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    videoEventListenerCaptor.firstValue.onVideoCompleted(inMobiNativeWrapper.inMobiNative)
    verify(mediationNativeAdCallback).onVideoComplete()
  }

  private fun setupWrappedInMobiNativeAd(): Unit {
    whenever(wrappedNativeAd.adCtaText).thenReturn("SomeCtaText")
    whenever(wrappedNativeAd.adDescription).thenReturn("AdDescription")
    whenever(wrappedNativeAd.adIconUrl).thenReturn("http://www.example.com/docs/resource1.html")
    whenever(wrappedNativeAd.adTitle).thenReturn("adTitle")
    whenever(wrappedNativeAd.isVideo).thenReturn(true)
  }
}
