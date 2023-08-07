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
import static com.google.ads.mediation.ironsource.IronSourceConstants.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyInterstitialListener;
import com.ironsource.mediationsdk.logger.IronSourceError;

public class IronSourceInterstitialAdListener implements ISDemandOnlyInterstitialListener {
  @Override
  public void onInterstitialAdReady(String instanceId) {
    Log.d(TAG, String.format("IronSource interstitial is ready for instance ID: %s", instanceId));
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      if (ironSourceInterstitialAd.mediationInterstitialAdLoadCallback != null) {
        ironSourceInterstitialAd.setInterstitialAdCallback(
            ironSourceInterstitialAd.mediationInterstitialAdLoadCallback.onSuccess(
                ironSourceInterstitialAd));
      }
    }
  }

  @Override
  public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
    final AdError loadError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage =
        String.format(
            "IronSource failed to load interstitial ad for instance ID: %s. Error: %s",
            instanceId, loadError.getMessage());
    Log.w(TAG, errorMessage);
    IronSourceInterstitialAd ironSourceInterstitialAd =
        IronSourceInterstitialAd.getFromAvailableInstances(instanceId);

    if (ironSourceInterstitialAd != null) {
      if (ironSourceInterstitialAd.mediationInterstitialAdLoadCallback != null) {
        ironSourceInterstitialAd.mediationInterstitialAdLoadCallback.onFailure(loadError);
      }
    }

    IronSourceInterstitialAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onInterstitialAdOpened(String instanceId) {
    Log.d(TAG, String.format("IronSource interstitial opened for instance ID: %s", instanceId));
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
  public void onInterstitialAdClosed(String instanceId) {
    Log.d(TAG, String.format("IronSource interstitial closed for instance ID: %s", instanceId));
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
  public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
    AdError showError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage =
        String.format(
            "IronSource failed to show interstitial ad for instance ID: %s. Error: %s",
            instanceId, showError.getMessage());
    Log.w(TAG, errorMessage);
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
  public void onInterstitialAdClicked(String instanceId) {
    Log.d(TAG, String.format("IronSource interstitial clicked for instance ID: %s", instanceId));
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
