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

package com.google.ads.mediation.bigo

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.MobileAds.getRequestConfiguration
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
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
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions
import sg.bigo.ads.api.AdConfig

/**
 * Bigo Adapter for GMA SDK used to initialize and load ads from the Bigo SDK. This class should not
 * be used directly by publishers.
 */
class BigoMediationAdapter : RtbAdapter() {

  private lateinit var bannerAd: BigoBannerAd
  private lateinit var interstitialAd: BigoInterstitialAd
  private lateinit var rewardedAd: BigoRewardedAd
  private lateinit var rewardedInterstitialAd: BigoRewardedAd
  private lateinit var nativeAd: BigoNativeAd
  private lateinit var appOpenAd: BigoAppOpenAd

  val versionString: String

  init {
    versionString = "GMA_SDK_${MobileAds.getVersion()}_adapter_$versionInfo"
  }

  override fun getSDKVersionInfo(): VersionInfo =
    bigoSdkVersionDelegate?.let { getSDKVersionInfo(it) }
      ?: getSDKVersionInfo(BigoAdSdk.getSDKVersionName())

  private fun getSDKVersionInfo(sdkVersion: String): VersionInfo {
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
    if (BigoAdSdk.isInitialized()) {
      initializationCompleteCallback.onInitializationSucceeded()
      return
    }

    val applicationIds =
      mediationConfigurations.mapNotNull {
        val appId = it.serverParameters.getString(APP_ID_KEY)
        if (appId.isNullOrEmpty()) {
          null
        } else {
          appId
        }
      }
    if (applicationIds.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_APP_ID)
      return
    }
    val appId = applicationIds[0]
    if (appId.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_APP_ID)
      return
    }
    if (applicationIds.size > 1) {
      val message =
        "Multiple $APP_ID_KEY entries found: ${applicationIds}. Using '${appId}' to initialize the Bigo SDK"
      Log.w(TAG, message)
    }

    configureBigoPrivacy(context)

    val adConfig = AdConfig.Builder().setAppId(appId).build()

    BigoAdSdk.initialize(context, adConfig) {
      initializationCompleteCallback.onInitializationSucceeded()
    }
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    val bidToken = BigoAdSdk.getBidderToken()
    callback.onSuccess(bidToken ?: "")
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    BigoBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadAd()
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    BigoInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadAd(versionString)
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    BigoRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadAd(versionString)
    }
  }

  override fun loadRtbRewardedInterstitialAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    // Reuses Rewarded Ads
    BigoRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedInterstitialAd = it
      rewardedInterstitialAd.loadAd(versionString)
    }
  }

  override fun loadRtbNativeAdMapper(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    BigoNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadAd(versionString)
    }
  }

  override fun loadRtbAppOpenAd(
    mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
    callback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  ) {
    BigoAppOpenAd.newInstance(mediationAppOpenAdConfiguration, callback).onSuccess {
      appOpenAd = it
      appOpenAd.loadAd(versionString)
    }
  }

  private fun configureBigoPrivacy(context: Context) {
    val tagForChildDirected = getRequestConfiguration().tagForChildDirectedTreatment
    val isTaggedForChildDirected =
      tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
    // A value of "true" indicates that the user is not a child under 13 years old, and a value of
    // "false" indicates that the user is a child under 13 years old.
    BigoAdSdk.setUserConsent(context, ConsentOptions.COPPA, !isTaggedForChildDirected)
  }

  internal companion object {
    private val TAG = BigoMediationAdapter::class.simpleName
    @VisibleForTesting var bigoSdkVersionDelegate: String? = null
    @VisibleForTesting var adapterVersionDelegate: String? = null
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.bigo"
    const val SDK_ERROR_DOMAIN = "sg.bigo.ads"
    const val APP_ID_KEY = "application_id"
    const val SLOT_ID_KEY = "slot_id"
    const val ERROR_MSG_MISSING_APP_ID = "App Id to initialize Bigo SDK is empty or missing."
    const val ERROR_CODE_MISSING_SLOT_ID = 101
    const val ERROR_MSG_MISSING_SLOT_ID = "Missing or empty Bigo Slot Id"
  }
}
