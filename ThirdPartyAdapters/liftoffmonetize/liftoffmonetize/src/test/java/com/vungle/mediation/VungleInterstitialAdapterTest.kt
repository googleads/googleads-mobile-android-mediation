package com.vungle.mediation

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.vungle.SdkWrapper
import com.google.ads.mediation.vungle.VungleConstants
import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.VungleMediationAdapter
import com.google.ads.mediation.vungle.VungleSdkWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationAdRequest
import com.google.android.gms.ads.mediation.MediationBannerListener
import com.google.android.gms.ads.mediation.MediationInterstitialListener
import com.vungle.ads.AdConfig
import com.vungle.ads.BannerAd
import com.vungle.ads.BannerAdSize
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleError
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Tests for [VungleInterstitialAdapter]. */
@RunWith(AndroidJUnit4::class)
class VungleInterstitialAdapterTest {
    private lateinit var adapter: VungleInterstitialAdapter

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockSdkWrapper = mock<SdkWrapper>()
    private val mockVungleInitializer = mock<VungleInitializer>()
    private val vungleAdConfig = mock<AdConfig>()
    private val vungleFactory =
        mock<VungleFactory> {
            on { createAdConfig() } doReturn vungleAdConfig
        }

    // Interstitial ad objects
    private val mockInterstitialListener = mock<MediationInterstitialListener>()
    private val vungleInterstitial = mock<InterstitialAd>()

    // Banner ad objects
    private val mockBannerListener = mock<MediationBannerListener>()
    private val vungleBanner = mock<BannerAd>()

    @Before
    fun setUp() {
        VungleSdkWrapper.delegate = mockSdkWrapper
        adapter = VungleInterstitialAdapter(vungleFactory)
    }

    // region interstitial ad tests
    @Test
    fun loadInterstitialAd_withoutAppId_callsLoadFailure() {
        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID)
        val mediationExtras = bundleOf(VungleConstants.KEY_ORIENTATION to AdConfig.LANDSCAPE)

        adapter.requestInterstitialAd(
            context,
            mockInterstitialListener,
            serverParameters,
            mockMediationAdRequest,
            mediationExtras
        )

