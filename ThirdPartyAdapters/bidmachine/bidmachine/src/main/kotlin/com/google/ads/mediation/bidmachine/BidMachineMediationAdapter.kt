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

package com.google.ads.mediation.bidmachine

import android.content.Context
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

/**
 * BidMachine Adapter for GMA SDK used to initialize and load ads from the BidMachine SDK. This
 * class should not be used directly by publishers.
 */
class BidMachineMediationAdapter : RtbAdapter() {

  private lateinit var bannerAd: BidMachineBannerAd
  private lateinit var interstitialAd: BidMachineInterstitialAd
  private lateinit var rewardedAd: BidMachineRewardedAd
  private lateinit var nativeAd: BidMachineNativeAd

  override fun getSDKVersionInfo(): VersionInfo {
    // TODO: Update the version number returned.
    return VersionInfo(0, 0, 0)
  }

  override fun getVersionInfo(): VersionInfo {
    // TODO: Update the version number returned.
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
    BidMachineBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadAd()
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    BidMachineInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadAd()
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    BidMachineRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadAd()
    }
  }

  override fun loadRtbNativeAdMapper(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    BidMachineNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadAd()
    }
  }

  companion object {
    private val TAG = BidMachineMediationAdapter::class.simpleName
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.bidmachine"
    const val SDK_ERROR_DOMAIN = "" // TODO: Update the third party SDK error domain.
  }
}
