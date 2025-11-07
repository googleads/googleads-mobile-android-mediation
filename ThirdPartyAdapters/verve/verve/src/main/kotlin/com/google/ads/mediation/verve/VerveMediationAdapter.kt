// Copyright 2025 Google LLC
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

package com.google.ads.mediation.verve

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import net.pubnative.lite.sdk.HyBid

/**
 * Verve Adapter for GMA SDK used to initialize and load ads from the Verve SDK. This class should
 * not be used directly by publishers.
 */
class VerveMediationAdapter : RtbAdapter() {

  private lateinit var bannerAd: VerveBannerAd
  private lateinit var interstitialAd: VerveInterstitialAd
  private lateinit var rewardedAd: VerveRewardedAd
  private lateinit var rewardedInterstitialAd: VerveRewardedAd
  private lateinit var nativeAd: VerveNativeAd

  override fun getSDKVersionInfo(): VersionInfo {
    val sdkVersion = HyBid.getHyBidVersion()
    val splits = sdkVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    if (splits.size >= 3) {
      val major = splits[0].toInt()
      val minor = splits[1].toInt()
      val micro = splits[2].toInt()
      return VersionInfo(major, minor, micro)
    }

    val logMessage =
      String.format(
        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
        sdkVersion,
      )
    Log.w(TAG, logMessage)
    return VersionInfo(0, 0, 0)
  }

  override fun getVersionInfo(): VersionInfo =
    adapterVersionDelegate?.let { getVersionInfo(it) }
      ?: getVersionInfo(BuildConfig.ADAPTER_VERSION)

  private fun getVersionInfo(versionString: String): VersionInfo {
    val splits = versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (splits.size >= 4) {
      val major = splits[0].toInt()
      val minor = splits[1].toInt()
      val micro = splits[2].toInt() * 100 + splits[3].toInt()
      return VersionInfo(major, minor, micro)
    }

    val logMessage =
      String.format(
        "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
        versionString,
      )
    Log.w(TAG, logMessage)
    return VersionInfo(0, 0, 0)
  }

  override fun initialize(
    context: Context,
    initializationCompleteCallback: InitializationCompleteCallback,
    mediationConfigurations: List<MediationConfiguration>,
  ) {
    val requestConfiguration: RequestConfiguration = MobileAds.getRequestConfiguration()
    val isChildUser =
      (requestConfiguration.tagForChildDirectedTreatment ==
        TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) ||
        requestConfiguration.tagForUnderAgeOfConsent == TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
    if (isChildUser) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_CHILD_USER)
      return
    }
    val appTokens =
      mediationConfigurations.mapNotNull {
        val sourceId = it.serverParameters.getString(APP_TOKEN_KEY)
        if (sourceId.isNullOrEmpty()) {
          null
        } else {
          sourceId
        }
      }
    if (appTokens.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_APP_TOKEN)
      return
    }

    val appTokenForInit = appTokens[0]
    if (appTokenForInit.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_APP_TOKEN)
      return
    }
    if (appTokens.size > 1) {
      val message =
        "Multiple $APP_TOKEN_KEY entries found: ${appTokens}. Using '${appTokenForInit}' to initialize the BidMachine SDK"
      Log.w(TAG, message)
    }
    HyBid.setTestMode(VerveExtras.isTestMode)
    HyBid.initialize(appTokenForInit, context.applicationContext as Application) { success ->
      if (success) {
        initializationCompleteCallback.onInitializationSucceeded()
      } else {
        initializationCompleteCallback.onInitializationFailed(ERROR_MSG_ERROR_INITIALIZE_VERVE_SDK)
      }
    }
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    val adSize = signalData.adSize
    if (
      signalData.configurations.isNotEmpty() &&
        signalData.configurations.first().format == AdFormat.BANNER &&
        adSize != null &&
        VerveBannerAd.mapAdSize(adSize, signalData.context) == null
    ) {
      val adError =
        AdError(ERROR_CODE_UNSUPPORTED_AD_SIZE, ERROR_MSG_UNSUPPORTED_AD_SIZE, ADAPTER_ERROR_DOMAIN)
      callback.onFailure(adError)
      return
    }
    val signals = HyBid.getEncodedCustomRequestSignalData(signalData.context, "Admob")
    callback.onSuccess(signals)
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    VerveBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadAd(mediationBannerAdConfiguration.context)
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    VerveInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadAd(mediationInterstitialAdConfiguration.context)
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    VerveRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadAd(mediationRewardedAdConfiguration.context)
    }
  }

  override fun loadRtbRewardedInterstitialAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    VerveRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedInterstitialAd = it
      rewardedInterstitialAd.loadAd(mediationRewardedAdConfiguration.context)
    }
  }

  override fun loadRtbNativeAdMapper(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    VerveNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadAd()
    }
  }

  companion object {
    private val TAG = VerveMediationAdapter::class.simpleName
    @VisibleForTesting var adapterVersionDelegate: String? = null
    const val APP_TOKEN_KEY = "AppToken"
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.verve"
    const val SDK_ERROR_DOMAIN = "net.pubnative.lite.sdk"
    const val ERROR_CODE_UNSUPPORTED_AD_SIZE = 101
    const val ERROR_MSG_UNSUPPORTED_AD_SIZE = "HyBid: Unsupported Ad Size requested"
    const val ERROR_CODE_AD_LOAD_FAILED_TO_LOAD = 102
    const val ERROR_MSG_MISSING_APP_TOKEN = "AppToken is missing or empty"
    const val ERROR_CODE_FULLSCREEN_AD_IS_NULL = 103
    const val ERROR_MSG_FULLSCREEN_AD_IS_NULL = "Attempted to show a null fullscreen ad"
    const val ERROR_MSG_ERROR_INITIALIZE_VERVE_SDK =
      "There was an internal error during the initialization of HyBid SDK."
    const val ERROR_MSG_CHILD_USER =
      "MobileAds.getRequestConfiguration() indicates the user is a child. Verve will be dropped"
  }
}
