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

package com.google.ads.mediation.pubmatic

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
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
import com.pubmatic.sdk.common.OpenWrapSDK
import com.pubmatic.sdk.common.OpenWrapSDKConfig
import com.pubmatic.sdk.common.OpenWrapSDKInitializer
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.openwrap.core.signal.POBSignalConfig
import java.lang.NumberFormatException

/**
 * PubMatic Adapter for GMA SDK used to initialize and load ads from the PubMatic SDK. This class
 * should not be used directly by publishers.
 */
class PubMaticMediationAdapter(
  private val pubMaticSignalGenerator: PubMaticSignalGenerator = PubMaticSignalGeneratorImpl(),
  private val pubMaticAdFactory: PubMaticAdFactory = PubMaticAdFactoryImpl(),
) : RtbAdapter() {

  private lateinit var bannerAd: PubMaticBannerAd
  private lateinit var interstitialAd: PubMaticInterstitialAd
  private lateinit var rewardedAd: PubMaticRewardedAd
  private lateinit var nativeAd: PubMaticNativeAd

  override fun getSDKVersionInfo(): VersionInfo {
    val sdkVersion = OpenWrapSDK.getVersion()
    val splits = sdkVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    if (splits.size >= 3) {
      val major = splits[0].toInt()
      val minor = splits[1].toInt()
      val micro = splits[2].toInt()
      return VersionInfo(major, minor, micro)
    }

    val logMessage = "Unexpected SDK version format: $sdkVersion. Returning 0.0.0 for SDK version."
    Log.w(TAG, logMessage)
    return VersionInfo(0, 0, 0)
  }

  override fun getVersionInfo(): VersionInfo {
    val adapterVersion = BuildConfig.ADAPTER_VERSION
    val splits = adapterVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (splits.size >= 4) {
      val major = splits[0].toInt()
      val minor = splits[1].toInt()
      val micro = splits[2].toInt() * 100 + splits[3].toInt()
      return VersionInfo(major, minor, micro)
    }

    val logMessage =
      "Unexpected adapter version format: $adapterVersion. Returning 0.0.0 for adapter version."
    Log.w(TAG, logMessage)
    return VersionInfo(0, 0, 0)
  }

  override fun initialize(
    context: Context,
    initializationCompleteCallback: InitializationCompleteCallback,
    mediationConfigurations: List<MediationConfiguration>,
  ) {
    // Set child-directed bit as part of initialization.
    val tagForChildDirectedTreatment =
      MobileAds.getRequestConfiguration().tagForChildDirectedTreatment
    if (tagForChildDirectedTreatment == TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
      OpenWrapSDK.setCoppa(true)
    } else if (tagForChildDirectedTreatment == TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
      OpenWrapSDK.setCoppa(false)
    }

    val publisherId = getPublisherId(mediationConfigurations)
    if (publisherId == null) {
      val adError =
        AdError(ERROR_MISSING_PUBLISHER_ID, "Publisher ID is missing.", ADAPTER_ERROR_DOMAIN)
      initializationCompleteCallback.onInitializationFailed(adError.toString())
      return
    }

    val profileIds = getProfileIds(mediationConfigurations)

    val openWrapSDKConfig =
      OpenWrapSDKConfig.Builder(publisherId = publisherId, profileIds = profileIds).build()
    OpenWrapSDK.initialize(
      context,
      openWrapSDKConfig,
      object : OpenWrapSDKInitializer.Listener {
        override fun onFailure(error: POBError) {
          val adError = AdError(error.errorCode, error.errorMessage, SDK_ERROR_DOMAIN)
          initializationCompleteCallback.onInitializationFailed(adError.toString())
        }

        override fun onSuccess() {
          initializationCompleteCallback.onInitializationSucceeded()
        }
      },
    )
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    // The ad format set in the GMA SDK ad request.
    val gmaAdFormat = signalData.configurations.firstOrNull()?.format
    if (gmaAdFormat == null) {
      callback.onFailure(
        AdError(
          ERROR_INVALID_AD_FORMAT,
          "Ad format missing in RTB signal data",
          ADAPTER_ERROR_DOMAIN,
        )
      )
      return
    }

    val pubMaticAdFormat = getPubMaticFormatFromGmaFormat(gmaAdFormat, signalData.adSize)
    if (pubMaticAdFormat == null) {
      callback.onFailure(
        AdError(ERROR_INVALID_AD_FORMAT, "Ad format unsupported by PubMatic", ADAPTER_ERROR_DOMAIN)
      )
      return
    }

    callback.onSuccess(
      pubMaticSignalGenerator.generateSignal(
        signalData.context,
        POBBiddingHost.ADMOB,
        POBSignalConfig.Builder(pubMaticAdFormat).build(),
      )
    )
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    PubMaticBannerAd.newInstance(mediationBannerAdConfiguration, callback, pubMaticAdFactory)
      .onSuccess {
        bannerAd = it
        bannerAd.loadAd()
      }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    PubMaticInterstitialAd.newInstance(
        mediationInterstitialAdConfiguration,
        callback,
        pubMaticAdFactory,
      )
      .onSuccess {
        interstitialAd = it
        interstitialAd.loadAd()
      }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    PubMaticRewardedAd.newInstance(mediationRewardedAdConfiguration, callback, pubMaticAdFactory)
      .onSuccess {
        rewardedAd = it
        rewardedAd.loadAd()
      }
  }

  override fun loadRtbNativeAdMapper(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    PubMaticNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadAd()
    }
  }

  companion object {
    private val TAG = PubMaticMediationAdapter::class.simpleName
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.pubmatic"
    const val SDK_ERROR_DOMAIN = "com.pubmatic.sdk"
    const val KEY_PUBLISHER_ID = "publisher_id"
    const val KEY_PROFILE_ID = "profile_id"

    const val ERROR_MISSING_PUBLISHER_ID = 101

    const val ERROR_INVALID_AD_FORMAT = 102

    const val ERROR_AD_NOT_READY = 103

    /**
     * Gets the PubMatic publisher ID from ad unit mappings.
     *
     * Returns null if no publisher ID was found.
     */
    fun getPublisherId(mediationConfigurations: List<MediationConfiguration>): String? {
      val publisherIds =
        mediationConfigurations
          .mapNotNull { it.serverParameters.getString(KEY_PUBLISHER_ID) }
          .toSet()

      if (publisherIds.isEmpty()) {
        return null
      }

      val chosenPublisherId = publisherIds.iterator().next()

      if (publisherIds.size > 1) {
        Log.w(
          TAG,
          "Found more than one PubMatic publisher ID. Using $chosenPublisherId. Please update your app's ad unit mappings on Admob/GAM UI to use a single publisher ID for ad serving to work as expected.",
        )
      }

      return chosenPublisherId
    }

    /** Gets PubMatic profile IDs from ad unit mappings. */
    fun getProfileIds(mediationConfigurations: List<MediationConfiguration>): List<Int> {
      val profileIds =
        mediationConfigurations
          .mapNotNull {
            val profileIdString = it.serverParameters.getString(KEY_PROFILE_ID)
            try {
              profileIdString?.toInt()
            } catch (numberFormatException: NumberFormatException) {
              Log.w(TAG, "PubMatic profile ID should be an integer. Found $profileIdString")
              null
            }
          }
          .toSet()

      if (profileIds.isEmpty()) {
        Log.w(TAG, "Found zero PubMatic profile IDs.")
      }

      return profileIds.toList()
    }

    /**
     * Maps Google ad format to PubMatic ad format.
     *
     * Returns null if the ad format is not supported by PubMatic.
     */
    fun getPubMaticFormatFromGmaFormat(gmaFormat: AdFormat, adSize: AdSize?): POBAdFormat? =
      when (gmaFormat) {
        AdFormat.BANNER -> {
          if (adSize == AdSize.MEDIUM_RECTANGLE) {
            POBAdFormat.MREC
          } else {
            POBAdFormat.BANNER
          }
        }
        AdFormat.INTERSTITIAL -> POBAdFormat.INTERSTITIAL
        AdFormat.REWARDED -> POBAdFormat.REWARDEDAD
        AdFormat.NATIVE -> POBAdFormat.NATIVE
        else -> null
      }
  }
}
