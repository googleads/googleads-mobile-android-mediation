package com.google.android.gms.ads.mediation

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import io.mockk.*
import net.zucks.listener.AdBannerListener
import net.zucks.view.AdBanner
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class ZucksBannerAdapterTest {

    private lateinit var mockAdapter: ZucksAdapter
    private lateinit var listener: AdBannerListener

    @Before
    fun setUp() {
        mockAdapter = mockk(relaxed = true)
        listener = mockk(relaxed = true)
    }

    /**
     * Returns false if AdSize is unsupported and/or invalid.
     */
    @Test
    fun testIsSizeSupported_invalidSize() {
        val adSize = AdSize(Int.MAX_VALUE, Int.MAX_VALUE)

        assertThat(
            ZucksBannerAdapter.isSizeSupported(adSize),
            `is`(false)
        )
    }

    /**
     * Returns error instance if internal state is invalid.
     */
    @Test
    fun testIsValidAdSize_invalidSize() {
        val adSize = AdSize.BANNER
        val banner: AdBanner = mockk {
            every { widthInDp } returns Int.MAX_VALUE
            every { heightInDp } returns Int.MAX_VALUE
        }

        assertThat(adSize.width, `is`(not(banner.widthInDp)))
        assertThat(adSize.height, `is`(not(banner.heightInDp)))

        val error = ZucksBannerAdapter.isValidAdSize(adSize, banner)

        assertThat(error, `is`(not(nullValue())))
    }

    /**
     * Returns null if internal state is valid.
     */
    @Test
    fun testIsValidAdSize_validSize() {
        val adSize = AdSize.BANNER
        val banner: AdBanner = mockk {
            every { widthInDp } returns adSize.width
            every { heightInDp } returns adSize.height
        }

        val error = ZucksBannerAdapter.isValidAdSize(adSize, banner)

        assertThat(error, `is`(nullValue()))
    }

    /**
     * Returns error if params is invalid.
     */
    @Test
    fun testLoadBannerAd_invalidParams() {
        // region `configureBannerAd` returns error if params is invalid.
        val parent: ZucksBannerAdapter = mockk {
            every { configureBannerAd(any(), any(), any(), any()) } returns mockk()
        }

        assertThat(
            parent.configureBannerAd(mockk(), mockk(), mockk(), mockk()),
            `is`(not(nullValue()))
        )
        // endregion

        val target = ZucksBannerAdapter.ZucksMediationBannerAd(parent)
        val callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> = mockk(relaxed = true)

        target.loadBannerAd(mockk(relaxed = true), callback)

        val slot = slot<AdError>()
        verify(exactly = 1) { callback.onFailure(capture(slot)) }
        verify(exactly = 0) { callback.onSuccess(any()) }
        verify(exactly = 0) { callback.onFailure(any<String>()) }
        assertThat(slot.captured, `is`(not(nullValue())))
    }

    /**
     * `onFailure` is not called if params are valid.
     */
    @Test
    fun testLoadBannerAd_validParams() {
        // region `configureBannerAd` returns null if params is valid.
        val parent: ZucksBannerAdapter = mockk {
            every { configureBannerAd(any(), any(), any(), any()) } returns null
        }

        assertThat(parent.configureBannerAd(mockk(), mockk(), mockk(), mockk()), `is`(nullValue()))
        // endregion

        val target = ZucksBannerAdapter.ZucksMediationBannerAd(parent)
        val callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> = mockk(relaxed = true)

        target.loadBannerAd(mockk(relaxed = true), callback)

        verify(exactly = 0) { callback.onFailure(any<AdError>()) }
        verify(exactly = 0) { callback.onFailure(any<String>()) }

        // Succeeded callbacks are not called at this time.
        // `AdBanner#load` runs on `configureBannerAd`.
        verify(exactly = 0) { callback.onSuccess(any()) }
    }

    /**
     * Returns error if `AdSize` is invalid.
     */
    @Test
    fun testZucksMediationBannerAd_onReceiveAd_invalidBannerSize() {
        // region `isValidAdSize` returns non-null if param is invalid.
        val parent: ZucksBannerAdapter = mockk {
            every { isValidAdSize(any()) } returns mockk()
        }

        assertThat(parent.isValidAdSize(mockk()), `is`(not(nullValue())))
        // endregion

        val callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> = mockk(relaxed = true)

        val target = ZucksBannerAdapter.ZucksMediationBannerAd(parent)
        target.setLoadCallback(callback)
        target.listener.onReceiveAd(mockk())

        verify(exactly = 1) { callback.onFailure(any<AdError>()) }
        verify(exactly = 0) { callback.onFailure(any<String>()) }
        verify(exactly = 0) { callback.onSuccess(any()) }
    }

    /**
     * Validate callbacks if parameters is valid.
     */
    @Test
    fun testZucksMediationBannerAd_onReceiveAd_validBannerSize() {
        // region `isValidAdSize` returns null if param is valid.
        val parent: ZucksBannerAdapter = mockk {
            every { isValidAdSize(any()) } returns null
        }

        assertThat(parent.isValidAdSize(mockk()), `is`(nullValue()))
        // endregion

        val adCallback: MediationBannerAdCallback = mockk(relaxed = true)
        val loadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> = mockk(relaxed = true) {
            every { onSuccess(any()) } returns adCallback
        }

        val target = ZucksBannerAdapter.ZucksMediationBannerAd(parent)
        target.setLoadCallback(loadCallback)
        target.listener.onReceiveAd(mockk())

        verify(exactly = 0) { loadCallback.onFailure(any<AdError>()) }
        verify(exactly = 0) { loadCallback.onFailure(any<String>()) }

        verify(exactly = 1) { loadCallback.onSuccess(any()) }
        verify(exactly = 1) { adCallback.reportAdImpression() }
    }

    /**
     * Returns non-null if params are invalid.
     */
    @Test
    fun testRequestBannerAd_invalidParams() {
        // region `configureBannerAd` returns error if params is invalid.
        val parent: ZucksBannerAdapter = mockk {
            every { configureBannerAd(any(), any(), any(), any()) } returns mockk()
        }

        assertThat(parent.configureBannerAd(mockk(), mockk(), mockk(), mockk()), `is`(not(nullValue())))
        // endregion

        val target = ZucksBannerAdapter.ZucksMediationBannerAdapter(parent)

        val callback: MediationBannerListener = mockk(relaxed = true)
        target.requestBannerAd(mockk(), callback, mockk(), mockk(), mockk(), mockk())

        val slot = slot<AdError>()
        verify(exactly = 1) { callback.onAdFailedToLoad(any(), capture(slot)) }
        verify(exactly = 0) { callback.onAdFailedToLoad(any(), any<Int>()) }
        verify(exactly = 0) { callback.onAdLoaded(any()) }

        assertThat(slot.captured, `is`(not(nullValue())))
    }

    /**
     * `onAdFailedToLoad` is not called if params are valid.
     */
    @Test
    fun testRequestBannerAd_validParams() {
        // region `configureBannerAd` returns null if params is valid.
        val parent: ZucksBannerAdapter = mockk {
            every { configureBannerAd(any(), any(), any(), any()) } returns null
        }

        assertThat(parent.configureBannerAd(mockk(), mockk(), mockk(), mockk()), `is`(nullValue()))
        // endregion

        val target = ZucksBannerAdapter.ZucksMediationBannerAdapter(parent)

        val callback: MediationBannerListener = mockk(relaxed = true)
        target.requestBannerAd(mockk(), callback, mockk(), mockk(), mockk(), mockk())

        verify(exactly = 0) { callback.onAdFailedToLoad(any(), any<AdError>()) }
        verify(exactly = 0) { callback.onAdFailedToLoad(any(), any<Int>()) }

        // Succeeded callbacks are not called at this time.
        // `AdBanner#load` runs on `configureBannerAd`.
        verify(exactly = 0) { callback.onAdLoaded(any()) }
    }

    /**
     * Returns non-null if params are invalid.
     */
    @Test
    fun testZucksMediationBannerAdapter_onReceiveAd_invalidBannerSize() {
        // region `isValidAdSize` returns non-null if param is invalid.
        val parent: ZucksBannerAdapter = mockk {
            every { isValidAdSize(any()) } returns mockk()
        }

        assertThat(parent.isValidAdSize(mockk()), `is`(not(nullValue())))
        // endregion

        val callback: MediationBannerListener = mockk(relaxed = true)

        val target = ZucksBannerAdapter.ZucksMediationBannerAdapter(parent)
        target.setCallback(callback)
        target.listener.onReceiveAd(mockk())

        verify(exactly = 1) { callback.onAdFailedToLoad(any(), any<AdError>()) }
        verify(exactly = 0) { callback.onAdFailedToLoad(any(), any<Int>()) }
        verify(exactly = 0) { callback.onAdLoaded(any()) }
    }

    /**
     * Validate callbacks if parameters is valid.
     */
    @Test
    fun testZucksMediationBannerAdapter_onReceiveAd_validBannerSize() {
        // region `isValidAdSize` returns null if params is valid.
        val parent: ZucksBannerAdapter = mockk {
            every { isValidAdSize(any()) } returns null
        }

        assertThat(parent.isValidAdSize(mockk()), `is`(nullValue()))
        // endregion

        val callback: MediationBannerListener = mockk(relaxed = true)

        val target = ZucksBannerAdapter.ZucksMediationBannerAdapter(parent)
        target.setCallback(callback)
        target.listener.onReceiveAd(mockk())

        verify(exactly = 1) { callback.onAdLoaded(any()) }
        verify(exactly = 0) { callback.onAdFailedToLoad(any(), any<AdError>()) }
        verify(exactly = 0) { callback.onAdFailedToLoad(any(), any<Int>()) }
    }

}
