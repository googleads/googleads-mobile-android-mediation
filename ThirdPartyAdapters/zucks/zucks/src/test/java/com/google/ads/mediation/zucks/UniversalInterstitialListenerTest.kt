package com.google.ads.mediation.zucks

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException

class UniversalInterstitialListenerTest {

    private lateinit var callback: UniversalInterstitialListener.Callback

    @Before
    fun setUp() {
        callback = mockk(relaxed = true)
    }

    @Test
    fun testInterstitial() {
        val target = UniversalInterstitialListener.Interstitial(callback)

        target.use().onReceiveAd()
        target.use().onShowAd()
        target.use().onCancelDisplayRate()
        target.use().onTapAd()
        target.use().onCloseAd()

        verify(exactly = 1) { callback.onReceiveAd() }
        verify(exactly = 1) { callback.onShowAd() }
        verify(exactly = 1) { callback.onCancelDisplayRate() }
        verify(exactly = 1) { callback.onCloseAd() }
        verify(exactly = 1) { callback.onTapAd() }

        val e = RuntimeException("foo")

        target.use().onLoadFailure(e)
        target.use().onShowFailure(e)

        verify(exactly = 1) { callback.onLoadFailure(e) }
        verify(exactly = 1) { callback.onShowFailure(e) }
    }

    @Test
    fun testFullscreenInterstitial() {
        val target = UniversalInterstitialListener.FullscreenInterstitial(callback)

        target.use().onShowAd()
        target.use().onCancelDisplayRate()
        target.use().onTapAd()
        target.use().onCloseAd()
        target.use().onReceiveAd()

        verify(exactly = 1) { callback.onReceiveAd() }
        verify(exactly = 1) { callback.onShowAd() }
        verify(exactly = 1) { callback.onCancelDisplayRate() }
        verify(exactly = 1) { callback.onCloseAd() }
        verify(exactly = 1) { callback.onTapAd() }

        val e = RuntimeException("foo")

        target.use().onLoadFailure(e)
        target.use().onShowFailure(e)

        verify(exactly = 1) { callback.onLoadFailure(e) }
        verify(exactly = 1) { callback.onShowFailure(e) }
    }

}
