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
import com.five_corp.ad.AdLoader
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdCustomLayoutEventListener
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterface
import com.five_corp.ad.FiveAdLoadListener
import com.google.ads.mediation.line.LineExtras.Companion.KEY_ENABLE_AD_SOUND
import com.google.ads.mediation.line.LineMediationAdapter.Companion.SDK_ERROR_DOMAIN
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
  private val slotId: String?,
  private val bidResponse: String,
  private val watermark: String,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  private val adSize: AdSize,
  private val networkExtras: Bundle?,
) : MediationBannerAd, FiveAdLoadListener, FiveAdCustomLayoutEventListener {

  private var mediationBannerAdCallback: MediationBannerAdCallback? = null
  private lateinit var adView: FiveAdCustomLayout

  fun loadAd() {
    if (slotId.isNullOrEmpty()) {
      val adError =
        AdError(
          LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
          LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
          LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
        )
      mediationAdLoadCallback.onFailure(adError)
      return
    }
    LineInitializer.initialize(context, appId)
    // FiveAd SDK requires the size of the banner given in pixels.
    adView =
      LineSdkFactory.delegate.createFiveAdCustomLayout(
        context,
        slotId,
        adSize.getWidthInPixels(context),
      )
    adView.setLoadListener(this)
    if (networkExtras != null) {
      adView.enableSound(networkExtras.getBoolean(KEY_ENABLE_AD_SOUND, false))
    }
    adView.loadAdAsync()
  }

  fun loadRtbAd() {
    val fiveAdConfig = FiveAdConfig(appId)
    val adLoader = AdLoader.forConfig(context, fiveAdConfig) ?: return
    val bidData = BidData(bidResponse, watermark)
    adLoader.loadBannerAd(
      bidData,
      object : AdLoader.LoadBannerAdCallback {
        override fun onLoad(fiveAdCustomLayout: FiveAdCustomLayout) {
          adView = fiveAdCustomLayout
          if (networkExtras != null) {
            adView.enableSound(networkExtras.getBoolean(KEY_ENABLE_AD_SOUND, false))
          }
          adView.setEventListener(this@LineBannerAd)
          mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this@LineBannerAd)
        }

        override fun onError(adErrorCode: FiveAdErrorCode) {
          val adError = AdError(adErrorCode.value, adErrorCode.name, SDK_ERROR_DOMAIN)
          mediationAdLoadCallback.onFailure(adError)
        }
      },
    )
  }

  override fun getView(): View = adView

  override fun onFiveAdLoad(ad: FiveAdInterface) {
    // This callback is not used in the RTB flow.
    Log.d(TAG, "Finished loading Line Banner Ad for slotId: ${ad.slotId}")
    val loadedAd = ad as? FiveAdCustomLayout
    loadedAd?.let {
      // Transforming ad size from pixels to dips
      val density = context.resources.displayMetrics.density
      val returnedAdSize =
        AdSize((it.logicalWidth / density).roundToInt(), (it.logicalHeight / density).roundToInt())
      Log.d(
        TAG,
        "Received Banner Ad dimensions: ${returnedAdSize.width} x ${returnedAdSize.height}",
      )
      val closestSize = MediationUtils.findClosestSize(context, adSize, listOf(returnedAdSize))
      if (closestSize == null) {
        val logMessage =
          ERROR_MSG_MISMATCH_AD_SIZE.format(
            adSize.width,
            adSize.height,
            it.logicalWidth,
            it.logicalHeight,
          )
        Log.w(TAG, logMessage)
        val adError =
          AdError(
            ERROR_CODE_MISMATCH_AD_SIZE,
            logMessage,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(adError)
        return
      }
    }
    adView.setEventListener(this)
    mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onFiveAdLoadError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    // This callback is not used in the RTB flow.
    val adError =
      AdError(
        errorCode.value,
        String.format(LineMediationAdapter.ERROR_MSG_AD_LOADING, errorCode.name),
        SDK_ERROR_DOMAIN,
      )
    Log.w(TAG, adError.message)
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onViewError(
    fiveAdCustomLayout: FiveAdCustomLayout,
    fiveAdErrorCode: FiveAdErrorCode,
  ) {
    Log.w(TAG, "There was an error displaying the ad.")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when banner is clicked */
  override fun onClick(fiveAdCustomLayout: FiveAdCustomLayout) {
    Log.d(TAG, "Line banner ad did record a click.")
    mediationBannerAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  override fun onRemove(fiveAdCustomLayout: FiveAdCustomLayout) {
    Log.d(TAG, "Line banner ad removed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when the loaded banner ad is a video and it starts playing. */
  override fun onPlay(fiveAdCustomLayout: FiveAdCustomLayout) {
    Log.d(TAG, "Line banner ad played")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /**
   * Called when the loaded banner ad is a video, it hasn't finished and the app moves out from the
   * Foreground.
   */
  override fun onPause(fiveAdCustomLayout: FiveAdCustomLayout) {
    Log.d(TAG, "Line banner ad paused")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when the loaded banner ad is a video and it finishes playing it completely. */
  override fun onViewThrough(fiveAdCustomLayout: FiveAdCustomLayout) {
    Log.d(TAG, "Line banner ad viewed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  /** Called when a new banner ad appears. */
  override fun onImpression(fiveAdCustomLayout: FiveAdCustomLayout) {
    Log.d(TAG, "Line banner ad recorded an impression.")
    mediationBannerAdCallback?.reportAdImpression()
  }

  companion object {
    private val TAG = LineBannerAd::class.simpleName
    const val ERROR_CODE_MISMATCH_AD_SIZE = 103
    const val ERROR_MSG_MISMATCH_AD_SIZE =
      "Unexpected ad size loaded. Expected %sx%s but received %sx%s."

    fun newInstance(
      mediationBannerAdConfiguration: MediationBannerAdConfiguration,
      mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
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
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN,
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val slotId = serverParameters.getString(LineMediationAdapter.KEY_SLOT_ID)
      val bidResponse = mediationBannerAdConfiguration.bidResponse
      val watermark = mediationBannerAdConfiguration.watermark

      return Result.success(
        LineBannerAd(
          context,
          appId,
          slotId,
          bidResponse,
          watermark,
          mediationAdLoadCallback,
          adSize,
          mediationBannerAdConfiguration.mediationExtras,
        )
      )
    }
  }
}
