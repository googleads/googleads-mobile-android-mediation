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
import com.google.android.gms.ads.mediation.NativeAdMapper
import jp.co.imobile.sdkads.android.FailNotificationReason
import jp.co.imobile.sdkads.android.ImobileSdkAdListener
import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData

/** Listener which listens to I-Mobile's native ad data. */
class NativeAdDataListener(
  private val adLoadCallback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  private val activity: Activity,
) : ImobileSdkAdListener() {

  override fun onNativeAdDataReciveCompleted(adDataList: MutableList<ImobileSdkAdsNativeAdData>?) {
    if (adDataList.isNullOrEmpty()) {
      val error =
        AdError(
          IMobileMediationAdapter.ERROR_EMPTY_NATIVE_ADS_LIST,
          "i-mobile's native ad load success callback returned an empty native ads list.",
          IMobileMediationAdapter.ERROR_DOMAIN,
        )
      Log.w(IMobileMediationAdapter.TAG, error.message)
      adLoadCallback.onFailure(error)
      return
    }

    val adData = adDataList[0]
    adData.getAdImage(activity, NativeAdImageListener(adLoadCallback, activity, adData))
  }

  override fun onFailed(reason: FailNotificationReason) {
    val error = AdapterHelper.getAdError(reason)
    Log.w(IMobileMediationAdapter.TAG, error.message)
    adLoadCallback.onFailure(error)
  }
}
