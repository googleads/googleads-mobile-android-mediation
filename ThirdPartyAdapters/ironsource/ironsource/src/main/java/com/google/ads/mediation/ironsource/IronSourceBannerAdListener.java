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
import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS;
import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerListener;
import com.ironsource.mediationsdk.logger.IronSourceError;

public class IronSourceBannerAdListener implements ISDemandOnlyBannerListener {

  public void onBannerAdLoaded(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource banner ad loaded for instance ID: %s", instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd == null || ironSourceBannerAd.getIronSourceAdView() == null) {
      return;
    }

    ironSourceBannerAd
        .getIronSourceAdView()
        .addView(ironSourceBannerAd.getIronSourceBannerLayout());

    if (ironSourceBannerAd.getAdLoadCallback() != null) {
      ironSourceBannerAd.setBannerAdCallback(
          ironSourceBannerAd.getAdLoadCallback().onSuccess(ironSourceBannerAd));
    }
  }

  public void onBannerAdLoadFailed(
      @NonNull final String instanceId, @NonNull final IronSourceError ironSourceError) {
    final AdError loadError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    Log.w(TAG, loadError.toString());
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd == null) {
      return;
    }

    MediationAdLoadCallback adLoadCallback = ironSourceBannerAd.getAdLoadCallback();
    if (adLoadCallback != null) {
      adLoadCallback.onFailure(loadError);
    }

    /* If the IronSource SDK is already loading a banner ad with the current instance ID,
    remove all the other instance IDs from the mapping. */
    if (ironSourceError.getErrorCode() != ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS
        && ironSourceError.getErrorCode() != ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS) {
      IronSourceBannerAd.removeFromAvailableInstances(instanceId);
    }
  }

  public void onBannerAdShown(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource banner ad shown for instance ID: %s", instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);
    // The banner ad instance will be null if it fails to load or another banner ad is showing.
    if (ironSourceBannerAd != null) {
      MediationBannerAdCallback adCallback = ironSourceBannerAd.getBannerAdCallback();
      if (adCallback != null) {
        adCallback.reportAdImpression();
      }
    }

    IronSourceBannerAd.clearAllAvailableInstancesExceptOne(instanceId);
  }

  public void onBannerAdClicked(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource banner ad clicked for instance ID: %s", instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd != null) {
      MediationBannerAdCallback adCallback = ironSourceBannerAd.getBannerAdCallback();
      if (adCallback != null) {
        adCallback.onAdOpened();
        adCallback.reportAdClicked();
      }
    }
  }

  public void onBannerAdLeftApplication(@NonNull String instanceId) {
    Log.d(
        TAG,
        String.format(
            "IronSource banner ad has caused user to leave the application for instance ID: %s",
            instanceId));
    IronSourceBannerAd ironSourceBannerAd =
        IronSourceBannerAd.getFromAvailableInstances(instanceId);

    if (ironSourceBannerAd != null) {
      MediationBannerAdCallback adCallback = ironSourceBannerAd.getBannerAdCallback();
      if (adCallback != null) {
        adCallback.onAdLeftApplication();
      }
    }
  }
}
