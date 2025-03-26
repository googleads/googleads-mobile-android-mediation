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
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyRewardedVideoListener;
import com.ironsource.mediationsdk.logger.IronSourceError;

public class IronSourceRewardedAdListener implements ISDemandOnlyRewardedVideoListener {

  @Override
  public void onRewardedVideoAdLoadSuccess(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad loaded for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      if (ironSourceRewardedAd.getMediationAdLoadCallback() != null) {
        ironSourceRewardedAd.setRewardedAdCallback(
            ironSourceRewardedAd.getMediationAdLoadCallback().onSuccess(ironSourceRewardedAd));
      }
    }
  }

  @Override
  public void onRewardedVideoAdLoadFailed(
      @NonNull String instanceId, @NonNull IronSourceError ironSourceError) {
    AdError loadError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    Log.e(TAG, loadError.toString());
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      if (ironSourceRewardedAd.getMediationAdLoadCallback() != null) {
        ironSourceRewardedAd.getMediationAdLoadCallback().onFailure(loadError);
      }
    }

    IronSourceRewardedAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onRewardedVideoAdOpened(@NonNull final String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad opened for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onAdOpened();
        adCallBack.onVideoStart();
        adCallBack.reportAdImpression();
      }
    }
  }

  @Override
  public void onRewardedVideoAdClosed(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad closed for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onAdClosed();
      }
    }

    IronSourceRewardedAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onRewardedVideoAdRewarded(@NonNull String instanceId) {
    Log.d(
        TAG,
        String.format("IronSource rewarded ad received reward for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onVideoComplete();
        adCallBack.onUserEarnedReward();
      }
    }
  }

  @Override
  public void onRewardedVideoAdShowFailed(
      @NonNull String instanceId, @NonNull IronSourceError ironSourceError) {
    AdError showError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    Log.e(TAG, showError.toString());
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      // Check currently showing instance existence.
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onAdFailedToShow(showError);
      }
    }

    IronSourceRewardedAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onRewardedVideoAdClicked(@NonNull String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad clicked for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.reportAdClicked();
      }
    }
  }
}
