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

import static com.google.ads.mediation.ironsource.IronSourceConstants.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.TAG;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.ironsource.mediationsdk.IronSource;

import java.util.concurrent.ConcurrentHashMap;

public class IronSourceRewardedAd implements MediationRewardedAd {

  private static final ConcurrentHashMap<String, IronSourceRewardedAd> availableInstances =
      new ConcurrentHashMap<>();

  private static final IronSourceRewardedAdListener ironSourceRewardedListener =
      new IronSourceRewardedAdListener();

  private MediationRewardedAdCallback mediationRewardedAdCallback;

  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  private final Context context;

  private final String instanceID;

  public IronSourceRewardedAd(
      @NonNull MediationRewardedAdConfiguration rewardedAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationAdLoadCallback) {
    Bundle serverParameters = rewardedAdConfiguration.getServerParameters();
    instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
    context = rewardedAdConfiguration.getContext();
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Getters and Setters. */
  public static IronSourceRewardedAd getFromAvailableInstances(String instanceId) {
    return availableInstances.get(instanceId);
  }

  public static void removeFromAvailableInstances(String instanceId) {
    availableInstances.remove(instanceId);
  }

  public static IronSourceRewardedAdListener getIronSourceRewardedListener() {
    return ironSourceRewardedListener;
  }

  public MediationRewardedAdCallback getRewardedAdCallback() {
    return mediationRewardedAdCallback;
  }

  public void setRewardedAdCallback(MediationRewardedAdCallback adCallback) {
    mediationRewardedAdCallback = adCallback;
  }

  public MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      getMediationAdLoadCallback() {
    return mediationAdLoadCallback;
  }

  public void loadAd() {
    if (!isParamsValid()) {
      return;
    }

    Activity activity = (Activity) context;
    availableInstances.put(instanceID, this);
    Log.d(TAG, String.format("Loading IronSource rewarded ad with instance ID: %s", instanceID));
    IronSource.loadISDemandOnlyRewardedVideo(activity, instanceID);
  }

  /** Checks if the parameters for loading this instance are valid. */
  private boolean isParamsValid() {
    // Check that the context is an Activity and that the instance ID is valid.
    AdError loadError = IronSourceAdapterUtils.validateIronSourceAdLoadParams(context, instanceID);
    if (loadError != null) {
      onAdFailedToLoad(loadError);
      return false;
    }

    // Check that an Ad for this instance ID is not already loading.
    if (!IronSourceAdapterUtils.canLoadIronSourceAdInstance(instanceID, availableInstances)) {
      String errorMessage =
          String.format(
              "An IronSource Rewarded ad is already loading for instance ID: %s", instanceID);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
      onAdFailedToLoad(concurrentError);
      return false;
    }

    return true;
  }

  /** Rewarded Video show Ad. */
  @Override
  public void showAd(@NonNull Context context) {
    Log.d(
        TAG, String.format("Showing IronSource rewarded ad for instance ID: %s", this.instanceID));
    IronSource.showISDemandOnlyRewardedVideo(this.instanceID);
  }

  /** Pass Load Fail from IronSource SDK to Google Mobile Ads. */
  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.w(TAG, loadError.getMessage());
    mediationAdLoadCallback.onFailure(loadError);
  }
}
