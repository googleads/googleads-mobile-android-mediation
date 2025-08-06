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
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.MobileAds.getRequestConfiguration
import com.google.android.gms.ads.RequestConfiguration
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
import io.bidmachine.AdPlacementConfig
import io.bidmachine.AdsFormat
import io.bidmachine.BidMachine
import io.bidmachine.banner.BannerView
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.nativead.NativeAd
import io.bidmachine.rewarded.RewardedAd

/**
 * BidMachine Adapter for GMA SDK used to initialize and load ads from the BidMachine SDK. This
 * class should not be used directly by publishers.
 */
class BidMachineMediationAdapter : RtbAdapter() {

  private lateinit var bannerAd: BidMachineBannerAd
  private lateinit var interstitialAd: BidMachineInterstitialAd
  private lateinit var rewardedAd: BidMachineRewardedAd
  private lateinit var nativeAd: BidMachineNativeAd

  override fun getSDKVersionInfo(): VersionInfo =
    bidMachineSdkVersionDelegate?.let { getSDKVersionInfo(it) }
      ?: getSDKVersionInfo(BidMachine.VERSION)

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
    val sourceIds =
      mediationConfigurations.mapNotNull {
        val sourceId = it.serverParameters.getString(SOURCE_ID_KEY)
        if (sourceId.isNullOrEmpty()) {
          null
        } else {
          sourceId
        }
      }
    if (sourceIds.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_SOURCE_ID)
      return
    }

    val sourceIdForInit = sourceIds[0]
    if (sourceIdForInit.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(ERROR_MSG_MISSING_SOURCE_ID)
      return
    }
    if (sourceIds.size > 1) {
      val message =
        "Multiple $SOURCE_ID_KEY entries found: ${sourceIds}. Using '${sourceIdForInit}' to initialize the BidMachine SDK"
      Log.w(TAG, message)
    }

    configureBidMachinePrivacy()

    BidMachine.initialize(context, sourceIdForInit) {
      initializationCompleteCallback.onInitializationSucceeded()
    }
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    if (signalData.configurations.isEmpty()) {
      val adError =
        AdError(
          ERROR_CODE_EMPTY_SIGNAL_CONFIGURATIONS,
          ERROR_MSG_EMPTY_SIGNAL_CONFIGURATIONS,
          ADAPTER_ERROR_DOMAIN,
        )
      callback.onFailure(adError)
      return
    }
    val adsFormat = mapAdFormatToBidMachineAdsFormat(signalData.configurations[0].format)
    if (adsFormat == null) {
      val adError =
        AdError(ERROR_CODE_INVALID_AD_FORMAT, ERROR_MSG_INVALID_AD_FORMAT, ADAPTER_ERROR_DOMAIN)
      callback.onFailure(adError)
      return
    }
    val placementId = signalData.configurations[0].serverParameters.getString(PLACEMENT_ID_KEY)
    val adPlacementConfig = AdPlacementConfig(adsFormat, placementId, customParams = null)
    BidMachine.getBidToken(signalData.context, adPlacementConfig) { bidToken ->
      callback.onSuccess(bidToken)
    }
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    BidMachineBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      val bannerView = BannerView(mediationBannerAdConfiguration.context)
      bannerAd.loadAdOnBannerView(bannerView)
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    BidMachineInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      val bidMachineInterstitialAd = InterstitialAd(mediationInterstitialAdConfiguration.context)
      interstitialAd.loadAd(bidMachineInterstitialAd)
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    BidMachineRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      val bidMachineRewardedAd = RewardedAd(mediationRewardedAdConfiguration.context)
      rewardedAd.loadAd(bidMachineRewardedAd)
    }
  }

  override fun loadRtbNativeAdMapper(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    BidMachineNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      val bidMachineNativeAd = NativeAd(mediationNativeAdConfiguration.context)
      nativeAd.loadAd(bidMachineNativeAd)
    }
  }

  private fun mapAdFormatToBidMachineAdsFormat(adFormat: AdFormat): AdsFormat? =
    when (adFormat) {
      AdFormat.BANNER -> AdsFormat.Banner
      AdFormat.INTERSTITIAL -> AdsFormat.Interstitial
      AdFormat.REWARDED -> AdsFormat.Rewarded
      AdFormat.NATIVE -> AdsFormat.Native
      else -> null
    }

  private fun configureBidMachinePrivacy() {
    val tagForChildDirected = getRequestConfiguration().tagForChildDirectedTreatment
    val isTaggedForChildDirected =
      tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
    BidMachine.setCoppa(isTaggedForChildDirected)
  }

  internal companion object {
    private val TAG = BidMachineMediationAdapter::class.simpleName
    @VisibleForTesting var bidMachineSdkVersionDelegate: String? = null
    @VisibleForTesting var adapterVersionDelegate: String? = null
    @VisibleForTesting const val SOURCE_ID_KEY = "source_id"
    const val PLACEMENT_ID_KEY = "placement_id"
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.bidmachine"
    const val SDK_ERROR_DOMAIN = "io.bidmachine"
    @VisibleForTesting const val ERROR_MSG_MISSING_SOURCE_ID = "Source Id is missing or empty"
    const val ERROR_CODE_NO_PLACEMENT_ID = 100
    const val ERROR_MSG_NO_PLACEMENT_ID = "Invalid or empty placement id received."
    const val ERROR_CODE_EMPTY_SIGNAL_CONFIGURATIONS = 101
    const val ERROR_MSG_EMPTY_SIGNAL_CONFIGURATIONS =
      "Error during signal collection: No Signal Data Configuration found."
    const val ERROR_CODE_INVALID_AD_FORMAT = 102
    const val ERROR_MSG_INVALID_AD_FORMAT = "Invalid Ad Format received during signal collection."
    const val ERROR_CODE_INVALID_AD_SIZE = 103
    const val ERROR_MSG_INVALID_AD_SIZE =
      "Requested ad size could not be mapped to bidmachine.BannerSize"
    const val ERROR_CODE_AD_REQUEST_EXPIRED = 104
    const val ERROR_MSG_AD_REQUEST_EXPIRED = "Loaded BidMachine ad request has expired."
    const val ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD = 105
    const val ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD = "Fullscreen ad could not be shown."
    const val ERROR_CODE_EMPTY_NATIVE_AD_DATA = 106
    const val ERROR_MSG_EMPTY_NATIVE_AD_DATA = "Native Ad Data received was null."
  }
}
