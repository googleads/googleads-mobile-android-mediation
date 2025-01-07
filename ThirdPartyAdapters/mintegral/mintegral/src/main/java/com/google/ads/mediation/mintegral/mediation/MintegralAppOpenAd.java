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

package com.google.ads.mediation.mintegral.mediation;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralSplashAdWrapper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;
import com.mbridge.msdk.out.MBSplashLoadWithCodeListener;
import com.mbridge.msdk.out.MBSplashShowListener;
import com.mbridge.msdk.out.MBridgeIds;

/** Used to load Mintegral splash ads. */
public abstract class MintegralAppOpenAd extends MBSplashLoadWithCodeListener
    implements MediationAppOpenAd, MBSplashShowListener {

  protected final MediationAppOpenAdConfiguration adConfiguration;
  protected final MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>
      adLoadCallback;

  protected MediationAppOpenAdCallback appOpenAdCallback;
  protected MintegralSplashAdWrapper splashAdWrapper;

  protected MintegralAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  public abstract void loadAd();

  @Override
  public void onLoadSuccessed(MBridgeIds mBridgeIds, int type) {
    appOpenAdCallback = adLoadCallback.onSuccess(this);
  }

  @Override
  public void isSupportZoomOut(MBridgeIds mBridgeIds, boolean isSupported) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onLoadFailedWithCode(MBridgeIds mBridgeIds, int code, String msg, int reqType) {
    AdError adError = MintegralConstants.createSdkError(code, msg);
    Log.d(TAG, adError.toString());
    adLoadCallback.onFailure(adError);
  }

  @Override
  public void onShowSuccessed(MBridgeIds mBridgeIds) {
    if (appOpenAdCallback != null) {
      appOpenAdCallback.onAdOpened();
      appOpenAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onShowFailed(MBridgeIds mBridgeIds, String msg) {
    if (appOpenAdCallback != null) {
      AdError error =
          MintegralConstants.createSdkError(MintegralConstants.ERROR_MINTEGRAL_SDK, msg);
      Log.w(TAG, error.toString());
      appOpenAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdClicked(MBridgeIds mBridgeIds) {
    if (appOpenAdCallback != null) {
      appOpenAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onDismiss(MBridgeIds mBridgeIds, int type) {
    if (appOpenAdCallback != null) {
      appOpenAdCallback.onAdClosed();
    }
    if (splashAdWrapper != null) {
      splashAdWrapper.onDestroy();
    }
  }

  @Override
  public void onAdTick(MBridgeIds mBridgeIds, long millisUntilFinished) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onZoomOutPlayStart(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onZoomOutPlayFinish(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }
}
