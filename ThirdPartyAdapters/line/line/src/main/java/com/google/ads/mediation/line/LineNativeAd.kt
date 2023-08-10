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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterface
import com.five_corp.ad.FiveAdLoadListener
import com.five_corp.ad.FiveAdNative
import com.five_corp.ad.FiveAdViewEventListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.formats.NativeAd
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Used to load Line native ads and mediate callbacks between Google Mobile Ads SDK and FiveAd SDK.
 */
class LineNativeAd
private constructor(
  private val context: Context,
  private val appId: String,
  private val mediationNativeAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  private val nativeAd: FiveAdNative,
  private val adapterScope: CoroutineScope,
) : UnifiedNativeAdMapper(), FiveAdLoadListener, FiveAdViewEventListener {

  private var mediationNativeAdCallback: MediationNativeAdCallback? = null

  fun loadAd() {
    LineInitializer.initialize(context, appId)
    nativeAd.setLoadListener(this)
    nativeAd.loadAdAsync()
  }

  private suspend fun mapNativeAd() = coroutineScope {
    headline = nativeAd.adTitle
    body = nativeAd.descriptionText
    callToAction = nativeAd.buttonText
    setMediaView(nativeAd.adMainView)
    advertiser = nativeAd.advertiserName

    val requiredImagesLoaded = loadImages()
    if (!requiredImagesLoaded) {
      val adError =
        AdError(
          LineMediationAdapter.ERROR_CODE_MINIMUM_NATIVE_INFO_NOT_RECEIVED,
          LineMediationAdapter.ERROR_MSG_MINIMUM_NATIVE_INFO_NOT_RECEIVED,
          LineMediationAdapter.SDK_ERROR_DOMAIN
        )
      Log.w(TAG, adError.message)
      mediationNativeAdLoadCallback.onFailure(adError)
    }
  }

  private suspend fun loadImages() = suspendCancellableCoroutine { continuation ->
    nativeAd.loadIconImageAsync { image ->
      if (image != null) {
        icon = LineNativeImage(image.toDrawable(context.resources))
      }
    }
    nativeAd.loadInformationIconImageAsync { image ->
      if (image != null) {
        val informationIcon = ImageView(context)
        informationIcon.setImageBitmap(image)
        adChoicesContent = informationIcon
        continuation.resume(true)
      } else {
        // Native Ad loaded should not continue if Information Icon is not correctly received.
        continuation.resume(false)
      }
    }
  }

  override fun onFiveAdLoad(ad: FiveAdInterface) {
    Log.d(TAG, "Finished loading Line Native Ad for slotId: ${ad.slotId}")
    nativeAd.setViewEventListener(this)
    adapterScope.async {
      mapNativeAd()
      mediationNativeAdCallback = mediationNativeAdLoadCallback.onSuccess(this@LineNativeAd)
    }
  }

  override fun onFiveAdLoadError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    adapterScope.cancel()
    val adError =
      AdError(
        errorCode.value,
        LineMediationAdapter.ERROR_MSG_AD_LOADING.format(errorCode.name),
        LineMediationAdapter.SDK_ERROR_DOMAIN
      )
    Log.w(TAG, adError.message)
    mediationNativeAdLoadCallback.onFailure(adError)
  }

  override fun onFiveAdViewError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    Log.w(TAG, "There was an error displaying the ad.")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdClick(ad: FiveAdInterface) {
    Log.d(TAG, "Line native ad did record a click.")
    mediationNativeAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  override fun onFiveAdClose(ad: FiveAdInterface) {
    Log.d(TAG, "Line native ad closed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdStart(ad: FiveAdInterface) {
    Log.d(TAG, "Line video native ad start")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdPause(ad: FiveAdInterface) {
    Log.d(TAG, "Line video native ad paused")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdResume(ad: FiveAdInterface) {
    Log.d(TAG, "Line video native ad resumed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdViewThrough(ad: FiveAdInterface) {
    Log.d(TAG, "Line video native ad viewed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdReplay(ad: FiveAdInterface) {
    Log.d(TAG, "Line video native ad replayed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdImpression(ad: FiveAdInterface) {
    Log.d(TAG, "Line native ad recorded an impression.")
    mediationNativeAdCallback?.reportAdImpression()
  }

  override fun onFiveAdStall(ad: FiveAdInterface) {
    Log.d(TAG, "Line native ad stalled")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdRecover(ad: FiveAdInterface) {
    Log.d(TAG, "Line native ad recovered")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  internal class LineNativeImage constructor(private val drawable: Drawable) : NativeAd.Image() {

    override fun getScale(): Double = 1.0

    override fun getDrawable(): Drawable = drawable

    override fun getUri(): Uri = Uri.EMPTY
  }

  companion object {
    private val TAG = LineNativeAd::class.simpleName

    fun newInstance(
      mediationNativeAdConfiguration: MediationNativeAdConfiguration,
      mediationNativeAdLoadCallback:
        MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
      coroutineContext: CoroutineContext =
        LineSdkFactory.BACKGROUND_EXECUTOR.asCoroutineDispatcher()
    ): Result<LineNativeAd> {
      val context = mediationNativeAdConfiguration.context
      val serverParameters = mediationNativeAdConfiguration.serverParameters

      val appId = serverParameters.getString(LineMediationAdapter.KEY_APP_ID)
      if (appId.isNullOrEmpty()) {
        val adError =
          AdError(
            LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
            LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN
          )
        mediationNativeAdLoadCallback.onFailure(adError)
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
        mediationNativeAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val nativeAd = LineSdkFactory.delegate.createFiveAdNative(context, slotId)
      val nativeAdOptions = mediationNativeAdConfiguration.nativeAdOptions
      val videoOptions = nativeAdOptions.videoOptions
      if (videoOptions != null) {
        nativeAd.enableSound(!videoOptions.startMuted)
      }

      val adapterScope = CoroutineScope(coroutineContext)

      val instance =
        LineNativeAd(context, appId, mediationNativeAdLoadCallback, nativeAd, adapterScope)
      return Result.success(instance)
    }
  }
}
