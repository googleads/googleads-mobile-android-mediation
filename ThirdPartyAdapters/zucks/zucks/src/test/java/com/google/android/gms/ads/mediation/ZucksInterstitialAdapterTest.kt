package com.google.android.gms.ads.mediation

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class ZucksInterstitialAdapterTest {

    private lateinit var zucksAdapter: ZucksAdapter

    @Before
    fun setUp() {
        zucksAdapter = mockk(relaxed = true)
    }

    /**
     * Returns error if passed Context is not Activity.
     */
    @Test
    fun testConfigureInterstitialAd_nonActivityContext() {
        val context: Context = mockk()
        assertThat(context, `is`(not(instanceOf(Activity::class.java))))

        val error = ZucksInterstitialAdapter(zucksAdapter).configureInterstitialAd(
            context,
            mockk(),
            mockk(),
            mockk()
        )

        assertThat(error, not(nullValue()))
    }

    /**
     * Returns error if params is invalid.
     */
    @Test
    fun testLoadInterstitialAd_invalidParams() {
        // region `configureInterstitialAd` returns non-null if params are invalid.
        val parent: ZucksInterstitialAdapter = mockk {
            every { configureInterstitialAd(any(), any()) } returns mockk()
        }
        assertThat(parent.configureInterstitialAd(mockk(), mockk()), `is`(not(nullValue())))
        // endregion


        val target = ZucksInterstitialAdapter.ZucksMediationInterstitialAd(parent)

        val callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> = mockk(relaxed = true)
        target.loadInterstitialAd(mockk(), callback)

        verify(exactly = 1) { callback.onFailure(any<AdError>()) }
        verify(exactly = 0) { callback.onFailure(any<String>()) }
        verify(exactly = 0) { callback.onSuccess(any()) }
    }

    /**
     * Validate callbacks if params is valid.
     */
    @Test
    fun testLoadInterstitialAd_validParams() {
        // region `configureInterstitialAd` returns null if params are valid.
        val parent: ZucksInterstitialAdapter = mockk {
            every { configureInterstitialAd(any(), any()) } returns null
        }
        assertThat(parent.configureInterstitialAd(mockk(), mockk()), `is`(nullValue()))
        // endregion

        val target = ZucksInterstitialAdapter.ZucksMediationInterstitialAd(parent)

        val callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> = mockk(relaxed = true)
        target.loadInterstitialAd(mockk(), callback)

        verify(exactly = 0) { callback.onFailure(any<AdError>()) }
        verify(exactly = 0) { callback.onFailure(any<String>()) }

        // Succeeded callbacks are not called at this time.
        // `AdInterstitial#load` runs on `configureInterstitialAd`.
        verify(exactly = 0) { callback.onSuccess(any()) }
    }

}
