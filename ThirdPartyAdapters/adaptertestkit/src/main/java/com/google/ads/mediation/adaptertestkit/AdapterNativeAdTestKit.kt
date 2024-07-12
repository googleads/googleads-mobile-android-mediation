// Copyright 2023 Google LLC
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

package com.google.ads.mediation.adaptertestkit

import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.Adapter
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.nativead.NativeAdOptions
import org.mockito.Mockito.mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Calls [Adapter.loadNativeAd] with the given [MediationNativeAdConfiguration] and verifies
 * [MediationAdLoadCallback.onFailure] with the expected [AdError].
 */
fun Adapter.loadNativeAdWithFailure(
  configuration: MediationNativeAdConfiguration,
  callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  expectedAdError: AdError,
) {

  this.loadNativeAd(configuration, callback)

  verify(callback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/**
 * Calls [RtbAdapter.loadRtbNativeAd] with the given [MediationNativeAdConfiguration] and verifies
 * [MediationAdLoadCallback.onFailure] with the expected [AdError].
 */
fun RtbAdapter.loadRtbNativeAdWithFailure(
  configuration: MediationNativeAdConfiguration,
  callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  expectedAdError: AdError,
) {

  this.loadRtbNativeAd(configuration, callback)

  verify(callback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/** Returns a [MediationNativeAdConfiguration] used to initialize [UnifiedNativeAdMapper]. */
fun createMediationNativeAdConfiguration(
  context: Context,
  bidResponse: String = "",
  serverParameters: Bundle = bundleOf(),
  mediationExtras: Bundle = bundleOf(),
  isTesting: Boolean = true,
  location: Location? = null,
  taggedForChildDirectedTreatment: Int =
    RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
  taggedForUnderAgeTreatment: Int = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
  maxAdContentRating: String? = null,
  watermark: String = "",
): MediationNativeAdConfiguration {
  return mock<MediationNativeAdConfiguration>().apply {
    whenever(this.context).thenReturn(context)
    whenever(this.bidResponse).thenReturn(bidResponse)
    whenever(this.serverParameters).thenReturn(serverParameters)
    whenever(this.mediationExtras).thenReturn(mediationExtras)
    whenever(this.isTestRequest).thenReturn(isTesting)
    whenever(this.taggedForChildDirectedTreatment()).thenReturn(taggedForChildDirectedTreatment)
    whenever(this.taggedForUnderAgeTreatment()).thenReturn(taggedForUnderAgeTreatment)
    whenever(this.maxAdContentRating).thenReturn(maxAdContentRating)
    whenever(this.watermark).thenReturn(watermark)
    whenever(this.nativeAdOptions).thenReturn(NativeAdOptions.Builder().build())
  }
}
