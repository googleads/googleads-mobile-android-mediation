// Copyright 2026 Google LLC
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

package com.google.ads.mediation.mytarget

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.MediaView
import com.my.target.common.CachePolicy
import com.my.target.common.models.IAdLoadingError
import com.my.target.common.models.ImageData
import com.my.target.nativeads.MediationHelper
import com.my.target.nativeads.NativeAd
import com.my.target.nativeads.banners.NativePromoBanner
import com.my.target.nativeads.views.MediaAdView

class MyTargetNativeAd(
  private val adLoadCallback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>
) : NativeAdMapper(), NativeAd.NativeAdListener {

  private var nativeAdCallback: MediationNativeAdCallback? = null

  private lateinit var context: Context

  // myTarget Native ad object
  private lateinit var myTargetNativeAd: NativeAd

  private lateinit var myTargetMediaView: MediaAdView

  fun loadAd(adConfiguration: MediationNativeAdConfiguration) {
    context = adConfiguration.context
    val serverParameters = adConfiguration.serverParameters

    val slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters)
    if (slotId < 0) {
      val slotIdError =
        AdError(
          MyTargetMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Slot ID.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      Log.e(TAG, slotIdError.message)
      adLoadCallback.onFailure(slotIdError)
      return
    }

    Log.d(TAG, "Requesting myTarget native mediation with Slot ID: $slotId")
    myTargetNativeAd = MyTargetSdkWrapper.createNativeAd(slotId, context)

    var cachePolicy = CachePolicy.IMAGE
    if (adConfiguration.nativeAdOptions?.shouldReturnUrlsForImageAssets() == true) {
      cachePolicy = CachePolicy.NONE
    }
    Log.d(TAG, "Set cache policy to $cachePolicy")
    myTargetNativeAd.cachePolicy = cachePolicy

    val params = myTargetNativeAd.customParams
    MyTargetTools.handleMediationExtras(TAG, adConfiguration.mediationExtras, params)
    params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE)

    myTargetNativeAd.listener = this
    myTargetNativeAd.load()
  }

  // region myTarget NativeAdListener implementation

  override fun onLoad(banner: NativePromoBanner, nativeAd: NativeAd) {
    if (myTargetNativeAd != nativeAd) {
      val matchingError =
        AdError(
          MyTargetMediationAdapter.ERROR_INVALID_NATIVE_AD_LOADED,
          "Loaded native ad object does not match the requested ad object.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      Log.e(TAG, matchingError.message)

      adLoadCallback.onFailure(matchingError)
      return
    }

    if (banner.image == null || banner.icon == null) {
      val assetError =
        AdError(
          MyTargetMediationAdapter.ERROR_MISSING_REQUIRED_NATIVE_ASSET,
          "Native ad is missing one of the following required assets: image or icon.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      Log.e(TAG, assetError.message)
      adLoadCallback.onFailure(assetError)
      return
    }

    mapNativeAd(nativeAd, context)
    Log.d(TAG, "Ad loaded successfully.")
    nativeAdCallback = adLoadCallback.onSuccess(this)
  }

  override fun onNoAd(reason: IAdLoadingError, nativeAd: NativeAd) {
    val loadError =
      AdError(
        MyTargetMediationAdapter.ERROR_MY_TARGET_SDK,
        reason.message,
        MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN,
      )
    Log.e(TAG, loadError.message)
    adLoadCallback.onFailure(loadError)
  }

  override fun onShow(nativeAd: NativeAd) {
    Log.d(TAG, "Ad show.")
    nativeAdCallback?.reportAdImpression()
  }

  override fun onClick(view: View?, nativeAd: NativeAd) {
    Log.d(TAG, "Ad clicked.")
    nativeAdCallback?.reportAdClicked()
    nativeAdCallback?.onAdOpened()
    nativeAdCallback?.onAdLeftApplication()
  }

  // Note: myTarget has deprecated onClick(NativeAd) in favor of onClick(View, NativeAd). So, only
  // onClick(View, NativeAd) is expected to be called by the myTarget SDK. But, we still have to
  // implement onClick(NativeAd) for the code to build.
  override fun onClick(nativeAd: NativeAd) {
    Log.d(TAG, "Ad clicked.")
    nativeAdCallback?.reportAdClicked()
    nativeAdCallback?.onAdOpened()
    nativeAdCallback?.onAdLeftApplication()
  }

  override fun onVideoPlay(nativeAd: NativeAd) {
    Log.d(TAG, "Play ad video.")
    nativeAdCallback?.onVideoPlay()
  }

  override fun onVideoPause(nativeAd: NativeAd) {
    Log.d(TAG, "Pause ad video.")
    nativeAdCallback?.onVideoPause()
  }

  override fun onVideoComplete(nativeAd: NativeAd) {
    Log.d(TAG, "Complete ad video.")
    nativeAdCallback?.onVideoComplete()
  }

  // endregion

  fun mapNativeAd(nativeAd: NativeAd, context: Context) {
    myTargetMediaView = MediaAdView(context)
    overrideClickHandling = true
    overrideImpressionRecording = true

    val banner = nativeAd.banner
    if (banner == null) {
      val bannerError =
        AdError(
          MyTargetMediationAdapter.ERROR_MISSING_REQUIRED_NATIVE_ASSET,
          "Native ad is missing one of the following required assets: banner model.",
          MyTargetMediationAdapter.ERROR_DOMAIN,
        )
      Log.e(TAG, bannerError.message)
      adLoadCallback.onFailure(bannerError)
      return
    }

    body = banner.description.toString()
    callToAction = banner.ctaText.toString()
    headline = banner.title.toString()

    val bannerIcon = banner.icon
    if (bannerIcon != null && !TextUtils.isEmpty(bannerIcon.url)) {
      icon = MyTargetNativeAdImage(bannerIcon, context.resources)
    }

    setHasVideoContent(true)
    if (myTargetMediaView.mediaAspectRatio > 0) {
      mediaContentAspectRatio = myTargetMediaView.mediaAspectRatio
    }

    setMediaView(myTargetMediaView)

    val image = banner.image
    if (image != null && !TextUtils.isEmpty(image.url)) {
      val imageArrayList = ArrayList<com.google.android.gms.ads.nativead.NativeAd.Image>()
      imageArrayList.add(MyTargetNativeAdImage(image, context.resources))
      images = imageArrayList
    }

    advertiser = banner.domain.toString()
    starRating = banner.rating.toDouble()

    val myTargetExtras = Bundle()
    val ageRestrictions = banner.ageRestrictions
    if (!TextUtils.isEmpty(ageRestrictions)) {
      myTargetExtras.putString(EXTRA_KEY_AGE_RESTRICTIONS, ageRestrictions)
    }
    val advertisingLabel = banner.advertisingLabel
    if (!TextUtils.isEmpty(advertisingLabel)) {
      myTargetExtras.putString(EXTRA_KEY_ADVERTISING_LABEL, advertisingLabel)
    }
    val category = banner.category
    if (!TextUtils.isEmpty(category)) {
      myTargetExtras.putString(EXTRA_KEY_CATEGORY, category)
    }
    val subCategory = banner.subCategory
    if (!TextUtils.isEmpty(subCategory)) {
      myTargetExtras.putString(EXTRA_KEY_SUBCATEGORY, subCategory)
    }
    val votes = banner.votes
    if (votes > 0) {
      myTargetExtras.putInt(EXTRA_KEY_VOTES, votes)
    }
    extras = myTargetExtras
  }

  private class MyTargetNativeAdImage(imageData: ImageData, resources: Resources?) :
    com.google.android.gms.ads.nativead.NativeAd.Image() {

    private var drawable: Drawable? = null
    private var uri: Uri

    init {
      val bitmap: Bitmap? = imageData.bitmap

      if (bitmap != null) {
        drawable = BitmapDrawable(resources, bitmap)
      }
      uri = Uri.parse(imageData.url)
    }

    override fun getDrawable(): Drawable? = drawable

    override fun getUri(): Uri = uri

    override fun getScale(): Double = 1.0
  }

  // region NativeAdMapper implementation

  override fun trackViews(
    containerView: View,
    clickableAssetViews: Map<String, View>,
    nonclickableAssetViews: Map<String, View>,
  ) {
    val clickableViews: java.util.ArrayList<View?> =
      java.util.ArrayList<View?>(clickableAssetViews.values)
    containerView.post {
      val mediaPosition = findMediaAdViewPosition(clickableViews, myTargetMediaView)
      if (mediaPosition >= 0) {
        clickableViews.removeAt(mediaPosition)
        clickableViews.add(myTargetMediaView)
      }
      MediationHelper.registerView(
        myTargetNativeAd,
        containerView,
        clickableViews,
        myTargetMediaView,
      )
    }
  }

  override fun untrackView(view: View) {
    myTargetNativeAd.unregisterView()
  }

  // endregion

  private fun findMediaAdViewPosition(clickableViews: List<View?>, mediaAdView: MediaAdView): Int {
    for (i in clickableViews.indices) {
      val view = clickableViews[i]
      if (view is MediaView) {
        val mediaView = view // For clarity
        val childCount = mediaView.childCount
        for (j in 0..<childCount) {
          val innerView = mediaView.getChildAt(j)
          if (innerView === mediaAdView) {
            return i
          }
        }
        break
      }
    }
    return -1
  }

  companion object {
    const val TAG = "MyTargetNativeAd"

    const val EXTRA_KEY_AGE_RESTRICTIONS = "ageRestrictions"
    const val EXTRA_KEY_ADVERTISING_LABEL = "advertisingLabel"
    const val EXTRA_KEY_CATEGORY = "category"
    const val EXTRA_KEY_SUBCATEGORY = "subcategory"
    const val EXTRA_KEY_VOTES = "votes"
  }
}
