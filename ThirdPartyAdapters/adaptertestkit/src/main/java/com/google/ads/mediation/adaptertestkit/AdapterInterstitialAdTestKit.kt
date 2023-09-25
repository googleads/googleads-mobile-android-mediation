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
 * Calls the [Adapter.loadInterstitialAd] method using the given
 * [MediationInterstitialAdConfiguration] and later verifies that
 * [MediationAdLoadCallback.onFailure] is called once with the [expectedAdError].
 */
fun Adapter.loadInterstitialAdWithFailure(
  mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
  mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  expectedAdError: AdError,
) {

  this.loadInterstitialAd(mediationInterstitialAdConfiguration, mediationAdLoadCallback)

  verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/**
 * Calls the [RtbAdapter.loadRtbInterstitialAd] method using the given
 * [MediationInterstitialAdConfiguration] and later verifies that
 * [MediationAdLoadCallback.onFailure] is called once with the [expectedAdError].
 */
fun RtbAdapter.loadRtbInterstitialAdWithFailure(
  mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
  mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  expectedAdError: AdError,
) {

  this.loadRtbInterstitialAd(mediationInterstitialAdConfiguration, mediationAdLoadCallback)

  verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
}

/**
 * Returns an instance of [MediationInterstitialAdConfiguration] with the configuration data used to
 * initialize a [MediationInterstitialAd] before loading the ad.
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
