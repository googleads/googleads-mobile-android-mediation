// Copyright 2024 Google LLC
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

/**
 * Used to load ironSource RTB rewarded ads and mediate callbacks between Google Mobile Ads SDK and
 * ironSource SDK.
 */
public class IronSourceRtbRewardedAd
    implements MediationRewardedAd, RewardedAdLoaderListener, RewardedAdListener {

  @VisibleForTesting private MediationRewardedAdCallback mediationRewardedAdCallback;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  private RewardedAd ad = null;

  public IronSourceRtbRewardedAd(
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationRewardedAdLoadCallback) {
    mediationAdLoadCallback = mediationRewardedAdLoadCallback;
  }

  /** Attempts to load an ironSource @{link RewardedAd} using a Bid token. */
  public void loadRtbAd(@NonNull MediationRewardedAdConfiguration adConfiguration) {
    Bundle serverParameters = adConfiguration.getServerParameters();
    String instanceID = serverParameters.getString(KEY_INSTANCE_ID, "");
    if (TextUtils.isEmpty(instanceID)) {
      AdError loadError =
          IronSourceAdapterUtils.buildAdErrorAdapterDomain(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid instance ID.");
      mediationAdLoadCallback.onFailure(loadError);
      return;
    }

    String watermark = adConfiguration.getWatermark();
    Bundle watermarkBundle = new Bundle();
    watermarkBundle.putString(WATERMARK, watermark);

    String bidToken = adConfiguration.getBidResponse();
    RewardedAdRequest adRequest =
        new RewardedAdRequest.Builder(instanceID, bidToken)
            .withExtraParams(watermarkBundle)
            .build();

    RewardedAdLoader.loadAd(adRequest, this);
  }

  /** Attempts to show the loaded @{link RewardedAd} bidding ad. */
  @Override
  public void showAd(@NonNull Context context) {
    if (ad == null) {
      AdError contextError =
          IronSourceAdapterUtils.buildAdErrorAdapterDomain(
              ERROR_CALL_SHOW_BEFORE_LOADED_SUCCESS, "ad is null");
      reportAdFailedToShow(contextError);
      return;
    }

    try {
      Log.d(TAG, "Showing IronSource rewarded ad");
      Activity activity = (Activity) context;
      ad.setListener(this);
      ad.show(activity);
    } catch (ClassCastException e) {
      AdError contextError =
          IronSourceAdapterUtils.buildAdErrorAdapterDomain(
              ERROR_REQUIRES_ACTIVITY_CONTEXT,
              "IronSource requires an Activity context to load ads.");
      reportAdFailedToShow(contextError);
    }
  }

  private void reportAdFailedToShow(@NonNull AdError showError) {
    Log.w(TAG, showError.toString());
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdFailedToShow(showError);
    }
  }

  @Override
  public void onRewardedAdClicked(@NonNull RewardedAd rewardedAd) {
    if (mediationRewardedAdCallback == null) {
      return;
    }
    mediationRewardedAdCallback.reportAdClicked();
  }

  @Override
  public void onRewardedAdLoadFailed(@NonNull IronSourceError ironSourceError) {
    AdError adError =
        IronSourceAdapterUtils.buildAdErrorIronSourceDomain(
            ironSourceError.getErrorCode(), ironSourceError.getErrorMessage());
    mediationAdLoadCallback.onFailure(adError);
  }

  @Override
  public void onRewardedAdLoaded(@NonNull RewardedAd rewardedAd) {
    ad = rewardedAd;
    mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onRewardedAdDismissed(@NonNull RewardedAd rewardedAd) {
    if (mediationRewardedAdCallback == null) {
      return;
    }
    mediationRewardedAdCallback.onAdClosed();
  }

  @Override
  public void onRewardedAdFailedToShow(
      @NonNull RewardedAd rewardedAd, @NonNull IronSourceError ironSourceError) {
    AdError adError =
        IronSourceAdapterUtils.buildAdErrorIronSourceDomain(
            ironSourceError.getErrorCode(), ironSourceError.getErrorMessage());
    reportAdFailedToShow(adError);
  }

  @Override
  public void onRewardedAdShown(@NonNull RewardedAd rewardedAd) {
    if (mediationRewardedAdCallback == null) {
      return;
    }
    mediationRewardedAdCallback.onAdOpened();
    mediationRewardedAdCallback.onVideoStart();
    mediationRewardedAdCallback.reportAdImpression();
  }

  @Override
  public void onUserEarnedReward(@NonNull RewardedAd rewardedAd) {
    if (mediationRewardedAdCallback == null) {
      return;
    }
    mediationRewardedAdCallback.onVideoComplete();
    mediationRewardedAdCallback.onUserEarnedReward();
  }
}
