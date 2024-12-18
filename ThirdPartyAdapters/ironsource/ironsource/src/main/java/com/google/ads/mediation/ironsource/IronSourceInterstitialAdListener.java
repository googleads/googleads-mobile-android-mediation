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

package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceConstants.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyInterstitialListener;
import com.ironsource.mediationsdk.logger.IronSourceError;

public class IronSourceInterstitialAdListener implements ISDemandOnlyInterstitialListener {

  @Override
  public void onInterstitialAdReady(@NonNull String instanceId) {
    Log.d(
        TAG, String.format("IronSource interstitial ad is ready for instance ID: %s", instanceId));
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      if (ironSourceInterstitialAd.getMediationAdLoadCallback() != null) {
        ironSourceInterstitialAd.setInterstitialAdCallback(
            ironSourceInterstitialAd
                .getMediationAdLoadCallback()
                .onSuccess(ironSourceInterstitialAd));
      }
    }
  }

  @Override
  public void onInterstitialAdLoadFailed(
      @NonNull String instanceId, @NonNull IronSourceError ironSourceError) {
    final AdError loadError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    Log.w(TAG, loadError.toString());
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      if (ironSourceInterstitialAd.getMediationAdLoadCallback() != null) {
        ironSourceInterstitialAd.getMediationAdLoadCallback().onFailure(loadError);
      }
    }

    IronSourceInterstitialAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onInterstitialAdOpened(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource interstitial ad opened for instance ID: %s", instanceId));
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      MediationInterstitialAdCallback adCallback =
          ironSourceInterstitialAd.getInterstitialAdCallback();
      if (adCallback != null) {
        adCallback.onAdOpened();
        adCallback.reportAdImpression();
      }
    }
  }

  @Override
  public void onInterstitialAdClosed(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource interstitial ad closed for instance ID: %s", instanceId));
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      MediationInterstitialAdCallback adCallback =
          ironSourceInterstitialAd.getInterstitialAdCallback();
      if (adCallback != null) {
        adCallback.onAdClosed();
      }
    }

    IronSourceInterstitialAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onInterstitialAdShowFailed(
      @NonNull String instanceId, @NonNull IronSourceError ironSourceError) {
    AdError showError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    Log.w(TAG, showError.toString());
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      MediationInterstitialAdCallback adCallback =
          ironSourceInterstitialAd.getInterstitialAdCallback();
      if (adCallback != null) {
        adCallback.onAdFailedToShow(showError);
      }
    }

    IronSourceInterstitialAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onInterstitialAdClicked(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource interstitial ad clicked for instance ID: %s", instanceId));
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      MediationInterstitialAdCallback adCallback =
          ironSourceInterstitialAd.getInterstitialAdCallback();
      if (adCallback != null) {
        adCallback.reportAdClicked();
      }
    }
  }
}
