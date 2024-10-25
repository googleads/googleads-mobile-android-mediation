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

package com.google.ads.mediation.line

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.five_corp.ad.AdLoader
import com.five_corp.ad.AdLoader.CollectSignalCallback
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.google.android.gms.ads.AdError
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
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks

/**
 * Line Adapter for GMA SDK used to initialize and load ads from the Line SDK. This class should not
 * be used directly by publishers.
 */
class LineMediationAdapter : RtbAdapter() {

  private lateinit var bannerAd: LineBannerAd
  private lateinit var interstitialAd: LineInterstitialAd
  private lateinit var rewardedAd: LineRewardedAd
  private lateinit var nativeAd: LineNativeAd
  private lateinit var adLoader: AdLoader

  override fun getSDKVersionInfo(): VersionInfo {
    val versionString = LineSdkWrapper.delegate.getSdkVersion()
    val splits = versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    if (splits.size >= 3) {
      val major = splits[0].toInt()
      val minor = splits[1].toInt()
      val micro = splits[2].toInt()
      return VersionInfo(major, minor, micro)
    }

    val logMessage =
      String.format(
        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
        versionString,
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
    val appIds =
      mediationConfigurations.mapNotNull {
        val appId = it.serverParameters.getString(KEY_APP_ID)
        if (appId.isNullOrEmpty()) {
          null
        } else {
          appId
        }
      }

    if (appIds.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_APP_ID)
      return
    }

    val appIdForInit = appIds[0]
    if (appIds.size > 1) {
      val message =
        "Multiple $KEY_APP_ID entries found: ${appIds}. Using '${appIdForInit}' to initialize the Line SDK"
      Log.w(TAG, message)
    }

    val loader = AdLoader.getAdLoader(context, FiveAdConfig(appIdForInit))
    if (loader == null) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_NULL_AD_LOADER)
      return
    }

    adLoader = loader

    try {
      LineInitializer.initialize(context, appIdForInit)
    } catch (exception: IllegalArgumentException) {
      exception.message?.let { initializationCompleteCallback.onInitializationFailed(it) }
      return
    }

    initializationCompleteCallback.onInitializationSucceeded()
  }

  override fun collectSignals(signalData: RtbSignalData, signalCallbacks: SignalCallbacks) {
    val slotIds =
      signalData.configurations.mapNotNull {
        val appId = it.serverParameters.getString(KEY_SLOT_ID)
        if (appId.isNullOrEmpty()) {
          null
        } else {
          appId
        }
      }
    if (slotIds.isEmpty() || slotIds.first().isEmpty()) {
      val adError =
        AdError(ERROR_CODE_MISSING_SLOT_ID, ERROR_MSG_MISSING_SLOT_ID, ADAPTER_ERROR_DOMAIN)
      signalCallbacks.onFailure(adError)
      return
    }
    adLoader.collectSignal(
      slotIds.first(),
      object : CollectSignalCallback {
        override fun onCollect(signalString: String) {
          signalCallbacks.onSuccess(signalString)
        }

        override fun onError(fiveAdErrorCode: FiveAdErrorCode) {
          val adError = AdError(fiveAdErrorCode.value, fiveAdErrorCode.name, SDK_ERROR_DOMAIN)
          signalCallbacks.onFailure(adError)
        }
      },
    )
  }

  override fun loadBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    LineBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadAd()
    }
  }

  override fun loadInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    LineInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadAd()
    }
  }

  override fun loadRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    LineRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadAd()
    }
  }

  override fun loadNativeAd(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  ) {
    LineNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadAd()
    }
  }

  override fun loadRtbBannerAd(
    adConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    LineBannerAd.newInstance(adConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadRtbAd()
    }
  }

  override fun loadRtbInterstitialAd(
    adConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    LineInterstitialAd.newInstance(adConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadRtbAd()
    }
  }

  override fun loadRtbRewardedAd(
    adConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    LineRewardedAd.newInstance(adConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadRtbAd()
    }
  }

  override fun loadRtbNativeAdMapper(
    adConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    super.loadRtbNativeAdMapper(adConfiguration, callback)
  }

  companion object {
    private val TAG = LineMediationAdapter::class.simpleName
    @VisibleForTesting var adapterVersionDelegate: String? = null
    const val KEY_APP_ID = "application_id"
    const val KEY_SLOT_ID = "slot_id"
    const val ERROR_MSG_MISSING_APP_ID =
      "Missing or invalid Application ID configured for this ad source instance in the AdMob or Ad Manager UI."
    const val ERROR_MSG_MISSING_SLOT_ID =
      "Missing or invalid Slot ID configured for this ad source instance in the AdMob or Ad Manager UI."
    const val ERROR_MSG_NULL_AD_LOADER = "Null AdLoader from Five Ad SDK."
    const val ERROR_CODE_MISSING_APP_ID = 101
    const val ERROR_CODE_MISSING_SLOT_ID = 102
    const val ERROR_MSG_AD_LOADING = "FiveAd SDK returned a load error with code %s."
    const val ERROR_MSG_AD_SHOWING = "FiveAd SDK could not show ad with error with code %s."
    const val ERROR_CODE_CONTEXT_NOT_AN_ACTIVITY = 104
    const val ERROR_MSG_CONTEXT_NOT_AN_ACTIVITY =
      "Line Interstitial requires an Activity context to load this ad"
    const val ERROR_CODE_FAILED_TO_SHOW_FULLSCREEN = 105
    const val ERROR_MSG_FAILED_TO_SHOW_FULLSCREEN = "Failed to show the ad in fullscreen."
    const val ERROR_CODE_MINIMUM_NATIVE_INFO_NOT_RECEIVED = 106
    const val ERROR_MSG_MINIMUM_NATIVE_INFO_NOT_RECEIVED =
      "Complete required data for Native ads was not received. Skipping Ad."
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.line"
    const val SDK_ERROR_DOMAIN = "com.five_corp.ad"
  }
}
