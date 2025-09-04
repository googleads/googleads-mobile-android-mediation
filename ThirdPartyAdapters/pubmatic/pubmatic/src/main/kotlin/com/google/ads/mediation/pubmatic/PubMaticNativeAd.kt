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
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_AD_UNIT
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticUtils.runtimeGmaSdkListensToAdapterReportedImpressions
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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine

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
  private val adapterScope: CoroutineScope,
  private val pobNativeAdLoader: POBNativeAdLoader,
  private val isRtb: Boolean,
) : NativeAdMapper(), POBNativeAdLoaderListener, POBNativeAdListener {

  private var pobNativeAd: POBNativeAd? = null

  private var mediationNativeAdCallback: MediationNativeAdCallback? = null

  fun loadAd() {
    pobNativeAdLoader.setAdLoaderListener(this)
    if (isRtb) {
      pobNativeAdLoader.addExtraInfo(KEY_POB_ADMOB_WATERMARK, watermark)
      pobNativeAdLoader.loadAd(bidResponse, POBBiddingHost.ADMOB)
      return
    }
    pobNativeAdLoader.loadAd()
  }

  override fun onAdReceived(pobNativeAdLoader: POBNativeAdLoader, pobNativeAd: POBNativeAd) {
    this.pobNativeAd = pobNativeAd
    mapNativeAd()
    adapterScope.async {
      mapNativeAdAsync()
      mediationNativeAdCallback = mediationAdLoadCallback.onSuccess(this@PubMaticNativeAd)
    }
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

  class PubMaticNativeAdImage(imageUrl: String?, drawable: Drawable?) : Image() {

    private var imageUri: Uri? = imageUrl?.toUri()

    private val imageDrawable: Drawable? = drawable

    override fun getScale(): Double = 1.0 // Default scale is 1.0

    override fun getDrawable(): Drawable? = imageDrawable

    override fun getUri(): Uri? = imageUri
  }

  /** Maps PubMatic's native assets that must be done on main thread */
  private fun mapNativeAd() {
    val pobNativeAdInfoIcon = pobNativeAd?.adInfoIcon
    if (pobNativeAdInfoIcon != null) {
      // Wrapping on a layout to keep layoutParams
      val frameLayout = FrameLayout(context)
      frameLayout.addView(pobNativeAdInfoIcon)
      adChoicesContent = frameLayout
    }
  }

  /** Maps PubMatic's native assets that can be done on back thread and also downloads images */
  private suspend fun mapNativeAdAsync() = coroutineScope {
    val pobNativeAdTitle = pobNativeAd?.title?.title
    if (pobNativeAdTitle != null) {
      headline = pobNativeAdTitle
    }

    val pobNativeAdDescription = pobNativeAd?.description?.value
    if (pobNativeAdDescription != null) {
      body = pobNativeAdDescription
    }

    loadImages()

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

    setHasVideoContent(false)
    overrideClickHandling = true
    if (runtimeGmaSdkListensToAdapterReportedImpressions()) {
      overrideImpressionRecording = true
    }
  }

  private suspend fun loadImages() = suspendCancellableCoroutine { continuation ->
    val iconUrl = pobNativeAd?.icon?.imageURL
    if (iconUrl != null) {
      val iconDrawable = Glide.with(context).asDrawable().load(iconUrl).submit().get()
      icon = PubMaticNativeAdImage(iconUrl, iconDrawable)
    }

    val mainImage = pobNativeAd?.mainImage
    if (mainImage != null) {
      val mediaView = FrameLayout(context)
      val widthInPixels =
        TypedValue.applyDimension(
            COMPLEX_UNIT_DIP,
            mainImage.width.toFloat(),
            context.resources.displayMetrics,
          )
          .toInt()
      val heightInPixels =
        TypedValue.applyDimension(
            COMPLEX_UNIT_DIP,
            mainImage.height.toFloat(),
            context.resources.displayMetrics,
          )
          .toInt()
      val adViewLayoutParams: FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(widthInPixels, heightInPixels)
      val imageViewForMedia = ImageView(context)
      imageViewForMedia.layoutParams = adViewLayoutParams
      mediaView.addView(imageViewForMedia)
      setMediaView(mediaView)
      val image = Glide.with(context).asDrawable().load(mainImage.imageURL).submit().get()
      images = listOf(PubMaticNativeAdImage(mainImage.imageURL, image))
      imageViewForMedia.setImageDrawable(image)
    }

    continuation.resume(true)
  }

  companion object {
    fun newInstance(
      mediationNativeAdConfiguration: MediationNativeAdConfiguration,
      mediationNativeAdLoadCallback:
        MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
      pubMaticAdFactory: PubMaticAdFactory,
      coroutineContext: CoroutineContext =
        PubMaticAdFactory.BACKGROUND_EXECUTOR.asCoroutineDispatcher(),
      isRtb: Boolean,
    ): Result<PubMaticNativeAd> {
      val context = mediationNativeAdConfiguration.context
      val adapterScope = CoroutineScope(coroutineContext)
      val pobNativeAdLoader =
        if (isRtb) {
          pubMaticAdFactory.createPOBNativeAdLoader(context)
        } else {
          val serverParameters = mediationNativeAdConfiguration.serverParameters
          val pubId = serverParameters.getString(KEY_PUBLISHER_ID)
          val profileId = serverParameters.getString(KEY_PROFILE_ID)?.toIntOrNull()
          val adUnit = serverParameters.getString(KEY_AD_UNIT)
          if (pubId.isNullOrEmpty()) {
            val adError =
              AdError(
                ERROR_MISSING_PUBLISHER_ID,
                ERROR_MISSING_PUBLISHER_ID_MSG,
                ADAPTER_ERROR_DOMAIN,
              )
            mediationNativeAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          if (profileId == null) {
            val adError =
              AdError(
                ERROR_MISSING_OR_INVALID_PROFILE_ID,
                ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG,
                ADAPTER_ERROR_DOMAIN,
              )
            mediationNativeAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          if (adUnit.isNullOrEmpty()) {
            val adError =
              AdError(ERROR_MISSING_AD_UNIT_ID, ERROR_MISSING_AD_UNIT_ID_MSG, ADAPTER_ERROR_DOMAIN)
            mediationNativeAdLoadCallback.onFailure(adError)
            return Result.failure(NoSuchElementException(adError.toString()))
          }
          pubMaticAdFactory.createPOBNativeAdLoader(context, pubId, profileId, adUnit)
        }
      return Result.success(
        PubMaticNativeAd(
          context,
          mediationAdLoadCallback = mediationNativeAdLoadCallback,
          bidResponse = mediationNativeAdConfiguration.bidResponse,
          watermark = mediationNativeAdConfiguration.watermark,
          adapterScope,
          pobNativeAdLoader,
          isRtb,
        )
      )
    }
  }
}
