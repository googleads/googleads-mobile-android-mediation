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
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.mediation.Adapter
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration

/**
 * Line Adapter for GMA SDK used to initialize and load ads from the Line SDK. This class should not
 * be used directly by publishers.
 */
class LineMediationAdapter : Adapter() {

  private lateinit var bannerAd: LineBannerAd

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
        versionString
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
        versionString
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

    try {
      LineInitializer.initialize(context, appIdForInit)
    } catch (exception: IllegalArgumentException) {
      exception.message?.let { initializationCompleteCallback.onInitializationFailed(it) }
      return
    }

    initializationCompleteCallback.onInitializationSucceeded()
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

  companion object {
    private val TAG = LineMediationAdapter::class.simpleName
    @VisibleForTesting var adapterVersionDelegate: String? = null
    const val KEY_APP_ID = "application_id"
    const val KEY_SLOT_ID = "slot_id"
    const val ERROR_MSG_MISSING_APP_ID =
      "Missing or invalid Application ID configured for this ad source instance in the AdMob or Ad Manager UI."
    const val ERROR_MSG_MISSING_SLOT_ID =
      "Missing or invalid Slot ID configured for this ad source instance in the AdMob or Ad Manager UI."
    const val ERROR_CODE_MISSING_APP_ID = 101
    const val ERROR_CODE_MISSING_SLOT_ID = 102
    const val ERROR_MSG_AD_LOADING = "FiveAd SDK returned a load error with code %s."
    const val ADAPTER_ERROR_DOMAIN = "com.google.ads.mediation.line"
    const val SDK_ERROR_DOMAIN = "com.five_corp.ad"
  }
}
