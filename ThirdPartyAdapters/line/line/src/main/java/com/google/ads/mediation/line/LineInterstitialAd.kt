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

import android.app.Activity
import android.content.Context
import android.util.Log
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterface
import com.five_corp.ad.FiveAdInterstitial
import com.five_corp.ad.FiveAdLoadListener
import com.five_corp.ad.FiveAdViewEventListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import java.lang.ref.WeakReference

/**
 * Used to load Line interstitial ads and mediate callbacks between Google Mobile Ads SDK and Line
 * (aka FiveAd) SDK.
 */
class LineInterstitialAd
private constructor(
  private val activityReference: WeakReference<Activity>,
  private val appId: String,
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  private val interstitialAd: FiveAdInterstitial,
) : MediationInterstitialAd, FiveAdLoadListener, FiveAdViewEventListener {

  private var mediationInterstitialAdCallback: MediationInterstitialAdCallback? = null

  fun loadAd() {
    val activity = activityReference.get() ?: return
    LineInitializer.initialize(activity, appId)
    interstitialAd.setLoadListener(this)
    interstitialAd.loadAdAsync()
  }

  override fun showAd(context: Context) {
    val activity = activityReference.get() ?: return
    val showedFullscreen = interstitialAd.show(activity)
    if (!showedFullscreen) {
      val adError =
        AdError(
          LineMediationAdapter.ERROR_CODE_FAILED_TO_SHOW_FULLSCREEN,
          LineMediationAdapter.ERROR_MSG_FAILED_TO_SHOW_FULLSCREEN,
          LineMediationAdapter.SDK_ERROR_DOMAIN
        )
      Log.w(TAG, adError.message)
      mediationInterstitialAdCallback?.onAdFailedToShow(adError)
      return
    }
    mediationInterstitialAdCallback?.onAdOpened()
  }

  override fun onFiveAdLoad(ad: FiveAdInterface) {
    Log.d(TAG, "Finished loading Line Interstitial Ad for slotId: ${ad.slotId}")
    mediationInterstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
    interstitialAd.setViewEventListener(this)
  }

  override fun onFiveAdLoadError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    val adError =
      AdError(
        errorCode.value,
        String.format(LineMediationAdapter.ERROR_MSG_AD_LOADING, errorCode.name),
        LineMediationAdapter.SDK_ERROR_DOMAIN
      )
    Log.w(TAG, adError.message)
    mediationAdLoadCallback.onFailure(adError)
  }

  override fun onFiveAdViewError(ad: FiveAdInterface, errorCode: FiveAdErrorCode) {
    val adError =
      AdError(
        errorCode.value,
        String.format(LineMediationAdapter.ERROR_MSG_AD_SHOWING, errorCode.name),
        LineMediationAdapter.SDK_ERROR_DOMAIN
      )
    Log.w(TAG, adError.message)
    mediationInterstitialAdCallback?.onAdFailedToShow(adError)
  }

  override fun onFiveAdClick(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial ad did record a click.")
    mediationInterstitialAdCallback?.apply {
      reportAdClicked()
      onAdLeftApplication()
    }
  }

  override fun onFiveAdClose(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial ad closed")
    mediationInterstitialAdCallback?.onAdClosed()
  }

  override fun onFiveAdStart(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial video ad start")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdPause(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial video ad paused")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdResume(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial video ad resumed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdViewThrough(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial video ad viewed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdReplay(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial video ad replayed")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdImpression(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial ad recorded an impression.")
    mediationInterstitialAdCallback?.reportAdImpression()
  }

  override fun onFiveAdStall(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial ad stalled")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  override fun onFiveAdRecover(ad: FiveAdInterface) {
    Log.d(TAG, "Line interstitial ad recovered")
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  companion object {
    private val TAG = LineInterstitialAd::class.simpleName

    fun newInstance(
      mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
      mediationAdLoadCallback:
        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
    ): Result<LineInterstitialAd> {
      val activity = mediationInterstitialAdConfiguration.context as? Activity
      if (activity == null) {
        val adError =
          AdError(
            LineMediationAdapter.ERROR_CODE_CONTEXT_NOT_AN_ACTIVITY,
            LineMediationAdapter.ERROR_MSG_CONTEXT_NOT_AN_ACTIVITY,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }
      val serverParameters = mediationInterstitialAdConfiguration.serverParameters

      val appId = serverParameters.getString(LineMediationAdapter.KEY_APP_ID)
      if (appId.isNullOrEmpty()) {
        val adError =
          AdError(
            LineMediationAdapter.ERROR_CODE_MISSING_APP_ID,
            LineMediationAdapter.ERROR_MSG_MISSING_APP_ID,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val slotId = serverParameters.getString(LineMediationAdapter.KEY_SLOT_ID)
      if (slotId.isNullOrEmpty()) {
        val adError =
          AdError(
            LineMediationAdapter.ERROR_CODE_MISSING_SLOT_ID,
            LineMediationAdapter.ERROR_MSG_MISSING_SLOT_ID,
            LineMediationAdapter.ADAPTER_ERROR_DOMAIN
          )
        mediationAdLoadCallback.onFailure(adError)
        return Result.failure(NoSuchElementException(adError.message))
      }

      val fiveAdInterstitialAd = LineSdkFactory.delegate.createFiveAdInterstitial(activity, slotId)

      return Result.success(
        LineInterstitialAd(
          WeakReference(activity),
          appId,
          mediationAdLoadCallback,
          fiveAdInterstitialAd
        )
      )
    }
  }
}
