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
import com.google.ads.mediation.common.AgeRestrictedTreatmentUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
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
import io.bidmachine.BMError
import io.bidmachine.BidMachine
import io.bidmachine.BidMachineInstance
import io.bidmachine.BidTokenCallback
import io.bidmachine.model.BidMachineConfiguration
import io.bidmachine.model.ConsentData
import io.bidmachine.model.MediationInfo
import io.bidmachine.model.Placement

/**
 * BidMachine Adapter for GMA SDK used to initialize and load ads from the BidMachine SDK. This
 * class should not be used directly by publishers.
 */
class BidMachineMediationAdapter
@JvmOverloads
constructor(private val mediationUtils: MediationUtilsWrapper = MediationUtilsWrapper()) :
  RtbAdapter() {

  lateinit var bannerAd: BidMachineBannerAd
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
    val sourceIds = mediationConfigurations.mapNotNull {
      val sourceId = it.serverParameters.getString(SOURCE_ID_KEY)
      if (sourceId.isNullOrEmpty()) {
        null
      } else {
        sourceId
      }
    }
    if (sourceIds.isEmpty()) {
      val adError =
        AdError(ERROR_CODE_MISSING_SOURCE_ID, ERROR_MSG_MISSING_SOURCE_ID, ADAPTER_ERROR_DOMAIN)
      initializationCompleteCallback.onInitializationFailed(adError)
      return
    }

    val sourceIdForInit = sourceIds[0]
    if (sourceIdForInit.isEmpty()) {
      val adError =
        AdError(ERROR_CODE_MISSING_SOURCE_ID, ERROR_MSG_MISSING_SOURCE_ID, ADAPTER_ERROR_DOMAIN)
      initializationCompleteCallback.onInitializationFailed(adError)
      return
    }
    if (sourceIds.size > 1) {
      val message =
        "Multiple $SOURCE_ID_KEY entries found: ${sourceIds}. Using '${sourceIdForInit}' to initialize the BidMachine SDK"
      Log.w(TAG, message)
    }

    configureBidMachinePrivacy()

    val mediationInfo =
      MediationInfo(MEDIATION_NAME, getGmaSdkVersion(), BuildConfig.ADAPTER_VERSION)
    bidMachineInstance =
      BidMachine.initialize(context, sourceIdForInit, BidMachineConfiguration(mediationInfo))
    initializationCompleteCallback.onInitializationSucceeded()
  }

  private fun getGmaSdkVersion(): String {
    val versionInfo = MobileAds.getVersion()
    return "${versionInfo.majorVersion}.${versionInfo.minorVersion}.${versionInfo.microVersion}"
  }

  override fun collectSignals(signalData: RtbSignalData, callback: SignalCallbacks) {
    val bidMachineInstance = bidMachineInstance
    if (bidMachineInstance == null) {
      callback.onFailure(createSdkNotInitializedError())
      return
    }
    val supportedConfigurations = signalData.configurations.filter {
      it.format in SUPPORTED_AD_FORMATS
    }
    if (supportedConfigurations.isEmpty()) {
      val adError =
        AdError(
          ERROR_CODE_NO_SUPPORTED_SIGNAL_CONFIGURATIONS,
          ERROR_MSG_NO_SUPPORTED_SIGNAL_CONFIGURATIONS,
          ADAPTER_ERROR_DOMAIN,
        )
      callback.onFailure(adError)
      return
    }
    val placements = supportedConfigurations.mapNotNull {
      createPlacement(it.serverParameters.getString(PLACEMENT_ID_KEY))
    }
    bidMachineInstance.collectToken(
      placements,
      object : BidTokenCallback {
        override fun onCollected(bidToken: String) {
          callback.onSuccess(bidToken)
        }

        override fun onCollectionFailed(bmError: BMError) {
          callback.onFailure(AdError(bmError.code, bmError.message, SDK_ERROR_DOMAIN))
        }
      },
    )
  }

  override fun loadBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadWaterfallAd(bidMachineInstance)
    }
  }

  override fun loadInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadWaterfallAd(bidMachineInstance)
    }
  }

  override fun loadRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadWaterfallAd(bidMachineInstance)
    }
  }

  override fun loadNativeAdMapper(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadWaterfallAd(bidMachineInstance)
    }
  }

  override fun loadRtbBannerAd(
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
    callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineBannerAd.newInstance(mediationBannerAdConfiguration, callback).onSuccess {
      bannerAd = it
      bannerAd.loadRtbAd(bidMachineInstance)
    }
  }

  override fun loadRtbInterstitialAd(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineInterstitialAd.newInstance(mediationInterstitialAdConfiguration, callback).onSuccess {
      interstitialAd = it
      interstitialAd.loadRtbAd(bidMachineInstance)
    }
  }

  override fun loadRtbRewardedAd(
    mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
    callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineRewardedAd.newInstance(mediationRewardedAdConfiguration, callback).onSuccess {
      rewardedAd = it
      rewardedAd.loadRtbAd(bidMachineInstance)
    }
  }

  override fun loadRtbNativeAdMapper(
    mediationNativeAdConfiguration: MediationNativeAdConfiguration,
    callback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>,
  ) {
    val bidMachineInstance = getBidMachineInstanceOrFail(callback) ?: return
    BidMachineNativeAd.newInstance(mediationNativeAdConfiguration, callback).onSuccess {
      nativeAd = it
      nativeAd.loadRtbAd(bidMachineInstance)
    }
  }

  private fun getBidMachineInstanceOrFail(
    callback: MediationAdLoadCallback<*, *>
  ): BidMachineInstance? {
    val bidMachineInstance = bidMachineInstance
    if (bidMachineInstance == null) {
      callback.onFailure(createSdkNotInitializedError())
    }
    return bidMachineInstance
  }

  private fun createSdkNotInitializedError(): AdError =
    AdError(ERROR_CODE_SDK_NOT_INITIALIZED, ERROR_MSG_SDK_NOT_INITIALIZED, ADAPTER_ERROR_DOMAIN)

  private fun configureBidMachinePrivacy() {
    val tagForChildDirected = getRequestConfiguration().tagForChildDirectedTreatment
    val tagForUnderAgeOfConsent = getRequestConfiguration().tagForUnderAgeOfConsent
    val isAgeRestrictedTreatmentChild =
      AgeRestrictedTreatmentUtils.runtimeGmaSdkSupportsChildAgeRestrictedTreatment() &&
        getRequestConfiguration().ageRestrictedTreatment == AgeRestrictedTreatment.CHILD
    val coppa =
      if (
        tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE ||
          tagForUnderAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE ||
          isAgeRestrictedTreatmentChild
      ) {
        true
      } else if (
        tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE ||
          tagForUnderAgeOfConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
      ) {
        false
      } else {
        return
      }
    BidMachine.setConsentData(ConsentData.Builder().setCoppa(coppa).build())
  }

  internal companion object {
    private val TAG = BidMachineMediationAdapter::class.simpleName
    @VisibleForTesting var bidMachineSdkVersionDelegate: String? = null
    @VisibleForTesting var adapterVersionDelegate: String? = null
    // The BidMachine SDK instance is shared by all the adapter instances.
    @VisibleForTesting var bidMachineInstance: BidMachineInstance? = null
    @VisibleForTesting const val MEDIATION_NAME = "google"
    private val SUPPORTED_AD_FORMATS =
      setOf(AdFormat.BANNER, AdFormat.INTERSTITIAL, AdFormat.REWARDED, AdFormat.NATIVE)

    /** Returns the BidMachine placement for the [placementId], or `null` if it's missing. */
    fun createPlacement(placementId: String?): Placement? =
      if (placementId.isNullOrEmpty()) null else Placement(placementId)
    @VisibleForTesting const val SOURCE_ID_KEY = "source_id"
    const val PLACEMENT_ID_KEY = "placement_id"
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.bidmachine"
    const val SDK_ERROR_DOMAIN = "io.bidmachine"
    const val WATERMARK_KEY = "google_watermark"
    @VisibleForTesting const val ERROR_MSG_MISSING_SOURCE_ID = "Source Id is missing or empty"
    const val ERROR_CODE_NO_SUPPORTED_SIGNAL_CONFIGURATIONS = 101
    const val ERROR_MSG_NO_SUPPORTED_SIGNAL_CONFIGURATIONS =
      "Error during signal collection: No supported Signal Data Configuration found."
    const val ERROR_CODE_COULD_NOT_SHOW_FULLSCREEN_AD = 105
    const val ERROR_MSG_COULD_NOT_SHOW_FULLSCREEN_AD = "Fullscreen ad could not be shown."
    const val ERROR_CODE_MISSING_SOURCE_ID = 107
    const val ERROR_CODE_SDK_NOT_INITIALIZED = 108
    const val ERROR_MSG_SDK_NOT_INITIALIZED = "BidMachine SDK is not initialized."
  }
}
