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
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper

/** I-Mobile native ad loader. */
class IMobileNativeAdLoader {

  fun loadAd(
    mediationNativeAdConfig: MediationNativeAdConfiguration,
    adLoadCallback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
    iMobileSdkWrapper: IMobileSdkWrapper,
  ) {
    val context = mediationNativeAdConfig.context
    // Validate Context.
    if (context !is Activity) {
      val error =
        AdError(
          IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Context is not an Activity. ",
          IMobileMediationAdapter.ERROR_DOMAIN,
        )
      Log.w(IMobileMediationAdapter.TAG, error.message)
      adLoadCallback.onFailure(error)
      return
    }
    val activity = context as Activity

    val serverParameters = mediationNativeAdConfig.serverParameters
    // Get parameters for i-mobile SDK.
    val publisherId: String? = serverParameters.getString(Constants.KEY_PUBLISHER_ID)
    val mediaId: String? = serverParameters.getString(Constants.KEY_MEDIA_ID)
    val spotId: String? = serverParameters.getString(Constants.KEY_SPOT_ID)

    // Call i-mobile SDK.
    iMobileSdkWrapper.registerSpotInline(activity, publisherId, mediaId, spotId)
    iMobileSdkWrapper.start(spotId)
    iMobileSdkWrapper.getNativeAdData(
      activity,
      spotId,
      NativeAdDataListener(adLoadCallback, activity),
    )
  }
}
