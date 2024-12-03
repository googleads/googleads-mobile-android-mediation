package com.google.ads.mediation.ironsource

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.ironsource.mediationsdk.logger.IronSourceError
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.robolectric.RobolectricTestRunner
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_CODE_DECRYPT_FAILED
import com.unity3d.ironsourceads.banner.BannerAdView
import com.unity3d.ironsourceads.banner.BannerAdViewListener
import org.mockito.kotlin.mock

@RunWith(RobolectricTestRunner::class)
class IronSourceRtbBannerAdTest {
    private val bundle = bundleOf(
        "instanceId" to "mockInstanceId"
    )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockInterstitialAdConfig: MediationBannerAdConfiguration =
        mock {
            on { context } doReturn context
            on { serverParameters } doReturn bundle
            on { getBidResponse() } doReturn AdapterTestKitConstants.TEST_BID_RESPONSE
            on { getWatermark() } doReturn AdapterTestKitConstants.TEST_WATERMARK
            on { adSize } doReturn AdSize.BANNER
        }

    private val mockMediationBannerAdCallback: MediationBannerAdCallback = mock()

    private val bannerAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
        org.mockito.kotlin.mock {
            on { onSuccess(any()) } doReturn mockMediationBannerAdCallback
        }
    private val mockIsListener: BannerAdViewListener = mock()
    private val mockBannerAd: BannerAdView =
        org.mockito.kotlin.mock { on { listener } doReturn mockIsListener }

    private val ironSourceRtbBannerAd =
        IronSourceRtbBannerAd(mockInterstitialAdConfig, bannerAdLoadCallback)

    @Test
    fun onBannerAdLoaded_withValidBannerAd_expectOnSuccessCallback() {
        // Given
        ironSourceRtbBannerAd.loadRtbAd()

        // When
        ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)

        // Then
        verify(bannerAdLoadCallback).onSuccess(ironSourceRtbBannerAd)
        assertThat(mockBannerAd.listener).isEqualTo(mockIsListener)
        assertThat(ironSourceRtbBannerAd.view).isNotNull();

    }

    @Test
    fun onLoadBanner_withoutBannerAdLoaded_expectNoCallback() {
        // When
        ironSourceRtbBannerAd.loadRtbAd()

        // Then
        verifyNoInteractions(bannerAdLoadCallback)
    }

    @Test
    fun onBannerAdLoaded_withoutBanner_expectNoCallback() {
        // When
        ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)

        // Then
        verifyNoInteractions(bannerAdLoadCallback)
    }

    @Test
    fun onBannerAdLoadFailed_withValidBannerAd_expectOnFailureCallback() {
        // Given
        ironSourceRtbBannerAd.loadRtbAd()
        val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")
        ironSourceRtbBannerAd.onBannerAdLoadFailed(ironSourceError)

        val adErrorCaptor = argumentCaptor<AdError>()
        // When
        verify(bannerAdLoadCallback).onFailure(adErrorCaptor.capture())

        //Then
        with(adErrorCaptor.firstValue) {
            assertThat(code).isEqualTo(ERROR_CODE_DECRYPT_FAILED)
            assertThat(message).isEqualTo("Decrypt failed.")
            assertThat(domain).isEqualTo(IRONSOURCE_SDK_ERROR_DOMAIN)
        }
    }

    @Test
    fun onBannerAdShown_withValidBannerAd_expectReportAdImpression() {
        // Given
        ironSourceRtbBannerAd.loadRtbAd()
        ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)

        // When
        ironSourceRtbBannerAd.onBannerAdShown(mockBannerAd)

        // Then
        verify(mockMediationBannerAdCallback).reportAdImpression()
    }

    @Test
    fun onBannerAdClicked_withValidBannerAd_expectOnBannerAdClickedCallbacks() {
        // Given
        ironSourceRtbBannerAd.loadRtbAd()
        ironSourceRtbBannerAd.onBannerAdLoaded(mockBannerAd)
        ironSourceRtbBannerAd.onBannerAdShown(mockBannerAd)

        // When
        ironSourceRtbBannerAd.onBannerAdClicked(mockBannerAd)

        // Then
        verify(mockMediationBannerAdCallback).onAdOpened()
        verify(mockMediationBannerAdCallback).reportAdClicked()

    }
}