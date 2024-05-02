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
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.ironsource.mediationsdk.IronSource;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

public class IronSourceRewardedAd implements MediationRewardedAd {

  @VisibleForTesting
  static final ConcurrentHashMap<String, WeakReference<IronSourceRewardedAd>> availableInstances =
      new ConcurrentHashMap<>();

  private static final IronSourceRewardedAdListener ironSourceRewardedListener =
      new IronSourceRewardedAdListener();

  private MediationRewardedAdCallback mediationRewardedAdCallback;

  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  private final Context context;

  private final String instanceID;

  private final String bidToken;

  private final String watermark;

  public IronSourceRewardedAd(
      @NonNull MediationRewardedAdConfiguration rewardedAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationAdLoadCallback) {
    Bundle serverParameters = rewardedAdConfiguration.getServerParameters();
    instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
    context = rewardedAdConfiguration.getContext();
    bidToken = rewardedAdConfiguration.getBidResponse();
    watermark = rewardedAdConfiguration.getWatermark();
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Getters and Setters. */
  static IronSourceRewardedAd getFromAvailableInstances(@NonNull String instanceId) {
    return availableInstances.containsKey(instanceId)
        ? availableInstances.get(instanceId).get()
        : null;
  }

  static void removeFromAvailableInstances(@NonNull String instanceId) {
    availableInstances.remove(instanceId);
  }

  static IronSourceRewardedAdListener getIronSourceRewardedListener() {
    return ironSourceRewardedListener;
  }

  MediationRewardedAdCallback getRewardedAdCallback() {
    return mediationRewardedAdCallback;
  }

  void setRewardedAdCallback(@NonNull MediationRewardedAdCallback adCallback) {
    mediationRewardedAdCallback = adCallback;
  }

  public MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      getMediationAdLoadCallback() {
    return mediationAdLoadCallback;
  }

  private boolean loadValidConfig() {
    if (!isParamsValid()) {
      return false;
    }

    availableInstances.put(instanceID, new WeakReference<>(this));
    Log.d(TAG, String.format("Loading IronSource rewarded ad with instance ID: %s", instanceID));
    return true;
  }

  public void loadWaterfallAd() {
    if (!loadValidConfig()) {
      return;
    }
    Activity activity = (Activity) context;
    IronSource.loadISDemandOnlyRewardedVideo(activity, instanceID);
  }

  public void loadRtbAd() {
    if (!loadValidConfig()) {
      return;
    }
    Activity activity = (Activity) context;

    IronSourceAdapterUtils.setWatermark(watermark);
    IronSource.loadISDemandOnlyRewardedVideoWithAdm(activity, instanceID, bidToken);
  }

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

  @Override
  public void showAd(@NonNull Context context) {
    Log.d(
        TAG, String.format("Showing IronSource rewarded ad for instance ID: %s", this.instanceID));
    IronSource.showISDemandOnlyRewardedVideo(this.instanceID);
  }

  /** Forward ad load failure event to Google Mobile Ads SDK. */
  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.w(TAG, loadError.toString());
    mediationAdLoadCallback.onFailure(loadError);
  }
}
