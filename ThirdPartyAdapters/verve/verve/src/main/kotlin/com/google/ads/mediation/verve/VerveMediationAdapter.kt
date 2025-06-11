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

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
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
  private lateinit var rewardedInterstitialAd: VerveRewardedInterstitialAd
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
    // TODO: Implement this method.
    initializationCompleteCallback.onInitializationSucceeded()
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    // TODO: Implement this method.
    callback.onSuccess("")
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    VerveBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadAd()
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    VerveInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadAd()
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    VerveRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadAd()
    }
  }

  override fun loadRtbRewardedInterstitialAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    VerveRewardedInterstitialAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedInterstitialAd = it
      rewardedInterstitialAd.loadAd()
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
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.verve"
    const val SDK_ERROR_DOMAIN = "" // TODO: Update the third party SDK error domain.
  }
}
