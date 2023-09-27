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
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify

/**
 * Calls [Adapter.loadNativeAd] with the given [MediationNativeAdConfiguration] and verifies
 * [MediationAdLoadCallback.onFailure] with the expected [AdError].
 */
fun Adapter.loadNativeAdWithFailure(
  configuration: MediationNativeAdConfiguration,
  callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  expectedAdError: AdError,
) {

  this.loadNativeAd(mediationNativeAdConfiguration, mediationAdLoadCallback)

  verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
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

  this.loadRtbNativeAd(mediationNativeAdConfiguration, mediationAdLoadCallback)

  verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
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
  watermark: String = ""
): MediationNativeAdConfiguration =
  MediationNativeAdConfiguration(
    context,
    bidResponse,
    serverParameters,
    mediationExtras,
    isTesting,
    location,
    taggedForChildDirectedTreatment,
    taggedForUnderAgeTreatment,
    maxAdContentRating,
    watermark,
    null
  )
