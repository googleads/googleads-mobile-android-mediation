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

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.PLACEMENT_ID_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.WATERMARK_KEY
import com.google.ads.mediation.bidmachine.BidMachineMediationAdapter.Companion.createPlacement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import io.bidmachine.Ad
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.BidMachineInstance
import io.bidmachine.format.nativead.NativeAd
import io.bidmachine.format.nativead.NativeClickableView
import io.bidmachine.format.nativead.NativeListener
import io.bidmachine.model.Placement

/**
 * Used to load BidMachine native ads and mediate callbacks between Google Mobile Ads SDK and
 * BidMachine SDK.
 */
class BidMachineNativeAd
private constructor(
  private val mediationNativeAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  @get:VisibleForTesting internal val placement: Placement?,
) : NativeAdMapper(), AdLoadCallback<NativeAd>, NativeListener {
  private var nativeAdCallback: MediationNativeAdCallback? = null
  private var bidMachineNativeAd: NativeAd? = null

  fun loadWaterfallAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadNative(placement, this)
  }

  fun loadRtbAd(bidMachineInstance: BidMachineInstance) {
    bidMachineInstance.loadNative(bidResponse, this)
  }

  override fun onAdLoaded(ad: NativeAd) {
    bidMachineNativeAd = ad
    setWatermark(ad)
    ad.setListener(this)
    mapNativeAd(ad)
    nativeAdCallback = mediationNativeAdLoadCallback.onSuccess(this)
  }

  override fun onAdLoadFailed(bmError: BMError) {
    val adError = AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN)
    mediationNativeAdLoadCallback.onFailure(adError)
  }

  override fun trackViews(
    container: View,
    clickableAssetViews: Map<String, View>,
    nonClickableAssetViews: Map<String, View>,
  ) {
    bidMachineNativeAd?.bind(
      container as ViewGroup,
      clickableAssetViews.values.map { NativeClickableView(it) },
    )
  }

  override fun untrackView(view: View) {
    bidMachineNativeAd?.unbind()
  }

  override fun destroy() {
    val nativeAd = bidMachineNativeAd
    bidMachineNativeAd = null
    nativeAdCallback = null
    nativeAd?.setListener(null)
    nativeAd?.destroy()
  }

  override fun onAdPresented(ad: NativeAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdImpression(ad: NativeAd) {
    nativeAdCallback?.reportAdImpression()
  }

  override fun onAdShowFailed(ad: NativeAd, bmError: BMError) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdClicked(ad: NativeAd) {
    nativeAdCallback?.reportAdClicked()
    nativeAdCallback?.onAdOpened()
    nativeAdCallback?.onAdLeftApplication()
  }

  override fun onAdExpired(ad: NativeAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  private fun mapNativeAd(nativeAd: NativeAd) {
    nativeAd.getTitle()?.let { headline = it }
    nativeAd.getBody()?.let { body = it }
    nativeAd.getCallToAction()?.let { callToAction = it }
    nativeAd.getAdvertiser()?.let { advertiser = it }
    nativeAd.getStarRating()?.let { starRating = it }
    nativeAd.getIcon()?.let { icon = NativeAdImage(it.getImage(), it.getLocalUri()) }
    nativeAd.getMainImage()?.let {
      images = listOf(NativeAdImage(it.getImage(), it.getLocalUri()))
    }
    setMediaView(nativeAd.getMediaView())
    setHasVideoContent(nativeAd.hasVideoContent())
    adChoicesContent = nativeAd.getAdChoiceView()
    overrideClickHandling = true
    overrideImpressionRecording = true
  }

  private fun setWatermark(ad: Ad) {
    ad.setExtras(mapOf(WATERMARK_KEY to watermark))
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
      val bidResponse = mediationNativeAdConfiguration.bidResponse
      val watermark = mediationNativeAdConfiguration.watermark
      val placement =
        createPlacement(mediationNativeAdConfiguration.serverParameters.getString(PLACEMENT_ID_KEY))

      return Result.success(
        BidMachineNativeAd(mediationNativeAdLoadCallback, bidResponse, watermark, placement)
      )
    }
  }
}
