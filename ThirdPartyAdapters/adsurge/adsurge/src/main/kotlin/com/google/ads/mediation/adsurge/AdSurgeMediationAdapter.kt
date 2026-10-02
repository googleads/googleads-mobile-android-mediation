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

package com.google.ads.mediation.adsurge

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.adsurge.adn.ads.AdSurgeAdError
import com.adsurge.adn.managers.AdSurgeAdSdk
import com.adsurge.adn.managers.OnStartListener
import com.google.android.gms.ads.MobileAds
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
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks

/**
 * AdSurge Adapter for GMA SDK used to initialize and load ads from the AdSurge SDK. This class
 * should not be used directly by publishers.
 */
class AdSurgeMediationAdapter : RtbAdapter() {

  private lateinit var bannerAd: AdSurgeBannerAd
  private lateinit var interstitialAd: AdSurgeInterstitialAd
  private lateinit var rewardedAd: AdSurgeRewardedAd

  override fun getSDKVersionInfo(): VersionInfo {
    val versionString = sdkVersionDelegate ?: AdSurgeFactory.getSdkVersion()
    val splits = versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
    if (splits.size >= 3) {
      val major = splits[0].toIntOrNull()
      val minor = splits[1].toIntOrNull()
      val micro = splits[2].toIntOrNull()
      if (major != null && minor != null && micro != null) {
        return VersionInfo(major, minor, micro)
      }
    }

    Log.w(TAG, "Unexpected SDK version format: $versionString. Returning 0.0.0 for SDK version.")
    return VersionInfo(0, 0, 0)
  }

  override fun getVersionInfo(): VersionInfo {
    val versionString = adapterVersionDelegate ?: BuildConfig.ADAPTER_VERSION
    val splits = versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
    if (splits.size >= 4) {
      val major = splits[0].toIntOrNull()
      val minor = splits[1].toIntOrNull()
      val micro = splits[2].toIntOrNull()
      val patch = splits[3].toIntOrNull()
      if (major != null && minor != null && micro != null && patch != null) {
        return VersionInfo(major, minor, micro * 100 + patch)
      }
    }

    Log.w(
      TAG,
      "Unexpected adapter version format: $versionString. Returning 0.0.0 for adapter version.",
    )
    return VersionInfo(0, 0, 0)
  }

  override fun initialize(
    context: Context,
    initializationCompleteCallback: InitializationCompleteCallback,
    mediationConfigurations: List<MediationConfiguration>,
  ) {
    val appId = AdSurgeUtils.getAppId(mediationConfigurations)
    if (appId.isNullOrEmpty()) {
      initializationCompleteCallback.onInitializationFailed(
        AdSurgeUtils.createAdapterError(
          AdSurgeUtils.ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid application_id.",
        )
      )
      return
    }

    val requestConfiguration = MobileAds.getRequestConfiguration()
    AdSurgeUtils.applyPrivacyConfiguration(
      childDirected = requestConfiguration.tagForChildDirectedTreatment,
      underAgeOfConsent = requestConfiguration.tagForUnderAgeOfConsent,
      factory = AdSurgeFactory,
    )

    AdSurgeFactory.init(
      context,
      appId,
      object : OnStartListener {
        override fun onStartComplete() {
          initializationCompleteCallback.onInitializationSucceeded()
        }

        override fun onStartFailed(error: AdSurgeAdError) {
          initializationCompleteCallback.onInitializationFailed(AdSurgeUtils.createSdkError(error))
        }
      },
    )
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    AdSurgeFactory.getBidderToken(
      object : AdSurgeAdSdk.BidderTokenCallback {
        override fun onSuccess(token: String) {
          callback.onSuccess(token)
        }

        override fun onFailure(error: AdSurgeAdError) {
          callback.onFailure(AdSurgeUtils.createSdkError(error))
        }
      }
    )
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    AdSurgeBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadAd()
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    AdSurgeInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadAd()
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    AdSurgeRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadAd()
    }
  }

  companion object {
    private val TAG = AdSurgeMediationAdapter::class.simpleName
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.adsurge"
    const val SDK_ERROR_DOMAIN = "com.adsurge.adn"

    @VisibleForTesting internal var adapterVersionDelegate: String? = null
    @VisibleForTesting internal var sdkVersionDelegate: String? = null
  }
}
