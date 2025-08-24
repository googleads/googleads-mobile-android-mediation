package com.google.ads.mediation.pubmatic

import android.content.Context
import com.pubmatic.sdk.nativead.POBNativeAdLoader
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial
import com.pubmatic.sdk.rewardedad.POBRewardedAd
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/** Interface for factory to create PubMatic ad objects. */
interface PubMaticAdFactory {
  fun createPOBInterstitial(context: Context): POBInterstitial

  fun createPOBInterstitial(
    context: Context,
    pubId: String,
    profileId: Int,
    adUnit: String,
  ): POBInterstitial

  fun createPOBRewardedAd(context: Context): POBRewardedAd

  fun createPOBBannerView(context: Context): POBBannerView

  fun createPOBNativeAdLoader(context: Context): POBNativeAdLoader

  companion object {
    private const val MIN_NUMBER_GENERIC_WORKERS = 2
    private const val MAX_NUMBER_GENERIC_WORKERS = Integer.MAX_VALUE
    private val THREAD_KEEP_ALIVE_TIME = 10.seconds

    private fun newThreadFactory(poolName: String) =
      object : ThreadFactory {
        private val threadId = AtomicInteger(1)

        override fun newThread(runnable: Runnable): Thread {
          return Thread(runnable, "GMA-Mediation($poolName) ${threadId.getAndIncrement()}")
        }
      }

    internal val BACKGROUND_EXECUTOR =
      ThreadPoolExecutor(
        MIN_NUMBER_GENERIC_WORKERS,
        MAX_NUMBER_GENERIC_WORKERS,
        THREAD_KEEP_ALIVE_TIME.inWholeSeconds,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        newThreadFactory("BG"),
      )
  }
}
