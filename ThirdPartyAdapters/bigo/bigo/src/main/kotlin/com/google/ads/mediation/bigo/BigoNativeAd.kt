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

package com.google.ads.mediation.bigo

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_CODE_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.ERROR_MSG_MISSING_SLOT_ID
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.ads.mediation.bigo.BigoMediationAdapter.Companion.SLOT_ID_KEY
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_BODY
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_CALL_TO_ACTION
import com.google.android.gms.ads.nativead.NativeAdAssetNames.ASSET_HEADLINE
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.AdOptionsView
import sg.bigo.ads.api.AdTag
import sg.bigo.ads.api.MediaView
import sg.bigo.ads.api.NativeAd
import sg.bigo.ads.api.VideoController

/**
 * Used to load Bigo native ads and mediate callbacks between Google Mobile Ads SDK and Bigo SDK.
 */
class BigoNativeAd
private constructor(
  private val mediationNativeAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  private val bidResponse: String,
  private val slotId: String,
  private val mediaView: MediaView,
  private val iconView: ImageView,
  private val adOptionsView: AdOptionsView,
  private val watermark: String,
) :
  NativeAdMapper(),
  AdLoadListener<NativeAd>,
  AdInteractionListener,
  VideoController.VideoLifeCallback {

  private var nativeAdCallback: MediationNativeAdCallback? = null
  private var nativeAd: NativeAd? = null
  private var videoController: VideoController? = null

  fun loadAd(versionString: String) {
    val adRequest = BigoFactory.delegate.createNativeAdRequest(bidResponse, slotId, watermark)
    val nativeAdLoader = BigoFactory.delegate.createNativeAdLoader()
    nativeAdLoader.initializeAdLoader(loadListener = this, versionString)
    nativeAdLoader.loadAd(adRequest)
  }

  override fun onError(adError: AdError) {
    val gmaAdError = BigoUtils.getGmaAdError(adError.code, adError.message, SDK_ERROR_DOMAIN)
    mediationNativeAdLoadCallback.onFailure(gmaAdError)
  }

  override fun onAdLoaded(nativeAd: NativeAd) {
    nativeAd.setAdInteractionListener(this)
    mapNativeAd(nativeAd)
    this.nativeAd = nativeAd
    nativeAdCallback = mediationNativeAdLoadCallback.onSuccess(this)
  }

  override fun onAdError(adError: AdError) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onAdImpression() {
    nativeAdCallback?.reportAdImpression()
  }

  override fun onAdClicked() {
    nativeAdCallback?.reportAdClicked()
  }

  override fun onAdOpened() {
    nativeAdCallback?.onAdOpened()
  }

  override fun onAdClosed() {
    nativeAdCallback?.onAdClosed()
  }

  override fun onVideoStart() {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onVideoPlay() {
    nativeAdCallback?.onVideoPlay()
  }

  override fun onVideoPause() {
    nativeAdCallback?.onVideoPause()
  }

  override fun onVideoEnd() {
    nativeAdCallback?.onVideoComplete()
  }

  override fun onMuteChange(mute: Boolean) {
    if (mute) {
      nativeAdCallback?.onVideoMute()
    } else {
      nativeAdCallback?.onVideoUnmute()
    }
  }

  override fun trackViews(
    container: View,
    clickableAssetViews: Map<String?, View?>,
    nonClickableAssetViews: Map<String?, View?>,
  ) {
    container.tag = AdTag.NATIVE_AD_VIEW
    iconView.tag = AdTag.ICON_VIEW
    mediaView.tag = AdTag.MEDIA_VIEW
    adOptionsView.tag = AdTag.OPTION_VIEW
    for (asset in clickableAssetViews) {
      when (asset.key) {
        ASSET_BODY -> asset.value?.tag = AdTag.DESCRIPTION
        ASSET_CALL_TO_ACTION -> asset.value?.tag = AdTag.CALL_TO_ACTION
        ASSET_HEADLINE -> asset.value?.tag = AdTag.TITLE
      }
    }
    nativeAd?.registerViewForInteraction(
      container as ViewGroup,
      mediaView,
      iconView,
      adOptionsView,
      clickableAssetViews.values.toList(),
    )
  }

  private fun mapNativeAd(nativeAd: NativeAd) {
    headline = nativeAd.title
    body = nativeAd.description
    if (nativeAd.hasIcon()) {
      icon = NativeAdImage(iconView.drawable)
    }
    callToAction = nativeAd.callToAction
    setMediaView(mediaView)
    advertiser = nativeAd.advertiser
    if (nativeAd.creativeType == NativeAd.CreativeType.VIDEO) {
      setHasVideoContent(true)
      videoController = nativeAd.videoController
      videoController?.videoLifeCallback = this
    }
    mediaContentAspectRatio = nativeAd.mediaContentAspectRatio
    adChoicesContent = adOptionsView

    overrideImpressionRecording = true
    overrideClickHandling = true
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
    ): Result<BigoNativeAd> {
      val context = mediationNativeAdConfiguration.context
      val serverParameters = mediationNativeAdConfiguration.serverParameters
      val bidResponse = mediationNativeAdConfiguration.bidResponse
      val slotId = serverParameters.getString(SLOT_ID_KEY)
      val watermark = mediationNativeAdConfiguration.watermark

      if (slotId.isNullOrEmpty()) {
        val gmaAdError =
          BigoUtils.getGmaAdError(
            ERROR_CODE_MISSING_SLOT_ID,
            ERROR_MSG_MISSING_SLOT_ID,
            ADAPTER_ERROR_DOMAIN,
          )
        mediationNativeAdLoadCallback.onFailure(gmaAdError)
        return Result.failure(IllegalArgumentException(gmaAdError.toString()))
      }

      return Result.success(
        BigoNativeAd(
          mediationNativeAdLoadCallback,
          bidResponse,
          slotId,
          MediaView(context),
          ImageView(context),
          AdOptionsView(context),
          watermark,
        )
      )
    }
  }
}
