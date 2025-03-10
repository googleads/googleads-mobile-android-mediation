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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
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
  private val bidResponse: String,
  private val watermark: String,
  private val mediationNativeAdLoadCallback:
  MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
) : NativeAdMapper() {
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal var nativeAd: NativeAd? = null

  fun loadAd() {
    Moloco.createNativeAd(adUnitId, watermark) { returnedAd, adCreateError ->
      if (returnedAd == null) {
        val adError = if (adCreateError != null) {
            AdError(
              adCreateError.errorCode,
              adCreateError.description,
              MolocoMediationAdapter.SDK_ERROR_DOMAIN,
            )
        } else {
          AdError(
            MolocoMediationAdapter.ERROR_CODE_AD_IS_NULL,
            MolocoMediationAdapter.ERROR_MSG_AD_IS_NULL,
            MolocoMediationAdapter.ADAPTER_ERROR_DOMAIN,
          )
        }

        mediationNativeAdLoadCallback.onFailure(adError)
        return@createNativeAd
      }

      nativeAd = returnedAd

      val loadListener = object : AdLoad.Listener {
        override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
          val adError =
            AdError(
              molocoAdError.errorType.errorCode,
              molocoAdError.errorType.description,
              MolocoMediationAdapter.SDK_ERROR_DOMAIN,
            )
          mediationNativeAdLoadCallback.onFailure(adError)
        }

        override fun onAdLoadSuccess(molocoAd: MolocoAd) {
          overrideClickHandling = true
          nativeAd?.apply {
            assets?.apply {
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

          val showCallback = mediationNativeAdLoadCallback.onSuccess(this@MolocoNativeAd)
          nativeAd?.interactionListener = object : NativeAd.InteractionListener {
            override fun onImpressionHandled() {}

            override fun onGeneralClickHandled() = showCallback.reportAdClicked()
          }
        }

      }

      nativeAd?.load(bidResponse, loadListener)
    }
  }

  override fun handleClick(view: View) {
    nativeAd?.handleGeneralAdClick()
  }

  override fun recordImpression() {
    nativeAd?.handleImpression()
  }

  override fun trackViews(
    containerView: View,
    clickableAssetViews: MutableMap<String, View>,
    nonClickableAssetViews: MutableMap<String, View>,
  ) {
    containerView.setOnClickListener { nativeAd?.handleGeneralAdClick() }
    clickableAssetViews.values.forEach {
      it.setOnClickListener { nativeAd?.handleGeneralAdClick() }
    }
  }

  fun destroy() {
    nativeAd?.destroy()
    nativeAd = null
  }

  companion object {
    fun newInstance(
      mediationNativeAdConfiguration: MediationNativeAdConfiguration,
      mediationNativeAdLoadCallback:
      MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
    ): Result<MolocoNativeAd> {
      val serverParameters = mediationNativeAdConfiguration.serverParameters

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

      return Result.success(MolocoNativeAd(adUnitId, bidResponse, watermark, mediationNativeAdLoadCallback))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    const val MEDIA_VIEW_TAG = "native_ad_media_view"
  }

  internal class MolocoNativeMappedImage(
    private val drawable: Drawable,
    private val uri: Uri = Uri.EMPTY,
    private val scale: Double = 1.0,
  ) : com.google.android.gms.ads.nativead.NativeAd.Image() {
    override fun getScale() = scale
    override fun getDrawable() = drawable
    override fun getUri() = uri
  }
}
