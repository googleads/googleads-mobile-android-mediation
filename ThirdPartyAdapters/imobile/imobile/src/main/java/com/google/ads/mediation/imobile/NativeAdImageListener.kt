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
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import jp.co.imobile.sdkads.android.ImobileSdkAdListener
import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData

/** Listener which listens to I-Mobile's native ad image. */
class NativeAdImageListener(
  private val adLoadCallback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  private val activity: Activity,
  private val adData: ImobileSdkAdsNativeAdData,
) : ImobileSdkAdListener() {

  override fun onNativeAdImageReciveCompleted(image: Bitmap?) {
    val drawable: Drawable = BitmapDrawable(activity.resources, image)
    adLoadCallback.onSuccess(IMobileNativeAdMapper(adData, drawable))
  }
}
