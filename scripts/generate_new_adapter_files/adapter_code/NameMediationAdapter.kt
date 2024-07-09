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

package com.google.ads.mediation.##adapter_name_lower_cased##

import android.content.Context
import com.google.android.gms.ads.VersionInfo
//W->import com.google.android.gms.ads.mediation.Adapter
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
//AO->import com.google.android.gms.ads.mediation.MediationAppOpenAd
//AO->import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
//AO->import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
//B->import com.google.android.gms.ads.mediation.MediationBannerAd
//B->import com.google.android.gms.ads.mediation.MediationBannerAdCallback
//B->import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration
//I->import com.google.android.gms.ads.mediation.MediationInterstitialAd
//I->import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
//I->import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
//N->import com.google.android.gms.ads.mediation.MediationNativeAdCallback
//N->import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
//R->import com.google.android.gms.ads.mediation.MediationRewardedAd
//R->import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
//R->import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
//N->import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
//RTB->import com.google.android.gms.ads.mediation.rtb.RtbAdapter
//RTB->import com.google.android.gms.ads.mediation.rtb.RtbSignalData
//RTB->import com.google.android.gms.ads.mediation.rtb.SignalCallbacks

/**
 * ##adapter_name## Adapter for GMA SDK used to initialize and load ads from the ##adapter_name## SDK. This class should not
 * be used directly by publishers.
 */
//W->class ##adapter_name##MediationAdapter : Adapter() {
//RTB->class ##adapter_name##MediationAdapter : RtbAdapter() {

  //B->private lateinit var bannerAd: ##adapter_name##BannerAd
  //I->private lateinit var interstitialAd: ##adapter_name##InterstitialAd
  //R->private lateinit var rewardedAd: ##adapter_name##RewardedAd
  //RI->private lateinit var rewardedInterstitialAd: ##adapter_name##RewardedInterstitialAd
  //N->private lateinit var nativeAd: ##adapter_name##NativeAd
  //AO->private lateinit var appOpenAd: ##adapter_name##AppOpenAd

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
//WB->
  //WB->override fun loadBannerAd(
  //WB->  mediationBannerAdConfiguration: MediationBannerAdConfiguration,
  //WB->  callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  //WB->) {
  //WB->  ##adapter_name##BannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
  //WB->    bannerAd = it
  //WB->    bannerAd.loadAd()
  //WB->  }
  //WB->}
//WI->
  //WI->override fun loadInterstitialAd(
  //WI->  mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
  //WI->  callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  //WI->) {
  //WI->  ##adapter_name##InterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
  //WI->    interstitialAd = it
  //WI->    interstitialAd.loadAd()
  //WI->  }
  //WI->}
//WR->
  //WR->override fun loadRewardedAd(
  //WR->  mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
  //WR->  callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  //WR->) {
  //WR->  ##adapter_name##RewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
  //WR->    rewardedAd = it
  //WR->    rewardedAd.loadAd()
  //WR->  }
  //WR->}
//WRI->
  //WRI->override fun loadRewardedInterstitialAd(
  //WRI->  mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
  //WRI->  callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  //WRI->) {
  //WRI->  ##adapter_name##RewardedInterstitialAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
  //WRI->    rewardedInterstitialAd = it
  //WRI->    rewardedInterstitialAd.loadAd()
  //WRI->  }
  //WRI->}
//WN->
  //WN->override fun loadNativeAd(
  //WN->  mediationNativeAdConfiguration: MediationNativeAdConfiguration,
  //WN->  callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  //WN->) {
  //WN->  ##adapter_name##NativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
  //WN->    nativeAd = it
  //WN->    nativeAd.loadAd()
  //WN->  }
  //WN->}
//WAO->
  //WAO->override fun loadAppOpenAd(
  //WAO->  mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
  //WAO->  callback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  //WAO->) {
  //WAO->  ##adapter_name##AppOpenAd.newInstance(mediationAppOpenAdConfiguration, callback).onSuccess {
  //WAO->    appOpenAd = it
  //WAO->    appOpenAd.loadAd()
  //WAO->  }
  //WAO->}
//RTB->
  //RTB->override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
  //RTB->  // TODO: Implement this method.
  //RTB->  callback.onSuccess("")
  //RTB->}
//RTBB->
  //RTBB->override fun loadRtbBannerAd(
  //RTBB->  mediationBannerAdConfiguration: MediationBannerAdConfiguration,
  //RTBB->  callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  //RTBB->) {
  //RTBB->  ##adapter_name##BannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
  //RTBB->    bannerAd = it
  //RTBB->    bannerAd.loadAd()
  //RTBB->  }
  //RTBB->}
//RTBI->
  //RTBI->override fun loadRtbInterstitialAd(
  //RTBI->  mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
  //RTBI->  callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  //RTBI->) {
  //RTBI->  ##adapter_name##InterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
  //RTBI->    interstitialAd = it
  //RTBI->    interstitialAd.loadAd()
  //RTBI->  }
  //RTBI->}
//RTBR->
  //RTBR->override fun loadRtbRewardedAd(
  //RTBR->  mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
  //RTBR->  callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  //RTBR->) {
  //RTBR->  ##adapter_name##RewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
  //RTBR->    rewardedAd = it
  //RTBR->    rewardedAd.loadAd()
  //RTBR->  }
  //RTBR->}
//RTBRI->
  //RTBRI->override fun loadRtbRewardedInterstitialAd(
  //RTBRI->  mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
  //RTBRI->  callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  //RTBRI->) {
  //RTBRI->  ##adapter_name##RewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
  //RTBRI->    rewardedAd = it
  //RTBRI->    rewardedAd.loadAd()
  //RTBRI->  }
  //RTBRI->}
//RTBN->
  //RTBN->override fun loadRtbNativeAd(
  //RTBN->  mediationNativeAdConfiguration: MediationNativeAdConfiguration,
  //RTBN->  callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  //RTBN->) {
  //RTBN->  ##adapter_name##NativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
  //RTBN->    nativeAd = it
  //RTBN->    nativeAd.loadAd()
  //RTBN->  }
  //RTBN->}
//RTBAO->
  //RTBAO->override fun loadRtbAppOpenAd(
  //RTBAO->  mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
  //RTBAO->  callback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>,
  //RTBAO->) {
  //RTBAO->  ##adapter_name##AppOpenAd.newInstance(mediationAppOpenAdConfiguration, callback).onSuccess {
  //RTBAO->    appOpenAd = it
  //RTBAO->    appOpenAd.loadAd()
  //RTBAO->  }
  //RTBAO->}
}
