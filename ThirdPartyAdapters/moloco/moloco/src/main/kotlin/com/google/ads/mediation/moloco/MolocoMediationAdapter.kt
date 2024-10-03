// Copyright 2024 Google LLC
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

package com.google.ads.mediation.moloco

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.moloco.sdk.publisher.Initialization
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.init.MolocoInitParams

/**
 * Moloco Adapter for GMA SDK used to initialize and load ads from the Moloco SDK. This class should
 * not be used directly by publishers.
 */
class MolocoMediationAdapter : RtbAdapter() {

  private lateinit var bannerAd: MolocoBannerAd
  private lateinit var interstitialAd: MolocoInterstitialAd
  private lateinit var rewardedAd: MolocoRewardedAd
  private lateinit var nativeAd: MolocoNativeAd

  override fun getSDKVersionInfo(): VersionInfo {
    return VersionInfo(
      com.moloco.sdk.BuildConfig.SDK_VERSION_MAJOR,
      com.moloco.sdk.BuildConfig.SDK_VERSION_MINOR,
      com.moloco.sdk.BuildConfig.SDK_VERSION_MICRO,
    )
  }

  override fun getVersionInfo(): VersionInfo {
    val adapterVersion = MolocoAdapterUtils.adapterVersion
    val splits = adapterVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (splits.size >= 4) {
      val major = splits[0].toInt()
      val minor = splits[1].toInt()
      val micro = splits[2].toInt() * 100 + splits[3].toInt()
      return VersionInfo(major, minor, micro)
    }

    val logMessage =
      String.format(
        "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
        adapterVersion,
      )
    Log.w(TAG, logMessage)
    return VersionInfo(0, 0, 0)
  }

  private fun configurePrivacy() {
    val isAgeRestricted =
      MobileAds.getRequestConfiguration().tagForChildDirectedTreatment ==
        TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE ||
        MobileAds.getRequestConfiguration().tagForUnderAgeOfConsent ==
          TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
    MolocoAdapterUtils.setMolocoIsAgeRestricted(isAgeRestricted)
  }

  override fun initialize(
    context: Context,
    initializationCompleteCallback: InitializationCompleteCallback,
    mediationConfigurations: List<MediationConfiguration>,
  ) {
    val appKeys =
      mediationConfigurations.mapNotNull {
        val appKey = it.serverParameters.getString(KEY_APP_KEY)
        if (appKey.isNullOrEmpty()) {
          null
        } else {
          appKey
        }
      }

    if (appKeys.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_APP_KEY)
      return
    }

    val appKeyForInit = appKeys[0]
    if (appKeys.size > 1) {
      val message =
        "Multiple $KEY_APP_KEY entries found: ${appKeys}. Using '${appKeyForInit}' to initialize the Moloco SDK"
      Log.w(TAG, message)
    }

    val mediationInfo = MediationInfo(Companion::class.java.name)
    val initParams = MolocoInitParams(context, appKeyForInit, mediationInfo)
    Moloco.initialize(initParams) { status ->
      if (status.initialization == Initialization.SUCCESS) {
        configurePrivacy()
        initializationCompleteCallback.onInitializationSucceeded()
      } else {
        initializationCompleteCallback.onInitializationFailed(
          "Moloco SDK failed to initialize: ${status.description}."
        )
      }
    }
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    Moloco.getBidToken { bidToken: String, errorType: MolocoAdError.ErrorType? ->
      if (errorType != null) {
        val adError = AdError(errorType.errorCode, errorType.description, SDK_ERROR_DOMAIN)
        callback.onFailure(adError)
        return@getBidToken
      }
      callback.onSuccess(bidToken)
    }
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    MolocoBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadAd()
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    MolocoInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadAd()
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    MolocoRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadAd()
    }
  }

  override fun loadRtbNativeAd(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  ) {
    MolocoNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadAd()
    }
  }

  companion object {
    private val TAG = MolocoMediationAdapter::class.simpleName
    const val KEY_APP_KEY = "app_key"
    const val KEY_AD_UNIT_ID = "ad_unit_id"
    const val ERROR_CODE_MISSING_APP_KEY = 101
    const val ERROR_CODE_MISSING_AD_UNIT = 102
    const val ERROR_CODE_MISSING_AD_FAILED_TO_CREATE = 103
    const val ERROR_MSG_MISSING_APP_KEY =
      "Missing or invalid App Key configured for this ad source instance in the AdMob or Ad Manager UI."
    const val ERROR_MSG_MISSING_AD_UNIT =
      "Missing or invalid Ad Unit configured for this ad source instance in the AdMob or Ad Manager UI."
    const val ERROR_MSG_MISSING_AD_FAILED_TO_CREATE = "Create Ad object returned was null."
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.moloco"
    const val SDK_ERROR_DOMAIN = "com.moloco.sdk"
  }
}
