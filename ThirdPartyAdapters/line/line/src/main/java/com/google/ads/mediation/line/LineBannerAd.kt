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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterface
import com.five_corp.ad.FiveAdLoadListener
import com.five_corp.ad.FiveAdViewEventListener
import com.google.ads.mediation.line.LineExtras.Companion.KEY_ENABLE_AD_SOUND
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MediationUtils
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import kotlin.math.roundToInt

/**
 * Used to load Line banner ads and mediate callbacks between Google Mobile Ads SDK and FiveAd SDK.
 */
class LineBannerAd
private constructor(
  private val context: Context,
  private val appId: String,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val adView: FiveAdCustomLayout,
  private val adSize: AdSize,
  private val networkExtras: Bundle?,
) : MediationBannerAd, FiveAdLoadListener, FiveAdViewEventListener {

  private var mediationBannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd() {
    LineInitializer.initialize(context, appId)
    adView.setLoadListener(this)
    if (networkExtras != null) {
      adView.enableSound(networkExtras.getBoolean(KEY_ENABLE_AD_SOUND, false))
    }
    adView.loadAdAsync()
  }

  override fun getView(): View = adView

  override fun onFiveAdLoad(ad: FiveAdInterface) {
    Log.d(TAG, "Finished loading Line Banner Ad for slotId: ${ad.slotId}")
    val loadedAd = ad as? FiveAdCustomLayout
    loadedAd?.let {
      // Transforming ad size from pixels to dips
      val density = context.resources.displayMetrics.density
      val returnedAdSize =
        AdSize((it.logicalWidth / density).roundToInt(), (it.logicalHeight / density).roundToInt())
      Log.d(
        TAG,
        "Received Banner Ad dimensions: ${returnedAdSize.width} x ${returnedAdSize.height}"
      )
      val closestSize = MediationUtils.findClosestSize(context, adSize, listOf(returnedAdSize))
      if (closestSize == null) {
        val logMessage =
          ERROR_MSG_MISMATCH_AD_SIZE.format(
            adSize.width,
            adSize.height,
            it.logicalWidth,
            it.logicalHeight
          )
        Log.w(TAG, logMessage)
        val adError =
          AdError(
            ERROR_CODE_MISMATCH_AD_SIZE,
            logMessage,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN
          )
        mediationAdLoadCallback.onFailure(adError)
        return
      }
    }
    adView.setViewEventListener(this)
    mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onFiveAdLoadError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    val adError =
      AdError(
        errorCode.value,
        String.format(LineMediationAdapter.ERROR_MSG_AD_LOADING, errorCode.name),
        LineMediationAdapter.SDK_ERROR_DOMAIN
      )
    Log.w(TAG, adError.message)
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onFiveAdViewError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    Log.w(TAG, "There was an error displaying the ad.")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when banner is clicked */
  override fun onFiveAdClick(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad did record a click.")
    mediationBannerAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  override fun onFiveAdClose(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad closed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when the loaded banner ad is a video and it starts playing. */
  override fun onFiveAdStart(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad start")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /**
   * Called when the loaded banner ad is a video, it hasn't finished and the app moves out from the
   * Foreground.
   */
  override fun onFiveAdPause(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad paused")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when the loaded banner ad is a video and the app moves into the Foreground. */
  override fun onFiveAdResume(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad resumed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when the loaded banner ad is a video and it finishes playing it completely. */
  override fun onFiveAdViewThrough(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad viewed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /**
   * Called when the loaded banner ad is a video, it has finished playing and the replay button is
   * clicked to play again. onFiveAdStart is not called.
   */
  override fun onFiveAdReplay(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad replayed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when a new banner ad appears. */
  override fun onFiveAdImpression(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad recorded an impression.")
    mediationBannerAdCallback?.reportAdImpression()
  }

  override fun onFiveAdStall(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad stalled")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdRecover(ad: FiveAdInterface) {
    Log.d(TAG, "Line banner ad recovered")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  companion object {
    private val TAG = LineBannerAd::class.simpleName
    const val ERROR_CODE_MISMATCH_AD_SIZE = 103
    const val ERROR_MSG_MISMATCH_AD_SIZE =
      "Unexpected ad size loaded. Expected %sx%s but received %sx%s."

    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
    ): Result<LineBannerAd> {
      val context = mediationBannerAdConfiguration.context
      val serverParameters = mediationBannerAdConfiguration.serverParameters
      val adSize = mediationBannerAdConfiguration.adSize

      val appId = serverParameters.getString(LineMediationAdapter.KEY_APP_ID)
      if (appId.isNullOrEmpty()) {
        val adError =
          AdError(
            LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
            LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val slotId = serverParameters.getString(LineMediationAdapter.KEY_SLOT_ID)
      if (slotId.isNullOrEmpty()) {
        val adError =
          AdError(
            LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
            LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }
      // FiveAd SDK requires the size of the banner given in pixels.
      val bannerAdView =
        LineSdkFactory.delegate.createFiveAdCustomLayout(
          context,
          slotId,
          adSize.getWidthInPixels(context)
        )
      return Result.success(
        LineBannerAd(
          context,
          appId,
          mediationAdLoadCallback,
          bannerAdView,
          adSize,
          mediationBannerAdConfiguration.mediationExtras,
        )
      )
    }
  }
}
