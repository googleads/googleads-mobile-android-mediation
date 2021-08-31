package com.google.android.gms.ads.mediation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import io.mockk.*
import net.zucks.listener.AdBannerListener
import net.zucks.view.AdBanner
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class ZucksBannerAdapterTest {

    private lateinit var mockAdapter: ZucksAdapter
    private lateinit var listener: AdBannerListener

    @Before
    fun setUp() {
        mockAdapter = mockk(relaxed = true)
        listener = mockk(relaxed = true)
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

}

@RunWith(RobolectricTestRunner::class)
class ZucksBannerAdapterTest_Robolectric {

    private val context by lazy { ApplicationProvider.getApplicationContext<Context>() }

    /**
     * Returns false if AdSize is unsupported and/or invalid.
     */
    @Test
    fun testIsSizeSupported_invalidSize() {
        val adSize = AdSize(Int.MAX_VALUE, Int.MAX_VALUE)

        assertThat(
            ZucksBannerAdapter.isSizeSupported(context, adSize),
            `is`(false)
        )
    }

    /**
     * Returns true if AdSize is supported.
     */
    @Test
    fun testIsSizeSupported_validSize() {
        val adSize = AdSize.BANNER

        assertThat(
            ZucksBannerAdapter.isSizeSupported(context, adSize),
            `is`(true)
        )
    }

}
