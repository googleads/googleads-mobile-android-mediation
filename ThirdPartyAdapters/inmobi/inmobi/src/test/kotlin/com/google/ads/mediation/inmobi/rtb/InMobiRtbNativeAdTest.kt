package com.google.ads.mediation.inmobi.rtb

import android.content.Context
import android.net.Uri
import android.view.View
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
import com.google.common.truth.Truth.assertThat
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
class InMobiRtbNativeAdTest {
  private val nativeAdConfiguration = mock<MediationNativeAdConfiguration>()
  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>>()
  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiNativeWrapper = mock<InMobiNativeWrapper>()
  private val wrappedNativeAd = mock<InMobiNativeWrapper>()
  private val mediationNativeAdCallback = mock<MediationNativeAdCallback>()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  lateinit var rtbNativeAd: InMobiRtbNativeAd
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

    rtbNativeAd =
      InMobiRtbNativeAd(
        nativeAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory,
      )
  }

  @Test
  fun onAdLoadSucceeded_whenNativeAdOptionsNotNullAndValid_invokesOnSuccessCallback() {
    rtbNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    assertThat(rtbNativeAd.inMobiUnifiedNativeAdMapper.headline).isEqualTo(wrappedNativeAd.adTitle)
    assertThat(rtbNativeAd.inMobiUnifiedNativeAdMapper.body)
      .isEqualTo(wrappedNativeAd.adDescription)
    assertThat(rtbNativeAd.inMobiUnifiedNativeAdMapper.callToAction)
      .isEqualTo(wrappedNativeAd.adCtaText)
    assertThat(rtbNativeAd.inMobiUnifiedNativeAdMapper.icon.drawable).isNull()
    val iconURL = URL(wrappedNativeAd.adIconUrl)
    val iconUri = Uri.parse(iconURL.toURI().toString())
    assertThat(rtbNativeAd.inMobiUnifiedNativeAdMapper.icon.uri).isEqualTo(iconUri)
    assertThat(rtbNativeAd.inMobiUnifiedNativeAdMapper.icon.scale).isEqualTo(1.0)
    assertThat(rtbNativeAd.inMobiUnifiedNativeAdMapper.hasVideoContent()).isTrue()

    verify(mediationAdLoadCallback).onSuccess(any())
  }

  @Test
  fun onAdLoadFailed_invokesFailureCallback() {
    var inMobiAdRequestStatus =
      InMobiAdRequestStatus(InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR)

    rtbNativeAd.onAdLoadFailed(inMobiNativeWrapper.inMobiNative, inMobiAdRequestStatus)

    val captor = argumentCaptor<AdError>()
    verify(mediationAdLoadCallback).onFailure(captor.capture())
    assertThat(captor.firstValue.code)
      .isEqualTo(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus))
    assertThat(captor.firstValue.domain).isEqualTo(InMobiConstants.INMOBI_SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdFullScreenDismissed_invokesOnAdClosed() {
    // mimic an ad load first
    rtbNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    rtbNativeAd.onAdFullScreenDismissed(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).onAdClosed()
  }

  @Test
  fun onAdFullScreenDisplayed_invokesOnAdOpened() {
    // mimic an ad load first
    rtbNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    rtbNativeAd.onAdFullScreenDisplayed(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).onAdOpened()
  }

  @Test
  fun onUserWillLeaveApplication_invokesOnAdLeftApplication() {
    // mimic an ad load first
    rtbNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    rtbNativeAd.onUserWillLeaveApplication(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdClicked_invokesReportAdClicked() {
    // mimic an ad load first
    rtbNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    rtbNativeAd.onAdClicked(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).reportAdClicked()
  }

  @Test
  fun onAdImpression_invokesReportAdImpression() {
    // mimic an ad load first
    rtbNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    rtbNativeAd.onAdImpression(inMobiNativeWrapper.inMobiNative)

    verify(mediationNativeAdCallback).reportAdImpression()
  }

  @Test
  fun untrackView_invokesUntrackView() {
    // mimic an ad load first
    rtbNativeAd.onAdLoadSucceeded(inMobiNativeWrapper.inMobiNative, adMetaInfo)

    rtbNativeAd.inMobiUnifiedNativeAdMapper.untrackView(View(context))

    verify(wrappedNativeAd).unTrackViews()
  }

  private fun setupWrappedInMobiNativeAd(): Unit {
    whenever(wrappedNativeAd.adCtaText).thenReturn("SomeCtaText")
    whenever(wrappedNativeAd.adDescription).thenReturn("AdDescription")
    whenever(wrappedNativeAd.adIconUrl).thenReturn("http://www.example.com/docs/resource1.html")
    whenever(wrappedNativeAd.adTitle).thenReturn("adTitle")
    whenever(wrappedNativeAd.isVideo).thenReturn(true)
  }
}
