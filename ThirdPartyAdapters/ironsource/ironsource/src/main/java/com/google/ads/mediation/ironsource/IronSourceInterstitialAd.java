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

import static com.google.ads.mediation.ironsource.IronSourceConstants.DEFAULT_NON_RTB_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ADAPTER_ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.ironsource.mediationsdk.IronSource;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

public class IronSourceInterstitialAd implements MediationInterstitialAd {

  @VisibleForTesting
  static final ConcurrentHashMap<String, WeakReference<IronSourceInterstitialAd>>
          availableInterstitialInstances = new ConcurrentHashMap<>();

  private static final IronSourceInterstitialAdListener ironSourceInterstitialListener =
          new IronSourceInterstitialAdListener();

  private MediationInterstitialAdCallback interstitialAdCallback;

  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback;

  private final Context context;

  private final String instanceID;

  public IronSourceInterstitialAd(
          @NonNull MediationInterstitialAdConfiguration interstitialAdConfig,
          @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
                  mediationInterstitialAdLoadCallback) {
    Bundle serverParameters = interstitialAdConfig.getServerParameters();
    instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_NON_RTB_INSTANCE_ID);
    context = interstitialAdConfig.getContext();
    this.mediationAdLoadCallback = mediationInterstitialAdLoadCallback;
  }

  /**
   * Getters and Setters.
   */
  static IronSourceInterstitialAd getFromAvailableInstances(@NonNull String instanceId) {
    return availableInterstitialInstances.containsKey(instanceId)
            ? availableInterstitialInstances.get(instanceId).get()
            : null;
  }

  static void removeFromAvailableInstances(@NonNull String instanceId) {
    availableInterstitialInstances.remove(instanceId);
  }

  static IronSourceInterstitialAdListener getIronSourceInterstitialListener() {
    return ironSourceInterstitialListener;
  }

  MediationInterstitialAdCallback getInterstitialAdCallback() {
    return interstitialAdCallback;
  }

  void setInterstitialAdCallback(@NonNull MediationInterstitialAdCallback adCallback) {
    interstitialAdCallback = adCallback;
  }

  public MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
  getMediationAdLoadCallback() {
    return mediationAdLoadCallback;
  }

  /**
   * Attempts to load an @{link IronSource} interstitial ad.
   */
  public void loadWaterfallAd() {
    if (!loadValidConfig()) {
      return;
    }
    Activity activity = (Activity) context;
    IronSource.loadISDemandOnlyInterstitial(activity, instanceID);
  }

  /**
   * Returns true if all the parameters needed to load an ad are valid.
   */
  private boolean loadValidConfig() {
    if (!isParamsValid()) {
      return false;
    }

    availableInterstitialInstances.put(instanceID, new WeakReference<>(this));
    Log.d(
            TAG, String.format("Loading IronSource interstitial ad with instance ID: %s", instanceID));
    return true;
  }

  private boolean isParamsValid() {
    AdError loadError = IronSourceAdapterUtils.validateIronSourceAdLoadParams(context, instanceID);
    if (loadError != null) {
      onAdFailedToLoad(loadError);
      return false;
    }

    // Check that an Ad for this instance ID is not already loading.
    if (!IronSourceAdapterUtils.canLoadIronSourceAdInstance(
            instanceID, availableInterstitialInstances)) {
      String errorMessage =
              String.format(
                      "An IronSource interstitial ad is already loading for instance ID: %s", instanceID);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ADAPTER_ERROR_DOMAIN);
      onAdFailedToLoad(concurrentError);
      return false;
    }

    return true;
  }

  @Override
  public void showAd(@NonNull Context context) {
    IronSource.showISDemandOnlyInterstitial(instanceID);
  }

  /**
   * Forward ad load failure event to Google Mobile Ads SDK.
   */
  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.e(TAG, loadError.toString());
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(loadError);
    }
  }
}