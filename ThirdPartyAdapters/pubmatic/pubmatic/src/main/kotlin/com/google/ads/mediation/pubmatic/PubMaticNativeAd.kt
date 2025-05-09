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

package com.google.ads.mediation.pubmatic

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.core.net.toUri
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.NativeAd.Image
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.nativead.POBNativeAd
import com.pubmatic.sdk.nativead.POBNativeAdListener
import com.pubmatic.sdk.nativead.POBNativeAdLoader
import com.pubmatic.sdk.nativead.POBNativeAdLoaderListener
import com.pubmatic.sdk.openwrap.core.POBConstants.KEY_POB_ADMOB_WATERMARK
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost

/**
 * Used to load PubMatic native ads and mediate callbacks between Google Mobile Ads SDK and PubMatic
 * SDK.
 */
class PubMaticNativeAd
private constructor(
  private val context: Context,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  private val bidResponse: String,
  private val watermark: String,
  private val pubMaticAdFactory: PubMaticAdFactory,
) : NativeAdMapper(), POBNativeAdLoaderListener, POBNativeAdListener {

  private var pobNativeAd: POBNativeAd? = null

  private var mediationNativeAdCallback: MediationNativeAdCallback? = null

  fun loadAd() {
    val pobNativeAdLoader = pubMaticAdFactory.createPOBNativeAdLoader(context)
    pobNativeAdLoader.setAdLoaderListener(this)
    pobNativeAdLoader.addExtraInfo(KEY_POB_ADMOB_WATERMARK, watermark)
    pobNativeAdLoader.loadAd(bidResponse, POBBiddingHost.ADMOB)
  }

  override fun onAdReceived(pobNativeAdLoader: POBNativeAdLoader, pobNativeAd: POBNativeAd) {
    this.pobNativeAd = pobNativeAd
    mapNativeAd()
    mediationNativeAdCallback = mediationAdLoadCallback.onSuccess(this)
  }

  override fun onFailedToLoad(pobNativeAdLoader: POBNativeAdLoader, pobError: POBError) {
    mediationAdLoadCallback.onFailure(
      AdError(pobError.errorCode, pobError.errorMessage, SDK_ERROR_DOMAIN)
    )
  }

  override fun onNativeAdRendered(pobNativeAd: POBNativeAd) {
    // This method will not be called for SDK Bidding flow
  }

  override fun onNativeAdRenderingFailed(pobNativeAd: POBNativeAd, pobError: POBError) {
    // This method will not be called for SDK Bidding flow
  }

  override fun onNativeAdImpression(pobNativeAd: POBNativeAd) {
    mediationNativeAdCallback?.reportAdImpression()
  }

  override fun onNativeAdClicked(pobNativeAd: POBNativeAd) {
    mediationNativeAdCallback?.reportAdClicked()
  }

  override fun onNativeAdClicked(pobNativeAd: POBNativeAd, assetId: String) {
    // This method will not be called for SDK Bidding flow
  }

  override fun onNativeAdLeavingApplication(pobNativeAd: POBNativeAd) {
    mediationNativeAdCallback?.onAdLeftApplication()
  }

  override fun onNativeAdOpened(pobNativeAd: POBNativeAd) {
    mediationNativeAdCallback?.onAdOpened()
  }

  override fun onNativeAdClosed(pobNativeAd: POBNativeAd) {
    mediationNativeAdCallback?.onAdClosed()
  }

  override fun trackViews(
    containerView: View,
    clickableAssetViews: Map<String?, View?>,
    nonclickableAssetViews: Map<String?, View?>,
  ) {
    pobNativeAd?.registerViewForInteraction(
      containerView,
      clickableAssetViews.values.toList(),
      this,
    )
  }

  class PubMaticNativeAdImage(imageUrl: String?) : Image() {

    private var imageUri: Uri? = imageUrl?.toUri()

    override fun getScale(): Double = 1.0 // Default scale is 1.0

    override fun getDrawable(): Drawable? = null

    override fun getUri(): Uri? = imageUri
  }

  fun mapNativeAd() {
    val pobNativeAdTitle = pobNativeAd?.title?.title
    if (pobNativeAdTitle != null) {
      headline = pobNativeAdTitle
    }

    val pobNativeAdDescription = pobNativeAd?.description?.value
    if (pobNativeAdDescription != null) {
      body = pobNativeAdDescription
    }

    icon = PubMaticNativeAdImage(pobNativeAd?.icon?.imageURL)

    val pobNativeAdCallToAction = pobNativeAd?.callToAction?.value
    if (pobNativeAdCallToAction != null) {
      callToAction = pobNativeAdCallToAction
    }

    val pobNativeAdAdvertiser = pobNativeAd?.advertiser?.value
    if (pobNativeAdAdvertiser != null) {
      advertiser = pobNativeAdAdvertiser
    }

    val pobNativeAdPrice = pobNativeAd?.price?.value
    if (pobNativeAdPrice != null) {
      price = pobNativeAdPrice
    }

    try {
      val pobNativeAdRating = pobNativeAd?.rating?.value?.toDouble()
      if (pobNativeAdRating != null) {
        starRating = pobNativeAdRating
      }
    } catch (numberFormatException: NumberFormatException) {
      // Do nothing.
    }

    images = listOf(PubMaticNativeAdImage(pobNativeAd?.mainImage?.imageURL))

    val pobNativeAdInfoIcon = pobNativeAd?.adInfoIcon
    if (pobNativeAdInfoIcon != null) {
      adChoicesContent = pobNativeAdInfoIcon
    }

    setHasVideoContent(false)
    overrideClickHandling = true
    overrideImpressionRecording = true
  }

  companion object {
    fun newInstance(
      mediationNativeAdConfiguration: MediationNativeAdConfiguration,
      mediationNativeAdLoadCallback:
        MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
      pubMaticAdFactory: PubMaticAdFactory,
    ) =
      Result.success(
        PubMaticNativeAd(
          context = mediationNativeAdConfiguration.context,
          mediationAdLoadCallback = mediationNativeAdLoadCallback,
          bidResponse = mediationNativeAdConfiguration.bidResponse,
          watermark = mediationNativeAdConfiguration.watermark,
          pubMaticAdFactory = pubMaticAdFactory,
        )
      )
  }
}
