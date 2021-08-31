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

        val callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> = mockk(relaxed = true)

        ZucksInterstitialAdapter(
            zucksAdapter,
            context,
            mockk(),
            mockk(),
            callback
        ).loadInterstitialAd()

        verify(exactly = 1) { callback.onFailure(any<AdError>()) }
    }

}
