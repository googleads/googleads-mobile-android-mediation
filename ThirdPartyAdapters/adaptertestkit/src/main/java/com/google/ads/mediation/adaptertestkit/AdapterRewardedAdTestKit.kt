package com.google.ads.mediation.adaptertestkit

import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.Adapter
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify

/**
 * Calls [Adapter.loadRewardedAd] with the given [MediationRewardedAdConfiguration] and verifies
 * [MediationAdLoadCallback.onFailure] with the expected [AdError].
 */
fun Adapter.loadRewardedAdWithFailure(
  configuration: MediationRewardedAdConfiguration,
  callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  expectedAdError: AdError,
) {

  this.loadRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback)

  verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/**
 * Calls [RtbAdapter.loadRtbRewardedAd] with the given [MediationRewardedAdConfiguration] and
 * verifies [MediationAdLoadCallback.onFailure] with the expected [AdError].
 */
fun RtbAdapter.loadRtbRewardedAdWithFailure(
  configuration: MediationRewardedAdConfiguration,
  callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  expectedAdError: AdError,
) {

  this.loadRtbRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback)

  verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/** Returns a [MediationRewardedAdConfiguration] used to initialize [MediationRewardedAd]. */
fun createMediationRewardedAdConfiguration(
  context: Context,
  bidResponse: String = "",
  serverParameters: Bundle = bundleOf(),
  mediationExtras: Bundle = bundleOf(),
  isTesting: Boolean = true,
  location: Location? = null,
  taggedForChildDirectedTreatment: Int = TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
  taggedForUnderAgeTreatment: Int = TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
  maxAdContentRating: String? = null,
  watermark: String = ""
): MediationRewardedAdConfiguration =
  MediationRewardedAdConfiguration(
    context,
    bidResponse,
    serverParameters,
    mediationExtras,
    isTesting,
    location,
    taggedForChildDirectedTreatment,
    taggedForUnderAgeTreatment,
    maxAdContentRating,
    watermark
  )
