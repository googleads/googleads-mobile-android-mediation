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

package com.google.ads.mediation.imobile

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MediationUtils
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import jp.co.imobile.sdkads.android.AdMobMediationSupportAdSize
import jp.co.imobile.sdkads.android.FailNotificationReason
import jp.co.imobile.sdkads.android.ImobileSdkAdListener
import kotlin.math.min

/** Loads I-Mobile banner ad. */
class IMobileBannerAd(
  val adLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
) : MediationBannerAd, ImobileSdkAdListener() {

  private var bannerView: View? = null

  private var bannerAdCallback: MediationBannerAdCallback? = null

  fun loadAd(adConfig: MediationBannerAdConfiguration, iMobileSdkWrapper: IMobileSdkWrapper) {
    // Validate Context.
    if (adConfig.context !is Activity) {
      val error =
        AdError(
          IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Context is not an Activity.",
          IMobileMediationAdapter.ERROR_DOMAIN,
        )
      Log.w(IMobileAdapter.TAG, error.message)
      adLoadCallback.onFailure(error)
      return
    }
    val activity = adConfig.context as Activity

    // Validate AdSize.
    val adSize = adConfig.adSize
    val iMobileAdSizes = AdMobMediationSupportAdSize.entries.toTypedArray()
    val supportedSizes = ArrayList<AdSize?>()
    for (adSize in iMobileAdSizes) {
      supportedSizes.add(AdSize(adSize.width, adSize.height))
    }
    val supportedAdSize = MediationUtils.findClosestSize(adConfig.context, adSize, supportedSizes)
    if (supportedAdSize == null) {
      val error =
        AdError(
          IMobileMediationAdapter.ERROR_BANNER_SIZE_MISMATCH,
          "Ad size $adSize is not supported.",
          IMobileMediationAdapter.ERROR_DOMAIN,
        )
      Log.w(IMobileAdapter.TAG, error.message)
      adLoadCallback.onFailure(error)
      return
    }

    // Get parameters for i-mobile SDK.
    val serverParameters = adConfig.serverParameters
    val publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID)
    val mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID)
    val spotId = serverParameters.getString(Constants.KEY_SPOT_ID)

    // Call i-mobile SDK.
    Log.d(IMobileAdapter.TAG, "Requesting banner with ad size: $adSize")
    iMobileSdkWrapper.registerSpotInline(activity, publisherId, mediaId, spotId)
    iMobileSdkWrapper.start(spotId)
    iMobileSdkWrapper.setImobileSdkAdListener(spotId, this)

    // Create view to display banner ads.
    bannerView = FrameLayout(activity)
    val scaleRatio =
      if (canScale(supportedAdSize)) {
        calcScaleRatio(activity, adSize, supportedAdSize)
      } else {
        1.0f
      }
    bannerView?.setLayoutParams(
      FrameLayout.LayoutParams(
        (supportedAdSize.getWidthInPixels(activity) * scaleRatio).toInt(),
        (supportedAdSize.getHeightInPixels(activity) * scaleRatio).toInt(),
      )
    )
    iMobileSdkWrapper.showAdForAdMobMediation(activity, spotId, bannerView as ViewGroup, scaleRatio)
  }

  // region Utility methods.
  private fun canScale(iMobileAdSize: AdSize): Boolean {
    return iMobileAdSize.width == 320 && (iMobileAdSize.height == 50 || iMobileAdSize.height == 100)
  }

  private fun calcScaleRatio(
    context: Context,
    requestedAdSize: AdSize,
    iMobileAdSize: AdSize,
  ): Float {
    return min(
      (requestedAdSize.getWidthInPixels(context).toFloat() /
        iMobileAdSize.getWidthInPixels(context)),
      (requestedAdSize.getHeightInPixels(context).toFloat() /
        iMobileAdSize.getHeightInPixels(context)),
    )
  }

  // endregion

  // region ImobileSdkAdListener implementation
  override fun onAdReadyCompleted() {
    bannerAdCallback = adLoadCallback.onSuccess(this)
  }

  override fun onAdCliclkCompleted() {
    if (bannerAdCallback != null) {
      bannerAdCallback?.reportAdClicked()
      bannerAdCallback?.onAdOpened()
      bannerAdCallback?.onAdLeftApplication()
    }
  }

  override fun onDismissAdScreen() {
    if (bannerAdCallback != null) {
      bannerAdCallback?.onAdClosed()
    }
  }

  override fun onFailed(reason: FailNotificationReason) {
    val error = AdapterHelper.getAdError(reason)
    Log.w(IMobileAdapter.TAG, error.message)
    adLoadCallback.onFailure(error)
  }

  // endregion

  // region MediationBannerAd implementation
  override fun getView() = bannerView!!
  // endregion
}
