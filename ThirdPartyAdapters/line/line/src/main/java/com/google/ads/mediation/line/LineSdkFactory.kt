// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.line

import android.app.Activity
import android.content.Context
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdInterstitial
import com.five_corp.ad.FiveAdNative
import com.five_corp.ad.FiveAdVideoReward
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Wrapper singleton to enable mocking of [FiveAd] different ad formats for unit testing.
 *
 * **Note:** It is used as a layer between the Line Adapter's and the FiveAd SDK. It is required to
 * use this class instead of calling the FiveAd SDK methods directly.
 */
object LineSdkFactory {

  private const val MIN_NUMBER_GENERIC_WORKERS = 2
  private const val MAX_NUMBER_GENERIC_WORKERS = Integer.MAX_VALUE
  private val THREAD_KEEP_ALIVE_TIME = 10.seconds

  /** Delegate used on unit tests to help mock calls to create [FiveAd] formats. */
  internal var delegate: SdkFactory =
    object : SdkFactory {
      override fun createFiveAdConfig(appId: String): FiveAdConfig = FiveAdConfig(appId)

      override fun createFiveAdCustomLayout(
        context: Context,
        slotId: String,
        width: Int,
      ): FiveAdCustomLayout = FiveAdCustomLayout(context, slotId, width)

      override fun createFiveAdInterstitial(
        activity: Activity,
        slotId: String,
      ): FiveAdInterstitial = FiveAdInterstitial(activity, slotId)

      override fun createFiveVideoRewarded(activity: Activity, slotId: String): FiveAdVideoReward =
        FiveAdVideoReward(activity, slotId)

      override fun createFiveAdNative(context: Context, slotId: String) =
        FiveAdNative(context, slotId)
    }

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
      newThreadFactory("BG")
    )
}

/** Declares the methods that will invoke the [FiveAd] SDK */
interface SdkFactory {
  fun createFiveAdConfig(appId: String): FiveAdConfig

  fun createFiveAdCustomLayout(context: Context, slotId: String, width: Int): FiveAdCustomLayout

  fun createFiveAdInterstitial(activity: Activity, slotId: String): FiveAdInterstitial

  fun createFiveVideoRewarded(activity: Activity, slotId: String): FiveAdVideoReward

  fun createFiveAdNative(context: Context, slotId: String): FiveAdNative
}
