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
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify

/**
 * Calls [Adapter.loadInterstitialAd] with the given [MediationInterstitialAdConfiguration] and
 * verifies [MediationAdLoadCallback.onFailure] with the expected [AdError].
 */
fun Adapter.loadInterstitialAdWithFailure(
  configuration: MediationInterstitialAdConfiguration,
  callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  expectedAdError: AdError,
) {

  this.loadInterstitialAd(configuration, callback)

  verify(callback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/**
 * Calls [RtbAdapter.loadRtbInterstitialAd] with the given [MediationInterstitialAdConfiguration]
 * and verifies [MediationAdLoadCallback.onFailure] with the expected [AdError].
 */
fun RtbAdapter.loadRtbInterstitialAdWithFailure(
  configuration: MediationInterstitialAdConfiguration,
  callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  expectedAdError: AdError,
) {

  this.loadRtbInterstitialAd(configuration, callback)

  verify(callback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/**
 * Returns a [MediationInterstitialAdConfiguration] used to initialize [MediationInterstitialAd].
 */
fun createMediationInterstitialAdConfiguration(
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
): MediationInterstitialAdConfiguration =
  MediationInterstitialAdConfiguration(
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
