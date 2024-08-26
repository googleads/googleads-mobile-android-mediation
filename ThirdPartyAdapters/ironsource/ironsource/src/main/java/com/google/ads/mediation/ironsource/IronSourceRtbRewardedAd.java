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
import static com.google.ads.mediation.ironsource.IronSourceConstants.WATERMARK;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_CALL_SHOW_BEFORE_LOADED_SUCCESS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.unity3d.ironsourceads.rewarded.RewardedAd;
import com.unity3d.ironsourceads.rewarded.RewardedAdListener;
import com.unity3d.ironsourceads.rewarded.RewardedAdLoader;
import com.unity3d.ironsourceads.rewarded.RewardedAdLoaderListener;
import com.unity3d.ironsourceads.rewarded.RewardedAdRequest;

public class IronSourceRtbRewardedAd
    implements MediationRewardedAd, RewardedAdLoaderListener, RewardedAdListener {

  @VisibleForTesting private MediationRewardedAdCallback mediationRewardedAdCallback;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  private final Context context;

  private final String instanceID;

  private final String bidToken;

  private RewardedAd ad = null;

  private final String watermark;

  public IronSourceRtbRewardedAd(
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

  public void loadRtbAd() {
    if (TextUtils.isEmpty(instanceID)) {
      AdError loadError =
          IronSourceAdapterUtils.buildAdError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid instance ID.");
      this.mediationAdLoadCallback.onFailure(loadError);
    }

    Bundle watermarkBundle = new Bundle();
    watermarkBundle.putString(WATERMARK, this.watermark);

    RewardedAdRequest adRequest =
        new RewardedAdRequest.Builder(instanceID, bidToken)
            .withExtraParams(watermarkBundle)
            .build();

    RewardedAdLoader.loadAd(adRequest, this);
  }

  @Override
  public void showAd(@NonNull Context context) {
    Log.d(
        TAG, String.format("Showing IronSource rewarded ad for instance ID: %s", this.instanceID));
    if (this.ad == null) {
      AdError contextError =
          IronSourceAdapterUtils.buildAdError(ERROR_CALL_SHOW_BEFORE_LOADED_SUCCESS, "ad is null");
      this.reportAdFailedToShow(contextError);
    }

    try {

      Activity activity = (Activity) context;
      this.ad.setListener(this);
      this.ad.show(activity);
    } catch (ClassCastException e) {
      AdError contextError =
          IronSourceAdapterUtils.buildAdError(
              ERROR_REQUIRES_ACTIVITY_CONTEXT,
              "IronSource requires an Activity context to load ads.");
      this.reportAdFailedToShow(contextError);
    }
  }

  private void reportAdFailedToShow(@NonNull AdError showError) {
    Log.w(TAG, showError.toString());
    if (mediationRewardedAdCallback != null) {
      this.mediationRewardedAdCallback.onAdFailedToShow(showError);
    }
  }

  @Override
  public void onRewardedAdClicked(@NonNull RewardedAd rewardedAd) {
    if (this.mediationRewardedAdCallback == null) {
      return;
    }
    this.mediationRewardedAdCallback.reportAdClicked();
  }

  @Override
  public void onRewardedAdLoadFailed(@NonNull IronSourceError ironSourceError) {
    AdError adError =
        IronSourceAdapterUtils.buildAdError(
            ironSourceError.getErrorCode(), ironSourceError.getErrorMessage());
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(adError);
    }
  }

  @Override
  public void onRewardedAdLoaded(@NonNull RewardedAd rewardedAd) {
    this.ad = rewardedAd;
    this.mediationRewardedAdCallback = this.mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onRewardedAdDismissed(@NonNull RewardedAd rewardedAd) {
    if (this.mediationRewardedAdCallback == null) {
      return;
    }
    this.mediationRewardedAdCallback.onAdClosed();
  }

  @Override
  public void onRewardedAdFailedToShow(
      @NonNull RewardedAd rewardedAd, @NonNull IronSourceError ironSourceError) {
    AdError adError =
        IronSourceAdapterUtils.buildAdError(
            ironSourceError.getErrorCode(), ironSourceError.getErrorMessage());
    this.reportAdFailedToShow(adError);
  }

  @Override
  public void onRewardedAdShown(@NonNull RewardedAd rewardedAd) {
    if (this.mediationRewardedAdCallback == null) {
      return;
    }
    this.mediationRewardedAdCallback.onAdOpened();
    this.mediationRewardedAdCallback.onVideoStart();
    this.mediationRewardedAdCallback.reportAdImpression();
  }

  @Override
  public void onUserEarnedReward(@NonNull RewardedAd rewardedAd) {
    if (this.mediationRewardedAdCallback == null) {
      return;
    }
    final IronSourceRewardItem ironSourceRewardItem = new IronSourceRewardItem();
    this.mediationRewardedAdCallback.onVideoComplete();
    this.mediationRewardedAdCallback.onUserEarnedReward(ironSourceRewardItem);
  }
}
