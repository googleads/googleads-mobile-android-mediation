package com.google.ads.mediation.inmobi.waterfall

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiAdFactory
import com.google.ads.mediation.inmobi.InMobiAdapterUtils
import com.google.ads.mediation.inmobi.InMobiConstants
import com.google.ads.mediation.inmobi.InMobiInitializer
import com.google.ads.mediation.inmobi.InMobiNativeWrapper
import com.google.ads.mediation.inmobi.InMobiNetworkValues
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.common.truth.Truth
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import java.net.URL
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiWaterfallNativeAdTest {
  private val nativeAdConfiguration = mock<MediationNativeAdConfiguration>()
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
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
        inMobiAdFactory
      )
  }

  @Test
  fun onAdLoadSucceeded_whenNativeAdOptionsNotNullAndValid_invokesOnSuccessCallback() {
    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.headline).isEqualTo(wrappedNativeAd.adTitle)
    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.body)
      .isEqualTo(wrappedNativeAd.adDescription)
    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.callToAction)
      .isEqualTo(wrappedNativeAd.adCtaText)
    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.extras.get(InMobiNetworkValues.LANDING_URL))
      .isEqualTo(wrappedNativeAd.adLandingPageUrl)
    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.icon.drawable).isNull()
    val iconURL = URL(wrappedNativeAd.adIconUrl)
    val iconUri = Uri.parse(iconURL.toURI().toString())
    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.icon.uri).isEqualTo(iconUri)
    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.icon.scale).isEqualTo(1.0)
    Truth.assertThat(waterfallNativeAd.inMobiUnifiedNativeAdMapper.hasVideoContent()).isTrue()

    verify(mediationAdLoadCallback).onSuccess(any())
  }

  // TODO(b/283150473) : Add a test case covering NativeAdOptions = null
  @Test
  fun onAdLoadSucceeded_whenNativeAdOptionsInvalid_invokesFailureCallback() {
    whenever(wrappedNativeAd.adTitle).thenReturn(null)

    waterfallNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    Truth.assertThat(captor.firstValue.code).isEqualTo(InMobiConstants.ERROR_MISSING_NATIVE_ASSETS)
    Truth.assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.ERROR_DOMAIN)
  }

  @Test
  fun onAdLoadFailed_invokesFailureCallback() {
    var inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)

    waterfallNativeAd.onAdLoadFailed(inMobiNativeWrapper.inMobiNative, inMobiAdRequestStatus)

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    Truth.assertThat(captor.firstValue.code)
      .isEqualTo(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus))
    Truth.assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.INMOBI_SDK_ERROR_DOMAIN)
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

  private fun setupWrappedInMobiNativeAd(): Unit {
    whenever(wrappedNativeAd.adCtaText).thenReturn("SomeCtaText")
    whenever(wrappedNativeAd.adDescription).thenReturn("AdDescription")
    whenever(wrappedNativeAd.adIconUrl).thenReturn("http://www.example.com/docs/resource1.html")
    whenever(wrappedNativeAd.adLandingPageUrl)
      .thenReturn("http://www.landing.com/docs/resource1.html")
    whenever(wrappedNativeAd.adTitle).thenReturn("adTitle")
    whenever(wrappedNativeAd.isVideo).thenReturn(true)
  }
}