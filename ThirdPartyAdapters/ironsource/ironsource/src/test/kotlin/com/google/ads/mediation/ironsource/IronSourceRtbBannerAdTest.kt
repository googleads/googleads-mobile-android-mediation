package com.google.ads.mediation.ironsource

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_CODE_DECRYPT_FAILED
import com.unity3d.ironsourceads.banner.BannerAdView
import com.unity3d.ironsourceads.banner.BannerAdViewListener
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IronSourceRtbBannerAdTest {
  private val bundle = bundleOf("instanceId" to "mockInstanceId")
  private val context: Context = ApplicationProvider.getApplicationContext()

  private val mockInterstitialAdConfig: MediationBannerAdConfiguration = mock {
    on { context } doReturn context
    on { serverParameters } doReturn bundle
    on { bidResponse } doReturn AdapterTestKitConstants.TEST_BID_RESPONSE
    on { watermark } doReturn AdapterTestKitConstants.TEST_WATERMARK
    on { adSize } doReturn AdSize.BANNER
  }

  private val bannerAdCallback = FakeMediationBannerAdCallback()

  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)
  private val mockIsListener: BannerAdViewListener = mock()
  private val mockBannerAd: BannerAdView = mock { on { listener } doReturn mockIsListener }
  private val mediationUtils: MediationUtilsWrapper = mock()

  private val ironSourceRtbBannerAd = IronSourceRtbBannerAd(bannerAdLoadCallback)

  @Test
  fun onBannerAdLoaded_withValidBannerAd_expectOnSuccessCallback() {
    // Given
    ironSourceRtbBannerAd.loadRtbAd(mockInterstitialAdConfig, mediationUtils)

    // When
    ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)

    // Then
    assertThat(bannerAdLoadCallback).hasSucceededWith(ironSourceRtbBannerAd)
    assertThat(mockBannerAd.listener).isEqualTo(mockIsListener)
    assertThat(ironSourceRtbBannerAd.view).isNotNull()
  }

  @Test
  fun onLoadBanner_withoutBannerAdLoaded_expectNoCallback() {
    // When
    ironSourceRtbBannerAd.loadRtbAd(mockInterstitialAdConfig, mediationUtils)

    // Then
    assertThat(bannerAdLoadCallback).hasNotSucceeded()
    assertThat(bannerAdLoadCallback).hasNoFailure()
  }

  @Test
  fun onBannerAdLoaded_withoutBanner_expectNoCallback() {
    // When
    ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)

    // Then
    assertThat(bannerAdLoadCallback).hasNotSucceeded()
    assertThat(bannerAdLoadCallback).hasNoFailure()
  }

  @Test
  fun onBannerAdLoadFailed_withValidBannerAd_expectOnFailureCallback() {
    // Given
    ironSourceRtbBannerAd.loadRtbAd(mockInterstitialAdConfig, mediationUtils)
    val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

    // When
    ironSourceRtbBannerAd.onBannerAdLoadFailed(ironSourceError)

    // Then
    val expectedError =
      AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
    assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
  }

  @Test
  fun onBannerAdShown_withValidBannerAd_expectReportAdImpression() {
    // Given
    ironSourceRtbBannerAd.loadRtbAd(mockInterstitialAdConfig, mediationUtils)
    ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)

    // When
    ironSourceRtbBannerAd.onBannerAdShown(mockBannerAd)

    // Then
    assertThat(bannerAdCallback.isImpressionReported).isTrue()
  }

  @Test
  fun onBannerAdClicked_withValidBannerAd_expectOnBannerAdClickedCallbacks() {
    // Given
    ironSourceRtbBannerAd.loadRtbAd(mockInterstitialAdConfig, mediationUtils)
    ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)
    ironSourceRtbBannerAd.onBannerAdShown(mockBannerAd)

    // When
    ironSourceRtbBannerAd.onBannerAdClicked(mockBannerAd)

    // Then
    assertThat(bannerAdCallback.isOpened).isTrue()
    assertThat(bannerAdCallback.isClicked).isTrue()
  }
}
