// Copyright 2024 Google LLC
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

package com.google.ads.mediation.moloco

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.NativeAd

/**
 * Used to load Moloco native ads and mediate callbacks between Google Mobile Ads SDK and Moloco
 * SDK.
 */
class MolocoNativeAd
private constructor(
  private val adUnitId: String,
  private val nativeAdOptions: NativeAdOptions, // TODO: Not sure where to use this?
  private val bidResponse: String,
  private val watermark: String,
  private val mediationNativeAdLoadCallback:
  MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
) : AdLoad.Listener, UnifiedNativeAdMapper() {
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal var nativeAd: NativeAd? = null

  fun loadAd() {
    Moloco.createNativeAd(adUnitId, watermark) { returnedAd, adCreateError ->
      if (adCreateError != null) {
        val adError =
          AdError(
            adCreateError.errorCode,
            adCreateError.description,
            MolocoMediationAdapter.SDK_ERROR_DOMAIN,
          )
        mediationNativeAdLoadCallback.onFailure(adError)
        return@createNativeAd
      }

      nativeAd = returnedAd

      // Now that the ad object is created, load the bid response
      nativeAd?.load(bidResponse, this)
    }
  }

  override fun onAdLoadSuccess(molocoAd: MolocoAd) {
    overrideClickHandling = true
    // If nativeAd is null here, then that means the ad was destroyed before load was successful or there is a bug
    // in the adapter
    nativeAd?.apply {
      assets?.apply {
        // Admob first uses rating, if not present the it uses sponsorText and if that is not present it will use the store.
        rating?.let { starRating = it.toDouble() }
        sponsorText?.let { advertiser = it }
        store = "Google Play"
        title?.let { headline = it }
        description?.let { body = it }
        callToActionText?.let { callToAction = it }
        iconUri?.let {
          Drawable.createFromPath(it.toString())?.apply {
            icon = MolocoNativeMappedImage(this)
          }
        }

        val mediaView = this.mediaView

        mediaView?.let {
          it.tag = MEDIA_VIEW_TAG
          setMediaView(it)
        }
      }
    }

    val showCallback = mediationNativeAdLoadCallback.onSuccess(this)
    nativeAd?.interactionListener = object : NativeAd.InteractionListener {
      /**
       * Not needed as Admob handles impressions on its own.
       */
      override fun onImpressionHandled() {}

      /**
       * When this Moloco function gets triggered, we inform Admob that ad click has occurred
       */
      override fun onGeneralClickHandled() = showCallback.reportAdClicked()
    }
  }

  override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
    val adError =
      AdError(
        molocoAdError.errorType.errorCode,
        molocoAdError.errorType.description,
        MolocoMediationAdapter.SDK_ERROR_DOMAIN,
      )
    mediationNativeAdLoadCallback.onFailure(adError)
  }

  /**
   * Admob informs us that a click has happened to the view(s) they create.
   * This excludes the view set in [setMediaView]. Click from said view must be handled elsewhere
   */
  override fun handleClick(view: View) {
    nativeAd?.handleGeneralAdClick()
  }

  /**
   * Admob informs us that an impression happened
   */
  override fun recordImpression() {
    nativeAd?.handleImpression()
  }

  override fun trackViews(
    containerView: View,
    clickableAssetViews: MutableMap<String, View>,
    nonClickableAssetViews: MutableMap<String, View>,
  ) {
    // set the listener to Moloco's NativeAd object. Then we read it from the Moloco's `NativeAd.interactionListener` callback
    containerView.setOnClickListener { nativeAd?.handleGeneralAdClick() }
    clickableAssetViews.values.forEach {
      it.setOnClickListener { nativeAd?.handleGeneralAdClick() }
    }
  }

  /**
   * To be called by the medation to destroy the ad object and any underlying references in the Moloco SDK. This should be called
   * when the ad is permanently "hidden" / never going to be visible again.
   */
  fun destroy() {
    nativeAd?.destroy()
    nativeAd = null
  }

  companion object {
    fun newInstance(
      mediationNativeAdConfiguration: MediationNativeAdConfiguration,
      mediationNativeAdLoadCallback:
      MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
    ): Result<MolocoNativeAd> {
      val context = mediationNativeAdConfiguration.context
      val serverParameters = mediationNativeAdConfiguration.serverParameters
      val nativeAdOptions = mediationNativeAdConfiguration.nativeAdOptions

      val adUnitId = serverParameters.getString(MolocoMediationAdapter.KEY_AD_UNIT_ID)
      if (adUnitId.isNullOrEmpty()) {
        val adError =
          AdError(
            MolocoMediationAdapter.ERROR_CODE_MISSING_AD_UNIT,
            MolocoMediationAdapter.ERROR_MSG_MISSING_AD_UNIT,
            MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
          )
        mediationNativeAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val bidResponse = mediationNativeAdConfiguration.bidResponse
      val watermark = mediationNativeAdConfiguration.watermark

      return Result.success(MolocoNativeAd(adUnitId, nativeAdOptions, bidResponse, watermark, mediationNativeAdLoadCallback))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    const val MEDIA_VIEW_TAG = "native_ad_media_view"
  }

  /**
   * Admob adapter only needs the [drawable] parameter. The rest are optional
   */
  @Suppress("DEPRECATION")
  internal class MolocoNativeMappedImage(
    private val drawable: Drawable,
    private val uri: Uri = Uri.EMPTY,
    private val scale: Double = 1.0,
  ) : com.google.android.gms.ads.formats.NativeAd.Image() { // Google deprecated the class, but didn't offer an alternative. So for now we *must* use the deprecated class.
    override fun getScale() = scale
    override fun getDrawable() = drawable
    override fun getUri() = uri
  }
}
