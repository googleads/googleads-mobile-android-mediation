// Copyright 2025 Google LLC
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

package com.google.ads.mediation.verve

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.ERROR_CODE_AD_LOAD_FAILED_TO_LOAD
import com.google.ads.mediation.verve.VerveMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import net.pubnative.lite.sdk.models.NativeAd
import net.pubnative.lite.sdk.request.HyBidNativeAdRequest

/**
 * Used to load Verve native ads and mediate callbacks between Google Mobile Ads SDK and Verve SDK.
 */
class VerveNativeAd
@VisibleForTesting
internal constructor(
  private val context: Context,
  private val mediationNativeAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  private val bidResponse: String,
  private val hyBidNativeAdRequest: HyBidNativeAdRequest,
) : NativeAdMapper(), HyBidNativeAdRequest.RequestListener, NativeAd.Listener {
  private var hyBidNativeAd: NativeAd? = null
  private var nativeAdCallback: MediationNativeAdCallback? = null

  fun loadAd() {
    hyBidNativeAdRequest.prepareAd(bidResponse, this)
  }

  override fun onRequestSuccess(ad: NativeAd?) {
    if (ad == null) {
      val adError =
        AdError(ERROR_CODE_AD_LOAD_FAILED_TO_LOAD, "Could not load native ad", SDK_ERROR_DOMAIN)
      mediationNativeAdLoadCallback.onFailure(adError)
      return
    }
    mapNativeAd(ad)
    hyBidNativeAd = ad
    nativeAdCallback = mediationNativeAdLoadCallback.onSuccess(this)
  }

  override fun onRequestFail(throwable: Throwable?) {
    val errorMessage = if (throwable == null) "null" else throwable.message.toString()
    val adError =
      AdError(
        ERROR_CODE_AD_LOAD_FAILED_TO_LOAD,
        "Could not load native ad. Error: $errorMessage",
        SDK_ERROR_DOMAIN,
      )
    mediationNativeAdLoadCallback.onFailure(adError)
  }

  override fun onAdImpression(ad: NativeAd?, view: View?) {
    nativeAdCallback?.onAdOpened()
    nativeAdCallback?.reportAdImpression()
  }

  override fun onAdClick(ad: NativeAd?, view: View?) {
    nativeAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  override fun trackViews(
    containerView: View,
    clickableAssetViews: Map<String?, View?>,
    nonClickableAssetViews: Map<String?, View?>,
  ) {
    hyBidNativeAd?.startTracking(containerView, this)
  }

  override fun untrackView(view: View) {
    hyBidNativeAd?.stopTracking()
  }

  private fun mapNativeAd(nativeAd: NativeAd) {
    headline = nativeAd.title
    body = nativeAd.description
    adChoicesContent = nativeAd.getContentInfo(context)
    callToAction = nativeAd.callToActionText
    starRating = nativeAd.rating.toDouble()
    icon =
      NativeAdImage(nativeAd.iconBitmap.toDrawable(context.resources), nativeAd.iconUrl.toUri())
    val imageView = ImageView(context)
    imageView.setImageBitmap(nativeAd.bannerBitmap)
    setMediaView(imageView)

    overrideClickHandling = true
    overrideImpressionRecording = true
  }

  internal class NativeAdImage(
    private val drawable: Drawable?,
    private val uri: Uri? = Uri.EMPTY,
    private val scale: Double = 1.0,
  ) : com.google.android.gms.ads.nativead.NativeAd.Image() {
    override fun getScale(): Double = scale

    override fun getDrawable(): Drawable? = drawable

    override fun getUri(): Uri? = uri
  }

  companion object {
    fun newInstance(
      mediationNativeAdConfiguration: MediationNativeAdConfiguration,
      mediationNativeAdLoadCallback:
        MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
    ): Result<VerveNativeAd> {
      val context = mediationNativeAdConfiguration.context

      val bidResponse = mediationNativeAdConfiguration.bidResponse
      val nativeAdRequest = HyBidNativeAdRequest()

      return Result.success(
        VerveNativeAd(context, mediationNativeAdLoadCallback, bidResponse, nativeAdRequest)
      )
    }
  }
}
