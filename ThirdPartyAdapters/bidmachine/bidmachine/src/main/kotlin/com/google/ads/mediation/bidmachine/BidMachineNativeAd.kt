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

package com.google.ads.mediation.bidmachine

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_CODE_EMPTY_NATIVE_AD_DATA
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_AD_REQUEST_EXPIRED
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.ERROR_MSG_EMPTY_NATIVE_AD_DATA
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import io.bidmachine.AdPlacementConfig
import io.bidmachine.RendererConfiguration
import io.bidmachine.models.AuctionResult
import io.bidmachine.nativead.NativeAd
import io.bidmachine.nativead.NativeListener
import io.bidmachine.nativead.NativePublicData
import io.bidmachine.nativead.NativeRequest
import io.bidmachine.nativead.view.NativeMediaView
import io.bidmachine.utils.BMError

/**
 * Used to load BidMachine native ads and mediate callbacks between Google Mobile Ads SDK and
 * BidMachine SDK.
 */
class BidMachineNativeAd
private constructor(
  private val context: Context,
  private val mediationNativeAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  @get:VisibleForTesting internal val adPlacementConfig: AdPlacementConfig,
) : NativeAdMapper(), NativeRequest.AdRequestListener, NativeListener {
  private var nativeAdCallback: MediationNativeAdCallback? = null
  @VisibleForTesting internal var nativeRequestBuilder = NativeRequest.Builder(adPlacementConfig)
  private lateinit var bidMachineNativeAd: NativeAd
  private var bidMachineMediaView: NativeMediaView? = null

  fun loadWaterfallAd(nativeAd: NativeAd) {
    val nativeRequest = nativeRequestBuilder.setListener(this).build()
    loadAd(nativeAd, nativeRequest)
  }

  fun loadRtbAd(nativeAd: NativeAd) {
    val nativeRequest = nativeRequestBuilder.setBidPayload(bidResponse).setListener(this).build()
    loadAd(nativeAd, nativeRequest)
  }

  private fun loadAd(nativeAd: NativeAd, nativeRequest: NativeRequest) {
    bidMachineNativeAd = nativeAd
    bidMachineNativeAd.setListener(this)
    nativeRequest.request(context)
  }

  override fun onRequestSuccess(nativeRequest: NativeRequest, auctionResult: AuctionResult) {
    if (nativeRequest.isExpired) {
      val adError =
        AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
      mediationNativeAdLoadCallback.onFailure(adError)
      nativeRequest.destroy()
      return
    }
    bidMachineNativeAd.setRendererConfiguration(
      RendererConfiguration(mapOf(WATERMARK_KEY to watermark))
    )
    bidMachineNativeAd.load(nativeRequest)
  }

  override fun onRequestFailed(nativeRequest: NativeRequest, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationNativeAdLoadCallback.onFailure(adError)
    nativeRequest.destroy()
  }

  override fun onRequestExpired(nativeRequest: NativeRequest) {
    val adError =
      AdError(ERROR_CODE_AD_REQUEST_EXPIRED, ERROR_MSG_AD_REQUEST_EXPIRED, ADAPTER_ERROR_DOMAIN)
    mediationNativeAdLoadCallback.onFailure(adError)
    nativeRequest.destroy()
  }

  override fun onAdLoaded(nativeAd: NativeAd) {
    val adData = bidMachineNativeAd.adData
    if (adData == null) {
      val adError =
        AdError(
          ERROR_CODE_EMPTY_NATIVE_AD_DATA,
          ERROR_MSG_EMPTY_NATIVE_AD_DATA,
          ADAPTER_ERROR_DOMAIN,
        )
      mediationNativeAdLoadCallback.onFailure(adError)
      return
    }
    mapNativeAd(adData)
    bidMachineMediaView = NativeMediaView(context)
    bidMachineMediaView?.let { setMediaView(it) }
    nativeAdCallback = mediationNativeAdLoadCallback.onSuccess(this)
  }

  override fun trackViews(
    container: View,
    clickableAssetViews: Map<String?, View?>,
    nonClickableAssetViews: Map<String?, View?>,
  ) {
    bidMachineNativeAd.registerView(
      container as ViewGroup,
      adChoicesContent,
      bidMachineMediaView,
      clickableAssetViews.values.toSet(),
    )
  }

  override fun onAdLoadFailed(nativeAd: NativeAd, bMError: BMError) {
    val adError = AdError(bMError.code, bMError.message, SDK_ERROR_DOMAIN)
    mediationNativeAdLoadCallback.onFailure(adError)
    bidMachineNativeAd.destroy()
  }

  override fun onAdImpression(nativeAd: NativeAd) {
    nativeAdCallback?.reportAdImpression()
  }

  override fun onAdShowFailed(nativeAd: NativeAd, bMError: BMError) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClicked(nativeAd: NativeAd) {
    nativeAdCallback?.reportAdClicked()
    nativeAdCallback?.onAdOpened()
    nativeAdCallback?.onAdLeftApplication()
  }

  override fun onAdExpired(nativeAd: NativeAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  private fun mapNativeAd(adData: NativePublicData) {
    headline = adData.title
    body = adData.description
    callToAction = adData.callToAction
    icon = NativeAdImage(adData.icon?.image, adData.icon?.localUri)
    starRating = adData.rating.toDouble()
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
    ): Result<BidMachineNativeAd> {
      val context = mediationNativeAdConfiguration.context
      val bidResponse = mediationNativeAdConfiguration.bidResponse
      val watermark = mediationNativeAdConfiguration.watermark
      val placementId = mediationNativeAdConfiguration.serverParameters.getString(PLACEMENT_ID_KEY)
      val adPlacementConfig = AdPlacementConfig.nativeBuilder().withPlacementId(placementId).build()

      return Result.success(
        BidMachineNativeAd(
          context,
          mediationNativeAdLoadCallback,
          bidResponse,
          watermark,
          adPlacementConfig,
        )
      )
    }
  }
}
