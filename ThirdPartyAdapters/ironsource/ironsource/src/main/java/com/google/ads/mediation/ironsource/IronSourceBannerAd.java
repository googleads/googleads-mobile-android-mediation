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
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerLayout;

import java.util.concurrent.ConcurrentHashMap;

public class IronSourceBannerAd implements MediationBannerAd {

  private static final ConcurrentHashMap<String, IronSourceBannerAd> availableBannerInstances =
      new ConcurrentHashMap<>();

  private static final IronSourceBannerAdListener ironSourceBannerListener =
      new IronSourceBannerAdListener();

  private MediationBannerAdCallback bannerAdCallback;

  private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback;

  private FrameLayout ironSourceAdView;

  private ISDemandOnlyBannerLayout ironSourceBannerLayout;

  private final AdSize adSize;

  private final Context context;

  private final String instanceID;

  public IronSourceBannerAd(
      @NonNull MediationBannerAdConfiguration bannerAdConfig,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback) {
    Bundle serverParameters = bannerAdConfig.getServerParameters();
    instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
    context = bannerAdConfig.getContext();
    adSize = bannerAdConfig.getAdSize();
    adLoadCallback = mediationAdLoadCallback;
  }

  /** Getters and Setters. */
  public MediationBannerAdCallback getBannerAdCallback() {
    return bannerAdCallback;
  }

  public MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> getAdLoadCallback() {
    return adLoadCallback;
  }

  public void setBannerAdCallback(MediationBannerAdCallback adCallback) {
    bannerAdCallback = adCallback;
  }

  public ISDemandOnlyBannerLayout getIronSourceBannerLayout() {
    return ironSourceBannerLayout;
  }

  public FrameLayout getIronSourceAdView() {
    return ironSourceAdView;
  }

  /** Instance map access. */
  public static IronSourceBannerAd getFromAvailableInstances(String instanceId) {
    return availableBannerInstances.get(instanceId);
  }

  public static void removeFromAvailableInstances(String instanceId) {
    availableBannerInstances.remove(instanceId);
  }

  /**
   * Removes from the available instances map and destroys all instances except for the instance
   * with the given instance ID.
   */
  public static void clearAllAvailableInstancesExceptOne(String instanceID) {
    for (String otherInstanceInMap : availableBannerInstances.keySet()) {
      if (!otherInstanceInMap.equals(instanceID)) {
        Log.d(
            TAG,
            String.format("IronSource Banner Destroy ad with instance ID: %s", otherInstanceInMap));
        IronSource.destroyISDemandOnlyBanner(otherInstanceInMap);
        removeFromAvailableInstances(otherInstanceInMap);
      }
    }
  }

  public void loadAd() {
    if (!isParamsValid()) {
      return;
    }

    ISBannerSize bannerSize = IronSourceAdapterUtils.getISBannerSizeFromGoogleAdSize(context, adSize);
    Activity activity = (Activity) context;
    availableBannerInstances.put(instanceID, this);
    ironSourceAdView = new FrameLayout(context);
    ironSourceBannerLayout = IronSource.createBannerForDemandOnly(activity, bannerSize);
    ironSourceBannerLayout.setBannerDemandOnlyListener(ironSourceBannerListener);
    Log.d(TAG, String.format("Loading IronSource banner ad with instance ID: %s", instanceID));
    IronSource.loadISDemandOnlyBanner(activity, ironSourceBannerLayout, instanceID);
  }

  /** Checks if the parameters for loading this instance are valid. */
  private boolean isParamsValid() {
    // Check that the context is an Activity and that the instance ID is valid..
    AdError loadError = IronSourceAdapterUtils.validateIronSourceAdLoadParams(context, instanceID);
    if (loadError != null) {
      onAdFailedToLoad(loadError);
      return false;
    }

    // Check that an Ad for this instance ID is not already loading.
    if (!IronSourceAdapterUtils.canLoadIronSourceAdInstance(instanceID, availableBannerInstances)) {
      loadError =
          new AdError(
              ERROR_AD_ALREADY_LOADED,
              "An IronSource banner is already loaded for instance ID: " + instanceID,
              ERROR_DOMAIN);
      onAdFailedToLoad(loadError);
      return false;
    }

    ISBannerSize bannerSize = IronSourceAdapterUtils.getISBannerSizeFromGoogleAdSize(context, adSize);
    if (bannerSize == null) {
      AdError sizeError =
          new AdError(
              ERROR_BANNER_SIZE_MISMATCH,
              "There is no matching IronSource banner ad size for Google ad size: %s" + adSize,
              ERROR_DOMAIN);
      Log.e(TAG, sizeError.toString());
      onAdFailedToLoad(sizeError);
      return false;
    }

    return true;
  }

  @NonNull
  @Override
  public View getView() {
    return ironSourceAdView;
  }

  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.w(TAG, loadError.getMessage());
    if (adLoadCallback != null) {
      adLoadCallback.onFailure(loadError);
    }
  }
}