        val expectedAdError =
            AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Failed to load waterfall interstitial ad from Liftoff Monetize. " +
                        "Missing or invalid App ID configured for this ad source instance " +
                        "in the AdMob or Ad Manager UI.",
                VungleMediationAdapter.ERROR_DOMAIN
            )

        Mockito.verify(mockInterstitialListener)
            .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(expectedAdError)))
    }

    @Test
    fun loadInterstitialAd_withoutPlacementId_callsLoadFailure() {
        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID)
        val mediationExtras = bundleOf(VungleConstants.KEY_ORIENTATION to AdConfig.LANDSCAPE)

        adapter.requestInterstitialAd(
            context,
            mockInterstitialListener,
            serverParameters,
            mockMediationAdRequest,
            mediationExtras
        )

        val expectedAdError =
            AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Failed to load waterfall interstitial ad from Liftoff Monetize. "
                        + "Missing or invalid Placement ID configured for this ad source instance "
                        + "in the AdMob or Ad Manager UI.",
                VungleMediationAdapter.ERROR_DOMAIN
            )

        Mockito.verify(mockInterstitialListener)
            .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(expectedAdError)))
    }

    @Test
    fun loadInterstitialAd_onLiftoffSdkInitializationError_callsLoadFailure() {
        val liftoffSdkInitError =
            AdError(
                VungleError.UNKNOWN_ERROR,
                "Liftoff Monetize SDK initialization failed.",
                VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
            )
        Mockito.doAnswer { invocation ->
            val args: Array<Any> = invocation.arguments
            (args[2] as VungleInitializer.VungleInitializationListener).onInitializeError(
                liftoffSdkInitError
            )
        }
            .whenever(mockVungleInitializer)
            .initialize(any(), any(), any())

        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(
                VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
                VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID
            )
        val mediationExtras = bundleOf(VungleConstants.KEY_ORIENTATION to AdConfig.LANDSCAPE)

        Mockito.mockStatic(VungleInitializer::class.java).use {
            whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer

            adapter.requestInterstitialAd(
                context,
                mockInterstitialListener,
                serverParameters,
                mockMediationAdRequest,
                mediationExtras
            )
        }

        Mockito.verify(mockInterstitialListener)
            .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(liftoffSdkInitError)))
    }

    @Test
    fun loadInterstitialAd_updatesCoppaStatus() {
        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(
                VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
                VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID
            )
        val mediationExtras = bundleOf(VungleConstants.KEY_ORIENTATION to AdConfig.LANDSCAPE)

        Mockito.mockStatic(VungleInitializer::class.java).use {
            whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer

            adapter.requestInterstitialAd(
                context,
                mockInterstitialListener,
                serverParameters,
                mockMediationAdRequest,
                mediationExtras
            )
        }

        Mockito.verify(mockVungleInitializer).updateCoppaStatus(any())
    }

    private fun initSuccessAndRequestInterstitialAd() {
        Mockito.doAnswer { invocation ->
            val args: Array<Any> = invocation.arguments
            (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
        }
            .whenever(mockVungleInitializer)
            .initialize(any(), any(), any())

        whenever(
            vungleFactory.createInterstitialAd(
                any(),
                any(),
                any()
            )
        ) doReturn vungleInterstitial

        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(
                VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
                VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID
            )
        val mediationExtras = bundleOf(VungleConstants.KEY_ORIENTATION to AdConfig.LANDSCAPE)

        Mockito.mockStatic(VungleInitializer::class.java).use {
            whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer

            adapter.requestInterstitialAd(
                context,
                mockInterstitialListener,
                serverParameters,
                mockMediationAdRequest,
                mediationExtras
            )
        }
    }

    @Test
    fun loadInterstitialAd_onLiftoffSdkInitializationSuccess() {
        initSuccessAndRequestInterstitialAd()

        Mockito.verify(mockVungleInitializer)
            .initialize(eq(AdapterTestKitConstants.TEST_APP_ID), eq(context), any())
        Mockito.verify(vungleAdConfig).adOrientation = AdConfig.LANDSCAPE
        Mockito.verify(vungleFactory)
            .createInterstitialAd(
                context,
                AdapterTestKitConstants.TEST_PLACEMENT_ID,
                vungleAdConfig
            )
        Mockito.verify(vungleInterstitial).adListener = adapter.vungleInterstitialListener
        Mockito.verify(vungleInterstitial).load(eq(null))
    }

    @Test
    fun showInterstitialAd_playsLiftoffAd() {
        initSuccessAndRequestInterstitialAd()

        adapter.showInterstitial()

        verify(vungleInterstitial).play(null)
    }

    @Test
    fun onAdLoaded_callsLoadSuccess() {
        initSuccessAndRequestInterstitialAd()

        adapter.vungleInterstitialListener.onAdLoaded(vungleInterstitial)

        verify(mockInterstitialListener).onAdLoaded(adapter)
    }

    @Test
    fun onAdFailedToLoad_callsLoadFailure() {
        initSuccessAndRequestInterstitialAd()

        val liftoffError =
            mock<VungleError> {
                on { code } doReturn VungleError.AD_FAILED_TO_DOWNLOAD
                on { errorMessage } doReturn "Liftoff Monetize SDK interstitial ad load failed."
            }

        adapter.vungleInterstitialListener.onAdFailedToLoad(vungleInterstitial, liftoffError)

        val expectedError =
            AdError(
                liftoffError.code,
                liftoffError.errorMessage,
                VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
            )
        verify(mockInterstitialListener).onAdFailedToLoad(
            eq(adapter),
            argThat(AdErrorMatcher(expectedError))
        )
    }

    @Test
    fun onAdStart_callsOnAdOpened() {
        initSuccessAndRequestInterstitialAd()

        adapter.vungleInterstitialListener.onAdStart(vungleInterstitial)

        verify(mockInterstitialListener).onAdOpened(adapter)
    }

    @Test
    fun onAdEnd_callsOnAdClosed() {
        initSuccessAndRequestInterstitialAd()

        adapter.vungleInterstitialListener.onAdEnd(vungleInterstitial)

        verify(mockInterstitialListener).onAdClosed(adapter)
    }

    @Test
    fun onAdClicked_callsOnAdClicked() {
        initSuccessAndRequestInterstitialAd()

        adapter.vungleInterstitialListener.onAdClicked(vungleInterstitial)

        verify(mockInterstitialListener).onAdClicked(adapter)
    }

    @Test
    fun onAdLeftApplication_callsOnAdLeftApplication() {
        initSuccessAndRequestInterstitialAd()

        adapter.vungleInterstitialListener.onAdLeftApplication(vungleInterstitial)

        verify(mockInterstitialListener).onAdLeftApplication(adapter)
    }

    @Test
    fun onAdFailedToPlay_callsNothing() {
        initSuccessAndRequestInterstitialAd()

        val liftoffSdkError = mock<VungleError> {
            on { code } doReturn VungleError.AD_UNABLE_TO_PLAY
            on { errorMessage } doReturn "Liftoff Monetize SDK interstitial ad play failed."
        }

        adapter.vungleInterstitialListener.onAdFailedToPlay(vungleInterstitial, liftoffSdkError)

        verifyNoMoreInteractions(mockInterstitialListener)
    }

    @Test
    fun onAdImpression_callsNothing() {
        initSuccessAndRequestInterstitialAd()

        adapter.vungleInterstitialListener.onAdImpression(vungleInterstitial)

        verifyNoMoreInteractions(mockInterstitialListener)
    }

    // endregion

    // region banner ad tests
    @Test
    fun loadBannerAd_withoutAppId_callsLoadFailure() {
        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID)

        adapter.requestBannerAd(
            context,
            mockBannerListener,
            serverParameters,
            AdSize.BANNER,
            mockMediationAdRequest,
            mock()
        )

        val expectedAdError =
            AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Failed to load waterfall banner ad from Liftoff Monetize. " +
                        "Missing or invalid App ID configured for this ad source instance " +
                        "in the AdMob or Ad Manager UI.",
                VungleMediationAdapter.ERROR_DOMAIN
            )

        Mockito.verify(mockBannerListener)
            .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(expectedAdError)))
    }

    @Test
    fun loadBannerAd_withoutPlacementId_callsLoadFailure() {
        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID)

        adapter.requestBannerAd(
            context,
            mockBannerListener,
            serverParameters,
            AdSize.BANNER,
            mockMediationAdRequest,
            mock()
        )

        val expectedAdError =
            AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Failed to load waterfall banner ad from Liftoff Monetize. "
                        + "Missing or invalid Placement ID configured for this ad source instance "
                        + "in the AdMob or Ad Manager UI.",
                VungleMediationAdapter.ERROR_DOMAIN
            )

        Mockito.verify(mockBannerListener)
            .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(expectedAdError)))
    }

    @Test
    fun loadBannerAd_onLiftoffSdkInitializationError_callsLoadFailure() {
        val liftoffSdkInitError =
            AdError(
                VungleError.UNKNOWN_ERROR,
                "Liftoff Monetize SDK initialization failed.",
                VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
            )
        Mockito.doAnswer { invocation ->
            val args: Array<Any> = invocation.arguments
            (args[2] as VungleInitializer.VungleInitializationListener).onInitializeError(
                liftoffSdkInitError
            )
        }
            .whenever(mockVungleInitializer)
            .initialize(any(), any(), any())

        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(
                VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
                VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID
            )

        Mockito.mockStatic(VungleInitializer::class.java).use {
            whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer

            adapter.requestBannerAd(
                context,
                mockBannerListener,
                serverParameters,
                AdSize.BANNER,
                mockMediationAdRequest,
                mock()
            )
        }

        Mockito.verify(mockBannerListener)
            .onAdFailedToLoad(eq(adapter), argThat(AdErrorMatcher(liftoffSdkInitError)))
    }

    @Test
    fun loadBannerAd_updatesCoppaStatus() {
        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(
                VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
                VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID
            )

        Mockito.mockStatic(VungleInitializer::class.java).use {
            whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer

            adapter.requestBannerAd(
                context,
                mockBannerListener,
                serverParameters,
                AdSize.BANNER,
                mockMediationAdRequest,
                mock()
            )
        }

        Mockito.verify(mockVungleInitializer).updateCoppaStatus(any())
    }

    private fun initSuccessAndRequestBannerAd() {
        Mockito.doAnswer { invocation ->
            val args: Array<Any> = invocation.arguments
            (args[2] as VungleInitializer.VungleInitializationListener).onInitializeSuccess()
        }
            .whenever(mockVungleInitializer)
            .initialize(any(), any(), any())

        whenever(vungleFactory.createBannerAd(any(), any(), any())) doReturn vungleBanner

        val mockMediationAdRequest = mock<MediationAdRequest>()
        val serverParameters =
            bundleOf(
                VungleConstants.KEY_APP_ID to AdapterTestKitConstants.TEST_APP_ID,
                VungleConstants.KEY_PLACEMENT_ID to AdapterTestKitConstants.TEST_PLACEMENT_ID
            )

        Mockito.mockStatic(VungleInitializer::class.java).use {
            whenever(VungleInitializer.getInstance()) doReturn mockVungleInitializer

            adapter.requestBannerAd(
                context,
                mockBannerListener,
                serverParameters,
                AdSize.MEDIUM_RECTANGLE,
                mockMediationAdRequest,
                mock()
            )
        }
    }

    @Test
    fun loadBannerAd_onLiftoffSdkInitializationSuccess() {
        initSuccessAndRequestBannerAd()

        Mockito.verify(mockVungleInitializer)
            .initialize(eq(AdapterTestKitConstants.TEST_APP_ID), eq(context), any())
        Mockito.verify(vungleFactory)
            .createBannerAd(
                context,
                AdapterTestKitConstants.TEST_PLACEMENT_ID,
                BannerAdSize.VUNGLE_MREC
            )
        Mockito.verify(vungleBanner).adListener = adapter.vungleBannerListener
        Mockito.verify(vungleBanner).load(eq(null))
    }

    @Test
    fun onBannerAdLoaded_playsLiftoffAd() {
        initSuccessAndRequestBannerAd()

        whenever(vungleBanner.getBannerView()).doReturn(mock())
        adapter.vungleBannerListener.onAdLoaded(vungleBanner)

        verify(mockBannerListener).onAdLoaded(adapter)
    }

    @Test
    fun onBannerAdLoaded_playsLiftoffAdWithError() {
        initSuccessAndRequestBannerAd()

        whenever(vungleBanner.getBannerView()).doReturn(null)
        adapter.vungleBannerListener.onAdLoaded(vungleBanner)

        val adError = argumentCaptor<AdError>()
        verify(mockBannerListener).onAdFailedToLoad(eq(adapter), adError.capture())
        adError.allValues.forEach {
            assert(it.code == VungleMediationAdapter.ERROR_VUNGLE_BANNER_NULL)
            assert(it.domain == VungleMediationAdapter.ERROR_DOMAIN)
        }
    }

    @Test
    fun onBannerAdFailedToLoad_callsLoadFailure() {
        initSuccessAndRequestBannerAd()

        val liftoffError =
            mock<VungleError> {
                on { code } doReturn VungleError.AD_FAILED_TO_DOWNLOAD
                on { errorMessage } doReturn "Liftoff Monetize SDK banner ad load failed."
            }

        adapter.vungleBannerListener.onAdFailedToLoad(vungleBanner, liftoffError)

        val expectedError =
            AdError(
                liftoffError.code,
                liftoffError.errorMessage,
                VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
            )
        verify(mockBannerListener).onAdFailedToLoad(
            eq(adapter),
            argThat(AdErrorMatcher(expectedError))
        )
    }

    @Test
    fun onBannerAdStart_callsNothing() {
        initSuccessAndRequestBannerAd()

        adapter.vungleBannerListener.onAdStart(vungleBanner)

        verifyNoMoreInteractions(mockBannerListener)
    }

    @Test
    fun onBannerAdEnd_callsNothing() {
        initSuccessAndRequestInterstitialAd()

        adapter.vungleInterstitialListener.onAdEnd(vungleBanner)

        verifyNoMoreInteractions(mockBannerListener)
    }

    @Test
    fun onBannerAdClicked_callsOnAdClickedAndAdOpened() {
        initSuccessAndRequestBannerAd()

        adapter.vungleBannerListener.onAdClicked(vungleBanner)

        verify(mockBannerListener).onAdClicked(adapter)
        verify(mockBannerListener).onAdOpened(adapter)
    }

    @Test
    fun onBannerAdLeftApplication_callsOnAdLeftApplication() {
        initSuccessAndRequestBannerAd()

        adapter.vungleBannerListener.onAdLeftApplication(vungleBanner)

        verify(mockBannerListener).onAdLeftApplication(adapter)
    }

    @Test
    fun onBannerAdFailedToPlay_callsNothing() {
        initSuccessAndRequestBannerAd()

        val liftoffSdkError = mock<VungleError> {
            on { code } doReturn VungleError.AD_UNABLE_TO_PLAY
            on { errorMessage } doReturn "Liftoff Monetize SDK banner ad play failed."
        }

        adapter.vungleBannerListener.onAdFailedToPlay(vungleBanner, liftoffSdkError)

        verifyNoMoreInteractions(mockBannerListener)
    }

    @Test
    fun onBannerAdImpression_callsNothing() {
        initSuccessAndRequestBannerAd()

        adapter.vungleBannerListener.onAdImpression(vungleBanner)

        verifyNoMoreInteractions(mockBannerListener)
    }

    @Test
    fun onBannerAdLoaded_callsDestroy() {
        initSuccessAndRequestBannerAd()

        adapter.onDestroy()

        verify(vungleBanner, times(1)).finishAd()
    }

    // endregion
}
